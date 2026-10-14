-- ============================================================
-- V6 — Auditoria do ciclo de vida da consulta.
-- Passa a registrar quando a consulta foi criada e QUEM (veterinário)
-- e QUANDO registrou o atendimento — antes só ficava o status.
-- ============================================================
ALTER TABLE consultas ADD COLUMN criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE consultas ADD COLUMN realizada_em TIMESTAMP;
ALTER TABLE consultas ADD COLUMN realizada_por_usuario_id BIGINT;

ALTER TABLE consultas
    ADD CONSTRAINT fk_consultas_realizada_por FOREIGN KEY (realizada_por_usuario_id) REFERENCES usuarios (id);

CREATE INDEX idx_consultas_realizada_por ON consultas (realizada_por_usuario_id);