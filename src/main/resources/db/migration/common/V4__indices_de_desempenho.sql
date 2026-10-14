-- ============================================================
-- V4 — Índices de desempenho.
--
-- Baseados nas consultas reais da aplicação:
--  * agenda clínica (/consultas/futuras): status + data
--  * listagem/ordenação de cuidados por status e data
--  * FKs de lembretes -> usuarios / lembretes (o PostgreSQL não cria
--    índice automático para FK; sem eles, deletes e joins varrem a tabela)
--  * login e escopo por perfil
-- ============================================================
CREATE INDEX idx_consultas_status_data ON consultas (status, data);

CREATE INDEX idx_lembretes_status_data ON lembretes (status, data_lembrete);
CREATE INDEX idx_lembretes_criado_por ON lembretes (criado_por_usuario_id);
CREATE INDEX idx_lembretes_concluido_por ON lembretes (concluido_por_usuario_id);
CREATE INDEX idx_lembretes_origem ON lembretes (origem_lembrete_id);

CREATE INDEX idx_interacoes_data_hora ON interacoes_ia (data_hora);
CREATE INDEX idx_usuarios_role ON usuarios (role);