var harness = new Siesta.Harness.Browser.ExtJS();

harness.configure({
    title: 'Traccar Test Suite',
    preload: [
        '//cdnjs.cloudflare.com/ajax/libs/extjs/6.0.0/ext-all.js',
        'locale.js',
        'app.js'
    ]
});

harness.start(
    'tests/010_sanity.t.js'
);
