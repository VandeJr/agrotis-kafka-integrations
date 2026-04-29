package com.vandejr.producer.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.vandejr.producer.domain.Evento;
import com.vandejr.producer.domain.SituacaoEvento;
import com.vandejr.producer.dto.EventoKafkaPayload;
import com.vandejr.producer.dto.EventoResponseDTO;
import com.vandejr.producer.exception.EventoNotFoundException;
import com.vandejr.producer.kafka.EventoProducer;
import com.vandejr.producer.repository.EventoRepository;

import jakarta.transaction.Transactional;

@Service
public class EventoService {

  private static final Logger log = LoggerFactory.getLogger(EventoService.class);

  private final EventoRepository repository;
  private final EventoProducer producer;

  public EventoService(EventoRepository repository, EventoProducer producer) {
    this.repository = repository;
    this.producer = producer;
  }

  @Transactional
  public void publicarEventosEncerrados() {
    List<Evento> eventos = repository.findBySituacao(SituacaoEvento.ENCERRADO);

    if (eventos.isEmpty()) {
      log.info("[EventoService] Nenhum evento ENCERRADO para publicar");
      return;
    }

    for (Evento evento : eventos) {
      EventoKafkaPayload payload = new EventoKafkaPayload(
          evento.getId(),
          evento.getDescricao(),
          evento.getSituacao().name());
      producer.publicar(payload);
      log.info("[EventoService] Publicando evento id={}", payload.idEvento());
    }
  }

  @Transactional
  public EventoResponseDTO integrarEvento(String id) {
    Evento evento = repository.findById(id).orElseThrow(() -> new EventoNotFoundException(id));

    evento.setSituacao(SituacaoEvento.INTEGRADO);
    repository.save(evento);

    log.info("[EventoService] Evento id={} atualizado para INTEGRADO", id);

    return new EventoResponseDTO(
        evento.getId(),
        evento.getDescricao(),
        evento.getSituacao().name(),
        evento.getAtualizadoEm());
  }

  @Transactional
  public EventoResponseDTO criarEvento(String descricao) {
    Evento evento = new Evento();
    evento.setDescricao(descricao);
    evento = repository.save(evento);
    return new EventoResponseDTO(
        evento.getId(),
        evento.getDescricao(),
        evento.getSituacao().name(),
        evento.getAtualizadoEm());
  }
}
