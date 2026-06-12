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
  useQuasarFieldValidation,
  useScopedI18n,
} from '@linagora/linid-im-front-corelib';
import { appConfig } from 'src/boot/config';
import type { FormField } from 'src/types/form';
import type { OrganizationalUnitForm } from 'src/types/organizationalUnits';

/**
 * Build the declarative field configuration for the organizational unit creation form. The parent identifier is
 * supplied by the navigation context and is therefore not part of the editable fields exposed here.
 *
 * @param i18nScope - I18n scope used for both labels (`fields.<name>`) and validator error messages
 *   (`validation.<rule>`).
 * @returns The ordered list of fields rendered by the creation form.
 */
export function useOrganizationalUnitCreationConfig(i18nScope: string) {
  const { t } = useScopedI18n(i18nScope);
  const { required } = useQuasarFieldValidation(i18nScope);

  const creationFields: FormField<OrganizationalUnitForm>[] = [
    {
      name: 'name',
      label: t('fields.name'),
      type: 'text',
      rules: [required],
    },
    {
      name: 'type',
      label: t('fields.type'),
      type: 'select',
      rules: [required],
      options: appConfig.organizationalUnitTypes,
    },
  ];

  return { creationFields };
}
