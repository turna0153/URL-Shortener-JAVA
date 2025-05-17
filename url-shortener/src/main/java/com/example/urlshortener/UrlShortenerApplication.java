package com.example.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
public class UrlShortenerApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}

@RestController
@RequestMapping("/api")
class UrlController {
    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    // 1. Shorten URL
    @PostMapping("/shorten")
    public ResponseEntity<String> shorten(@RequestBody String longUrl) {
        String code = urlService.shortenUrl(longUrl);
        return ResponseEntity.ok(code);
    }

    // 2. Redirect Logic
    @GetMapping("/{code}")
    public void redirect(@PathVariable String code, HttpServletResponse response) throws IOException {
        String longUrl = urlService.getOriginalUrl(code);
        if (longUrl != null) {
            urlService.incrementCount(code);
            response.sendRedirect(longUrl);
        } else {
            response.sendError(HttpStatus.NOT_FOUND.value(), "URL not found");
        }
    }

    // 3. Statistics: get access count
    @GetMapping("/stats/{code}")
    public ResponseEntity<Long> stats(@PathVariable String code) {
        Long count = urlService.getCount(code);
        if (count != null) {
            return ResponseEntity.ok(count);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}

@Service
class UrlService {
    private final ConcurrentHashMap<String, String> urlMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> countMap = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final int BASE = ALPHABET.length();

    public String shortenUrl(String longUrl) {
        long id = idCounter.getAndIncrement();
        String code = encode(id);
        urlMap.put(code, longUrl);
        countMap.put(code, new AtomicLong(0));
        return code;
    }

    public String getOriginalUrl(String code) {
        return urlMap.get(code);
    }

    public void incrementCount(String code) {
        AtomicLong counter = countMap.get(code);
        if (counter != null) counter.incrementAndGet();
    }

    public Long getCount(String code) {
        AtomicLong counter = countMap.get(code);
        return counter != null ? counter.get() : null;
    }

    private String encode(long num) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(ALPHABET.charAt((int)(num % BASE)));
            num /= BASE;
        }
        return sb.reverse().toString();
    }
}