package com.vandejr.producer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload para criação de um evento")
public record EventoRequestDTO(
    @NotBlank(message = "Descrição é obrigatória") @Schema(description = "Descrição do evento", example = "teste de topico do evento") String descricao) {
}
