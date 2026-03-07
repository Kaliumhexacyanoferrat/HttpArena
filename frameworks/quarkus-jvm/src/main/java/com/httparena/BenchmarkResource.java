package com.httparena;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Path("/")
public class BenchmarkResource {

    private final ObjectMapper mapper = new ObjectMapper();
    private List<Map<String, Object>> dataset;

    @PostConstruct
    public void init() throws IOException {
        String path = System.getenv("DATASET_PATH");
        if (path == null) path = "/data/dataset.json";
        File f = new File(path);
        if (f.exists()) {
            dataset = mapper.readValue(f, new TypeReference<>() {});
        }
    }

    @GET
    @Path("/pipeline")
    @Produces(MediaType.TEXT_PLAIN)
    public String pipeline() {
        return "ok";
    }

    @GET
    @Path("/baseline11")
    @Produces(MediaType.TEXT_PLAIN)
    public String baselineGet(@QueryParam("a") String a, @QueryParam("b") String b) {
        return String.valueOf(sumParams(a, b));
    }

    @POST
    @Path("/baseline11")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String baselinePost(@QueryParam("a") String a, @QueryParam("b") String b, String body) {
        int sum = sumParams(a, b);
        try {
            sum += Integer.parseInt(body.trim());
        } catch (NumberFormatException ignored) {}
        return String.valueOf(sum);
    }

    @GET
    @Path("/baseline2")
    @Produces(MediaType.TEXT_PLAIN)
    public String baseline2(@QueryParam("a") String a, @QueryParam("b") String b) {
        return String.valueOf(sumParams(a, b));
    }

    @GET
    @Path("/json")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> json() {
        List<Map<String, Object>> items = new ArrayList<>(dataset.size());
        for (Map<String, Object> item : dataset) {
            Map<String, Object> processed = new LinkedHashMap<>(item);
            double price = ((Number) item.get("price")).doubleValue();
            int quantity = ((Number) item.get("quantity")).intValue();
            processed.put("total", Math.round(price * quantity * 100.0) / 100.0);
            items.add(processed);
        }
        return Map.of("items", items, "count", items.size());
    }

    private int sumParams(String a, String b) {
        int sum = 0;
        if (a != null) try { sum += Integer.parseInt(a); } catch (NumberFormatException ignored) {}
        if (b != null) try { sum += Integer.parseInt(b); } catch (NumberFormatException ignored) {}
        return sum;
    }
}
