package org.traccar.storage.localCache;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import redis.clients.jedis.JedisPooled;


@Singleton
public class RedisCache {
    private final JedisPooled jedis;

    @Inject
    public RedisCache(Config config) {
        String host = config.getString(Keys.REDIS_HOST);
        int port = config.getInteger(Keys.REDIS_PORT);
        String username = config.getString(Keys.REDIS_USERNAME);
        String password = config.getString(Keys.REDIS_PASSWORD);

        String redisUrl = String.format("rediss://%s:%s@%s:%d", username, password, host, port);
        this.jedis = new JedisPooled(redisUrl);
    }


    public void set(String key, String value) {
        jedis.set(key, value);
    }

    public String get(String key) {
        return jedis.get(key);
    }

    public void delete(String key) {
        jedis.del(key);
    }

    public boolean exists(String key) {
        return jedis.exists(key);
    }
}
