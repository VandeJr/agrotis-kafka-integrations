package com.vandejr.producer.kafka;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.vandejr.producer.dto.EventoKafkaPayload;

@Component
public class EventoProducer {
  private static final Logger log = LoggerFactory.getLogger(EventoProducer.class);

  private final KafkaTemplate<String, EventoKafkaPayload> kafkaTemplate;

  @Value("${kafka.topic.eventos}")
  private String topico;

  public EventoProducer(KafkaTemplate<String, EventoKafkaPayload> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void publicar(EventoKafkaPayload payload) {
    CompletableFuture<SendResult<String, EventoKafkaPayload>> future = kafkaTemplate.send(topico, payload.idEvento(),
        payload);

    future.whenComplete((result, ex) -> {
      if (ex != null) {
        log.error("[EventoProducer] Exception ao publicar evento: id={} error={}", payload.idEvento(), ex.getMessage());
      } else {
        log.info("[EventoProducer] Publicado id={} offset={} ", payload.idEvento(),
            result.getRecordMetadata().offset());
      }
    });
  }
}
