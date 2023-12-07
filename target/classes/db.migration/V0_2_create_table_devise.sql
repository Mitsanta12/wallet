CREATE TABLE IF NOT EXISTS "devise" (
    id            varchar
                              constraint devise_pk primary key default uuid_generate_v4(),
    code VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    symbol VARCHAR(10) NOT NULL
);
create index if not exists devise_name_index on "devise" (name);