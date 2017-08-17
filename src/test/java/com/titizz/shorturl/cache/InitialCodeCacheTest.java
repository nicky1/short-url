package com.titizz.shorturl.cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by code4wt on 17/8/6.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class InitialCodeCacheTest {

    @Autowired
    private InitialCodeCache cache;

    @Autowired
    private StringRedisTemplate template;

    @Before
    public void prepareData() {
        template.opsForSet().add(InitialCodeCache.IN_USE_INITIAL_CODES, "0", "1", "2");
    }

    @After
    public void removeData() {
        Set<String> initialCodes = template.opsForSet().members(InitialCodeCache.IN_USE_INITIAL_CODES);
        template.opsForSet().remove(InitialCodeCache.IN_USE_INITIAL_CODES, initialCodes.toArray());
    }

    @Test
    public void readInUseCache() throws Exception {
        Set<String> initialCodes = cache.readInUseCache();

        assertTrue(initialCodes.contains("0"));
        assertTrue(initialCodes.contains("1"));
        assertTrue(initialCodes.contains("2"));
    }

    @Test
    public void appendInUseCache() throws Exception {
        Long initialCode = 3L;
        cache.addInUseCache(initialCode);

        Set<String> initialCodes = template.opsForSet().members(InitialCodeCache.IN_USE_INITIAL_CODES);
        assertTrue(initialCodes.contains(initialCode.toString()));
    }

    @Test
    public void removeInitialCode() throws Exception {
        Long initialCode = 2L;
        cache.removeInitialCode(initialCode);

        Set<String> initialCodes = template.opsForSet().members(InitialCodeCache.IN_USE_INITIAL_CODES);
        assertFalse(initialCodes.contains(initialCode));
    }
}