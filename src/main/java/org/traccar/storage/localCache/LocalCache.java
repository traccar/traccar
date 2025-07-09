package org.traccar.storage.localCache;

import com.github.benmanes.caffeine.cache.*;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import jakarta.inject.Singleton;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Singleton
public class LocalCache {
    private final LoadingCache<String, Object> cache;

    public LocalCache(CacheLoader<String, Object> cacheLoader) {
        cache = Caffeine.newBuilder()
                .maximumSize(60000)              // Maximum number of entries
                .expireAfterWrite(5, TimeUnit.MINUTES)  // Entry expiration time
                .recordStats()                    // Optional: enables statistics
                .build(cacheLoader);
    }

    public Object get(String key) {
        return cache.getIfPresent(key);
    }

    public void put(String key, Object value) {
        cache.put(key, value);
    }

    public Object getOrCompute(String key, Function<String, Object> mappingFunction) {
        return cache.get(key, mappingFunction);
    }

    public String logCacheStats() {
        CacheStats stats = cache.stats();
        String log = stats.toString();
//        String log = String.format("Cache stats: hitRate={}, hitCount={}, missCount={}, loadSuccessCount={}, " +
//                        "loadFailureCount={}, evictionCount={}",
//                stats.hitRate(), stats.hitCount(), stats.missCount(),
//                stats.loadSuccessCount(), stats.loadFailureCount(),
//                stats.evictionCount());
        return log;
    }


}
