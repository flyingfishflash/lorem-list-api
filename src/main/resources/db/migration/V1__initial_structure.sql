create schema if not exists public;

create table item (
  id uuid not null,
  name varchar(64) not null,
  description varchar(2048),
  owner varchar(64) not null,
  created timestamp with time zone not null,
  creator varchar(64) not null,
  updated timestamp with time zone not null,
  updater varchar(64) not null,
  primary key (id)
);

create table list (
  id uuid not null,
  name varchar(64) not null,
  description varchar(2048),
  public boolean not null,
  owner varchar(64) not null,
  created timestamp with time zone not null,
  creator varchar(64) not null,
  updated timestamp with time zone not null,
  updater varchar(64) not null,
  primary key (id)
);

create table list_item (
  item_id uuid not null,
  list_id uuid not null,
  item_quantity bigint not null,
  item_is_suppressed boolean not null,
  primary key (list_id, item_id)
);

create unique index item_id on item (id);
create unique index list_id on list (id);

alter table
  if exists list_item
add
  constraint fk_item_id foreign key (item_id) references item;

alter table
  if exists list_item
add
  constraint fk_list_id foreign key (list_id) references list;
