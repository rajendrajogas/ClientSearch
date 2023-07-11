package com.client.search.app.service;

import com.client.search.app.model.NewsRequest;
import com.google.cloud.language.v1beta2.AnalyzeSentimentRequest;
import com.google.cloud.language.v1beta2.ClassificationCategory;
import com.google.cloud.language.v1beta2.Document;
import com.google.cloud.language.v1beta2.LanguageServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NewsApiService {

    @Autowired
    RestTemplate restTemplate;

    @Value("${news.api.key}")
    private String newsApiKey;

    @Value("${news.api.endpoint}")
    private String newApiEndpoint;

    public Map<String, Object> getNewsApiResponse(NewsRequest request) {
        String queryString = URLEncoder.encode(request.getQueryString(), StandardCharsets.UTF_8);
        Date fromDate = request.getFromDate();
        Date toDate = request.getToDate();
        String sortBy = request.getSortBy();
        String url = String.format(newApiEndpoint, queryString, fromDate, toDate, sortBy, newsApiKey);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity requestEntity = new HttpEntity(httpHeaders);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity
                , new ParameterizedTypeReference<Map<String, Object>>() {
                });
        processNewsApiResponse(response.getBody());
        return response.getBody();
    }

    public Map<String, Object> processNewsApiResponse(Map<String, Object> request) {
        List<Map<String, Object>> articles = (List<Map<String, Object>>) request.get("articles");
        int i=1;


        if (articles.size() > 10) {
            for (int j = 0; j < articles.size(); j++) {
                if (j == 10 ) {
                    break;
                }
                String description = (String) articles.get(j).get("description");
                String url = (String) articles.get(j).get("url");
                try (LanguageServiceClient language = LanguageServiceClient.create()) {
                    Document doc = Document.newBuilder().setContent(description).setType(Document.Type.PLAIN_TEXT).build();
                    // analyzeSentiment API
                    com.google.cloud.language.v1beta2.Sentiment sentiment = language.analyzeSentiment(doc).getDocumentSentiment();
                    Map<String, Object> analysis = new HashMap<>();
                    analysis.put("magniture", sentiment.getMagnitude());
                    analysis.put("score", sentiment.getScore());
                    if (sentiment.getScore() > 0.0) {
                        analysis.put("sentiment", "Positive");
                    } else if (sentiment.getScore() < 0.0) {
                        analysis.put("sentiment", "Negative");
                    } else {
                        analysis.put("sentiment", "Neutral");
                    }
                    articles.get(j).put("analysis", analysis);

                    //Classification API
                    if (description.length() > 250) {
                        List<ClassificationCategory> categories = language.classifyText(doc).getCategoriesList();
                        if (categories != null && categories.size() > 0) {
                            Map<String, Object> classification = new HashMap<>();
                            for (ClassificationCategory category:categories) {
                                classification.put("confidence", category.getConfidence());
                                classification.put("name", category.getName());
                            }
                            articles.get(j).put("classification", classification);
                        }
                    }
                } catch (IOException e) {
                    //Ignore article
                }
                log.info("Counter" + j);
            }
        }
        /*for (Map<String, Object> article : articles) {
            String description = (String) article.get("description");
            String url = (String) article.get("url");
            try (LanguageServiceClient language = LanguageServiceClient.create()) {
                Document doc = Document.newBuilder().setContent(description).setType(Document.Type.PLAIN_TEXT).build();
                // analyzeSentiment API
                com.google.cloud.language.v1beta2.Sentiment sentiment = language.analyzeSentiment(doc).getDocumentSentiment();
                Map<String, Object> analysis = new HashMap<>();
                analysis.put("magniture", sentiment.getMagnitude());
                analysis.put("score", sentiment.getScore());
                if (sentiment.getScore() > 0.0) {
                    analysis.put("sentiment", "Positive");
                } else if (sentiment.getScore() < 0.0) {
                    analysis.put("sentiment", "Negative");
                } else {
                    analysis.put("sentiment", "Neutral");
                }
                article.put("analysis", analysis);

                //Classification API
                if (description.length() > 250) {
                    List<ClassificationCategory> categories = language.classifyText(doc).getCategoriesList();
                    if (categories != null && categories.size() > 0) {
                        Map<String, Object> classification = new HashMap<>();
                        for (ClassificationCategory category:categories) {
                            classification.put("confidence", category.getConfidence());
                            classification.put("name", category.getName());
                        }
                        article.put("classification", classification);
                    }
                }
            } catch (IOException e) {
                //Ignore article
            }
        }*/
        return request;
    }
}