#!/bin/sh

HOST=http://localhost:8282
CONTEXT=api/v1
RESOURCE=items
BASE_URL=$HOST/$CONTEXT/$RESOURCE
OPTIONS="--unsorted -v"

clear
echo
echo "--------------------------------"
printf "base url: $BASE_URL\n"
echo "--------------------------------"
echo

http post $HOST/$CONTEXT/lists name="list 1" description="the first list" $OPTIONS
echo "--------------------------------"
http post $HOST/$CONTEXT/lists name="list 2" description="the second list" $OPTIONS
echo "--------------------------------"
http post $BASE_URL name="item 1" description="the first item" $OPTIONS
echo "--------------------------------"
http --raw 1 post $BASE_URL/1/add-to-list $OPTIONS
echo "--------------------------------"
http post $BASE_URL name="item 2" description="the second item" $OPTIONS
echo "--------------------------------"
http --raw 1 post $BASE_URL/2/add-to-list $OPTIONS
echo "--------------------------------"
http get $HOST/$CONTEXT/items "withLists==true" $OPTIONS
echo "--------------------------------"
http post $BASE_URL/2/move-to-list fromListId:=1 toListId:=2 $OPTIONS
echo "--------------------------------"
http get $HOST/$CONTEXT/items "withLists==true" $OPTIONS
echo "--------------------------------"
http post $BASE_URL/2/move-to-list fromListId:=1 toListId:=3 $OPTIONS
