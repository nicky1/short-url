package com.titizz.shorturl.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Created by code4wt on 17/8/6.
 */
@Component
public class InitialCodeCache {

    static final String IN_USE_INITIAL_CODES = "in-use-initial-codes";

    /** initial code 过期时间（5分钟） */
    static final Long INITIAL_CODE_EXPIRATION = 5L * 60L * 1000L;

    @Autowired
    private StringRedisTemplate template;

    public Set<String> readInUseCache() {
        return template.opsForZSet().range(IN_USE_INITIAL_CODES, 0, -1);
    }

    public void addInUseCache(Long initialCode) {
        template.opsForZSet().add(IN_USE_INITIAL_CODES, initialCode.toString(), System.currentTimeMillis());
    }

    public void removeInitialCode(Long initialCode) {
        template.opsForZSet().remove(IN_USE_INITIAL_CODES, initialCode.toString());
    }

    public void removeExpiredInitialCode() {
        long current = System.currentTimeMillis();
        double min = 0, max = current - INITIAL_CODE_EXPIRATION;
        template.opsForZSet().removeRangeByScore(IN_USE_INITIAL_CODES, min, max);
    }

    public void refreshExpiration(Long initialCode) {
        addInUseCache(initialCode);
    }
}
