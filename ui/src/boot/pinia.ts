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
  setPiniaStore,
  useLinidZoneStore,
} from '@linagora/linid-im-front-corelib';
import AccountDetailsBadge from 'src/components/details/AccountDetailsBadge.vue';
import AccountLifecyclePanel from 'src/components/details/AccountLifecyclePanel.vue';
import OrganizationalUnitCreateChildButton from 'src/components/details/OrganizationalUnitCreateChildButton.vue';
import OrganizationalUnitDetailsBadge from 'src/components/details/OrganizationalUnitDetailsBadge.vue';
import OrganizationalUnitLifecyclePanel from 'src/components/details/OrganizationalUnitLifecyclePanel.vue';
import { defineBoot } from '#q-app/wrappers';

/**
 * Boot file that initializes Pinia in the corelib and registers the host zone entries: the shared dialogs, and the host
 * components injected in the generic details page zones.
 */
export default defineBoot(async ({ app }) => {
  setPiniaStore(app.config.globalProperties.$pinia);
  const linidZoneStore = useLinidZoneStore();

  linidZoneStore.registerPluginOnce(
    'base-layout.dialogComponent',
    'catalogUI/ConfirmationDialog'
  );
  linidZoneStore.registerPluginOnce(
    'base-layout.dialogComponent',
    'catalogUI/FormDialog'
  );

  linidZoneStore.registerComponent(
    'moduleAccountDetailsPage.titleAppend',
    AccountDetailsBadge
  );
  linidZoneStore.registerComponent(
    'moduleAccountDetailsPage.extraContent',
    AccountLifecyclePanel
  );

  linidZoneStore.registerComponent(
    'moduleOrganizationalUnitDetailsPage.titleAppend',
    OrganizationalUnitDetailsBadge
  );
  linidZoneStore.registerComponent(
    'moduleOrganizationalUnitDetailsPage.extraButtons',
    OrganizationalUnitCreateChildButton
  );
  linidZoneStore.registerComponent(
    'moduleOrganizationalUnitDetailsPage.extraContent',
    OrganizationalUnitLifecyclePanel
  );
});
