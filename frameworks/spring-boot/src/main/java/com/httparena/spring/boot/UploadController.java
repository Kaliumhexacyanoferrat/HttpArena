package com.httparena.spring.boot;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RestController
@RequestMapping("/upload")
public class UploadController {
    @PostMapping(consumes = MediaType.ALL_VALUE)
    public String countBodySize(InputStream body) throws IOException {
        long bodyLength = body.transferTo(OutputStream.nullOutputStream());
        return String.valueOf(bodyLength);
    }
}
