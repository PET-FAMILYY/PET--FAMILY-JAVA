package br.com.fiap.petfamily.controller;

import br.com.fiap.petfamily.dto.request.PetRequest;
import br.com.fiap.petfamily.dto.response.PetResponse;
import br.com.fiap.petfamily.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
@Tag(name = "Pets", description = "Gerenciamento de pets e seus dados clínicos")
public class PetController {

    private final PetService petService;

    @PostMapping
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Tutor cadastra um novo pet próprio")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pet cadastrado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<PetResponse> criar(@Valid @RequestBody PetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.criar(request));
    }

    @GetMapping
    @Operation(summary = "Listar pets com filtros por tutorId ou espécie")
    public ResponseEntity<Page<PetResponse>> listar(
            @RequestParam(required = false) Long tutorId,
            @RequestParam(required = false) String especie,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(petService.listar(tutorId, especie, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pet por ID com resumo clínico")
    public ResponseEntity<PetResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(petService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do pet")
    public ResponseEntity<PetResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody PetRequest request) {
        return ResponseEntity.ok(petService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover pet e todos os seus registros")
    @ApiResponse(responseCode = "204", description = "Pet removido com sucesso")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        petService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
