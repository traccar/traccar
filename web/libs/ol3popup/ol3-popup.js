/**
 * OpenLayers 3 Popup Overlay.
 * See [the examples](./examples) for usage. Styling can be done via CSS.
 * @constructor
 * @extends {ol.Overlay}
 * @param {Object} opt_options Overlay options, extends olx.OverlayOptions adding:
 *                              **`panMapIfOutOfView`** `Boolean` - Should the
 *                              map be panned so that the popup is entirely
 *                              within view.
 */
ol.Overlay.Popup = function(opt_options) {

    var options = opt_options || {};

    this.panMapIfOutOfView = options.panMapIfOutOfView;
    if (this.panMapIfOutOfView === undefined) {
        this.panMapIfOutOfView = true;
    }

    this.ani = options.ani;
    if (this.ani === undefined) {
        this.ani = ol.animation.pan;
    }

    this.ani_opts = options.ani_opts;
    if (this.ani_opts === undefined) {
        this.ani_opts = {'duration': 250};
    }

    this.container = document.createElement('div');
    this.container.className = 'ol-popup';

    this.closer = document.createElement('a');
    this.closer.className = 'ol-popup-closer';
    this.closer.href = '#';
    this.container.appendChild(this.closer);

    var that = this;
    this.closer.addEventListener('click', function(evt) {
        that.container.style.display = 'none';
        that.closer.blur();
        evt.preventDefault();
    }, false);

    this.content = document.createElement('div');
    this.content.className = 'ol-popup-content';
    this.container.appendChild(this.content);

    // Apply workaround to enable scrolling of content div on touch devices
    ol.Overlay.Popup.enableTouchScroll_(this.content);

    ol.Overlay.call(this, {
        element: this.container,
        stopEvent: true,
        insertFirst: (options.hasOwnProperty('insertFirst')) ? options.insertFirst : true
    });

};

ol.inherits(ol.Overlay.Popup, ol.Overlay);

/**
 * Show the popup.
 * @param {ol.Coordinate} coord Where to anchor the popup.
 * @param {String} html String of HTML to display within the popup.
 */
ol.Overlay.Popup.prototype.show = function(coord, html) {
    this.setPosition(coord);
    this.content.innerHTML = html;
    this.container.style.display = 'block';
    if (this.panMapIfOutOfView) {
        this.panIntoView_(coord);
    }
    this.content.scrollTop = 0;
    return this;
};

/**
 * @private
 */
ol.Overlay.Popup.prototype.panIntoView_ = function(coord) {

    var popSize = {
            width: this.getElement().clientWidth + 20,
            height: this.getElement().clientHeight + 20
        },
        mapSize = this.getMap().getSize();

    var tailHeight = 20,
        tailOffsetLeft = 60,
        tailOffsetRight = popSize.width - tailOffsetLeft,
        popOffset = this.getOffset(),
        popPx = this.getMap().getPixelFromCoordinate(coord);

    var fromLeft = (popPx[0] - tailOffsetLeft),
        fromRight = mapSize[0] - (popPx[0] + tailOffsetRight);

    var fromTop = popPx[1] - popSize.height + popOffset[1],
        fromBottom = mapSize[1] - (popPx[1] + tailHeight) - popOffset[1];

    var center = this.getMap().getView().getCenter(),
        curPx = this.getMap().getPixelFromCoordinate(center),
        newPx = curPx.slice();

    if (fromRight < 0) {
        newPx[0] -= fromRight;
    } else if (fromLeft < 0) {
        newPx[0] += fromLeft;
    }

    if (fromTop < 0) {
        newPx[1] += fromTop;
    } else if (fromBottom < 0) {
        newPx[1] -= fromBottom;
    }

    if (this.ani && this.ani_opts) {
        this.ani_opts.source = center;
        this.getMap().beforeRender(this.ani(this.ani_opts));
    }

    if (newPx[0] !== curPx[0] || newPx[1] !== curPx[1]) {
        this.getMap().getView().setCenter(this.getMap().getCoordinateFromPixel(newPx));
    }

    return this.getMap().getView().getCenter();

};

/**
 * @private
 * @desc Determine if the current browser supports touch events. Adapted from
 * https://gist.github.com/chrismbarr/4107472
 */
ol.Overlay.Popup.isTouchDevice_ = function() {
    try {
        document.createEvent("TouchEvent");
        return true;
    } catch(e) {
        return false;
    }
};

/**
 * @private
 * @desc Apply workaround to enable scrolling of overflowing content within an
 * element. Adapted from https://gist.github.com/chrismbarr/4107472
 */
ol.Overlay.Popup.enableTouchScroll_ = function(elm) {
    if(ol.Overlay.Popup.isTouchDevice_()){
        var scrollStartPos = 0;
        elm.addEventListener("touchstart", function(event) {
            scrollStartPos = this.scrollTop + event.touches[0].pageY;
        }, false);
        elm.addEventListener("touchmove", function(event) {
            this.scrollTop = scrollStartPos - event.touches[0].pageY;
        }, false);
    }
};

/**
 * Hide the popup.
 */
ol.Overlay.Popup.prototype.hide = function() {
    this.container.style.display = 'none';
    return this;
};
