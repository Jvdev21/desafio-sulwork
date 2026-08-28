ALTER TABLE cafe_da_manha
    ALTER COLUMN quantidade_itens_por_colaborador DROP NOT NULL;

COMMENT ON COLUMN cafe_da_manha.quantidade_itens_por_colaborador IS
    'Limite opcional de itens diferentes por colaborador; NULL representa ausencia de limite.';
