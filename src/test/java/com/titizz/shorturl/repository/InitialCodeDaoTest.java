package com.titizz.shorturl.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by code4wt on 17/8/4.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class InitialCodeDaoTest {

    @Autowired
    private InitialCodeDao dao;

    @Test
    public void insert() throws Exception {
        System.out.println("Initial Code: " + dao.insert());
    }
}