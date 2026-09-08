package br.com.fiap.petfamily.controller;

import br.com.fiap.petfamily.dto.response.DashboardResponse;
import br.com.fiap.petfamily.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Métricas e resumo clínico da plataforma Pet Family")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/resumo")
    @PreAuthorize("hasRole('VETERINARIO')")
    @Operation(summary = "Obter resumo geral com KPIs da plataforma",
               description = "Retorna contadores de tutores, pets, consultas, lembretes pendentes, " +
                             "interações com IA e taxa de adesão preventiva calculada.")
    public ResponseEntity<DashboardResponse> resumo() {
        return ResponseEntity.ok(dashboardService.gerarResumo());
    }
}
