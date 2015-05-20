#!/bin/bash

jars=''
for jar in `ls lib`;do jars=$jars"lib/$jar:";done
java -cp $jars docs.GoogleDocMigrationDemo $@