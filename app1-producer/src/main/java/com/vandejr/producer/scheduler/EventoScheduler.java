package com.vandejr.producer.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vandejr.producer.dto.EventoResponseDTO;
import com.vandejr.producer.service.EventoService;

@Component
public class EventoScheduler {
  private static final Logger log = LoggerFactory.getLogger(EventoScheduler.class);

  private final EventoService service;

  public EventoScheduler(EventoService service) {
    this.service = service;
  }

  @Scheduled(cron = "${scheduler.cron.eventos:*/15 * * * * *}")
  public void publicarEventos() {
    log.info("[EventoScheduler] Iniciando publicação de eventos ENCERRADOS");
    service.publicarEventosEncerrados();
  }

  @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
  public void criarEventoInicial() {
    EventoResponseDTO evento = service.criarEvento("teste de topico do evento");
    log.info("[EventoScheduler] Evento criado id={}", evento.id());
  }
}
