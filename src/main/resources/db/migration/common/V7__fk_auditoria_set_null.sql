ALTER TABLE lembretes DROP CONSTRAINT fk_lembretes_criado_por;
ALTER TABLE lembretes
    ADD CONSTRAINT fk_lembretes_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuarios (id) ON DELETE SET NULL;

ALTER TABLE lembretes DROP CONSTRAINT fk_lembretes_concluido_por;
ALTER TABLE lembretes
    ADD CONSTRAINT fk_lembretes_concluido_por
        FOREIGN KEY (concluido_por_usuario_id) REFERENCES usuarios (id) ON DELETE SET NULL;

ALTER TABLE consultas DROP CONSTRAINT fk_consultas_realizada_por;
ALTER TABLE consultas
    ADD CONSTRAINT fk_consultas_realizada_por
        FOREIGN KEY (realizada_por_usuario_id) REFERENCES usuarios (id) ON DELETE SET NULL;