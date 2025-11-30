Ext.define('Traccar.view.MainMenu', {
    extend: 'Ext.panel.Panel',
    xtype: 'mainMenu',

    requires: [
        'Traccar.view.savekid.SavekidChildrenView',
        'Traccar.view.savekid.SavekidChildProfilePanel',
        'Traccar.view.savekid.SavekidStatusPanel',
        'Traccar.view.savekid.SavekidHealthHistoryView',
        'Traccar.view.savekid.SavekidWeeklyReportPanel'
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
                Ext.create('Traccar.view.savekid.SavekidHealthHistoryView', {
                    title: 'Historial fisiológico diario'
                }).show();
            }
        }, {
            text: 'Reportes semanales',
            handler: function () {
                Ext.create('Traccar.view.savekid.SavekidWeeklyReportPanel', {
                    title: 'Reporte semanal'
                }).show();
            }
        }]
    }]
});
