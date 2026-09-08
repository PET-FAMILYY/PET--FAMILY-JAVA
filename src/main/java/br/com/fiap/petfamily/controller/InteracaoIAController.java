package br.com.fiap.petfamily.controller;

import br.com.fiap.petfamily.dto.request.InteracaoIARequest;
import br.com.fiap.petfamily.dto.response.InteracaoIAResponse;
import br.com.fiap.petfamily.service.InteracaoIAService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interacoes-ia")
@RequiredArgsConstructor
@Tag(name = "Interações IA", description = "Assistente de IA simulada para orientação sobre saúde animal")
public class InteracaoIAController {

    private final InteracaoIAService interacaoIAService;

    @PostMapping
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Enviar pergunta ao assistente de IA (sobre um pet próprio) e obter resposta simulada")
    @ApiResponse(responseCode = "201", description = "Interação registrada com resposta gerada")
    public ResponseEntity<InteracaoIAResponse> criar(@Valid @RequestBody InteracaoIARequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(interacaoIAService.criar(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Listar todas as interações com paginação (visão clínica)")
    public ResponseEntity<Page<InteracaoIAResponse>> listar(
            @PageableDefault(size = 10, sort = "dataHora") Pageable pageable) {
        return ResponseEntity.ok(interacaoIAService.listar(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Buscar interação por ID (visão clínica)")
    public ResponseEntity<InteracaoIAResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(interacaoIAService.buscarPorId(id));
    }

    @GetMapping("/pet/{petId}")
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Histórico de interações de um pet próprio (mais recentes primeiro)")
    public ResponseEntity<List<InteracaoIAResponse>> listarPorPet(@PathVariable Long petId) {
        return ResponseEntity.ok(interacaoIAService.listarPorPet(petId));
    }
}
