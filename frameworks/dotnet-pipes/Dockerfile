FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /app
COPY dotnet-pipes.csproj .
RUN dotnet restore
COPY Server.cs .
RUN dotnet publish -c Release -o out

FROM mcr.microsoft.com/dotnet/runtime:10.0
WORKDIR /app
COPY --from=build /app/out .
EXPOSE 8080
ENTRYPOINT ["dotnet", "dotnet-pipes.dll"]
