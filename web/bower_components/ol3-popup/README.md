# OpenLayers 3 Popup

Basic popup overlay for an [OL3](https://github.com/openlayers/ol3) map. By
default the map is centred so that the popup is entirely visible.

## Examples

The examples demonstrate usage and can be viewed online thanks to [RawGit](http://rawgit.com/):

* [Basic usage](http://rawgit.com/walkermatt/ol3-popup/master/examples/popup.html)
    * Create a popup instance, show it on single-click specifying the content
* [DOM Events](http://rawgit.com/walkermatt/ol3-popup/master/examples/dom-events.html)
    * Handle DOM events triggered by interacting with elements within the popup content
* [Scroll](http://rawgit.com/walkermatt/ol3-popup/master/examples/scroll.html)
    * Controlling popup dimensions and scrolling overflowing content

The source for all examples can be found in [examples](examples).

## API

### `new ol.Overlay.Popup(opt_options)`

OpenLayers 3 Popup Overlay.
See [the examples](./examples) for usage. Styling can be done via CSS.

#### Parameters:

|Name|Type|Description|
|:---|:---|:----------|
|`opt_options`|`Object`| Overlay options, extends olx.OverlayOptions adding: **`panMapIfOutOfView`** `Boolean` - Should the map be panned so that the popup is entirely within view. |

#### Extends

`ol.Overlay`

#### Methods

##### `show(coord,html)`

Show the popup.

###### Parameters:

|Name|Type|Description|
|:---|:---|:----------|
|`coord`|`ol.Coordinate`| Where to anchor the popup. |
|`html`|`String`| String of HTML to display within the popup. |


##### `hide()`

Hide the popup.

## License

MIT (c) Matt Walker.

## Credit

Based on an example by [Tim Schaub](https://github.com/tschaub) posted on the
[OL3-Dev list](https://groups.google.com/forum/#!forum/ol3-dev).

## Also see

If you find the popup useful you might also like the
[ol3-layerswitcher](https://github.com/walkermatt/ol3-layerswitcher).

