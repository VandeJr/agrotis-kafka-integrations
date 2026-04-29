package com.vandejr.producer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vandejr.producer.domain.Evento;
import com.vandejr.producer.domain.SituacaoEvento;

@Repository
public interface EventoRepository extends JpaRepository<Evento, String> {
  List<Evento> findBySituacao(SituacaoEvento situacao);
}
