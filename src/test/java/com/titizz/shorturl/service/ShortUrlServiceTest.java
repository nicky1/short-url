package com.titizz.shorturl.service;

import com.titizz.shorturl.NumberUtils;
import com.titizz.shorturl.exception.FailToAcquireDistributeLockException;
import com.titizz.shorturl.exception.NoMoreInitialCodeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.UUID;

/**
 * Created by code4wt on 17/8/6.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ShortUrlServiceTest {

    @Autowired
    private ShortUrlService service;

    private List<String> urls;

    @Before
    public void prepareData() {

    }

    @Test
    public void compress() throws Exception {

        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    service.compress("http://www.baidu.com/" + UUID.randomUUID());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (NoMoreInitialCodeException e) {
                    e.printStackTrace();
                } catch (FailToAcquireDistributeLockException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        service.compress("http://www.baidu.com/");
        service.compress("http://www.baidu.com/");
    }

    @Test
    public void decompress() throws Exception {
        Long code = 1001L;
        String base62 = NumberUtils.decimal2base62(code);
        String url = service.decompress(base62);
        System.out.println(url);
    }
}