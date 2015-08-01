SDK="/home/user/Documents/ext-6.0.0"

cd $(dirname $0)/../web

sencha compile --classpath=app.js,app,$SDK/packages/core/src,$SDK/packages/core/overrides,$SDK/classic/classic/src,$SDK/classic/classic/overrides \
       exclude -all \
       and \
       include -recursive -file app.js \
       and \
       exclude -namespace=Ext \
       and \
       concatenate -closure app.min.js
