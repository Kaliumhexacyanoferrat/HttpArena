namespace ServiceStack.Benchmarks;

public class DatasetItem
{
    public int Id { get; set; }
    public string Name { get; set; } = "";
    public string Category { get; set; } = "";
    public double Price { get; set; }
    public int Quantity { get; set; }
    public bool Active { get; set; }
    public List<string>? Tags { get; set; }
    public RatingInfo? Rating { get; set; }
}

public class ProcessedItem
{
    public int Id { get; set; }
    public string Name { get; set; } = "";
    public string Category { get; set; } = "";
    public double Price { get; set; }
    public int Quantity { get; set; }
    public bool Active { get; set; }
    public List<string>? Tags { get; set; }
    public RatingInfo? Rating { get; set; }
    public double Total { get; set; }
}

public class RatingInfo
{
    public double Score { get; set; }
    public int Count { get; set; }
}

public class ListWithCount<T>(List<T> items)
{

    public List<T> Items => items;

    public int Count => items.Count;

}

public static class DatasetItemExtensions
{
    public static ProcessedItem ToProcessed(this DatasetItem d) => new()
    {
        Id       = d.Id,       Name     = d.Name,
        Category = d.Category, Price    = d.Price,
        Quantity = d.Quantity, Active   = d.Active,
        Tags     = d.Tags,     Rating   = d.Rating,
        Total    = Math.Round(d.Price * d.Quantity, 2)
    };
}