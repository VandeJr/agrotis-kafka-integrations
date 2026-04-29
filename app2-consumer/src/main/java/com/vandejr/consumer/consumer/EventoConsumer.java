package com.vandejr.consumer.consumer;

import com.vandejr.consumer.client.App1Client;
import com.vandejr.consumer.dto.EventoKafkaPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class EventoConsumer {

  private static final Logger log = LoggerFactory.getLogger(EventoConsumer.class);

  private final App1Client client;

  public EventoConsumer(App1Client client) {
    this.client = client;
  }

  @KafkaListener(topics = "${kafka.topic.eventos}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
  public void consumir(
      @Payload EventoKafkaPayload payload,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset) {
    log.info("[Consumer] Mensagem recebida idEvento={} situacao={} partition={} offset={}",
        payload.idEvento(), payload.situacao(), partition, offset);

    if (!"ENCERRADO".equals(payload.situacao())) {
      log.info("[Consumer] Situacao={} ignorada para idEvento={}", payload.situacao(), payload.idEvento());
      return;
    }

    client.integrarEvento(payload.idEvento());
  }
}
