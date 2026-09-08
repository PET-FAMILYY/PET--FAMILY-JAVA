package br.com.fiap.petfamily.controller;

import br.com.fiap.petfamily.dto.request.AgendarConsultaRequest;
import br.com.fiap.petfamily.dto.request.RealizarConsultaRequest;
import br.com.fiap.petfamily.dto.response.ConsultaResponse;
import br.com.fiap.petfamily.entity.Consulta.StatusConsulta;
import br.com.fiap.petfamily.service.ConsultaService;
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

@RestController
@RequestMapping("/consultas")
@RequiredArgsConstructor
@Tag(name = "Consultas", description = "Agendamento e atendimento de consultas veterinárias")
public class ConsultaController {

    private final ConsultaService consultaService;

    @PostMapping("/agendar")
    @PreAuthorize("hasRole('TUTOR')")
    @Operation(summary = "Tutor agenda uma consulta para um pet próprio, em horário livre e futuro")
    @ApiResponse(responseCode = "201", description = "Consulta agendada")
    public ResponseEntity<ConsultaResponse> agendar(@Valid @RequestBody AgendarConsultaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.agendar(request));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancela uma consulta AGENDADA (tutor dono ou veterinário)")
    public ResponseEntity<ConsultaResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(consultaService.cancelar(id));
    }

    @PostMapping("/{id}/realizar")
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Veterinário registra o atendimento; consulta passa a REALIZADA")
    public ResponseEntity<ConsultaResponse> realizar(@PathVariable Long id,
                                                       @Valid @RequestBody RealizarConsultaRequest request) {
        return ResponseEntity.ok(consultaService.realizar(id, request));
    }

    @GetMapping
    @Operation(summary = "Listar consultas (tutor vê só as próprias; veterinário vê todas)")
    public ResponseEntity<Page<ConsultaResponse>> listar(
            @RequestParam(required = false) StatusConsulta status,
            @PageableDefault(size = 10, sort = "data") Pageable pageable) {
        return ResponseEntity.ok(consultaService.listar(status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar consulta por ID")
    public ResponseEntity<ConsultaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(consultaService.buscarPorId(id));
    }

    @GetMapping("/futuras")
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Listar consultas agendadas a partir de hoje (visão clínica)")
    public ResponseEntity<Page<ConsultaResponse>> listarFuturas(
            @PageableDefault(size = 10, sort = "data") Pageable pageable) {
        return ResponseEntity.ok(consultaService.listarFuturas(pageable));
    }
}
