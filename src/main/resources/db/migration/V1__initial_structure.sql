create schema if not exists public;

create table item (
  uuid uuid not null,
  name varchar(64) not null,
  description varchar(2048),
  quantity bigint,
  created timestamp with time zone not null,
  updated timestamp with time zone not null,
  primary key (uuid)
);

create table list (
  uuid uuid not null,
  name varchar(64) not null,
  description varchar(2048),
  created timestamp with time zone not null,
  updated timestamp with time zone not null,
  primary key (uuid)
);

create table lists_items (
  uuid uuid not null,
  item_uuid uuid not null,
  list_uuid uuid not null,
  unique (item_uuid, list_uuid),
  primary key (uuid)
);

create unique index item_uuid on item (uuid);
create unique index list_uuid on list (uuid);

alter table
  if exists lists_items
add
  constraint fk_item_uuid foreign key (item_uuid) references item;

alter table
  if exists lists_items
add
  constraint fk_list_uuid foreign key (list_uuid) references list;
