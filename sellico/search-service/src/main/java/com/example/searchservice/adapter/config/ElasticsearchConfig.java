package com.example.searchservice.adapter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.lang.NonNull;

@Configuration
@EnableElasticsearchRepositories(
        basePackages = "com.example.searchservice.adapter.out.elasticsearch"
)
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://elasticsearch:9200}")
    private String elasticsearchUri;

    @Override
    @NonNull
    public ClientConfiguration clientConfiguration() {
        String host = elasticsearchUri
                .replace("http://", "")
                .replace("https://", "");

        return ClientConfiguration.builder()
                .connectedTo(host)
                .build();
    }
}
