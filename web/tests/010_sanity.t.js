StartTest(function (t) {
    t.diag("Sanity");

    t.ok(Ext, 'Ext is defined');
    t.ok(Ext.Window, 'Ext.Window is defined');

    t.ok(Traccar, 'Traccar is defined');
    t.ok(Traccar.Application, 'Traccar.Application is defined');

    t.ok(strings, 'strings are defined');
    t.ok(styles, 'styles are defined');

    t.done();
});
