-- ============================================================
-- V3 — Corrige a FK de recorrência de lembretes.
--
-- Problema: lembretes.origem_lembrete_id apontava para lembretes(id)
-- SEM regra de exclusão. Ao excluir um pet (ou a conta do tutor) que
-- tinha um cuidado recorrente já concluído, o Hibernate apaga os
-- lembretes em ordem não garantida: se apagasse o "pai" antes do
-- "filho" (a próxima ocorrência), o banco rejeitava com violação de FK.
--
-- Correção: ao apagar o lembrete de origem, o vínculo do filho vira NULL.
-- ============================================================
ALTER TABLE lembretes DROP CONSTRAINT fk_lembretes_origem;

ALTER TABLE lembretes
    ADD CONSTRAINT fk_lembretes_origem
        FOREIGN KEY (origem_lembrete_id) REFERENCES lembretes (id) ON DELETE SET NULL;