package br.com.fiap.petfamily.controller;

import br.com.fiap.petfamily.dto.request.TutorRequest;
import br.com.fiap.petfamily.dto.response.TutorResponse;
import br.com.fiap.petfamily.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tutores")
@RequiredArgsConstructor
@Tag(name = "Tutores", description = "Gerenciamento do cadastro de tutores")
public class TutorController {

    private final TutorService tutorService;

    @GetMapping
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Listar tutores com paginação e filtro por nome (acesso clínico)")
    public ResponseEntity<Page<TutorResponse>> listar(
            @RequestParam(required = false) String nome,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(tutorService.listar(nome, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tutor por ID (o próprio tutor, ou veterinário)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tutor encontrado"),
        @ApiResponse(responseCode = "404", description = "Tutor não encontrado")
    })
    public ResponseEntity<TutorResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tutorService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Tutor atualiza o próprio cadastro")
    public ResponseEntity<TutorResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody TutorRequest request) {
        return ResponseEntity.ok(tutorService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Tutor exclui a própria conta (remove pets, consultas e cuidados associados)")
    @ApiResponse(responseCode = "204", description = "Conta removida com sucesso")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        tutorService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
