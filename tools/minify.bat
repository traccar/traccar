@echo off
cd C:\Users\Erez\Documents\traccar\web
set SDK=C:\inetpub\wwwroot\ext-6.0.0

sencha -sdk %SDK% compile -classpath=app.js,app,%SDK%\packages\core\src,%SDK%\packages\core\overrides,%SDK%\classic\classic\src,%SDK%\classic\classic\overrides exclude -all and include -recursive -file app.js and exclude -namespace=Ext and concatenate -closure app.min.js
