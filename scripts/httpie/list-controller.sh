#!/bin/sh

HOST=http://localhost:8282
CONTEXT=api/v1
RESOURCE=lists
BASE_URL=$HOST/$CONTEXT/$RESOURCE

clear
echo
echo "--------------------------------"
printf "base url: $BASE_URL\n"
echo "--------------------------------"
echo

http get $BASE_URL --unsorted -v
http get $BASE_URL/1 --unsorted -v
http post $BASE_URL name="my list name" description="my list description" --unsorted -v
http get $BASE_URL/1 --unsorted -v
http delete $BASE_URL/1 --unsorted -v
