package com.vandejr.consumer.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class App1Client {

  private static final Logger log = LoggerFactory.getLogger(App1Client.class);

  private final RestClient restClient;

  public App1Client(@Value("${app1.base-url}") String baseUrl) {
    this.restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build();
  }

  public void integrarEvento(String idEvento) {
    try {
      ResponseEntity<Void> response = restClient.patch()
          .uri("/eventos/{id}/integrar", idEvento)
          .retrieve()
          .toBodilessEntity();

      log.info("[App1Client] Evento id={} integrado com sucesso status={}",
          idEvento, response.getStatusCode());

    } catch (HttpClientErrorException.NotFound ex) {
      log.warn("[App1Client] Evento id={} não encontrado no App1", idEvento);
      throw ex;
    } catch (Exception ex) {
      log.error("[App1Client] Falha ao integrar evento id={} erro={}", idEvento, ex.getMessage());
      throw ex;
    }
  }
}
