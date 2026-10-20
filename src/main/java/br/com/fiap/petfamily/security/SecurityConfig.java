package br.com.fiap.petfamily.security;

import br.com.fiap.petfamily.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    // Só é true no perfil "dev" (application-dev.properties). Em prod/test a
    // rota /h2-console NÃO é liberada: cai em anyRequest().authenticated().
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleHabilitado;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        if ("*".equals(corsAllowedOrigins.trim())) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(List.of(corsAllowedOrigins.split(",")));
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                escreverErro(response, mapper, HttpStatus.UNAUTHORIZED,
                                        "Não autenticado", "Token ausente, inválido ou expirado.", request.getRequestURI()))
                        .accessDeniedHandler((request, response, ex) ->
                                escreverErro(response, mapper, HttpStatus.FORBIDDEN,
                                        "Acesso negado", "Você não tem permissão para executar esta ação.", request.getRequestURI()))
                )
                .authorizeHttpRequests(auth -> {
                    // AntPathRequestMatcher evita a ambiguidade MVC x servlet do H2 Console
                    auth.requestMatchers(AntPathRequestMatcher.antMatcher("/actuator/health/**")).permitAll();
                    auth.requestMatchers(AntPathRequestMatcher.antMatcher("/auth/login"),
                            AntPathRequestMatcher.antMatcher("/auth/registrar")).permitAll();
                    auth.requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-ui/**"),
                            AntPathRequestMatcher.antMatcher("/api-docs/**"),
                            AntPathRequestMatcher.antMatcher("/v3/api-docs/**")).permitAll();
                    if (h2ConsoleHabilitado) {
                        auth.requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                // frameOptions(sameOrigin) só é necessário para o H2 Console (usa <frame>);
                // com ele desligado mantém o padrão seguro (DENY).
                .headers(headers -> {
                    if (h2ConsoleHabilitado) {
                        headers.frameOptions(frame -> frame.sameOrigin());
                    }
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void escreverErro(HttpServletResponse response, ObjectMapper mapper, HttpStatus status,
                              String error, String message, String path) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(mapper.writeValueAsString(ErrorResponse.of(status, error, message, path)));
    }
}