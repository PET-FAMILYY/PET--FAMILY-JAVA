-- ============================================================
-- V2 (Oracle): paridade com a V7 do Java, índices da V4 e
-- normalização de HORARIO (Oracle não tem tipo TIME).
-- ============================================================

-- 1) FKs com ON DELETE SET NULL (equivalente à V7 do Java)
DECLARE
PROCEDURE recriar_fk(p_tabela VARCHAR2, p_coluna VARCHAR2, p_nome VARCHAR2, p_ref VARCHAR2) IS
    v_fk VARCHAR2(128);
BEGIN
SELECT cc.constraint_name INTO v_fk
FROM user_constraints c
         JOIN user_cons_columns cc ON cc.constraint_name = c.constraint_name
WHERE c.table_name = UPPER(p_tabela) AND c.constraint_type = 'R'
  AND cc.column_name = UPPER(p_coluna) AND ROWNUM = 1;
EXECUTE IMMEDIATE 'ALTER TABLE ' || p_tabela || ' DROP CONSTRAINT ' || v_fk;
EXECUTE IMMEDIATE 'ALTER TABLE ' || p_tabela || ' ADD CONSTRAINT ' || p_nome ||
                  ' FOREIGN KEY (' || p_coluna || ') REFERENCES ' || p_ref || ' ON DELETE SET NULL';
END;
BEGIN
  recriar_fk('lembretes', 'criado_por_usuario_id',    'fk_lembretes_criado_por',    'usuarios(id)');
  recriar_fk('lembretes', 'concluido_por_usuario_id', 'fk_lembretes_concluido_por', 'usuarios(id)');
  recriar_fk('consultas', 'realizada_por_usuario_id', 'fk_consultas_realizada_por', 'usuarios(id)');
END;
/

-- 2) Índices de desempenho (V4 do Java) que faltavam
CREATE INDEX idx_consultas_status_data   ON consultas (status, data);
CREATE INDEX idx_lembretes_status_data   ON lembretes (status, data_lembrete);
CREATE INDEX idx_lembretes_criado_por    ON lembretes (criado_por_usuario_id);
CREATE INDEX idx_lembretes_concluido_por ON lembretes (concluido_por_usuario_id);
CREATE INDEX idx_lembretes_origem        ON lembretes (origem_lembrete_id);
CREATE INDEX idx_consultas_realizada_por ON consultas (realizada_por_usuario_id);
CREATE INDEX idx_interacoes_data_hora    ON interacoes_ia (data_hora);
CREATE INDEX idx_usuarios_role           ON usuarios (role);

-- 3) Normaliza HORARIO para 1970-01-01 HH:MI:SS, qualquer que seja a origem
--    (Java/Hibernate ou PL/SQL). Assim a UNIQUE (data, horario) vale para os dois.
CREATE OR REPLACE TRIGGER trg_pf_norm_horario_consultas
BEFORE INSERT OR UPDATE OF horario ON consultas
    FOR EACH ROW
    WHEN (NEW.horario IS NOT NULL)
BEGIN
  :NEW.horario := TO_TIMESTAMP('1970-01-01 ' || TO_CHAR(:NEW.horario, 'HH24:MI:SS'),
                               'YYYY-MM-DD HH24:MI:SS');
END;
/

CREATE OR REPLACE TRIGGER trg_pf_norm_horario_slots
BEFORE INSERT OR UPDATE OF horario ON consulta_slots
    FOR EACH ROW
BEGIN
  :NEW.horario := TO_TIMESTAMP('1970-01-01 ' || TO_CHAR(:NEW.horario, 'HH24:MI:SS'),
                               'YYYY-MM-DD HH24:MI:SS');
END;
/

-- 4) Normaliza linhas que já existirem
UPDATE consultas SET horario = horario WHERE horario IS NOT NULL;
UPDATE consulta_slots SET horario = horario;
COMMIT;