using System.Buffers;
using System.Buffers.Text;
using System.IO.Pipelines;
using System.Net;
using System.Net.Sockets;
using System.Text;

var socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
socket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
socket.Bind(new IPEndPoint(IPAddress.Any, 8080));
socket.Listen(1024);

while (true)
{
    Socket client = await socket.AcceptAsync();
    _ = HandleAsync(client);
}

static async Task HandleAsync(Socket socket)
{
    socket.NoDelay = true;
    var ns = new NetworkStream(socket, ownsSocket: true);
    var reader = PipeReader.Create(ns, new StreamPipeReaderOptions(bufferSize: 16384));
    var writer = PipeWriter.Create(ns, new StreamPipeWriterOptions(leaveOpen: false));

    try
    {
        while (true)
        {
            bool isPost;
            long querySum;
            int contentLen;
            bool chunked, close;
            SequencePosition headersEnd;

            while (true)
            {
                ReadResult res = await reader.ReadAsync();
                ReadOnlySequence<byte> buf = res.Buffer;

                if (TryParseHeaders(buf, out headersEnd, out isPost, out querySum, out contentLen, out chunked, out close))
                    break;

                reader.AdvanceTo(buf.Start, buf.End);
                if (res.IsCompleted) return;
            }
            reader.AdvanceTo(headersEnd);

            long bodyVal = 0;
            if (isPost)
            {
                if (chunked)
                    bodyVal = await ReadChunkedValueAsync(reader);
                else if (contentLen > 0)
                    bodyVal = await ReadBodyValueAsync(reader, contentLen);
            }

            WriteResponse(writer, querySum + bodyVal);
            FlushResult fr = await writer.FlushAsync();
            if (fr.IsCompleted) return;

            if (close) return;
        }
    }
    catch { }
    finally
    {
        await reader.CompleteAsync();
        await writer.CompleteAsync();
    }
}

static bool TryParseHeaders(ReadOnlySequence<byte> buf, out SequencePosition end,
    out bool isPost, out long querySum, out int contentLen, out bool chunked, out bool close)
{
    isPost = false; querySum = 0; contentLen = 0; chunked = false; close = false; end = default;

    var sr = new SequenceReader<byte>(buf);
    ReadOnlySequence<byte> headerSeq;
    if (!sr.TryReadTo(out headerSeq, "\r\n\r\n"u8, advancePastDelimiter: true))
        return false;

    end = sr.Position;

    // Avoid a copy in the common single-segment case (virtually always true)
    ReadOnlySpan<byte> h = headerSeq.IsSingleSegment
        ? headerSeq.FirstSpan
        : headerSeq.ToArray();

    // Request line
    int rlEnd = h.IndexOf("\r\n"u8);
    if (rlEnd < 0) rlEnd = h.Length;
    var rl = h[..rlEnd];

    int sp1 = rl.IndexOf((byte)' ');
    if (sp1 >= 0)
    {
        isPost = rl[..sp1].SequenceEqual("POST"u8);
        var afterMethod = rl[(sp1 + 1)..];
        int sp2 = afterMethod.IndexOf((byte)' ');
        var target = sp2 >= 0 ? afterMethod[..sp2] : afterMethod;
        int qm = target.IndexOf((byte)'?');
        if (qm >= 0) querySum = ParseQuerySum(target[(qm + 1)..]);
    }

    // Header lines
    var lines = h[(rlEnd + 2)..];
    while (lines.Length > 0)
    {
        int nl = lines.IndexOf("\r\n"u8);
        var line = nl >= 0 ? lines[..nl] : lines;
        int colon = line.IndexOf((byte)':');
        if (colon > 0)
        {
            var name = Trim(line[..colon]);
            var val  = Trim(line[(colon + 1)..]);
            if (EqCI(name, "content-length"u8))
                Utf8Parser.TryParse(val, out contentLen, out _);
            else if (EqCI(name, "transfer-encoding"u8) && ContainsCI(val, "chunked"u8))
                chunked = true;
            else if (EqCI(name, "connection"u8) && EqCI(val, "close"u8))
                close = true;
        }
        if (nl < 0) break;
        lines = lines[(nl + 2)..];
    }

    return true;
}

static async ValueTask<long> ReadBodyValueAsync(PipeReader reader, int contentLen)
{
    byte[] buf = ArrayPool<byte>.Shared.Rent(contentLen);
    int filled = 0;
    try
    {
        while (filled < contentLen)
        {
            ReadResult res = await reader.ReadAsync();
            ReadOnlySequence<byte> seg = res.Buffer;
            int take = (int)Math.Min(seg.Length, contentLen - filled);
            seg.Slice(0, take).CopyTo(buf.AsSpan(filled));
            filled += take;
            reader.AdvanceTo(seg.GetPosition(take));
            if (res.IsCompleted) break;
        }
        return ParseI64(Trim(buf.AsSpan(0, filled)));
    }
    finally
    {
        ArrayPool<byte>.Shared.Return(buf);
    }
}

static async ValueTask<long> ReadChunkedValueAsync(PipeReader reader)
{
    long total = 0;
    while (true)
    {
        ReadOnlySequence<byte> sizeLine;
        while (true)
        {
            ReadResult res = await reader.ReadAsync();
            var sr = new SequenceReader<byte>(res.Buffer);
            if (sr.TryReadTo(out sizeLine, "\r\n"u8, advancePastDelimiter: true))
            {
                reader.AdvanceTo(sr.Position);
                break;
            }
            reader.AdvanceTo(res.Buffer.Start, res.Buffer.End);
            if (res.IsCompleted) return total;
        }

        ReadOnlySpan<byte> sizeSpan = sizeLine.IsSingleSegment
            ? sizeLine.FirstSpan
            : sizeLine.ToArray();
        if (!Utf8Parser.TryParse(Trim(sizeSpan), out int chunkSize, out _, 'X'))
            break;

        if (chunkSize == 0)
        {
            await SkipBytesAsync(reader, 2);
            break;
        }

        byte[] chunk = ArrayPool<byte>.Shared.Rent(chunkSize);
        try
        {
            await ReadExactAsync(reader, chunk.AsMemory(0, chunkSize));
            total += ParseI64(Trim(chunk.AsSpan(0, chunkSize)));
        }
        finally
        {
            ArrayPool<byte>.Shared.Return(chunk);
        }

        await SkipBytesAsync(reader, 2);
    }
    return total;
}

static async ValueTask ReadExactAsync(PipeReader reader, Memory<byte> dest)
{
    int filled = 0;
    while (filled < dest.Length)
    {
        ReadResult res = await reader.ReadAsync();
        ReadOnlySequence<byte> buf = res.Buffer;
        int take = (int)Math.Min(buf.Length, dest.Length - filled);
        buf.Slice(0, take).CopyTo(dest.Span[filled..]);
        filled += take;
        reader.AdvanceTo(buf.GetPosition(take));
        if (res.IsCompleted) break;
    }
}

static async ValueTask SkipBytesAsync(PipeReader reader, int count)
{
    int skipped = 0;
    while (skipped < count)
    {
        ReadResult res = await reader.ReadAsync();
        ReadOnlySequence<byte> buf = res.Buffer;
        int take = (int)Math.Min(buf.Length, count - skipped);
        skipped += take;
        reader.AdvanceTo(buf.GetPosition(take));
        if (res.IsCompleted) break;
    }
}

static void WriteResponse(PipeWriter writer, long value)
{
    Span<byte> valueBuf = stackalloc byte[21];
    Utf8Formatter.TryFormat(value, valueBuf, out int valueLen);

    Span<byte> clenBuf = stackalloc byte[10];
    Utf8Formatter.TryFormat(valueLen, clenBuf, out int clenLen);

    ReadOnlySpan<byte> prefix = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "u8;
    int total = prefix.Length + clenLen + 4 + valueLen;
    Span<byte> span = writer.GetSpan(total);
    int pos = 0;

    prefix.CopyTo(span[pos..]); pos += prefix.Length;
    clenBuf[..clenLen].CopyTo(span[pos..]); pos += clenLen;
    "\r\n\r\n"u8.CopyTo(span[pos..]); pos += 4;
    valueBuf[..valueLen].CopyTo(span[pos..]); pos += valueLen;

    writer.Advance(pos);
}

// ── Helpers ───────────────────────────────────────────────────────────────────

static long ParseQuerySum(ReadOnlySpan<byte> query)
{
    long sum = 0;
    while (query.Length > 0)
    {
        int amp = query.IndexOf((byte)'&');
        var pair = amp >= 0 ? query[..amp] : query;
        int eq = pair.IndexOf((byte)'=');
        if (eq >= 0 && Utf8Parser.TryParse(Trim(pair[(eq + 1)..]), out long v, out _))
            sum += v;
        if (amp < 0) break;
        query = query[(amp + 1)..];
    }
    return sum;
}

static long ParseI64(ReadOnlySpan<byte> s)
{
    s = Trim(s);
    bool neg = s.Length > 0 && s[0] == (byte)'-';
    if (neg) s = s[1..];
    long v = 0;
    foreach (byte c in s)
    {
        if ((uint)(c - '0') > 9) break;
        v = v * 10 + (c - '0');
    }
    return neg ? -v : v;
}

static ReadOnlySpan<byte> Trim(ReadOnlySpan<byte> s)
{
    while (s.Length > 0 && s[0] is (byte)' ' or (byte)'\t' or (byte)'\r' or (byte)'\n') s = s[1..];
    while (s.Length > 0 && s[^1] is (byte)' ' or (byte)'\t' or (byte)'\r' or (byte)'\n') s = s[..^1];
    return s;
}

static bool EqCI(ReadOnlySpan<byte> a, ReadOnlySpan<byte> b)
    => a.Length == b.Length && Ascii.EqualsIgnoreCase(a, b);

static bool ContainsCI(ReadOnlySpan<byte> h, ReadOnlySpan<byte> n)
{
    for (int i = 0; i <= h.Length - n.Length; i++)
        if (Ascii.EqualsIgnoreCase(h[i..(i + n.Length)], n)) return true;
    return false;
}
