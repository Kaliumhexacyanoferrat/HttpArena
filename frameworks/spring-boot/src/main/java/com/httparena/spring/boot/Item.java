package com.httparena.spring.boot;

import java.util.List;

public record Item(
        long id,
        String name,
        String category,
        double price,
        int quantity,
        boolean active,
        List<String> tags,
        Rating rating) {
}
