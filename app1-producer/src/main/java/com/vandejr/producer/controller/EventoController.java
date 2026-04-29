package com.vandejr.producer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vandejr.producer.dto.EventoRequestDTO;
import com.vandejr.producer.dto.EventoResponseDTO;
import com.vandejr.producer.service.EventoService;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.vandejr.producer.exception.GlobalExceptionHandler.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/eventos")
@Tag(name = "Eventos", description = "Gerenciamento de eventos")
public class EventoController {
  private static final Logger log = LoggerFactory.getLogger(EventoController.class);

  private final EventoService service;

  public EventoController(EventoService service) {
    this.service = service;
  }

  @PostMapping
  @Operation(summary = "Cria um novo evento com situação ENCERRADO")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Evento criado com sucesso"),
      @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<EventoResponseDTO> criar(@Valid @RequestBody EventoRequestDTO request) {
    log.info("[EventoController] Criando evento descricao={}", request.descricao());
    EventoResponseDTO evento = service.criarEvento(request.descricao());
    return ResponseEntity.status(HttpStatus.CREATED).body(evento);
  }

  @PatchMapping("/{id}/integrar")
  @Operation(summary = "Atualiza a situação do evento para INTEGRADO")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Evento integrado com sucesso"),
      @ApiResponse(responseCode = "404", description = "Evento não encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<EventoResponseDTO> integrar(
      @Parameter(description = "ID do evento") @PathVariable String id) {
    log.info("[EventoController] Integrando evento id={}", id);
    return ResponseEntity.ok(service.integrarEvento(id));
  }

}
