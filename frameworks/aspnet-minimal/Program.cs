using System.IO.Compression;
using System.Text.Json;
using System.Security.Cryptography.X509Certificates;
using Microsoft.AspNetCore.ResponseCompression;
using Microsoft.AspNetCore.Server.Kestrel.Core;
using Microsoft.Data.Sqlite;

var builder = WebApplication.CreateBuilder(args);
builder.Logging.ClearProviders();

var certPath = Environment.GetEnvironmentVariable("TLS_CERT") ?? "/certs/server.crt";
var keyPath = Environment.GetEnvironmentVariable("TLS_KEY") ?? "/certs/server.key";
var hasCert = File.Exists(certPath) && File.Exists(keyPath);

builder.WebHost.ConfigureKestrel(options =>
{
    options.Limits.Http2.MaxStreamsPerConnection = 256;
    options.Limits.Http2.InitialConnectionWindowSize = 2 * 1024 * 1024;
    options.Limits.Http2.InitialStreamWindowSize = 1024 * 1024;

    // HTTP/1.1 on port 8080
    options.ListenAnyIP(8080, lo =>
    {
        lo.Protocols = HttpProtocols.Http1;
    });

    // HTTPS + HTTP/2 + HTTP/3 on port 8443
    if (hasCert)
    {
        options.ListenAnyIP(8443, lo =>
        {
            lo.Protocols = HttpProtocols.Http1AndHttp2AndHttp3;
            lo.UseHttps(X509Certificate2.CreateFromPemFile(certPath, keyPath));
        });
    }
});

builder.Services.AddResponseCompression(options =>
{
    options.EnableForHttps = true;
    options.MimeTypes = new[] { "application/json" };
    options.Providers.Add<GzipCompressionProvider>();
});
builder.Services.Configure<GzipCompressionProviderOptions>(options =>
{
    options.Level = CompressionLevel.Fastest;
});

var app = builder.Build();

app.UseResponseCompression();

app.Use(async (ctx, next) =>
{
    ctx.Response.Headers["Server"] = "aspnet-minimal";
    await next();
});

// Shared JSON options: camelCase matching for deserialization, camelCase output for serialization
var jsonOptions = new JsonSerializerOptions
{
    PropertyNameCaseInsensitive = true,
    PropertyNamingPolicy = JsonNamingPolicy.CamelCase
};

// Load dataset at startup
var datasetPath = Environment.GetEnvironmentVariable("DATASET_PATH") ?? "/data/dataset.json";
List<DatasetItem>? datasetItems = null;
if (File.Exists(datasetPath))
{
    var json = File.ReadAllText(datasetPath);
    datasetItems = JsonSerializer.Deserialize<List<DatasetItem>>(json, jsonOptions);
}

// Load large dataset for compression endpoint
var largePath = "/data/dataset-large.json";
byte[]? largeJsonResponse = null;
if (File.Exists(largePath))
{
    var largeItems = JsonSerializer.Deserialize<List<DatasetItem>>(File.ReadAllText(largePath), jsonOptions);
    if (largeItems != null)
    {
        var responseItems = new List<ProcessedItem>(largeItems.Count);
        foreach (var item in largeItems)
        {
            responseItems.Add(new ProcessedItem
            {
                Id = item.Id, Name = item.Name, Category = item.Category,
                Price = item.Price, Quantity = item.Quantity, Active = item.Active,
                Tags = item.Tags, Rating = item.Rating,
                Total = Math.Round(item.Price * item.Quantity, 2)
            });
        }
        largeJsonResponse = JsonSerializer.SerializeToUtf8Bytes(new { items = responseItems, count = responseItems.Count }, jsonOptions);
    }
}

// Pre-load static files
var staticFileMap = new Dictionary<string, (byte[] Data, string ContentType)>();
var staticDir = "/data/static";
if (Directory.Exists(staticDir))
{
    var mimeTypes = new Dictionary<string, string>
    {
        {".css", "text/css"}, {".js", "application/javascript"}, {".html", "text/html"},
        {".woff2", "font/woff2"}, {".svg", "image/svg+xml"}, {".webp", "image/webp"}, {".json", "application/json"}
    };
    foreach (var file in Directory.GetFiles(staticDir))
    {
        var name = Path.GetFileName(file);
        var ext = Path.GetExtension(file);
        var ct = mimeTypes.GetValueOrDefault(ext, "application/octet-stream");
        staticFileMap[name] = (File.ReadAllBytes(file), ct);
    }
}

// Open SQLite database
SqliteConnection? dbConn = null;
var dbPath = "/data/benchmark.db";
if (File.Exists(dbPath))
{
    dbConn = new SqliteConnection($"Data Source={dbPath};Mode=ReadOnly");
    dbConn.Open();
    using var pragma = dbConn.CreateCommand();
    pragma.CommandText = "PRAGMA mmap_size=268435456";
    pragma.ExecuteNonQuery();
}

app.MapGet("/db", (HttpRequest req) =>
{
    if (dbConn == null)
        return Results.Problem("DB not available");
    double min = 10, max = 50;
    if (req.Query.ContainsKey("min") && double.TryParse(req.Query["min"], out double pmin))
        min = pmin;
    if (req.Query.ContainsKey("max") && double.TryParse(req.Query["max"], out double pmax))
        max = pmax;
    using var cmd = dbConn.CreateCommand();
    cmd.CommandText = "SELECT id, name, category, price, quantity, active, tags, rating_score, rating_count FROM items WHERE price BETWEEN @min AND @max LIMIT 50";
    cmd.Parameters.AddWithValue("@min", min);
    cmd.Parameters.AddWithValue("@max", max);
    using var reader = cmd.ExecuteReader();
    var items = new List<object>();
    while (reader.Read())
    {
        items.Add(new
        {
            id = reader.GetInt32(0),
            name = reader.GetString(1),
            category = reader.GetString(2),
            price = reader.GetDouble(3),
            quantity = reader.GetInt32(4),
            active = reader.GetInt32(5) == 1,
            tags = JsonSerializer.Deserialize<List<string>>(reader.GetString(6)),
            rating = new { score = reader.GetDouble(7), count = reader.GetInt32(8) },
        });
    }
    return Results.Json(new { items, count = items.Count });
});

app.MapGet("/static/{filename}", (string filename) =>
{
    if (staticFileMap.TryGetValue(filename, out var sf))
        return Results.Bytes(sf.Data, sf.ContentType);
    return Results.NotFound();
});

app.MapGet("/pipeline", () => Results.Text("ok"));

app.MapGet("/baseline11", (HttpRequest req) =>
{
    int sum = SumQuery(req);
    return Results.Text(sum.ToString());
});

app.MapPost("/baseline11", async (HttpRequest req) =>
{
    int sum = SumQuery(req);

    using var reader = new StreamReader(req.Body);

    var body = await reader.ReadToEndAsync();

    if (int.TryParse(body, out int b))
        sum += b;

    return Results.Text(sum.ToString());
});

app.MapGet("/baseline2", (HttpRequest req) =>
{
    int sum = SumQuery(req);
    return Results.Text(sum.ToString());
});

app.MapPost("/upload", async (HttpRequest req) =>
{
    using var ms = new MemoryStream();
    await req.Body.CopyToAsync(ms);
    return Results.Text(ms.Length.ToString());
});

app.MapGet("/json", () =>
{
    if (datasetItems == null)
        return Results.Problem("Dataset not loaded");

    var responseItems = new List<ProcessedItem>(datasetItems.Count);
    foreach (var item in datasetItems)
    {
        responseItems.Add(new ProcessedItem
        {
            Id = item.Id,
            Name = item.Name,
            Category = item.Category,
            Price = item.Price,
            Quantity = item.Quantity,
            Active = item.Active,
            Tags = item.Tags,
            Rating = item.Rating,
            Total = Math.Round(item.Price * item.Quantity, 2)
        });
    }

    return Results.Json(new { items = responseItems, count = responseItems.Count });
});

app.MapGet("/compression", async (HttpContext ctx) =>
{
    if (largeJsonResponse == null)
    {
        ctx.Response.StatusCode = 500;
        return;
    }
    ctx.Response.ContentType = "application/json";
    await ctx.Response.Body.WriteAsync(largeJsonResponse);
});

app.Run();

static int SumQuery(HttpRequest req)
{
    int sum = 0;
    foreach (var (_, values) in req.Query)
        foreach (var v in values)
            if (int.TryParse(v, out int n)) sum += n;
    return sum;
}

class DatasetItem
{
    public int Id { get; set; }
    public string Name { get; set; } = "";
    public string Category { get; set; } = "";
    public double Price { get; set; }
    public int Quantity { get; set; }
    public bool Active { get; set; }
    public List<string> Tags { get; set; } = new();
    public RatingInfo Rating { get; set; } = new();
}

class ProcessedItem
{
    public int Id { get; set; }
    public string Name { get; set; } = "";
    public string Category { get; set; } = "";
    public double Price { get; set; }
    public int Quantity { get; set; }
    public bool Active { get; set; }
    public List<string> Tags { get; set; } = new();
    public RatingInfo Rating { get; set; } = new();
    public double Total { get; set; }
}

class RatingInfo
{
    public double Score { get; set; }
    public int Count { get; set; }
}
