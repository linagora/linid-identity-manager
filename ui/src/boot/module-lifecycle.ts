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

import { linidModuleFederation } from '@linagora/linid-im-front-corelib';
import { defineBoot } from '@quasar/app-vite/wrappers';
import ExportApplicationScriptBtn from 'components/btn/ExportApplicationScriptBtn.vue';
import { appConfig } from './config';

/**
 * Application bootstrapping entry point.
 *
 * Delegates the whole Module Federation setup to the corelib: the remotes and module configuration files declared in
 * the application configuration are registered and loaded, the host-local components are made available to zones, and
 * all lifecycle phases are executed sequentially for each module.
 *
 * The boot process is asynchronous and blocks application startup until all lifecycle phases have been completed.
 *
 * @param boot - The framework-provided boot context; the router is used to register module routes.
 * @returns Resolves once all modules have completed every lifecycle phase.
 */
export default defineBoot(async ({ router }): Promise<void> => {
  await linidModuleFederation.init({
    router,
    remotes: appConfig.remotes,
    modules: appConfig.modules,
    localComponents: { ExportApplicationScriptBtn },
  });
});
