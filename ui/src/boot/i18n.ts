/*
 * Copyright (C) 2026 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
 * which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
 * of the interface window, the display of the “You are using the Open Source and free version of LinID™, powered by
 * Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!” infobox and in the e-mails
 * sent with the Program, notice appended to any type of outbound messages (e.g. e-mail and meeting requests) as well
 * as in the LinID Identity Manager user interface, (ii) retain all hypertext links between LinID Identity Manager
 * and https://linid.org/, as well as between LINAGORA and LINAGORA.com, and (iii) refrain from infringing LINAGORA
 * intellectual property rights over its trademarks and commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License and its applicable Additional Terms for
 * LinID Identity Manager along with this program. If not, see <http://www.gnu.org/licenses/> for the GNU Affero
 * General Public License version 3 and <http://www.linagora.com/licenses/> for the Additional Terms applicable to the
 * LinID Identity Manager software.
 */

import {
  fromDot,
  getI18nInstance,
  merge,
  setI18nInstance,
  useLinidUiStore,
} from '@linagora/linid-im-front-corelib';
import { api } from 'boot/axios';
import { appConfig } from 'boot/config';
import { Quasar } from 'quasar';
import type messages from 'src/i18n';
import { resolveLocale, syncLocale } from 'src/services/I18nSupportService';
import type { Composer } from 'vue-i18n';
import { createI18n, type I18n } from 'vue-i18n';
import { defineBoot } from '#q-app/wrappers';

/** Type defining the available message languages. */
export type MessageLanguages = keyof typeof messages;

/** Type-define 'en-US' as the master schema for the resource. */
export type MessageSchema = (typeof messages)['en-US'];

// See https://vue-i18n.intlify.dev/guide/advanced/typescript.html#global-resource-schema-type-definition
/* eslint-disable @typescript-eslint/no-empty-object-type */
declare module 'vue-i18n' {
  /** Define the locale messages schema. */
  export interface DefineLocaleMessage extends MessageSchema {}

  /** Define the datetime format schema. */
  export interface DefineDateTimeFormat {}

  /** Define the number format schema. */
  export interface DefineNumberFormat {}
}

/* eslint-enable @typescript-eslint/no-empty-object-type */
export default defineBoot(async ({ app }) => {
  const messages: Record<string, unknown> = {};

  for (const lang of appConfig.i18n.languages) {
    const appMessages = await fetch(`/i18n/${lang}.json`)
      .then((res) => res.json())
      .catch(() => ({}));
    const apiMessages = await api
      .get(`/i18n/${lang}.json`)
      .then(({ data }) => fromDot(data))
      .catch(() => ({}));

    messages[lang] = merge(apiMessages, appMessages);
  }

  const i18nLocale = resolveLocale();
  await syncLocale(i18nLocale);

  // eslint-disable-next-line jsdoc/require-jsdoc
  const i18n = createI18n<{ message: MessageSchema }, MessageLanguages>({
    locale: i18nLocale,
    legacy: false,
    fallbackLocale: appConfig.i18n.locale,
    // @ts-expect-error 'messages' type is not compatible with the expected type of createI18n,
    // but we know it matches the schema at runtime
    messages,
  }) as I18n;

  setI18nInstance(i18n);

  // Set i18n instance on app
  app.use(i18n);

  // Load Quasar language pack to translate built-in components (table pagination, etc.)
  const langIso = appConfig.i18n.locale.substring(0, 2);
  try {
    const quasarLang = await import(
      /* @vite-ignore */ `quasar/lang/${langIso}.js`
    );
    Quasar.lang.set(quasarLang.default);
  } catch {
    // Fallback: keep default English if language pack not found
  }

  // TODO: Should be removed with the issue 192
  const uiStore = useLinidUiStore();
  const { t } = getI18nInstance().global as Composer;

  uiStore.addMainNavigationMenuItems({
    id: 'accounts',
    label: t('AccountsPage.menuTitle'),
    path: '/accounts',
  });

  uiStore.addMainNavigationMenuItems({
    id: 'organizational-units',
    label: t('OrganizationalUnitsPage.menuTitle'),
    path: '/organizational-units',
  });
});
