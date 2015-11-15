StartTest(function (t) {
    t.diag("Sanity");

    t.ok(Ext, 'Ext is defined');
    t.ok(Ext.Window, 'Ext.Window is defined');

    t.ok(Traccar, 'Traccar is defined');
    t.ok(Traccar.Application, 'Traccar.Application is defined');

    t.ok(Strings, 'Strings are defined');
    t.ok(Traccar.Styles, 'Traccar.Styles are defined');

    t.done();
});
