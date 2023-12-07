INSERT INTO "devise" (id, code, name, symbol)
SELECT id, code, name, symbol
FROM (VALUES
    ('1', 'ARI', 'Ariary', 'Ar'),
    ('2', 'EUR', 'Euro', '€'),
    ('3', 'USD', 'US Dollar', '$')
) AS data(id, code, name, symbol)
WHERE NOT EXISTS (SELECT 1 FROM "devise" WHERE id = data.id);