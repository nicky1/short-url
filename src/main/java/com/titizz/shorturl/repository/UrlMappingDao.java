package com.titizz.shorturl.repository;


public interface UrlMappingDao {

    void create(Long intialCode);

    Long insert(Long initialCode, String url);

    void delete(Long code);

    String queryUrl(Long code);

    Boolean hasMoreSpace(Long initialCode);

    Boolean greatThanMaxCode(Long code);

    Boolean lessThanMaxCode(Long code);

    Integer getTableRowNum(Long initialCode);

}
