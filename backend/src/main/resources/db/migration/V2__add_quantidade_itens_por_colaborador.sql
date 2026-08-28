ALTER TABLE cafe_da_manha
    ADD COLUMN quantidade_itens_por_colaborador INTEGER NOT NULL DEFAULT 1;

ALTER TABLE cafe_da_manha
    ADD CONSTRAINT ck_cafe_quantidade_itens_por_colaborador
        CHECK (quantidade_itens_por_colaborador BETWEEN 1 AND 10);

ALTER TABLE cafe_da_manha
    ALTER COLUMN quantidade_itens_por_colaborador DROP DEFAULT;

COMMENT ON COLUMN cafe_da_manha.quantidade_itens_por_colaborador IS
    'Quantidade exata de itens diferentes que cada colaborador deve escolher neste cafe.';
