var Harness = Siesta.Harness.Browser.ExtJS;

Harness.configure({
    title: 'Traccar Test Suite',
    preload: [
        '//cdnjs.cloudflare.com/ajax/libs/extjs/6.0.0/ext-all.js',
        'locale.js',
        'app.js'
    ]
});

Harness.start(
    'tests/010_sanity.t.js'
);
