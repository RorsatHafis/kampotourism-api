-- ---------------------------------------------------------------------------
-- Categories: one table for every kind of content, so editors can manage them.
-- ---------------------------------------------------------------------------
create table category (
    id          bigint generated always as identity primary key,
    kind        varchar(30) not null
        check (kind in ('DESTINATION', 'EXPERIENCE', 'STAY_TYPE', 'TRANSPORT', 'BLOG')),
    slug        varchar(80) not null,
    sort_order  integer     not null default 0,
    version     bigint      not null default 0,
    constraint uq_category_kind_slug unique (kind, slug)
);

create table category_translation (
    id           bigint generated always as identity primary key,
    category_id  bigint       not null references category (id) on delete cascade,
    lang         varchar(10)  not null,
    name         varchar(100) not null,
    description  varchar(500),
    constraint uq_category_translation unique (category_id, lang)
);

insert into category (kind, slug, sort_order) values
 ('DESTINATION', 'nature',  1), ('DESTINATION', 'town',    2), ('DESTINATION', 'food',    3),
 ('DESTINATION', 'water',   4), ('DESTINATION', 'culture', 5),
 ('EXPERIENCE',  'adventure', 1), ('EXPERIENCE', 'food-and-drink', 2), ('EXPERIENCE', 'culture', 3),
 ('STAY_TYPE',   'hotel',   1), ('STAY_TYPE',   'resort',  2), ('STAY_TYPE',   'homestay', 3),
 ('TRANSPORT',   'moto',    1), ('TRANSPORT',   'bus',     2), ('TRANSPORT',   'car',      3),
 ('TRANSPORT',   'jet-ski', 4),
 ('BLOG',        'destinations', 1), ('BLOG', 'food', 2), ('BLOG', 'adventure', 3),
 ('BLOG',        'nature',  4), ('BLOG', 'tips', 5);

insert into category_translation (category_id, lang, name)
select c.id, 'en', v.name
from category c
join (values
 ('DESTINATION', 'nature', 'Nature'), ('DESTINATION', 'town', 'Town'), ('DESTINATION', 'food', 'Food'),
 ('DESTINATION', 'water', 'Water'), ('DESTINATION', 'culture', 'Culture'),
 ('EXPERIENCE', 'adventure', 'Adventure'), ('EXPERIENCE', 'food-and-drink', 'Food & drink'),
 ('EXPERIENCE', 'culture', 'Culture'),
 ('STAY_TYPE', 'hotel', 'Hotel'), ('STAY_TYPE', 'resort', 'Resort'), ('STAY_TYPE', 'homestay', 'Homestay'),
 ('TRANSPORT', 'moto', 'Moto'), ('TRANSPORT', 'bus', 'Bus'), ('TRANSPORT', 'car', 'Car'),
 ('TRANSPORT', 'jet-ski', 'Jet ski'),
 ('BLOG', 'destinations', 'Destinations'), ('BLOG', 'food', 'Food'), ('BLOG', 'adventure', 'Adventure'),
 ('BLOG', 'nature', 'Nature'), ('BLOG', 'tips', 'Tips')
) as v(kind, slug, name) on v.kind = c.kind and v.slug = c.slug;

-- Destinations move from the hard-coded enum column to the category table.
alter table destination add column category_id bigint references category (id);

update destination d
set category_id = (select c.id from category c
                   where c.kind = 'DESTINATION' and c.slug = lower(d.category));

alter table destination alter column category_id set not null;
alter table destination drop column category;   -- also drops its check constraint and old index
create index idx_destination_listing on destination (published, category_id, sort_order);

-- ---------------------------------------------------------------------------
-- Media: a file is an asset; alt text and captions are per language; the same
-- asset can be attached to any content type as a cover or a gallery image.
-- ---------------------------------------------------------------------------
create table media_asset (
    id                bigint generated always as identity primary key,
    storage_key       varchar(300) not null unique,
    mime_type         varchar(50)  not null,
    size_bytes        bigint       not null,
    width             integer,
    height            integer,
    original_filename varchar(255),
    version           bigint       not null default 0,
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now()
);

create table media_asset_translation (
    id        bigint generated always as identity primary key,
    asset_id  bigint      not null references media_asset (id) on delete cascade,
    lang      varchar(10) not null,
    alt_text  varchar(300),
    caption   varchar(500),
    constraint uq_media_asset_translation unique (asset_id, lang)
);

-- owner_type + owner_id is polymorphic (no FK to the owner table); the application
-- removes attachments when an owner is deleted. asset_id is a real FK, so an asset that
-- is still attached cannot be deleted by accident.
create table media_attachment (
    id          bigint generated always as identity primary key,
    owner_type  varchar(30) not null
        check (owner_type in ('DESTINATION', 'EXPERIENCE', 'HOTEL', 'TRANSPORT', 'BLOG')),
    owner_id    bigint      not null,
    asset_id    bigint      not null references media_asset (id),
    role        varchar(10) not null check (role in ('COVER', 'GALLERY')),
    sort_order  integer     not null default 0,
    constraint uq_media_attachment unique (owner_type, owner_id, asset_id)
);

create unique index uq_media_attachment_cover
    on media_attachment (owner_type, owner_id) where role = 'COVER';
create index idx_media_attachment_owner
    on media_attachment (owner_type, owner_id, role, sort_order);
create index idx_media_attachment_asset on media_attachment (asset_id);
