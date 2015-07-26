/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

Ext.Loader.setConfig({
    disableCaching: false
});

{
    var availableLanguages = {
        'cn': '中文',
        'de': 'Deutsch',
        'en': 'English',
        'es': 'Español',
        'fr': 'Français',
        'pl': 'Polski',
        'pt': 'Português',
        'ru': 'Русский',
        'th': 'ไทย'
    };

    var language = Ext.Object.fromQueryString(window.location.search.substring(1)).locale;
    if (language === undefined) {
        language = window.navigator.userLanguage || window.navigator.language;
        language = language.substr(0, 2);
    }

    if (!(language in availableLanguages)) {
        language = 'en'; // default
    }

    Ext.Loader.loadScript('/l10n/' + language + '.js');
    Ext.Loader.loadScript('http://cdn.sencha.com/ext/gpl/5.1.0/build/packages/ext-locale/build/ext-locale-' + language + '.js');
}

Ext.application({
    name: 'Traccar',
    extend: 'Traccar.Application'
});
