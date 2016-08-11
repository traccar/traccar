/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

Ext.define('Traccar.view.PopupWindow', {
    extend: 'Ext.window.Window',
    xtype: 'PopupWindowView',
    title: 'Windows',
    closable: true,
    width: 250,
    height: 250,

    initComponent: function () {
        //get the original template
        var originalTpl = Ext.XTemplate.getTpl(this, 'renderTpl');

        //add the triangle div (or img, span, etc.)
        this.renderTpl = new Ext.XTemplate([
            originalTpl.html,         //the html from the original tpl
            '<div class="popup-triangle"></div>',
            originalTpl.initialConfig //the config options from the original tpl
        ]);

        this.callParent();
    },

    beforeSetPosition: function () {
        //shift the menu down from its original position
        var pos = this.callParent(arguments);

        if (pos) {
            //pos.y += 5; //the offset (should be the height of your triangle)
        }

        return pos;
    }
});