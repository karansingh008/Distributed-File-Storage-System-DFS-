package com.example.dfs.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeduplicationService {

    private BloomFilter<String> bloomFilter;
    private final ConcurrentHashMap<String, Boolean> chunkIndex = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 100000, 0.01);
    }

    public boolean mightContain(String hash) {
        return bloomFilter.mightContain(hash);
    }

    public boolean isIndexed(String hash) {
        return chunkIndex.containsKey(hash);
    }

    public void add(String hash) {
        bloomFilter.put(hash);
        chunkIndex.put(hash, true);
    }
}
