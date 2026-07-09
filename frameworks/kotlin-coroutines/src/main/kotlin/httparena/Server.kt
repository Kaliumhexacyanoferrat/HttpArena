package httparena

import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.*

private const val PORT = 8080
private const val BUF = 16384
private val RESPONSE_PREFIX = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: ".encodeToByteArray()
private val CRLF2 = "\r\n\r\n".encodeToByteArray()

fun main() {
    val n = Runtime.getRuntime().availableProcessors()
    val workers = Array(n) { Thread.ofPlatform().name("w-$it").start(::runWorker) }
    workers.forEach { it.join() }
}

fun runWorker() {
    val sel = Selector.open()
    ServerSocketChannel.open().apply {
        setOption(StandardSocketOptions.SO_REUSEADDR, true)
        setOption(StandardSocketOptions.SO_REUSEPORT, true)
        bind(InetSocketAddress(PORT))
        configureBlocking(false)
        register(sel, SelectionKey.OP_ACCEPT)
    }
    while (true) {
        sel.select()
        val iter = sel.selectedKeys().iterator()
        while (iter.hasNext()) {
            val key = iter.next(); iter.remove()
            if (!key.isValid) continue
            when {
                key.isAcceptable -> onAccept(key, sel)
                key.isReadable   -> onRead(key)
            }
        }
    }
}

fun onAccept(key: SelectionKey, sel: Selector) {
    val ch = (key.channel() as ServerSocketChannel).accept() ?: return
    ch.setOption(StandardSocketOptions.TCP_NODELAY, true)
    ch.configureBlocking(false)
    ch.register(sel, SelectionKey.OP_READ, Conn(ch))
}

fun onRead(key: SelectionKey) {
    val c = key.attachment() as Conn
    val n = c.ch.read(ByteBuffer.wrap(c.buf, c.filled, c.buf.size - c.filled))
    if (n < 0) { c.ch.close(); key.cancel(); return }
    c.filled += n
    while (process(c)) {
        if (c.close) { c.ch.close(); key.cancel(); return }
    }
}

fun process(c: Conn): Boolean {
    if (!c.headersParsed) {
        val he = headerEnd(c.buf, c.filled)
        if (he < 0) return false
        parseHeaders(c, he)
        c.headersParsed = true
        c.bodyStart = he
    }

    var bodyVal = 0L
    if (c.isPost) {
        when {
            c.chunked -> {
                val r = decodeChunked(c.buf, c.bodyStart, c.filled) ?: return false
                bodyVal = r[0]; compact(c, c.bodyStart + r[1].toInt())
            }
            c.contentLen > 0 -> {
                if (c.filled - c.bodyStart < c.contentLen) return false
                bodyVal = parseI64(c.buf, c.bodyStart, c.contentLen)
                compact(c, c.bodyStart + c.contentLen)
            }
            else -> compact(c, c.bodyStart)
        }
    } else {
        compact(c, c.bodyStart)
    }

    sendResponse(c, c.querySum + bodyVal)
    c.headersParsed = false; c.isPost = false; c.chunked = false; c.contentLen = 0; c.querySum = 0
    return true
}

// ── Connection state ──────────────────────────────────────────────────────────

class Conn(val ch: SocketChannel) {
    val buf = ByteArray(BUF)
    var filled = 0
    var headersParsed = false
    var isPost = false
    var chunked = false
    var close = false
    var contentLen = 0
    var querySum = 0L
    var bodyStart = 0
}

// ── HTTP ──────────────────────────────────────────────────────────────────────

fun parseHeaders(c: Conn, he: Int) {
    val b = c.buf
    val rlEnd = indexOf(b, 0, he, '\r').let { if (it < 0) he else it }
    val sp1 = indexOf(b, 0, rlEnd, ' ')
    if (sp1 < 0) return
    c.isPost = sp1 == 4 && b[0] == 'P'.code.toByte() && b[1] == 'O'.code.toByte() &&
        b[2] == 'S'.code.toByte() && b[3] == 'T'.code.toByte()
    val sp2 = indexOf(b, sp1 + 1, rlEnd, ' ').let { if (it < 0) rlEnd else it }
    val qm = indexOf(b, sp1 + 1, sp2, '?')
    if (qm >= 0) c.querySum = parseQuerySum(b, qm + 1, sp2)

    var pos = rlEnd + 2
    while (pos < he - 2) {
        val nl = indexOf(b, pos, he, '\r').let { if (it < 0) he else it }
        val colon = indexOf(b, pos, nl, ':')
        if (colon >= 0) {
            var vs = colon + 1; while (vs < nl && b[vs] == ' '.code.toByte()) vs++
            var ve = nl; while (ve > vs && (b[ve-1] == ' '.code.toByte() || b[ve-1] == '\r'.code.toByte())) ve--
            when {
                eqCI(b, pos, colon, "content-length")    -> c.contentLen = parseI64(b, vs, ve - vs).toInt()
                eqCI(b, pos, colon, "transfer-encoding") && containsCI(b, vs, ve, "chunked") -> c.chunked = true
                eqCI(b, pos, colon, "connection")        && eqCI(b, vs, ve, "close")         -> c.close = true
            }
        }
        if (nl + 2 >= he) break
        pos = nl + 2
    }
}

fun headerEnd(buf: ByteArray, len: Int): Int {
    for (i in 0..len - 4)
        if (buf[i] == '\r'.code.toByte() && buf[i+1] == '\n'.code.toByte() &&
            buf[i+2] == '\r'.code.toByte() && buf[i+3] == '\n'.code.toByte()) return i + 4
    return -1
}

// Returns LongArray(value, consumed) or null if incomplete.
fun decodeChunked(buf: ByteArray, start: Int, len: Int): LongArray? {
    var pos = start; var total = 0L
    while (pos < len) {
        val nl = indexOf(buf, pos, len, '\r')
        if (nl < 0 || nl + 1 >= len) return null
        val size = parseHex(buf, pos, nl).toInt()
        pos = nl + 2
        if (size == 0) { if (pos + 2 > len) return null; return longArrayOf(total, (pos + 2 - start).toLong()) }
        if (pos + size + 2 > len) return null
        total += parseI64(buf, pos, size); pos += size + 2
    }
    return null
}

fun sendResponse(c: Conn, value: Long) {
    val body = value.toString().encodeToByteArray()
    val clen = body.size.toString().encodeToByteArray()
    val resp = ByteArray(RESPONSE_PREFIX.size + clen.size + 4 + body.size)
    var pos = 0
    RESPONSE_PREFIX.copyInto(resp, pos); pos += RESPONSE_PREFIX.size
    clen.copyInto(resp, pos); pos += clen.size
    CRLF2.copyInto(resp, pos); pos += 4
    body.copyInto(resp, pos)
    val bb = ByteBuffer.wrap(resp)
    while (bb.hasRemaining()) c.ch.write(bb)
}

fun compact(c: Conn, consumed: Int) {
    val rem = c.filled - consumed
    if (rem > 0) c.buf.copyInto(c.buf, 0, consumed, c.filled)
    c.filled = rem
}

// ── Byte helpers ──────────────────────────────────────────────────────────────

fun parseQuerySum(b: ByteArray, start: Int, end: Int): Long {
    var sum = 0L; var pos = start
    while (pos < end) {
        val amp = indexOf(b, pos, end, '&').let { if (it < 0) end else it }
        val eq = indexOf(b, pos, amp, '=')
        if (eq >= 0) sum += parseI64(b, eq + 1, amp - eq - 1)
        pos = if (amp < end) amp + 1 else end
    }
    return sum
}

fun parseI64(b: ByteArray, off: Int, len: Int): Long {
    var s = off; var e = off + len
    while (s < e && isWS(b[s])) s++
    while (e > s && isWS(b[e-1])) e--
    val neg = s < e && b[s] == '-'.code.toByte(); if (neg) s++
    var v = 0L
    for (i in s until e) { val d = b[i] - '0'.code; if (d < 0 || d > 9) break; v = v * 10 + d }
    return if (neg) -v else v
}

fun parseHex(b: ByteArray, off: Int, end: Int): Long {
    var v = 0L
    for (i in off until end) {
        val c = b[i].toInt().toChar()
        val d = when { c in '0'..'9' -> c.code - '0'.code; c in 'a'..'f' -> c.code - 'a'.code + 10; c in 'A'..'F' -> c.code - 'A'.code + 10; else -> return v }
        v = v * 16 + d
    }
    return v
}

fun indexOf(b: ByteArray, from: Int, to: Int, ch: Char): Int {
    val c = ch.code.toByte(); for (i in from until to) if (b[i] == c) return i; return -1
}

fun eqCI(b: ByteArray, off: Int, end: Int, s: String): Boolean {
    if (end - off != s.length) return false
    for (i in s.indices) if (b[off + i].toInt().toChar().lowercaseChar() != s[i]) return false
    return true
}

fun containsCI(b: ByteArray, off: Int, end: Int, needle: String): Boolean {
    val n = needle.length
    outer@ for (i in off..end - n) {
        for (j in 0 until n) if (b[i + j].toInt().toChar().lowercaseChar() != needle[j]) continue@outer
        return true
    }
    return false
}

private fun isWS(b: Byte) = b == ' '.code.toByte() || b == '\t'.code.toByte() ||
    b == '\r'.code.toByte() || b == '\n'.code.toByte()
