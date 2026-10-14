-- ============================================================
-- V5 — Regras de integridade no próprio banco (defesa em profundidade).
-- A API já valida via Bean Validation, mas o banco passa a recusar
-- dados inválidos mesmo que alguém grave por outro caminho
-- (script, outro serviço, console SQL).
-- ============================================================
ALTER TABLE pets
    ADD CONSTRAINT ck_pets_idade CHECK (idade IS NULL OR idade >= 0);

ALTER TABLE pets
    ADD CONSTRAINT ck_pets_peso CHECK (peso IS NULL OR peso > 0);

ALTER TABLE lembretes
    ADD CONSTRAINT ck_lembretes_recorrencia CHECK (recorrencia_dias IS NULL OR recorrencia_dias >= 1);

-- Consulta AGENDADA sempre precisa de horário (é ele que reserva o slot).
ALTER TABLE consultas
    ADD CONSTRAINT ck_consultas_agendada_horario CHECK (status <> 'AGENDADA' OR horario IS NOT NULL);