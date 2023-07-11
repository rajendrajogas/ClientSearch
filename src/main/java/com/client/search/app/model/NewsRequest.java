package com.client.search.app.model;

import lombok.Data;

import java.util.Date;

@Data
public class NewsRequest {
    private String queryString;
    private String country;
    private Date fromDate;
    private Date toDate;
    private String sortBy;
}
