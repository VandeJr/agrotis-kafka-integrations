package com.vandejr.producer.dto;

public record EventoKafkaPayload(
    String idEvento, String descricao, String situacao) {
}
