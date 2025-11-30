Ext.define('Traccar.store.Devices', {
    extend: 'Ext.data.Store',
    alias: 'store.devices',

    fields: ['id', 'name'],

    proxy: {
        type: 'ajax',
        url: '/api/devices',
        reader: {
            type: 'json'
        }
    },

    autoLoad: true
});
