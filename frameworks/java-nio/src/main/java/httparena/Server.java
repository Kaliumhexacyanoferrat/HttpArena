package httparena;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class Server {

    private static final int PORT = 8080;
    private static final int BUF  = 16384;

    public static void main(String[] args) throws InterruptedException {
        int n = Runtime.getRuntime().availableProcessors();
        Thread[] workers = new Thread[n];
        for (int i = 0; i < n; i++) {
            workers[i] = Thread.ofPlatform().name("w-" + i).start(Server::runWorker);
        }
        for (Thread t : workers) t.join();
    }

    static void runWorker() {
        try {
            Selector sel = Selector.open();

            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            ssc.setOption(StandardSocketOptions.SO_REUSEPORT, true);
            ssc.bind(new InetSocketAddress(PORT));
            ssc.configureBlocking(false);
            ssc.register(sel, SelectionKey.OP_ACCEPT);

            while (true) {
                sel.select();
                Iterator<SelectionKey> it = sel.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid()) continue;
                    if (key.isAcceptable()) onAccept(key, sel);
                    else if (key.isReadable()) onRead(key);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void onAccept(SelectionKey key, Selector sel) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel ch = ssc.accept();
        if (ch == null) return;
        ch.setOption(StandardSocketOptions.TCP_NODELAY, true);
        ch.configureBlocking(false);
        ch.register(sel, SelectionKey.OP_READ, new Conn(ch));
    }

    static void onRead(SelectionKey key) throws IOException {
        Conn conn = (Conn) key.attachment();
        ByteBuffer bb = ByteBuffer.wrap(conn.buf, conn.filled, conn.buf.length - conn.filled);
        int n = conn.ch.read(bb);
        if (n < 0) { conn.ch.close(); key.cancel(); return; }
        conn.filled += n;
        while (process(conn)) {
            if (conn.close) { conn.ch.close(); key.cancel(); return; }
        }
    }

    // Returns true if a complete request was processed (more may be buffered).
    static boolean process(Conn c) throws IOException {
        if (!c.headersParsed) {
            int he = headerEnd(c.buf, c.filled);
            if (he < 0) return false;
            parseHeaders(c, he);
            c.headersParsed = true;
            c.bodyStart = he;
        }

        long bodyVal = 0;

        if (c.isPost) {
            if (c.chunked) {
                int[] out = new int[1];
                Long v = decodeChunked(c.buf, c.bodyStart, c.filled, out);
                if (v == null) return false;
                bodyVal = v;
                compact(c, c.bodyStart + out[0]);
            } else if (c.contentLen > 0) {
                if (c.filled - c.bodyStart < c.contentLen) return false;
                bodyVal = parseI64(c.buf, c.bodyStart, c.contentLen);
                compact(c, c.bodyStart + c.contentLen);
            } else {
                compact(c, c.bodyStart);
            }
        } else {
            compact(c, c.bodyStart);
        }

        sendResponse(c, c.querySum + bodyVal);

        c.headersParsed = false;
        c.isPost = false;
        c.chunked = false;
        c.contentLen = 0;
        c.querySum = 0;
        return true;
    }

    // ── HTTP parsing ─────────────────────────────────────────────────────────

    static int headerEnd(byte[] buf, int len) {
        for (int i = 0; i <= len - 4; i++) {
            if (buf[i] == '\r' && buf[i+1] == '\n' && buf[i+2] == '\r' && buf[i+3] == '\n')
                return i + 4;
        }
        return -1;
    }

    static void parseHeaders(Conn c, int headerEnd) {
        byte[] b = c.buf;

        // Request line: find first \r\n
        int rlEnd = indexOf(b, 0, headerEnd, '\r');
        if (rlEnd < 0) rlEnd = headerEnd;

        // Method
        int sp1 = indexOf(b, 0, rlEnd, ' ');
        if (sp1 < 0) return;
        c.isPost = sp1 == 4 && b[0]=='P' && b[1]=='O' && b[2]=='S' && b[3]=='T';

        // Target (path?query)
        int sp2 = indexOf(b, sp1 + 1, rlEnd, ' ');
        if (sp2 < 0) sp2 = rlEnd;
        int qm = indexOf(b, sp1 + 1, sp2, '?');
        if (qm >= 0) c.querySum = parseQuerySum(b, qm + 1, sp2);

        // Header lines
        int pos = rlEnd + 2;
        while (pos < headerEnd - 2) {
            int nl = indexOf(b, pos, headerEnd, '\r');
            if (nl < 0) nl = headerEnd;
            int colon = indexOf(b, pos, nl, ':');
            if (colon >= 0) {
                int vs = colon + 1;
                while (vs < nl && b[vs] == ' ') vs++;
                int ve = nl;
                while (ve > vs && (b[ve-1] == ' ' || b[ve-1] == '\r')) ve--;

                if (eqCI(b, pos, colon, "content-length")) {
                    c.contentLen = (int) parseI64(b, vs, ve - vs);
                } else if (eqCI(b, pos, colon, "transfer-encoding") && containsCI(b, vs, ve, "chunked")) {
                    c.chunked = true;
                } else if (eqCI(b, pos, colon, "connection") && eqCI(b, vs, ve, "close")) {
                    c.close = true;
                }
            }
            if (nl + 2 >= headerEnd) break;
            pos = nl + 2;
        }
    }

    // Decode first chunk from buf[start..len) and return its integer value.
    // out[0] = total bytes consumed (all chunks + terminators).
    static Long decodeChunked(byte[] buf, int start, int len, int[] out) {
        int pos = start;
        long total = 0;

        while (pos < len) {
            // Find \r\n for chunk size line
            int nl = indexOf(buf, pos, len, '\r');
            if (nl < 0 || nl + 1 >= len) return null;
            int size = (int) parseHex(buf, pos, nl);
            pos = nl + 2;

            if (size == 0) {
                // Final chunk: consume trailing \r\n
                if (pos + 2 > len) return null;
                out[0] = (pos + 2) - start;
                return total;
            }

            if (pos + size + 2 > len) return null;
            total += parseI64(buf, pos, size);
            pos += size + 2; // skip chunk data + \r\n
        }
        return null;
    }

    static void sendResponse(Conn c, long value) throws IOException {
        byte[] body  = Long.toString(value).getBytes();
        byte[] clen  = Integer.toString(body.length).getBytes();
        byte[] resp  = new byte[59 + clen.length + 4 + body.length];
        int pos = 0;
        pos = put(resp, pos, "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: ");
        pos = put(resp, pos, clen);
        pos = put(resp, pos, "\r\n\r\n");
        System.arraycopy(body, 0, resp, pos, body.length);

        ByteBuffer bb = ByteBuffer.wrap(resp);
        while (bb.hasRemaining()) c.ch.write(bb);
    }

    // ── Byte helpers ──────────────────────────────────────────────────────────

    static long parseQuerySum(byte[] b, int start, int end) {
        long sum = 0;
        int pos = start;
        while (pos < end) {
            int amp = indexOf(b, pos, end, '&');
            int seg = amp >= 0 ? amp : end;
            int eq = indexOf(b, pos, seg, '=');
            if (eq >= 0) sum += parseI64(b, eq + 1, seg - eq - 1);
            pos = (amp >= 0) ? amp + 1 : end;
        }
        return sum;
    }

    static long parseI64(byte[] b, int off, int len) {
        int end = off + len;
        // trim
        while (off < end && (b[off] == ' ' || b[off] == '\t' || b[off] == '\r' || b[off] == '\n')) off++;
        while (end > off && (b[end-1] == ' ' || b[end-1] == '\t' || b[end-1] == '\r' || b[end-1] == '\n')) end--;
        boolean neg = off < end && b[off] == '-';
        if (neg) off++;
        long v = 0;
        for (int i = off; i < end; i++) {
            int d = b[i] - '0';
            if (d < 0 || d > 9) break;
            v = v * 10 + d;
        }
        return neg ? -v : v;
    }

    static long parseHex(byte[] b, int off, int end) {
        long v = 0;
        for (int i = off; i < end; i++) {
            byte c = b[i];
            int d;
            if (c >= '0' && c <= '9') d = c - '0';
            else if (c >= 'a' && c <= 'f') d = c - 'a' + 10;
            else if (c >= 'A' && c <= 'F') d = c - 'A' + 10;
            else break;
            v = v * 16 + d;
        }
        return v;
    }

    static int indexOf(byte[] b, int from, int to, char ch) {
        for (int i = from; i < to; i++) if (b[i] == ch) return i;
        return -1;
    }

    static boolean eqCI(byte[] b, int off, int end, String s) {
        if (end - off != s.length()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            byte x = b[off + i];
            if (Character.toLowerCase(x) != Character.toLowerCase(c)) return false;
        }
        return true;
    }

    static boolean containsCI(byte[] b, int off, int end, String needle) {
        int nlen = needle.length();
        outer:
        for (int i = off; i <= end - nlen; i++) {
            for (int j = 0; j < nlen; j++) {
                if (Character.toLowerCase(b[i+j]) != Character.toLowerCase(needle.charAt(j)))
                    continue outer;
            }
            return true;
        }
        return false;
    }

    static void compact(Conn c, int consumed) {
        int remaining = c.filled - consumed;
        if (remaining > 0) System.arraycopy(c.buf, consumed, c.buf, 0, remaining);
        c.filled = remaining;
    }

    static int put(byte[] dst, int pos, String s) {
        for (int i = 0; i < s.length(); i++) dst[pos + i] = (byte) s.charAt(i);
        return pos + s.length();
    }

    static int put(byte[] dst, int pos, byte[] src) {
        System.arraycopy(src, 0, dst, pos, src.length);
        return pos + src.length;
    }

    // ── Connection state ──────────────────────────────────────────────────────

    static final class Conn {
        final SocketChannel ch;
        final byte[] buf = new byte[BUF];
        int filled;

        boolean headersParsed;
        boolean isPost;
        boolean chunked;
        boolean close;
        int contentLen;
        long querySum;
        int bodyStart;

        Conn(SocketChannel ch) { this.ch = ch; }
    }
}
