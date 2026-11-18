-- Language-neutral facts live in "destination"; everything a visitor reads lives
-- in "destination_translation" so new languages need rows, not schema changes.

create table destination (
    id                   bigint generated always as identity primary key,
    slug                 varchar(120) not null unique,
    category             varchar(30)  not null
        check (category in ('NATURE', 'TOWN', 'FOOD', 'WATER', 'CULTURE')),
    image_url            varchar(500),
    latitude             double precision,
    longitude            double precision,
    entrance_fee         numeric(10, 2),
    entrance_fee_currency varchar(3),
    featured             boolean      not null default false,
    published            boolean      not null default true,
    sort_order           integer      not null default 0,
    created_at           timestamptz  not null default now(),
    updated_at           timestamptz  not null default now(),
    constraint chk_destination_coords
        check ((latitude is null) = (longitude is null)),
    constraint chk_destination_lat check (latitude  is null or latitude  between -90  and 90),
    constraint chk_destination_lng check (longitude is null or longitude between -180 and 180)
);

create index idx_destination_listing on destination (published, category, sort_order);

create table destination_translation (
    id                 bigint generated always as identity primary key,
    destination_id     bigint       not null references destination (id) on delete cascade,
    lang               varchar(10)  not null,
    name               varchar(200) not null,
    tag                varchar(200),
    summary            varchar(500) not null,
    lead               text         not null,
    description        jsonb        not null default '[]'::jsonb,
    highlights         jsonb        not null default '[]'::jsonb,
    tips               jsonb        not null default '[]'::jsonb,
    image_alt          varchar(300),
    distance           varchar(120),
    duration           varchar(120),
    best_for           varchar(200),
    bring              varchar(200),
    opening_hours      varchar(200),
    best_season        varchar(120),
    entrance_fee_note  varchar(300),
    constraint uq_destination_translation unique (destination_id, lang)
);
