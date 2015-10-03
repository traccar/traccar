#!/bin/sh

cd $(dirname $0)/../web

jshint --config ../tools/jshint.json .
