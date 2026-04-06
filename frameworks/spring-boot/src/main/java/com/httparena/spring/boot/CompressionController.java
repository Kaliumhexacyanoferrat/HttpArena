package com.httparena.spring.boot;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/compression")
public class CompressionController {

    private final byte[] responseBytes;

    public CompressionController(JsonMapper jsonMapper) {
        List<Item> items = jsonMapper.readValue(Paths.get("/data/dataset-large.json"), new TypeReference<>() {});
        final List<TotalItem> totalItems = items.stream()
                .map(TotalItem::fromItem)
                .toList();
        TotalItems response = new TotalItems(totalItems, totalItems.size());
        this.responseBytes = jsonMapper.writeValueAsBytes(response);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public byte[] getTotalItems() {
        return this.responseBytes;
    }
}
