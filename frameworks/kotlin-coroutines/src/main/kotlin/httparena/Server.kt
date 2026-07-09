package httparena

import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.*
import java.nio.channels.CompletionHandler as NioCompletionHandler
import java.util.concurrent.Executors
import kotlin.coroutines.*

fun main() {
    val threads = Runtime.getRuntime().availableProcessors()
    val pool = Executors.newFixedThreadPool(threads) { r -> Thread(r).also { it.isDaemon = false } }
    val group = AsynchronousChannelGroup.withThreadPool(pool)
    val dispatcher = pool.asCoroutineDispatcher()

    runBlocking(dispatcher) {
        repeat(threads) {
            launch {
                val server = AsynchronousServerSocketChannel.open(group).apply {
                    setOption(StandardSocketOptions.SO_REUSEADDR, true)
                    setOption(StandardSocketOptions.SO_REUSEPORT, true)
                    bind(InetSocketAddress(8080))
                }
                while (true) {
                    val ch = server.awaitAccept()
                    launch { ch.handleConnection() }
                }
            }
        }
    }
}

// ── NIO2 suspend wrappers ─────────────────────────────────────────────────────

suspend fun AsynchronousServerSocketChannel.awaitAccept(): AsynchronousSocketChannel =
    suspendCancellableCoroutine { cont ->
        accept(null, object : NioCompletionHandler<AsynchronousSocketChannel, Nothing?> {
            override fun completed(ch: AsynchronousSocketChannel, a: Nothing?) = cont.resume(ch)
            override fun failed(e: Throwable, a: Nothing?) = cont.resumeWithException(e)
        })
    }

suspend fun AsynchronousSocketChannel.awaitRead(buf: ByteBuffer): Int =
    suspendCancellableCoroutine { cont ->
        read(buf, null, object : NioCompletionHandler<Int, Nothing?> {
            override fun completed(n: Int, a: Nothing?) = cont.resume(n)
            override fun failed(e: Throwable, a: Nothing?) = cont.resumeWithException(e)
        })
    }

suspend fun AsynchronousSocketChannel.awaitWrite(buf: ByteBuffer) {
    while (buf.hasRemaining()) {
        suspendCancellableCoroutine<Int> { cont ->
            write(buf, null, object : NioCompletionHandler<Int, Nothing?> {
                override fun completed(n: Int, a: Nothing?) = cont.resume(n)
                override fun failed(e: Throwable, a: Nothing?) = cont.resumeWithException(e)
            })
        }
    }
}

// ── Connection handler ────────────────────────────────────────────────────────

suspend fun AsynchronousSocketChannel.handleConnection() {
    setOption(StandardSocketOptions.TCP_NODELAY, true)
    val buf = ByteArray(16384)
    var filled = 0
    try {
        while (true) {
            // Phase 1: read until headers are complete
            var he = headerEnd(buf, filled)
            while (he < 0) {
                val n = awaitRead(ByteBuffer.wrap(buf, filled, buf.size - filled))
                if (n < 0) return
                filled += n
                he = headerEnd(buf, filled)
            }

            val hdrs = parseHeaders(buf, he)

            // Phase 2: read body — returns LongArray(value, newFilled)
            val result = readBody(buf, he, filled, hdrs)
            val bodyVal = result[0]
            filled = result[1].toInt()

            // Phase 3: respond
            awaitWrite(buildResponse(hdrs.querySum + bodyVal))
            if (hdrs.close) return
        }
    } catch (_: Exception) {
    } finally {
        try { close() } catch (_: Exception) {}
    }
}

private suspend fun AsynchronousSocketChannel.readBody(
    buf: ByteArray,
    bodyStart: Int,
    initialFilled: Int,
    hdrs: Headers
): LongArray {
    var filled = initialFilled

    if (!hdrs.isPost) {
        buf.copyInto(buf, 0, bodyStart, filled)
        return longArrayOf(0L, (filled - bodyStart).toLong())
    }

    if (hdrs.chunked) {
        while (true) {
            val r = decodeChunked(buf, bodyStart, filled)
            if (r != null) return r
            val n = awaitRead(ByteBuffer.wrap(buf, filled, buf.size - filled))
            if (n < 0) return longArrayOf(0L, 0L)
            filled += n
        }
    }

    if (hdrs.contentLen > 0) {
        while (filled - bodyStart < hdrs.contentLen) {
            val n = awaitRead(ByteBuffer.wrap(buf, filled, buf.size - filled))
            if (n < 0) return longArrayOf(0L, 0L)
            filled += n
        }
        val v = parseI64(buf, bodyStart, hdrs.contentLen)
        val consumed = bodyStart + hdrs.contentLen
        buf.copyInto(buf, 0, consumed, filled)
        return longArrayOf(v, (filled - consumed).toLong())
    }

    buf.copyInto(buf, 0, bodyStart, filled)
    return longArrayOf(0L, (filled - bodyStart).toLong())
}

// ── HTTP parsing ──────────────────────────────────────────────────────────────

data class Headers(val isPost: Boolean, val querySum: Long, val contentLen: Int, val chunked: Boolean, val close: Boolean)

fun headerEnd(buf: ByteArray, len: Int): Int {
    for (i in 0..len - 4) {
        if (buf[i] == '\r'.code.toByte() && buf[i + 1] == '\n'.code.toByte() &&
            buf[i + 2] == '\r'.code.toByte() && buf[i + 3] == '\n'.code.toByte()
        ) return i + 4
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
            while (ve > vs && (buf[ve - 1] == ' '.code.toByte() || buf[ve - 1] == '\r'.code.toByte())) ve--

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

// Returns LongArray(value, newFilled) after compacting consumed bytes to front, or null if incomplete.
// Using LongArray avoids boxing both values (Pair<Long,Int> would box on JVM generics).
fun decodeChunked(buf: ByteArray, start: Int, len: Int): LongArray? {
    var pos = start
    var total = 0L
    while (pos < len) {
        val nl = indexOf(buf, pos, len, '\r')
        if (nl < 0 || nl + 1 >= len) return null
        val size = parseHex(buf, pos, nl).toInt()
        pos = nl + 2
        if (size == 0) {
            if (pos + 2 > len) return null
            val consumed = pos + 2
            buf.copyInto(buf, 0, consumed, len)
            return longArrayOf(total, (len - consumed).toLong())
        }
        if (pos + size + 2 > len) return null
        total += parseI64(buf, pos, size)
        pos += size + 2
    }
    return null
}

private val RESPONSE_PREFIX = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: ".encodeToByteArray()

fun buildResponse(value: Long): ByteBuffer {
    val body = value.toString().encodeToByteArray()
    val out = ByteBuffer.allocate(RESPONSE_PREFIX.size + 10 + 4 + body.size)
    out.put(RESPONSE_PREFIX)
    out.put(body.size.toString().encodeToByteArray())
    out.put("\r\n\r\n".encodeToByteArray())
    out.put(body)
    out.flip()
    return out
}

// ── Byte helpers ──────────────────────────────────────────────────────────────

fun parseQuerySum(b: ByteArray, start: Int, end: Int): Long {
    var sum = 0L
    var pos = start
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
    while (e > s && isWS(b[e - 1])) e--
    val neg = s < e && b[s] == '-'.code.toByte()
    if (neg) s++
    var v = 0L
    for (i in s until e) {
        val d = b[i] - '0'.code
        if (d < 0 || d > 9) break
        v = v * 10 + d
    }
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
    for (i in s.indices)
        if (b[off + i].toInt().toChar().lowercaseChar() != s[i]) return false
    return true
}

fun containsCI(b: ByteArray, off: Int, end: Int, needle: String): Boolean {
    val n = needle.length
    outer@ for (i in off..end - n) {
        for (j in 0 until n)
            if (b[i + j].toInt().toChar().lowercaseChar() != needle[j]) continue@outer
        return true
    }
    return false
}

@Suppress("NOTHING_TO_INLINE")
private inline fun isWS(b: Byte) = b == ' '.code.toByte() || b == '\t'.code.toByte() ||
    b == '\r'.code.toByte() || b == '\n'.code.toByte()
