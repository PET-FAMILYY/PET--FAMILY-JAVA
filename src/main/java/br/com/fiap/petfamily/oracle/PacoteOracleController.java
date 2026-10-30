package br.com.fiap.petfamily.oracle;

import br.com.fiap.petfamily.oracle.OracleDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints que executam as procedures/funções da package PKG_PETFAMILY_S4.
 * Só existem com SPRING_PROFILES_ACTIVE=oracle. Restritos ao VETERINARIO, pois as
 * procedures não aplicam a checagem de posse (tutor x pet) que os services JPA aplicam.
 */
@RestController
@RequestMapping("/oracle")
@Profile("oracle")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VETERINARIO')")
@Tag(name = "Oracle (package PL/SQL)", description = "Chamadas às procedures e funções da PKG_PETFAMILY_S4")
public class PacoteOracleController {

    private final PacoteOracleService service;

    @PostMapping("/tutores")
    @Operation(summary = "pr_cadastrar_tutor")
    public ResponseEntity<IdResponse> cadastrarTutor(@Valid @RequestBody TutorOracleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(service.cadastrarTutor(request)));
    }

    @PostMapping("/pets")
    @Operation(summary = "pr_cadastrar_pet")
    public ResponseEntity<IdResponse> cadastrarPet(@Valid @RequestBody PetOracleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(service.cadastrarPet(request)));
    }

    @PostMapping("/consultas")
    @Operation(summary = "pr_agendar_consulta")
    public ResponseEntity<IdResponse> agendar(@Valid @RequestBody AgendamentoOracleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(service.agendarConsulta(request)));
    }

    @PatchMapping("/consultas/{id}/status")
    @Operation(summary = "pr_atualizar_status (AGENDADA, REALIZADA ou CANCELADA)")
    public ResponseEntity<Void> atualizarStatus(@PathVariable Long id, @Valid @RequestBody StatusOracleRequest request) {
        service.atualizarStatus(id, request.status());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tutores/{id}/quantidade-pets")
    @Operation(summary = "fn_contar_pets")
    public ResponseEntity<QuantidadeResponse> contarPets(@PathVariable Long id) {
        return ResponseEntity.ok(new QuantidadeResponse(id, service.contarPets(id)));
    }

    @GetMapping("/status-valido")
    @Operation(summary = "fn_status_valido")
    public ResponseEntity<StatusValidoResponse> statusValido(@RequestParam String status) {
        return ResponseEntity.ok(new StatusValidoResponse(status, service.statusValido(status)));
    }

    @PostMapping(value = "/exportar/{colecao}")
    @Operation(summary = "pr_exportar_json: grava json/<colecao>.json (TUTORES, CONSULTAS ou INTERACOES_IA)")
    public ResponseEntity<ExportacaoResponse> exportar(@PathVariable String colecao) {
        return ResponseEntity.ok(service.exportarParaArquivo(colecao));
    }

    @PostMapping("/exportar")
    @Operation(summary = "pr_exportar_json para as três coleções de uma vez")
    public ResponseEntity<List<ExportacaoResponse>> exportarTudo() {
        return ResponseEntity.ok(List.of(
                service.exportarParaArquivo("TUTORES"),
                service.exportarParaArquivo("CONSULTAS"),
                service.exportarParaArquivo("INTERACOES_IA")));
    }

    @GetMapping(value = "/exportar/{colecao}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "pr_exportar_json: devolve o JSON sem gravar arquivo")
    public ResponseEntity<String> visualizar(@PathVariable String colecao) {
        return ResponseEntity.ok(service.exportarJson(colecao));
    }
}