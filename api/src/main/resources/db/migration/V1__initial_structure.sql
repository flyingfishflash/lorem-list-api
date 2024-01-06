create schema if not exists public;

create sequence item_seq start with 1 increment by 1;
create sequence list_seq start with 1 increment by 1;

create table item (
  createdInstant timestamp(6) with time zone,
  id bigint not null,
  quantity bigint,
  details varchar(255),
  name varchar(255),
  primary key (id)
);

create table list (
  createdInstant timestamp(6) with time zone,
  id bigint not null,
  description varchar(255),
  name varchar(255),
  primary key (id)
);

create table lists_items (
  item_id bigint not null,
  list_id bigint not null,
  primary key (item_id, list_id)
);

alter table
  if exists lists_items
add
  constraint fk_item_id foreign key (item_id) references item;

alter table
  if exists lists_items
add
  constraint fk_list_id foreign key (list_id) references list;
