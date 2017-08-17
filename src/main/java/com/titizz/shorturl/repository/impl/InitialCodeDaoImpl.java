package com.titizz.shorturl.repository.impl;

import com.mysql.jdbc.Statement;
import com.titizz.shorturl.repository.InitialCodeDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class InitialCodeDaoImpl implements InitialCodeDao {

    /** 自增 id 步进值 */
    public static final Long AUTO_INCREMENT_STEP = 1000L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Long insert() {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> conn.prepareStatement("INSERT INTO initial_code VALUE()",
                Statement.RETURN_GENERATED_KEYS), keyHolder);

        return keyHolder.getKey().intValue() - 1L;
    }
}
