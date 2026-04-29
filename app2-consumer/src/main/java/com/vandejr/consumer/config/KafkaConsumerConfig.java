package com.vandejr.consumer.config;

import com.vandejr.consumer.dto.EventoKafkaPayload;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

  private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${kafka.topic.dlq}")
  private String dlqTopic;

  @Value("${kafka.retry.max-attempts}")
  private int maxAttempts;

  @Bean
  public ConsumerFactory<String, EventoKafkaPayload> consumerFactory() {
    JsonDeserializer<EventoKafkaPayload> deserializer = new JsonDeserializer<>(EventoKafkaPayload.class);
    deserializer.setRemoveTypeHeaders(false);
    deserializer.addTrustedPackages("*");
    deserializer.setUseTypeMapperForKey(true);

    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "app2-consumer-group");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);

    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, EventoKafkaPayload> kafkaListenerContainerFactory(
      ConsumerFactory<String, EventoKafkaPayload> consumerFactory,
      KafkaTemplate<String, EventoKafkaPayload> kafkaTemplate) {
    FixedBackOff backOff = new FixedBackOff(3000L, maxAttempts - 1);

    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
        (record, ex) -> {
          log.error("[DLQ] Enviando mensagem para DLQ={} idEvento={} erro={}",
              dlqTopic, record.key(), ex.getMessage());
          return new TopicPartition(dlqTopic, 0);
        });

    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

    ConcurrentKafkaListenerContainerFactory<String, EventoKafkaPayload> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);

    return factory;
  }
}
