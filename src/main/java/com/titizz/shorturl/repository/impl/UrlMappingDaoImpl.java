package com.titizz.shorturl.repository.impl;

import com.mysql.jdbc.Statement;
import com.titizz.shorturl.repository.InitialCodeDao;
import com.titizz.shorturl.repository.UrlMappingDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;

@Repository
public class UrlMappingDaoImpl implements UrlMappingDao {

    private static final String URL_MAPPING_TABLE_PREFIX = "url_mapping_";

    public static final Long MAX_CODE = 56800235584L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void create(Long intialCode) {
        StringBuilder sql = new StringBuilder();
        sql.append(String.format("CREATE TABLE %s%d", URL_MAPPING_TABLE_PREFIX, intialCode));
        sql.append("(");
        sql.append("id BIGINT AUTO_INCREMENT PRIMARY KEY,");
        sql.append("url VARCHAR(255)");
        sql.append(String.format(") ENGINE = InnoDB, AUTO_INCREMENT = %d", intialCode));

        jdbcTemplate.update(sql.toString());
    }

    public Long insert(Long initialCode, String url) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                conn -> {
                    PreparedStatement statement = conn.prepareStatement(
                            String.format("INSERT INTO %s(url) VALUES(?)", getTableName(initialCode)),
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, url);
                    return statement;
                }, keyHolder);

        return calculatePhysicalCode(initialCode, keyHolder.getKey().longValue());
    }

    public void delete(Long code) {
        long initialCode = calculateInitialCode(code);
        long logicCode = calculateLogicCode(code);
        jdbcTemplate.update(String.format("DELETE FROM %s WHERE `id`=%d", getTableName(initialCode), logicCode));
    }

    public String queryUrl(Long code) {
        Long initialCode = calculateInitialCode(code);
        Long logicCode = calculateLogicCode(code);
        String table = getTableName(initialCode);
        String url = jdbcTemplate.queryForObject(
                String.format("SELECT `url` FROM %s WHERE `id`=?", getTableName(initialCode)),
                new Object[]{logicCode},
                (rs, rowNum) -> rs.getString("url"));

        return url;
    }

    public Boolean hasMoreSpace(Long initialCode) {
        Integer max = jdbcTemplate.queryForObject(
                String.format("SELECT max(id) AS max FROM %s", getTableName(initialCode)),
                (rs, rowNum) -> rs.getInt("max"));
        return lessThanMaxCode(max + InitialCodeDao.AUTO_INCREMENT_STEP);
    }

    public Boolean greatThanMaxCode(Long code) {
        return !lessThanMaxCode(code);
    }

    public Boolean lessThanMaxCode(Long code) {
        return code <= MAX_CODE;
    }

    public Integer getTableRowNum(Long initialCode) {
        Integer count = jdbcTemplate.queryForObject(
                String.format("SELECT count(id) AS count FROM %s", getTableName(initialCode)),
                (rs, rowNum) -> rs.getInt("count"));

        return count;
    }

    private String getTableName(Long intialCode) {
        return String.format("%s%d", URL_MAPPING_TABLE_PREFIX, intialCode);
    }

    private Long calculatePhysicalCode(Long initialCode, Long logicCode) {
        return (logicCode - initialCode) * InitialCodeDao.AUTO_INCREMENT_STEP + initialCode;
    }

    private Long calculateLogicCode(Long physicalCode) {
        Long initialCode = calculateInitialCode(physicalCode);
        return physicalCode / InitialCodeDao.AUTO_INCREMENT_STEP + initialCode;
    }

    private Long calculateInitialCode(Long physicalCode) {
        return physicalCode % InitialCodeDao.AUTO_INCREMENT_STEP;
    }
}
