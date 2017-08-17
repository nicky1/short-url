package com.titizz.shorturl.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Created by code4wt on 17/8/2.
 */
@Component
public class UrlMappingRedisCache implements Cache {

    /** 缓存过期时间(单位：小时) */
    private static final Long KEY_EXPIRE = 1L;

    @Autowired
    private StringRedisTemplate template;

    @Override
    public void put(String code, String url) {
        template.opsForValue().set(code, url, KEY_EXPIRE, TimeUnit.HOURS);
        template.opsForValue().set(url, code, KEY_EXPIRE, TimeUnit.HOURS);
    }

    @Override
    public Long getCode(String url) {
        String code = get(url);
        return code == null ? null : Long.parseLong(code);
    }

    @Override
    public String getUrl(String code) {
        return get(code);
    }

    private String get(String key) {
        refreshExpiration(key);
        return template.opsForValue().get(key);
    }

    private void refreshExpiration(String key) {
        template.expire(key, KEY_EXPIRE, TimeUnit.HOURS);
    }
}
