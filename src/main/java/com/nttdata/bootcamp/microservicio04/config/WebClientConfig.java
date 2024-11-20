package com.nttdata.bootcamp.microservicio04.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class WebClientConfig {

  @Value("${application.endpoints.url.account}")
  private String urlEndpointAccount;

  @Value("${application.endpoints.url.credit}")
  private String urlEndpointCredit;

  @Bean
  public WebClient webClientAccount() {
    return WebClient.builder()
        .baseUrl(urlEndpointAccount)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .filter(
            (request, next) ->
                next.exchange(request).doOnError(e -> log.info("WebClient request error", e)))
        .build();
  }

  @Bean
  public WebClient webClientCredit() {
    return WebClient.builder()
            .baseUrl(urlEndpointCredit)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .filter(
                    (request, next) ->
                            next.exchange(request).doOnError(e -> log.info("WebClient request error", e)))
            .build();
  }
}
