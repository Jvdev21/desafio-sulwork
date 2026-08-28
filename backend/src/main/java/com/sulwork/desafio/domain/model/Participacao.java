package com.sulwork.desafio.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "participacao")
public class Participacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "colaborador_id", nullable = false)
    private Long colaboradorId;

    @Column(name = "cafe_id", nullable = false)
    private Long cafeId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Participacao() {
    }

    public Long getId() {
        return id;
    }

    public Long getColaboradorId() {
        return colaboradorId;
    }

    public Long getCafeId() {
        return cafeId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
