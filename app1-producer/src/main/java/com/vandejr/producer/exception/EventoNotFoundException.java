package com.vandejr.producer.exception;

public class EventoNotFoundException extends RuntimeException {

  public EventoNotFoundException(String id) {
    super("Evento não encontrado para o id: " + id);
  }
}
