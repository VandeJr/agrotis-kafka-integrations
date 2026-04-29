package com.vandejr.producer.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta com os dados do evento")
public record EventoResponseDTO(
    @Schema(description = "Identificador único do evento") String id,
    @Schema(description = "Descrição do evento") String descricao,
    @Schema(description = "Situação atual do evento", example = "INTEGRADO") String situacao,
    @Schema(description = "Data da última atualização") LocalDateTime atualizadoEm) {
}
