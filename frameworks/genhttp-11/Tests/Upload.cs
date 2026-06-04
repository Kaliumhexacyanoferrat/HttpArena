using GenHTTP.Modules.Reflection;
using GenHTTP.Modules.Webservices;

namespace genhttp.Tests;

public class Upload
{

    [ResourceMethod(Method.Post)]
    public async ValueTask<long> Compute(Stream input)
    {
        var buffer = new byte[16384];

        long total = 0;

        var read = 0;

        while ((read = await input.ReadAsync(buffer)) > 0)
        {
            total += read;
        }

        return total;
    }

}
