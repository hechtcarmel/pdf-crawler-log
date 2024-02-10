package com.gil.pdfscrapper.beans;


import com.gil.pdfscrapper.AppConfigs;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestBeans {
    private final AppConfigs appConfigs;
    @Bean
    public RestClient restClient () {
        return RestClient.builder().baseUrl(appConfigs.BASE_URL).build();

    }

}
