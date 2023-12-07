INSERT INTO "accounts" (id, name, devise_id)
SELECT id, name, devise_id
FROM (VALUES
    ('a1', 'Account1', '1'),
    ('a2', 'Account2', '2')
) AS data(id, name, devise_id)
WHERE NOT EXISTS (SELECT 1 FROM "accounts" WHERE id = data.id);