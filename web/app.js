Ext.application({
    name   : 'Traccar',

    launch : function() {

       Ext.create('Ext.Panel', {
            renderTo     : Ext.getBody(),
            width        : 200,
            height       : 150,
            bodyPadding  : 5,
            title        : 'Hello World',
            html         : 'Hello <b>World</b>...'
        });

    }
});
