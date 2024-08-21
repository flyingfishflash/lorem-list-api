create schema if not exists public;

create table item (
  id uuid not null,
  name varchar(64) not null,
  description varchar(2048),
  quantity bigint,
  created timestamp with time zone not null,
  created_by varchar(64) not null,
  updated timestamp with time zone not null,
  updated_by varchar(64) not null,
  primary key (id)
);

create table list (
  id uuid not null,
  name varchar(64) not null,
  description varchar(2048),
  public boolean not null,
  created timestamp with time zone not null,
  created_by varchar(64) not null,
  updated timestamp with time zone not null,
  updated_by varchar(64) not null,
  primary key (id)
);

create table lists_items (
  id uuid not null,
  item_id uuid not null,
  list_id uuid not null,
  unique (item_id, list_id),
  primary key (id)
);

create unique index item_id on item (id);
create unique index list_id on list (id);

alter table
  if exists lists_items
add
  constraint fk_item_id foreign key (item_id) references item;

alter table
  if exists lists_items
add
  constraint fk_list_id foreign key (list_id) references list;
