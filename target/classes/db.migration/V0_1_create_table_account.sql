CREATE TABLE IF NOT EXISTS "accounts" (
    id            varchar
                              constraint accounts_pk primary key default uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    devise_id VARCHAR NOT NULL REFERENCES "devise"("id")
);
create index if not exists accounts_name_index on "accounts" (name);
