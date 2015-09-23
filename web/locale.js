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

var availableLanguages = {
    'bg': { name: 'Български', code: 'bg' },
    'cs': { name: 'Čeština', code: 'cs' },
    'de': { name: 'Deutsch', code: 'de' },
    'dk': { name: 'Dansk', code: 'dk' },
    'en': { name: 'English', code: 'en' },
    'es': { name: 'Español', code: 'es' },
    'fr': { name: 'Français', code: 'fr' },
    'hu': { name: 'Magyar', code: 'hu' },
    'lt': { name: 'Lietuvių', code: 'lt' },
    'nl': { name: 'Nederlands', code: 'nl' },
    'pl': { name: 'Polski', code: 'pl' },
    'pt': { name: 'Português', code: 'pt' },
    'ru': { name: 'Русский', code: 'ru' },
    'si': { name: 'සිංහල', code: 'en' },
    'sk': { name: 'Slovenčina', code: 'sk' },
    'sr': { name: 'Srpski', code: 'sr' },
    'th': { name: 'ไทย', code: 'th' },
    'zh': { name: '中文', code: 'zh_CN' }
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
Ext.Loader.loadScript('//cdnjs.cloudflare.com/ajax/libs/extjs/6.0.0/classic/locale/locale-' + availableLanguages[language].code + '.js');
