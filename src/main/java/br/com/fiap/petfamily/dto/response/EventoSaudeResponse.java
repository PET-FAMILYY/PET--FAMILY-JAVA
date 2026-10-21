package br.com.fiap.petfamily.dto.response;

import java.time.LocalDateTime;

public record EventoSaudeResponse(String tipo, LocalDateTime quando, String titulo, String detalhe, String status) {}