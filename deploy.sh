#!/bin/bash

SETTINGS="/Users/weasley/Development/program/apache-maven/conf/settings-sonatype.xml"
MODULE="http-rate-limiter-spring-boot-starter"

clear &&
  mvn clean deploy -pl :$MODULE -am --settings $SETTINGS &&
  mvn clean
