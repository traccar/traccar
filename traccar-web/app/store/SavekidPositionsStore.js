Ext.define('Traccar.store.SavekidPositionsStore', {
    extend: 'Ext.data.Store',
    alias: 'store.savekidPositions',

    fields: [
        'id', 'deviceId', 'latitude', 'longitude', 'address', 'fixTime', 'speed', 'course'
    ],

    proxy: {
        type: 'ajax',
        url: '/api/positions',
        reader: {
            type: 'json'
        }
    },

    autoLoad: false,

    listeners: {
        exception: function (proxy, response) {
            var message = 'Error al cargar posiciones';
            if (response && response.responseText) {
                message += ': ' + response.responseText;
            }
            Ext.Msg.alert('Error', message);
        }
    }
});
