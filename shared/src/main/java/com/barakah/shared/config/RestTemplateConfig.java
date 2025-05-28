package com.barakah.shared.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import org.springframework.context.annotation.Primary;

@Configuration
public class RestTemplateConfig {

   @Bean
   @Primary
   public RestTemplate restTemplate() {
       return new RestTemplateBuilder()
               .setConnectTimeout(Duration.ofSeconds(10))
               .setReadTimeout(Duration.ofSeconds(30))
               .build();
   }
}
