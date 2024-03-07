#!/bin/sh

HOST=http://localhost:8282
CONTEXT=api/v1
RESOURCE=items
BASE_URL=$HOST/$CONTEXT/$RESOURCE

clear
echo
echo "--------------------------------"
printf "base url: $BASE_URL\n"
echo "--------------------------------"
echo

http post $HOST/$CONTEXT/lists name="list 1" description="the first list" --unsorted -v
http post $BASE_URL name="item 1" description="the first item" --unsorted -v
http post $BASE_URL/1/to-list '1' --unsorted -v
http --raw 1 post $BASE_URL/1/to-list --unsorted -v
http post $BASE_URL name="item 2" description="the second item" --unsorted -v
http --raw 1 post $BASE_URL/2/to-list --unsorted -v
http get $HOST/$CONTEXT/lists "withItems==true" --unsorted -v
