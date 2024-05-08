create schema if not exists public;

create sequence item_sequence start with 1 increment by 1;
create sequence list_sequence start with 1 increment by 1;

create table item (
  id bigserial not null,
  uuid uuid not null,
  name varchar(64) not null,
  description varchar(2048),
  quantity bigint,
  created timestamp with time zone not null,
  updated timestamp with time zone not null,
  primary key (id)
);

create table list (
  id bigserial not null,
  uuid uuid not null,
  name varchar(64) not null,
  description varchar(2048),
  created timestamp with time zone not null,
  updated timestamp with time zone not null,
  primary key (id)
);

create table lists_items (
  uuid uuid not null,
  item_id bigint not null,
  list_id bigint not null,
  unique (item_id, list_id),
  primary key (uuid)
);

create unique index item_uuid on item (uuid);
create unique index list_uuid on list (uuid);

alter table
  if exists lists_items
add
  constraint fk_item_id foreign key (item_id) references item;

alter table
  if exists lists_items
add
  constraint fk_list_id foreign key (list_id) references list;
