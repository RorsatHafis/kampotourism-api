-- The content tables are shared by the editorial modules.  They deliberately keep
-- structured relationships (category, media, itinerary stops) relational while
-- allowing rich text blocks to evolve as JSON.
alter table category drop constraint if exists category_kind_check;
alter table category add constraint category_kind_check check
  (kind in ('DESTINATION', 'EXPERIENCE', 'STAY_TYPE', 'TRANSPORT', 'BLOG', 'TRAVEL_INFO'));

alter table media_attachment drop constraint if exists media_attachment_owner_type_check;
alter table media_attachment add constraint media_attachment_owner_type_check check
  (owner_type in ('DESTINATION', 'EXPERIENCE', 'HOTEL', 'TRANSPORT', 'BLOG',
                  'ACCOMMODATION', 'TRANSPORTATION', 'ARTICLE', 'TRAVEL_INFO', 'ITINERARY'));

create table content_item (
  id bigint generated always as identity primary key,
  type varchar(20) not null check (type in ('EXPERIENCE','ACCOMMODATION','TRANSPORTATION','ARTICLE','TRAVEL_INFO')),
  slug varchar(120) not null,
  category_id bigint references category(id),
  status varchar(12) not null default 'DRAFT' check (status in ('DRAFT','PUBLISHED','ARCHIVED')),
  featured boolean not null default false,
  sort_order integer not null default 0,
  address varchar(500), latitude double precision, longitude double precision,
  price numeric(10,2), currency varchar(3), pricing_unit varchar(60),
  contact_phone varchar(80), contact_email varchar(254), external_url varchar(1000),
  metadata jsonb not null default '{}'::jsonb,
  published_at timestamptz, version bigint not null default 0,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
  constraint uq_content_item_type_slug unique(type, slug),
  constraint chk_content_item_coords check ((latitude is null) = (longitude is null)),
  constraint chk_content_item_lat check (latitude is null or latitude between -90 and 90),
  constraint chk_content_item_lng check (longitude is null or longitude between -180 and 180),
  constraint chk_content_item_currency check (currency is null or currency ~ '^[A-Z]{3}$')
);
create index idx_content_item_public on content_item(type, status, featured, sort_order, id);
create index idx_content_item_category on content_item(type, category_id, status);

create table content_item_translation (
  id bigint generated always as identity primary key,
  content_id bigint not null references content_item(id) on delete cascade,
  lang varchar(10) not null, title varchar(250) not null, excerpt varchar(700),
  body jsonb not null default '[]'::jsonb, highlights jsonb not null default '[]'::jsonb,
  tips jsonb not null default '[]'::jsonb, seo_title varchar(250), seo_description varchar(500),
  confirmed_information boolean not null default true,
  constraint uq_content_item_translation unique(content_id, lang)
);

create table itinerary (
  id bigint generated always as identity primary key, slug varchar(120) not null unique,
  status varchar(12) not null default 'DRAFT' check (status in ('DRAFT','PUBLISHED','ARCHIVED')),
  featured boolean not null default false, sort_order integer not null default 0,
  days_count integer not null default 1 check(days_count between 1 and 60), published_at timestamptz,
  version bigint not null default 0, created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create index idx_itinerary_public on itinerary(status, featured, sort_order, id);
create table itinerary_translation (
  id bigint generated always as identity primary key, itinerary_id bigint not null references itinerary(id) on delete cascade,
  lang varchar(10) not null, title varchar(250) not null, description jsonb not null default '[]'::jsonb,
  seo_title varchar(250), seo_description varchar(500), constraint uq_itinerary_translation unique(itinerary_id, lang)
);
create table itinerary_day (
  id bigint generated always as identity primary key, itinerary_id bigint not null references itinerary(id) on delete cascade,
  day_number integer not null check(day_number between 1 and 60), constraint uq_itinerary_day unique(itinerary_id, day_number)
);
create table itinerary_activity (
  id bigint generated always as identity primary key, day_id bigint not null references itinerary_day(id) on delete cascade,
  sort_order integer not null default 0, destination_id bigint references destination(id), experience_id bigint references content_item(id),
  constraint chk_itinerary_activity_target check ((destination_id is null) <> (experience_id is null))
);
create index idx_itinerary_activity_day on itinerary_activity(day_id, sort_order, id);

create table site_setting (
  setting_key varchar(80) primary key, value jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now(), version bigint not null default 0
);
insert into site_setting(setting_key, value) values ('homepage', '{}'::jsonb) on conflict do nothing;
