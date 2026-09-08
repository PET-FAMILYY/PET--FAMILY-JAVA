package br.com.fiap.petfamily.controller;

import br.com.fiap.petfamily.dto.request.LembreteRequest;
import br.com.fiap.petfamily.dto.response.LembreteResponse;
import br.com.fiap.petfamily.entity.Lembrete.StatusLembrete;
import br.com.fiap.petfamily.service.LembreteService;
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
@RequestMapping("/lembretes")
@RequiredArgsConstructor
@Tag(name = "Lembretes", description = "Cuidados preventivos de saúde para pets")
public class LembreteController {

    private final LembreteService lembreteService;

    @PostMapping
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Veterinário define um cuidado preventivo para um pet")
    @ApiResponse(responseCode = "201", description = "Cuidado criado com sucesso")
    public ResponseEntity<LembreteResponse> criar(@Valid @RequestBody LembreteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lembreteService.criar(request));
    }

    @GetMapping
    @Operation(summary = "Listar cuidados (tutor vê só os dos próprios pets; veterinário vê todos)")
    public ResponseEntity<Page<LembreteResponse>> listar(
            @RequestParam(required = false) StatusLembrete status,
            @PageableDefault(size = 10, sort = "dataLembrete") Pageable pageable) {
        return ResponseEntity.ok(lembreteService.listar(status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cuidado por ID")
    public ResponseEntity<LembreteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(lembreteService.buscarPorId(id));
    }

    @GetMapping("/pet/{petId}/pendentes")
    @Operation(summary = "Listar cuidados pendentes de um pet específico")
    public ResponseEntity<List<LembreteResponse>> listarPendentesPorPet(@PathVariable Long petId) {
        return ResponseEntity.ok(lembreteService.listarPendentesPorPet(petId));
    }

    @GetMapping("/meus")
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Agenda de cuidados de todos os pets do tutor autenticado")
    public ResponseEntity<List<LembreteResponse>> meusCuidados() {
        return ResponseEntity.ok(lembreteService.listarDoTutorAutenticado());
    }

    @PostMapping("/{id}/concluir")
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Tutor confirma a execução do cuidado; gera a próxima ocorrência se recorrente")
    public ResponseEntity<LembreteResponse> concluir(@PathVariable Long id) {
        return ResponseEntity.ok(lembreteService.concluir(id));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Veterinário cancela um cuidado ainda pendente")
    public ResponseEntity<LembreteResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(lembreteService.cancelar(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Atualizar dados do cuidado (não altera status)")
    public ResponseEntity<LembreteResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody LembreteRequest request) {
        return ResponseEntity.ok(lembreteService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Remover cuidado")
    @ApiResponse(responseCode = "204", description = "Cuidado removido com sucesso")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        lembreteService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
