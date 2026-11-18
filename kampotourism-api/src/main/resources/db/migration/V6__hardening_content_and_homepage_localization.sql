-- Additive production hardening: API-facing accommodation/article fields and
-- independently localized homepage configuration.
alter table content_item add column best_for varchar(200);
alter table content_item add column tags jsonb not null default '[]'::jsonb;
alter table content_item add column author_name varchar(160);
alter table content_item add column author_url varchar(1000);
create index idx_content_item_title_filters on content_item(type, status, category_id, sort_order, id);

create table site_setting_translation (
  id bigint generated always as identity primary key,
  setting_key varchar(80) not null references site_setting(setting_key) on delete cascade,
  lang varchar(10) not null,
  value jsonb not null,
  constraint uq_site_setting_translation unique(setting_key, lang)
);
insert into site_setting_translation(setting_key, lang, value)
select setting_key, 'en', value from site_setting where setting_key = 'homepage'
on conflict (setting_key, lang) do nothing;
