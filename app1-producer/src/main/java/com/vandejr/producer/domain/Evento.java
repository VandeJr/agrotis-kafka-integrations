package com.vandejr.producer.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "eventos")
public class Evento {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private String descricao;

  @Enumerated(EnumType.STRING)
  private SituacaoEvento situacao;

  private LocalDateTime criadoEm;
  private LocalDateTime atualizadoEm;

  @PrePersist
  protected void onCreate() {
    this.criadoEm = LocalDateTime.now();
    this.atualizadoEm = LocalDateTime.now();
    this.situacao = SituacaoEvento.ENCERRADO;
  }

  @PreUpdate
  protected void onUpdate() {
    this.atualizadoEm = LocalDateTime.now();
  }

  // Getters e Setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescricao() {
    return descricao;
  }

  public void setDescricao(String descricao) {
    this.descricao = descricao;
  }

  public SituacaoEvento getSituacao() {
    return situacao;
  }

  public void setSituacao(SituacaoEvento situacao) {
    this.situacao = situacao;
  }

  public LocalDateTime getCriadoEm() {
    return criadoEm;
  }

  public LocalDateTime getAtualizadoEm() {
    return atualizadoEm;
  }
}
