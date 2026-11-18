-- Content editors. Visitors never have accounts; only people who manage content do.
create table admin_user (
    id             bigint generated always as identity primary key,
    email          varchar(254) not null,
    password_hash  varchar(100) not null,
    display_name   varchar(100) not null,
    role           varchar(20)  not null check (role in ('ADMIN', 'EDITOR')),
    enabled        boolean      not null default true,
    created_at     timestamptz  not null default now(),
    last_login_at  timestamptz
);

create unique index uq_admin_user_email on admin_user (lower(email));

-- Logout for stateless tokens: a revoked token id (jti) is rejected until it would have expired anyway.
create table revoked_token (
    jti        varchar(64) primary key,
    expires_at timestamptz not null
);

create index idx_revoked_token_expires on revoked_token (expires_at);

-- Optimistic locking: two editors saving the same destination must not silently overwrite each other.
alter table destination add column version bigint not null default 0;
