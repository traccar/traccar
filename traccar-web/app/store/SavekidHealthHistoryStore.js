Ext.define('Traccar.store.SavekidHealthHistoryStore', {
    extend: 'Ext.data.Store',
    alias: 'store.savekidHealthHistory',

    config: {
        childId: null,
        fromDate: null,
        toDate: null
    },

    fields: [
        { name: 'timestamp', type: 'date' },
        { name: 'heartRate', type: 'float' },
        { name: 'temperature', type: 'float' },
        { name: 'steps', type: 'int' },
        { name: 'sleepHours', type: 'float' }
    ],

    proxy: {
        type: 'ajax',
        url: '',
        reader: {
            type: 'json'
        }
    },

    autoLoad: false,

    setChildContext: function (childId, fromDate, toDate) {
        this.childId = childId;
        this.fromDate = fromDate;
        this.toDate = toDate;
    },

    listeners: {
        beforeload: function (store, operation) {
            if (!store.childId) {
                Ext.Msg.alert('Faltan datos', 'Seleccione un niño antes de consultar el historial.');
                return false;
            }

            var proxy = store.getProxy();
            proxy.setUrl('/api/savekid/children/' + store.childId + '/health-history');

            var params = {};
            if (store.fromDate) {
                params.from = Ext.Date.format(store.fromDate, 'Y-m-d');
            }
            if (store.toDate) {
                params.to = Ext.Date.format(store.toDate, 'Y-m-d');
            }
            operation.setParams(params);
        },
        exception: function (proxy, response) {
            var message = 'Error obteniendo historial de salud';
            if (response && response.responseText) {
                message += ': ' + response.responseText;
            }
            Ext.Msg.alert('Error', message);
        }
    }
});
