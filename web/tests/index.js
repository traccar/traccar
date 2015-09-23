var Harness = Siesta.Harness.Browser.ExtJS;

Harness.configure({
    title       : 'Awesome Test Suite',

    preload     : [
        '//cdnjs.cloudflare.com/ajax/libs/extjs/6.0.0/ext-all.js',
        '../app.min.js'
    ]
});

Harness.start(
    '010_sanity.t.js'
);
