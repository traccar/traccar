Ext.define('Traccar.store.SavekidChildrenStore', {
    extend: 'Ext.data.Store',
    alias: 'store.savekidChildren',

    fields: [
        'id', 'firstName', 'lastName', 'birthDate', 'deviceId', 'deviceName',
        'weight', 'height', 'medicalConditions', 'heartRate', 'temperature'
    ],

    proxy: {
        type: 'ajax',
        url: '/api/savekid/children',
        reader: {
            type: 'json'
        }
    },

    autoLoad: false,

    listeners: {
        exception: function (proxy, response) {
            var message = 'Error cargando perfiles de niños';
            if (response && response.responseText) {
                message += ': ' + response.responseText;
            }
            Ext.Msg.alert('Error', message);
        }
    }
});
