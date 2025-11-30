Ext.define('Traccar.view.savekid.SavekidHealthHistoryView', {
    extend: 'Ext.window.Window',
    xtype: 'savekid-health-history',

    requires: [
        'Ext.chart.CartesianChart',
        'Ext.chart.axis.Numeric',
        'Ext.chart.axis.Time',
        'Ext.chart.axis.Category',
        'Ext.chart.series.Line',
        'Ext.chart.series.Bar',
        'Traccar.store.SavekidChildrenStore',
        'Traccar.store.SavekidHealthHistoryStore',
        'Traccar.controller.SavekidController'
    ],

    controller: 'savekid',
    referenceHolder: true,
    layout: 'fit',
    modal: true,
    width: 1100,
    height: 750,
    bodyPadding: 10,
    closeAction: 'destroy',
    title: 'Historial fisiológico diario',

    initComponent: function () {
        var me = this;

        me.childrenStore = Ext.create('Traccar.store.SavekidChildrenStore');
        me.historyStore = Ext.create('Traccar.store.SavekidHealthHistoryStore');

        Ext.apply(me, {
            items: [{
                xtype: 'panel',
                layout: { type: 'vbox', align: 'stretch' },
                items: [me.createFilterBar(), me.createChartsRow()]
            }]
        });

        me.on('afterrender', function () {
            me.childrenStore.load();
        });

        me.callParent(arguments);
    },

    createFilterBar: function () {
        return {
            xtype: 'form',
            bodyPadding: 10,
            defaults: { margin: '0 10 0 0' },
            layout: { type: 'hbox', align: 'middle' },
            items: [{
                xtype: 'combo',
                reference: 'historyChildField',
                store: this.childrenStore,
                displayField: 'firstName',
                valueField: 'id',
                queryMode: 'local',
                editable: false,
                width: 220,
                emptyText: 'Seleccione niño'
            }, {
                xtype: 'datefield',
                reference: 'historyFromField',
                fieldLabel: 'Desde',
                labelWidth: 45,
                width: 170,
                format: 'Y-m-d',
                value: Ext.Date.add(new Date(), Ext.Date.DAY, -7)
            }, {
                xtype: 'datefield',
                reference: 'historyToField',
                fieldLabel: 'Hasta',
                labelWidth: 45,
                width: 170,
                format: 'Y-m-d',
                value: new Date()
            }, {
                xtype: 'button',
                text: 'Consultar',
                iconCls: 'x-fa fa-search',
                handler: 'onLoadHealthHistory'
            }, '->', {
                xtype: 'displayfield',
                reference: 'historyStatus',
                value: 'Seleccione un rango para ver las gráficas.'
            }]
        };
    },

    createChartsRow: function () {
        return {
            xtype: 'container',
            layout: 'column',
            defaults: { columnWidth: 0.333, height: 320, padding: '0 10 10 0' },
            items: [this.createHeartChart(), this.createTemperatureChart(), this.createStepsChart()]
        };
    },

    createHeartChart: function () {
        return {
            xtype: 'cartesian',
            reference: 'historyHeartChart',
            title: 'Frecuencia cardíaca vs tiempo',
            interactions: [{ type: 'panzoom' }],
            insetPadding: 15,
            axes: [{
                type: 'time',
                position: 'bottom',
                title: 'Fecha y hora'
            }, {
                type: 'numeric',
                position: 'left',
                title: 'HR (bpm)'
            }],
            series: [{
                type: 'line',
                xField: 'timestamp',
                yField: 'value',
                style: { stroke: '#e74c3c', lineWidth: 2 },
                marker: { type: 'circle', radius: 3 }
            }],
            store: { fields: ['timestamp', 'value'], data: [] }
        };
    },

    createTemperatureChart: function () {
        return {
            xtype: 'cartesian',
            reference: 'historyTemperatureChart',
            title: 'Temperatura vs tiempo',
            interactions: [{ type: 'panzoom' }],
            insetPadding: 15,
            axes: [{
                type: 'time',
                position: 'bottom',
                title: 'Fecha y hora'
            }, {
                type: 'numeric',
                position: 'left',
                title: '°C'
            }],
            series: [{
                type: 'line',
                xField: 'timestamp',
                yField: 'value',
                style: { stroke: '#f1c40f', lineWidth: 2 },
                marker: { type: 'square', radius: 3 }
            }],
            store: { fields: ['timestamp', 'value'], data: [] }
        };
    },

    createStepsChart: function () {
        return {
            xtype: 'cartesian',
            reference: 'historyStepsChart',
            title: 'Pasos por día',
            interactions: ['itemhighlight'],
            insetPadding: 15,
            axes: [{
                type: 'category',
                position: 'bottom',
                title: 'Día'
            }, {
                type: 'numeric',
                position: 'left',
                title: 'Pasos'
            }],
            series: [{
                type: 'bar',
                xField: 'day',
                yField: 'steps',
                style: { fill: '#27ae60' },
                highlightCfg: { fillStyle: '#2ecc71' }
            }],
            store: { fields: ['day', 'steps'], data: [] }
        };
    },

    updateHistoryVisuals: function (records) {
        var me = this;
        if (!records || records.length === 0) {
            me.lookup('historyStatus').setValue('Sin datos para el rango indicado.');
            me.lookup('historyHeartChart').getStore().removeAll();
            me.lookup('historyTemperatureChart').getStore().removeAll();
            me.lookup('historyStepsChart').getStore().removeAll();
            return;
        }

        me.lookup('historyStatus').setValue('Última actualización: ' + Ext.Date.format(new Date(), 'Y-m-d H:i'));

        var heartData = Ext.Array.map(records, function (rec) {
            return {
                timestamp: rec.get('timestamp'),
                value: rec.get('heartRate')
            };
        });

        var tempData = Ext.Array.map(records, function (rec) {
            return {
                timestamp: rec.get('timestamp'),
                value: rec.get('temperature')
            };
        });

        var stepsByDay = {};
        Ext.Array.forEach(records, function (rec) {
            var day = Ext.Date.format(rec.get('timestamp'), 'Y-m-d');
            var steps = rec.get('steps') || 0;
            if (!stepsByDay[day]) {
                stepsByDay[day] = 0;
            }
            stepsByDay[day] += steps;
        });

        var stepsData = [];
        Ext.Object.each(stepsByDay, function (day, value) {
            stepsData.push({ day: day, steps: value });
        });
        stepsData = Ext.Array.sort(stepsData, function (a, b) {
            return a.day > b.day ? 1 : -1;
        });

        me.lookup('historyHeartChart').getStore().setData(heartData);
        me.lookup('historyTemperatureChart').getStore().setData(tempData);
        me.lookup('historyStepsChart').getStore().setData(stepsData);
    }
});
