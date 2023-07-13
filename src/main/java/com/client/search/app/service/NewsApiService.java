package com.client.search.app.service;

import com.client.search.app.model.NewsRequest;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.language.v1beta2.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
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

    @Value("${gcp.service.account.json}")
    private Resource serviceAccountResource;

    public Map<String, Object> getNewsApiResponse(NewsRequest request) throws IOException {
        String queryString = URLEncoder.encode(request.getQueryString(), StandardCharsets.UTF_8);
        Date fromDate = request.getFromDate();
        Date toDate = request.getToDate();
        String sortBy = request.getSortBy();
        //String url = String.format(newApiEndpoint, queryString, fromDate, toDate, sortBy, newsApiKey);
        String url = String.format(newApiEndpoint, queryString,fromDate, toDate, sortBy, newsApiKey);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity requestEntity = new HttpEntity(httpHeaders);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity
                , new ParameterizedTypeReference<Map<String, Object>>() {
                });
        System.out.println("1 ===>" + response.getBody());
        processNewsApiResponse(response.getBody());
        return response.getBody();
    }

    public Map<String, Object> processNewsApiResponse(Map<String, Object> request) throws IOException {
        List<Map<String, Object>> articles = (List<Map<String, Object>>) request.get("articles");
System.out.println("2 ===>" + articles.size());
        for (Map<String, Object> article : articles) {
            String description = (String) article.get("description");
            System.out.println("3 ===>" + description);
            if(null == description)
                continue;
            String url = (String) article.get("url");

            InputStream resourceAsStream=serviceAccountResource.getInputStream();
            GoogleCredentials credential = GoogleCredentials.fromStream(resourceAsStream);
            LanguageServiceSettings languageServiceSettings= LanguageServiceSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(credential)).build();
            try (LanguageServiceClient language = LanguageServiceClient.create(languageServiceSettings)) {
                Document doc = Document.newBuilder().setContent(description).setType(Document.Type.PLAIN_TEXT).build();
                // analyzeSentiment API
                com.google.cloud.language.v1beta2.Sentiment sentiment = language.analyzeSentiment(doc).getDocumentSentiment();
                Map<String, Object> analysis = new HashMap<>();
                analysis.put("magnitude", sentiment.getMagnitude());
                analysis.put("score", sentiment.getScore());
                if (sentiment.getScore() > 0.0) {
                    analysis.put("sentiment", "Positive");
                } else if (sentiment.getScore() < 0.0) {
                    analysis.put("sentiment", "Negative");
                } else {
                    analysis.put("sentiment", "Neutral");
                }
                article.put("analysis", analysis);

                /*//Classification API
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
                }*/
            } catch (IOException e) {
                //Ignore article
            }
        }
        return request;
    }
}
