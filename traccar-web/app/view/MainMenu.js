Ext.define('Traccar.view.MainMenu', {
    extend: 'Ext.panel.Panel',
    xtype: 'mainMenu',

    requires: [
        'Traccar.view.savekid.SavekidChildrenView',
        'Traccar.view.savekid.SavekidChildProfilePanel',
        'Traccar.view.savekid.SavekidStatusPanel'
    ],

    title: 'Menú principal',
    bodyPadding: 10,
    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    // Menú principal reutilizable. Incluimos sección SaveKID con accesos rápidos
    // a las vistas solicitadas.
    items: [{
        xtype: 'fieldset',
        title: 'SaveKID',
        defaults: {
            xtype: 'button',
            margin: '0 0 5 0',
            width: '100%'
        },
        items: [{
            text: 'Perfiles de niños',
            handler: function () {
                Ext.create('Traccar.view.savekid.SavekidChildrenView', {
                    title: 'Perfiles de niños'
                }).show();
            }
        }, {
            text: 'Estado actual',
            handler: function () {
                Ext.create('Traccar.view.savekid.SavekidStatusPanel', {
                    title: 'Estado actual',
                    autoLoad: true
                }).show();
            }
        }, {
            text: 'Historial de salud',
            handler: function () {
                Ext.create('Traccar.view.savekid.SavekidChildProfilePanel', {
                    title: 'Historial de salud',
                    showHistoryOnly: true
                }).show();
            }
        }, {
            text: 'Reportes semanales',
            handler: function () {
                Ext.create('Traccar.view.savekid.SavekidChildProfilePanel', {
                    title: 'Reportes semanales',
                    showReportsOnly: true
                }).show();
            }
        }]
    }]
});
