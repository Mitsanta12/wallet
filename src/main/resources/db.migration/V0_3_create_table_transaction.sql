CREATE TABLE IF NOT EXISTS "transaction" (
    id            varchar
                              constraint transaction_pk primary key default uuid_generate_v4(),
    account_id VARCHAR NOT NULL REFERENCES "accounts"("id"),
    amount INTEGER NOT NULL,
    transaction_name VARCHAR(255) NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
    create index if not exists amount_index on "transaction" (amount);