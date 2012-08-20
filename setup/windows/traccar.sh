#!/bin/sh

tar -xzf wrapper-delta-pack-*.tar.gz
mv wrapper-delta-pack-*/ wrapper/

innoextract isetup-*.exe

wine app/ISCC.exe traccar.iss
mv Output/setup.exe .

zip traccar-windows-32.zip setup.exe README.txt

rm setup.exe
rm -rf Output
rm -rf wrapper
rm -rf app
rm -rf tmp

