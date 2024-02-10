package com.gil.pdfscrapper.beans;

import com.gil.pdfscrapper.AppConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfigs {

    @Bean(name= AppConstants.REQUESTS_EXECUTORS_BEAN_QUALIFIER)
    public ExecutorService requestsExecutorService(){
        return Executors.newVirtualThreadPerTaskExecutor();
    }

}
