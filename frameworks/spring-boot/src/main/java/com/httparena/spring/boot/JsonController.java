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
@RequestMapping("/json")
public class JsonController {

    private final List<Item> items;

    public JsonController(JsonMapper jsonMapper) {
        items = jsonMapper.readValue(Paths.get("/data/dataset.json"), new TypeReference<>() {});
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public TotalItems getTotalItems() {
        List<TotalItem> totalItems = items.stream()
                .map(TotalItem::fromItem)
                .toList();
        return new TotalItems(totalItems, totalItems.size());
    }

}
