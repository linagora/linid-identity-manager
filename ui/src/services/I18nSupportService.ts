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

import { useLinidUserPreference } from '@linagora/linid-im-front-corelib';
import { appConfig } from 'boot/config';
import { authService } from 'src/services/AuthService';

/**
 * Tells whether a language candidate is defined and part of the supported languages.
 *
 * @param lang - The language candidate to check.
 * @returns `true` when the candidate is a non-empty, supported language.
 */
function isSupportedLanguage(lang?: string | null): lang is string {
  return !!lang && appConfig.i18n.languages.includes(lang);
}

/**
 * Resolves the effective locale to apply, without any side effect.
 *
 * The locale is resolved by priority: stored user preference, then locally persisted language, falling back to the
 * configured default locale when none is supported.
 *
 * @returns The locale code the caller should apply to its i18n target.
 */
export function resolveLocale(): string {
  const { userPreferenceStore } = useLinidUserPreference();
  const storedPreference = userPreferenceStore.userPreferences?.language;

  return (
    [storedPreference, localStorage.getItem('language')].find(
      isSupportedLanguage
    ) ?? appConfig.i18n.locale
  );
}

/**
 * Synchronises the persisted state with the given locale.
 *
 * The locale is always written to localStorage. The server-side user preference is updated only when it differs from
 * the currently stored one and a user is authenticated.
 *
 * @param locale - The locale to persist.
 * @returns A promise that resolves once the persisted state has been synchronised.
 */
export async function syncLocale(locale: string): Promise<void> {
  const { userPreferenceStore, saveUserPreference } = useLinidUserPreference();
  const storedPreference = userPreferenceStore.userPreferences?.language;

  localStorage.setItem('language', locale);

  if (locale !== storedPreference && (await authService.getUser())) {
    await saveUserPreference('language', locale);
  }
}
