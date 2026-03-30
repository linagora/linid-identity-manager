/* eslint-disable */
/*
 * Copyright (C) 2025 Linagora
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

import { BootFileParams } from '#q-app';
import {
  ModuleLifecyclePhase,
  type FederatedModule,
  type RemoteModule,
} from '@linagora/linid-im-front-corelib';
import { loadRemote } from '@module-federation/enhanced/runtime';
import { defineBoot } from '@quasar/app-vite/wrappers';
import * as ModuleLifecycleService from 'src/services/ModuleLifecycleService';

/**
 * Application bootstrapping entry point.
 *
 * This boot function is responsible for orchestrating the lifecycle
 * of all remote federated modules. It performs the following steps:
 *
 * 1. Retrieves module configurations from the lifecycle service.
 * 2. Dynamically loads each remote module’s lifecycle entry point.
 * 3. Executes all lifecycle phases sequentially for each module,
 *    ensuring deterministic and ordered initialization.
 *
 * The boot process is asynchronous and blocks application startup
 * until all lifecycle phases have been completed.
 *
 * @param boot - The framework-provided boot context, propagated to all lifecycle phases so modules can register services, routes, stores, or side effects.
 *
 * @returns Resolves once all modules have completed every lifecycle phase.
 */
export default defineBoot(async (boot: BootFileParams): Promise<void> => {
  const configurations = await ModuleLifecycleService.getModulesConfiguration();
  const modules = new Map();

  for (const configuration of configurations) {
    const module = await loadRemote<FederatedModule<RemoteModule<unknown>>>(
      `${configuration.remoteName}/lifecycle`
    );

    modules.set(configuration.instanceId, module?.default);
  }

  const phases = [
    ModuleLifecyclePhase.SETUP,
    ModuleLifecyclePhase.CONFIGURE,
    ModuleLifecyclePhase.INITIALIZE,
    ModuleLifecyclePhase.READY,
    ModuleLifecyclePhase.POST_INIT,
  ];

  for (const phase of phases) {
    for (const configuration of configurations) {
      const module = modules.get(configuration.instanceId);

      await ModuleLifecycleService[phase](module, configuration, boot);
    }
  }
});
