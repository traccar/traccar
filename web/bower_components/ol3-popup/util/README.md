# OpenLayers 3 Popup

Basic popup for an OL3 map. By default the map is centered so that the popup is
entirely visible.

## Demo

Clone or download the repository and open
[examples/popup.html](examples/popup.html) in a browser or [view the example on
RawGit](http://rawgit.com/walkermatt/ol3-popup/master/examples/popup.html).
Click on the map to display a popup, click close to the edge of the map to see
it pan into view.

## Credit

Based on an example by [Tim Schaub](https://github.com/tschaub) posted on the
[OL3-Dev list](https://groups.google.com/forum/#!forum/ol3-dev).

## API

{% for class in classes -%}

### `new {{ class.longname }}({{ class.signature }})`

{{ class.description }}

#### Parameters:

|Name|Type|Description|
|:---|:---|:----------|
{% for param in class.params %}|`{{ param.name }}`|`{{ param.type.names[0] }}`| {{ param.description }} |{% endfor %}

#### Extends

`{{ class.augments }}`

#### Methods

{% for method in class.methods -%}
##### `{% if method.scope == 'static' %}(static) {{ class.longname }}.{% endif %}{{ method.name }}({{ method.signature }})`

{{ method.description }}

{% if method.params -%}
###### Parameters:

|Name|Type|Description|
|:---|:---|:----------|
{% for param in method.params -%}
|`{{ param.name }}`|`{{ param.type.names[0] }}`| {{ param.description }} |
{% endfor %}

{% endif %}
{%- endfor %}
{%- endfor -%}

## License

MIT (c) Matt Walker.

## Also see

If you find the popup useful you might also like the
[ol3-layerswitcher](https://github.com/walkermatt/ol3-layerswitcher).
