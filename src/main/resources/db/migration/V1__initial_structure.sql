create schema if not exists public;

create sequence item_sequence start with 1 increment by 1;
create sequence list_sequence start with 1 increment by 1;

create table item (
  created timestamp with time zone not null,
  id bigserial not null,
  uuid uuid not null,
  quantity bigint,
  description varchar(2048),
  name varchar(64) not null,
  primary key (id)
);

create table list (
  created timestamp with time zone not null,
  id bigserial not null,
  uuid uuid not null,
  description varchar(2048),
  name varchar(64) not null,
  primary key (id)
);

create table lists_items (
  item_id bigint not null,
  list_id bigint not null,
  primary key (item_id, list_id)
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
