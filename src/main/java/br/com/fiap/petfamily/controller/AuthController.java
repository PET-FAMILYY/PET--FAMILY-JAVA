package br.com.fiap.petfamily.controller;

import br.com.fiap.petfamily.dto.request.LoginRequest;
import br.com.fiap.petfamily.dto.request.RegistroRequest;
import br.com.fiap.petfamily.dto.response.LoginResponse;
import br.com.fiap.petfamily.dto.response.UsuarioResponse;
import br.com.fiap.petfamily.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Cadastro, login e sessão do usuário autenticado")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registrar")
    @Operation(summary = "Cadastro público — sempre cria um usuário TUTOR")
    @ApiResponse(responseCode = "201", description = "Conta criada e sessão iniciada")
    public ResponseEntity<LoginResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login por e-mail e senha, retorna token Bearer com expiração")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado (a partir do token enviado)")
    public ResponseEntity<UsuarioResponse> me() {
        return ResponseEntity.ok(authService.me());
    }
}
