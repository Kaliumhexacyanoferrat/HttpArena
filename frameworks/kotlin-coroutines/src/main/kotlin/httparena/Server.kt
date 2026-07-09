package httparena

import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.StandardSocketOptions

private val RESPONSE_PREFIX = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: ".encodeToByteArray()
private val CRLF2 = "\r\n\r\n".encodeToByteArray()

fun main() {
    val threads = Runtime.getRuntime().availableProcessors()
    repeat(threads - 1) { Thread.ofPlatform().daemon(true).start(::acceptLoop) }
    acceptLoop()
}

fun acceptLoop() {
    val server = ServerSocket()
    server.setOption(StandardSocketOptions.SO_REUSEADDR, true)
    server.setOption(StandardSocketOptions.SO_REUSEPORT, true)
    server.bind(InetSocketAddress(8080), 4096)
    while (true) {
        val client = server.accept()
        Thread.ofVirtual().start { handleConnection(client) }
    }
}

fun handleConnection(socket: Socket) {
    socket.tcpNoDelay = true
    val inp = socket.getInputStream()
    val out = socket.getOutputStream().buffered(4096)
    val buf = ByteArray(16384)
    var filled = 0
    try {
        while (true) {
            var he = headerEnd(buf, filled)
            while (he < 0) {
                val n = inp.read(buf, filled, buf.size - filled)
                if (n < 0) return
                filled += n
                he = headerEnd(buf, filled)
            }

            val hdrs = parseHeaders(buf, he)
            var bodyVal = 0L

            if (hdrs.isPost) {
                when {
                    hdrs.chunked -> {
                        val r = readChunked(buf, he, filled, inp) ?: return
                        bodyVal = r[0]; filled = r[1].toInt()
                    }
                    hdrs.contentLen > 0 -> {
                        while (filled - he < hdrs.contentLen) {
                            val n = inp.read(buf, filled, buf.size - filled)
                            if (n < 0) return
                            filled += n
                        }
                        bodyVal = parseI64(buf, he, hdrs.contentLen)
                        val consumed = he + hdrs.contentLen
                        buf.copyInto(buf, 0, consumed, filled)
                        filled -= consumed
                    }
                    else -> { buf.copyInto(buf, 0, he, filled); filled -= he }
                }
            } else {
                buf.copyInto(buf, 0, he, filled); filled -= he
            }

            writeResponse(out, hdrs.querySum + bodyVal)
            if (hdrs.close) return
        }
    } catch (_: Exception) {
    } finally {
        runCatching { socket.close() }
    }
}

private fun writeResponse(out: OutputStream, value: Long) {
    val body = value.toString().encodeToByteArray()
    out.write(RESPONSE_PREFIX)
    out.write(body.size.toString().encodeToByteArray())
    out.write(CRLF2)
    out.write(body)
    out.flush()
}

// ── HTTP parsing ──────────────────────────────────────────────────────────────

data class Headers(val isPost: Boolean, val querySum: Long, val contentLen: Int, val chunked: Boolean, val close: Boolean)

fun headerEnd(buf: ByteArray, len: Int): Int {
    for (i in 0..len - 4) {
        if (buf[i] == '\r'.code.toByte() && buf[i+1] == '\n'.code.toByte() &&
            buf[i+2] == '\r'.code.toByte() && buf[i+3] == '\n'.code.toByte()) return i + 4
    }
    return -1
}

fun parseHeaders(buf: ByteArray, headerEnd: Int): Headers {
    val rlEnd = indexOf(buf, 0, headerEnd, '\r').let { if (it < 0) headerEnd else it }
    val sp1 = indexOf(buf, 0, rlEnd, ' ')
    if (sp1 < 0) return Headers(false, 0, 0, false, false)

    val isPost = sp1 == 4 &&
        buf[0] == 'P'.code.toByte() && buf[1] == 'O'.code.toByte() &&
        buf[2] == 'S'.code.toByte() && buf[3] == 'T'.code.toByte()

    val sp2 = indexOf(buf, sp1 + 1, rlEnd, ' ').let { if (it < 0) rlEnd else it }
    val qm = indexOf(buf, sp1 + 1, sp2, '?')
    val querySum = if (qm >= 0) parseQuerySum(buf, qm + 1, sp2) else 0L

    var contentLen = 0
    var chunked = false
    var close = false

    var pos = rlEnd + 2
    while (pos < headerEnd - 2) {
        val nl = indexOf(buf, pos, headerEnd, '\r').let { if (it < 0) headerEnd else it }
        val colon = indexOf(buf, pos, nl, ':')
        if (colon >= 0) {
            var vs = colon + 1
            while (vs < nl && buf[vs] == ' '.code.toByte()) vs++
            var ve = nl
            while (ve > vs && (buf[ve-1] == ' '.code.toByte() || buf[ve-1] == '\r'.code.toByte())) ve--

            when {
                eqCI(buf, pos, colon, "content-length") -> contentLen = parseI64(buf, vs, ve - vs).toInt()
                eqCI(buf, pos, colon, "transfer-encoding") && containsCI(buf, vs, ve, "chunked") -> chunked = true
                eqCI(buf, pos, colon, "connection") && eqCI(buf, vs, ve, "close") -> close = true
            }
        }
        if (nl + 2 >= headerEnd) break
        pos = nl + 2
    }
    return Headers(isPost, querySum, contentLen, chunked, close)
}

// Returns LongArray(value, newFilled) or null on EOF.
fun readChunked(buf: ByteArray, start: Int, initialFilled: Int, inp: InputStream): LongArray? {
    var pos = start
    var filled = initialFilled
    var total = 0L

    while (true) {
        // Ensure we have a complete chunk size line (\r\n)
        while (true) {
            val nl = indexOf(buf, pos, filled, '\r')
            if (nl >= 0 && nl + 1 < filled) {
                val size = parseHex(buf, pos, nl).toInt()
                pos = nl + 2
                if (size == 0) {
                    while (filled - pos < 2) {
                        val n = inp.read(buf, filled, buf.size - filled); if (n < 0) return null; filled += n
                    }
                    val consumed = pos + 2
                    buf.copyInto(buf, 0, consumed, filled)
                    return longArrayOf(total, (filled - consumed).toLong())
                }
                while (filled - pos < size + 2) {
                    val n = inp.read(buf, filled, buf.size - filled); if (n < 0) return null; filled += n
                }
                total += parseI64(buf, pos, size)
                pos += size + 2
                break
            }
            val n = inp.read(buf, filled, buf.size - filled); if (n < 0) return null; filled += n
        }
    }
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
        val d = when {
            c in '0'..'9' -> c.code - '0'.code
            c in 'a'..'f' -> c.code - 'a'.code + 10
            c in 'A'..'F' -> c.code - 'A'.code + 10
            else -> return v
        }
        v = v * 16 + d
    }
    return v
}

fun indexOf(b: ByteArray, from: Int, to: Int, ch: Char): Int {
    val c = ch.code.toByte()
    for (i in from until to) if (b[i] == c) return i
    return -1
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
