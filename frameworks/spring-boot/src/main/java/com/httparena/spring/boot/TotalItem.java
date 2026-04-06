package com.httparena.spring.boot;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record TotalItem(@JsonUnwrapped Item item, double total) {
    public static TotalItem fromItem(Item item) {
        double total = item.price() * item.quantity();
        return new TotalItem(item, total);
    }
}
