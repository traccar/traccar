// also supports: startTest(function(t) {
StartTest(function(t) {
    t.diag("Sanity");

    t.ok(Ext, 'ExtJS is here');
    t.ok(Ext.Window, '.. indeed');


    t.ok(Your.Project, 'My project is here');
    t.ok(Your.Project.Util, '.. indeed');

    t.done();   // Optional, marks the correct exit point from the test
})
