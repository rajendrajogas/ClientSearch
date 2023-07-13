package com.client.search.app.controller;

import com.client.search.app.ClientSearchApplication;
import com.client.search.app.model.NewsRequest;
import com.client.search.app.service.NewsApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/services")
@CrossOrigin
public class ServiceController {
    private static Logger LOG = LoggerFactory.getLogger(ServiceController.class);

    @Autowired
    private NewsApiService newsApiService;

    //@PreAuthorize("hasAuthority('SCOPE_internal') || hasAuthority('Admin')")
    @RequestMapping("/test")
    public String getResponse(){
        return "success";
    }

    @PostMapping("/news")
    public Map<String, Object> newsApi(@RequestBody NewsRequest request) throws IOException {

        LOG.info("Inside newsApi() : Enter");
        return newsApiService.getNewsApiResponse(request);
    }

    @PostMapping("/news/process")
    public Map<String, Object> processNewsApi(@RequestBody Map<String, Object> request) throws IOException {
        LOG.info("Inside processNewsApi() : Enter");
        return newsApiService.processNewsApiResponse(request);
    }


}
