package com.vandejr.consumer.dto;

public record EventoKafkaPayload(
    String idEvento, String descricao, String situacao) {
}
