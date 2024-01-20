#!/bin/sh

exitcode=0

case $SONAR_HOST in
  (*[![:blank:]]*)
    echo '$SONAR_HOST is not blank';;
  (*)
    echo '$SONAR_HOST contains only blanks or is empty or is unset'
    exitcode=1;;
esac

case $SONAR_TOKEN in
  (*[![:blank:]]*)
    echo '$SONAR_TOKEN is not blank';;
  (*)
    echo '$SONAR_TOKEN contains only blanks or is empty or is unset'
    exitcode=1;;
esac

exit $exitcode