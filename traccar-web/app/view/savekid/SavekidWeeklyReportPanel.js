Ext.define('Traccar.view.savekid.SavekidWeeklyReportPanel', {
    extend: 'Ext.window.Window',
    xtype: 'savekid-weekly-report',

    requires: [
        'Ext.chart.CartesianChart',
        'Ext.chart.series.Bar',
        'Ext.chart.axis.Numeric',
        'Ext.chart.axis.Category',
        'Traccar.store.SavekidChildrenStore',
        'Traccar.store.SavekidHealthHistoryStore',
        'Traccar.controller.SavekidController'
    ],

    controller: 'savekid',
    referenceHolder: true,
    layout: 'fit',
    modal: true,
    width: 1000,
    height: 700,
    bodyPadding: 10,
    closeAction: 'destroy',
    title: 'Resumen semanal de salud',

    initComponent: function () {
        var me = this;

        me.childrenStore = Ext.create('Traccar.store.SavekidChildrenStore');
        me.historyStore = Ext.create('Traccar.store.SavekidHealthHistoryStore');

        Ext.apply(me, {
            items: [{
                xtype: 'panel',
                layout: { type: 'vbox', align: 'stretch' },
                items: [me.createFilterBar(), me.createSummaryRow(), me.createChartsSection(), me.createRecommendations()]
            }]
        });

        me.on('afterrender', function () {
            me.childrenStore.load();
        });

        me.callParent(arguments);
    },

    createFilterBar: function () {
        var defaultFrom = Ext.Date.add(Ext.Date.clearTime(new Date()), Ext.Date.DAY, -6);
        var defaultTo = Ext.Date.clearTime(new Date());
        return {
            xtype: 'form',
            bodyPadding: 10,
            defaults: { margin: '0 10 0 0' },
            layout: { type: 'hbox', align: 'middle' },
            items: [{
                xtype: 'combo',
                reference: 'weeklyChildField',
                store: this.childrenStore,
                displayField: 'firstName',
                valueField: 'id',
                queryMode: 'local',
                editable: false,
                width: 220,
                emptyText: 'Seleccione niño'
            }, {
                xtype: 'datefield',
                reference: 'weeklyFromField',
                fieldLabel: 'Desde',
                labelWidth: 45,
                width: 170,
                format: 'Y-m-d',
                value: defaultFrom
            }, {
                xtype: 'datefield',
                reference: 'weeklyToField',
                fieldLabel: 'Hasta',
                labelWidth: 45,
                width: 170,
                format: 'Y-m-d',
                value: defaultTo
            }, {
                xtype: 'button',
                text: 'Generar reporte',
                iconCls: 'x-fa fa-bar-chart',
                handler: 'onLoadWeeklyReport'
            }, '->', {
                xtype: 'displayfield',
                reference: 'weeklyStatus',
                value: 'Rango sugerido: últimos 7 días.'
            }]
        };
    },

    createSummaryRow: function () {
        var cardCfg = function (title, ref, color) {
            return {
                xtype: 'container',
                flex: 1,
                padding: 10,
                margin: '0 10 10 0',
                style: 'background:' + color + '; border-radius:8px; color:#fff;',
                items: [{ xtype: 'component', html: '<div style="opacity:0.8">' + title + '</div>' }, {
                    xtype: 'component',
                    reference: ref,
                    style: 'font-size:28px;font-weight:800;',
                    html: '--'
                }]
            };
        };

        return {
            xtype: 'container',
            layout: 'hbox',
            items: [
                cardCfg('Promedio HR', 'weeklyAvgHr', 'linear-gradient(135deg,#e74c3c,#c0392b)'),
                cardCfg('Promedio temp', 'weeklyAvgTemp', 'linear-gradient(135deg,#f39c12,#d35400)'),
                cardCfg('Total pasos', 'weeklyTotalSteps', 'linear-gradient(135deg,#27ae60,#2ecc71)'),
                cardCfg('Horas de sueño', 'weeklySleep', 'linear-gradient(135deg,#2980b9,#3498db)')
            ]
        };
    },

    createChartsSection: function () {
        return {
            xtype: 'container',
            layout: 'column',
            defaults: { columnWidth: 0.5, height: 280, padding: '0 10 10 0' },
            items: [this.createVitalsChart(), this.createActivityChart()]
        };
    },

    createVitalsChart: function () {
        return {
            xtype: 'cartesian',
            reference: 'weeklyVitalsChart',
            title: 'Comparativa diaria HR / Temperatura',
            interactions: ['itemhighlight'],
            axes: [{ type: 'category', position: 'bottom', title: 'Día' }, {
                type: 'numeric', position: 'left', title: 'Valor'
            }],
            series: [{
                type: 'bar',
                xField: 'day',
                yField: ['avgHeartRate', 'avgTemperature'],
                stacked: false,
                style: { minGapWidth: 15 },
                title: ['HR promedio', 'Temp promedio']
            }],
            store: { fields: ['day', 'avgHeartRate', 'avgTemperature'], data: [] }
        };
    },

    createActivityChart: function () {
        return {
            xtype: 'cartesian',
            reference: 'weeklyActivityChart',
            title: 'Actividad y sueño por día',
            interactions: ['itemhighlight'],
            axes: [{ type: 'category', position: 'bottom', title: 'Día' }, {
                type: 'numeric', position: 'left', title: 'Cantidad'
            }],
            series: [{
                type: 'bar',
                xField: 'day',
                yField: ['steps', 'sleepHours'],
                stacked: false,
                style: { minGapWidth: 15 },
                title: ['Pasos', 'Horas de sueño']
            }],
            store: { fields: ['day', 'steps', 'sleepHours'], data: [] }
        };
    },

    createRecommendations: function () {
        return {
            xtype: 'panel',
            title: 'Recomendaciones finales',
            margin: '10 0 0 0',
            bodyPadding: 10,
            reference: 'weeklyRecommendations',
            html: '<ul><li>Consulta un rango para ver sugerencias personalizadas.</li></ul>'
        };
    },

    updateWeeklyReport: function (records) {
        var me = this;
        if (!records || records.length === 0) {
            me.lookup('weeklyStatus').setValue('Sin datos en el rango seleccionado.');
            me.lookup('weeklyVitalsChart').getStore().removeAll();
            me.lookup('weeklyActivityChart').getStore().removeAll();
            me.lookup('weeklyRecommendations').update('<ul><li>Sin datos disponibles.</li></ul>');
            Ext.Array.forEach(['weeklyAvgHr', 'weeklyAvgTemp', 'weeklyTotalSteps', 'weeklySleep'], function (ref) {
                me.lookup(ref).update('--');
            });
            return;
        }

        me.lookup('weeklyStatus').setValue('Última actualización: ' + Ext.Date.format(new Date(), 'Y-m-d H:i'));

        var totals = { hr: 0, temp: 0, steps: 0, sleep: 0 };
        var counts = { hr: 0, temp: 0, sleep: 0 };
        var grouped = {};

        Ext.Array.forEach(records, function (rec) {
            var day = Ext.Date.format(rec.get('timestamp'), 'Y-m-d');
            if (!grouped[day]) {
                grouped[day] = { hr: [], temp: [], steps: 0, sleep: 0 };
            }

            var hr = rec.get('heartRate');
            var temp = rec.get('temperature');
            var steps = rec.get('steps');
            var sleep = rec.get('sleepHours');

            if (hr !== undefined && hr !== null) {
                totals.hr += hr;
                counts.hr += 1;
                grouped[day].hr.push(hr);
            }
            if (temp !== undefined && temp !== null) {
                totals.temp += temp;
                counts.temp += 1;
                grouped[day].temp.push(temp);
            }
            if (steps !== undefined && steps !== null) {
                totals.steps += steps;
                grouped[day].steps += steps;
            }
            if (sleep !== undefined && sleep !== null) {
                totals.sleep += sleep;
                counts.sleep += 1;
                grouped[day].sleep += sleep;
            }
        });

        var vitalsData = [];
        var activityData = [];
        Ext.Object.each(grouped, function (day, payload) {
            var avgHr = payload.hr.length ? Ext.Array.mean(payload.hr) : 0;
            var avgTemp = payload.temp.length ? Ext.Array.mean(payload.temp) : 0;
            vitalsData.push({ day: day, avgHeartRate: avgHr, avgTemperature: avgTemp });
            activityData.push({ day: day, steps: payload.steps, sleepHours: payload.sleep });
        });

        vitalsData = Ext.Array.sort(vitalsData, function (a, b) { return a.day > b.day ? 1 : -1; });
        activityData = Ext.Array.sort(activityData, function (a, b) { return a.day > b.day ? 1 : -1; });

        me.lookup('weeklyVitalsChart').getStore().setData(vitalsData);
        me.lookup('weeklyActivityChart').getStore().setData(activityData);

        var avgHr = counts.hr ? Math.round((totals.hr / counts.hr) * 10) / 10 : 0;
        var avgTemp = counts.temp ? Math.round((totals.temp / counts.temp) * 10) / 10 : 0;
        var totalSteps = totals.steps;
        var avgSleep = counts.sleep ? Math.round((totals.sleep / counts.sleep) * 10) / 10 : 0;

        me.lookup('weeklyAvgHr').update(avgHr + ' bpm');
        me.lookup('weeklyAvgTemp').update(avgTemp + ' °C');
        me.lookup('weeklyTotalSteps').update(totalSteps);
        me.lookup('weeklySleep').update(avgSleep + ' h');

        me.lookup('weeklyRecommendations').update(this.buildRecommendations(avgHr, avgTemp, totalSteps, avgSleep));
    },

    buildRecommendations: function (avgHr, avgTemp, totalSteps, avgSleep) {
        var suggestions = [];
        if (avgHr > 110) {
            suggestions.push('HR elevado. Considere reposo y revisar hidratación.');
        } else if (avgHr < 60) {
            suggestions.push('HR bajo. Verifique que el dispositivo esté colocado correctamente.');
        } else {
            suggestions.push('Frecuencia cardíaca dentro de rangos esperados.');
        }

        if (avgTemp >= 37.5) {
            suggestions.push('Temperatura elevada. Monitorizar posibles síntomas de fiebre.');
        } else {
            suggestions.push('Temperatura estable en la semana.');
        }

        if (totalSteps < 35000) {
            suggestions.push('Aumentar actividad física: establecer metas diarias de pasos.');
        } else {
            suggestions.push('Buen nivel de actividad física mantenida.');
        }

        if (avgSleep < 8) {
            suggestions.push('Dormir al menos 8 horas diarias para una mejor recuperación.');
        } else {
            suggestions.push('Patrón de sueño adecuado.');
        }

        return '<ul><li>' + suggestions.join('</li><li>') + '</li></ul>';
    }
});
