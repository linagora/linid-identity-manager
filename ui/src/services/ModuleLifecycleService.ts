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

import type {
  FederatedModule,
  LinidRoute,
  ModuleHostConfig,
  RemoteModule,
} from '@linagora/linid-im-front-corelib';
import {
  getI18nInstance,
  getNunjucksEnv,
  merge,
  registerModuleHostConfiguration,
  renameKeys,
  useLinidZoneStore,
} from '@linagora/linid-im-front-corelib';
import { loadRemote } from '@module-federation/enhanced/runtime';
import type { Component } from 'vue';
import type { RouteMeta, RouteRecordRaw } from 'vue-router';
import type { BootFileParams } from '#q-app';
/**
 * Loads and aggregates configuration files for all federated modules.
 *
 * This function performs a multi-step fetch process:
 *
 * 1. Fetches the root `/modules.json` manifest.
 * 2. Extracts the list of module configuration file URLs.
 * 3. Fetches each module configuration in parallel.
 * 4. Filters out any modules that failed to load.
 *
 * The function is intentionally fault-tolerant:
 *
 * - A failure to load an individual module configuration does not abort the overall process.
 * - If the root manifest cannot be loaded, an empty configuration list is returned.
 *
 * @returns A promise resolving to the list of successfully loaded module host configurations.
 */
export async function getModulesConfiguration(): Promise<
  ModuleHostConfig<unknown>[]
> {
  try {
    const response = await fetch('/modules.json');

    if (!response.ok) {
      throw new Error('Failed to fetch /modules.json');
    }

    const {
      modules,
    }: {
      /** Modules files. */
      modules: string[];
    } = await response.json();

    const moduleConfigs = await Promise.all(
      modules.map(async (moduleFile) => {
        try {
          const moduleResponse = await fetch(moduleFile);

          if (!moduleResponse.ok) {
            return null;
          }

          console.debug(
            `[Module Lifecycle] Loaded config for module: ${moduleResponse.url}`
          );
          return moduleResponse.json();
        } catch {
          console.error(
            `[Module Lifecycle] Config file not found: ${moduleFile}`
          );
          return null;
        }
      })
    );

    // Filter out failed fetches
    return moduleConfigs.filter(
      (config): config is ModuleHostConfig<unknown> => config !== null
    );
  } catch (error) {
    console.error(
      '[Module Lifecycle] Failed to load module configurations:',
      error
    );
    return [];
  }
}

/**
 * Loads route definitions exposed by a remote federated module.
 *
 * This function attempts to dynamically import the remote module's route entry point and extract its default export.
 *
 * If the remote module does not expose any routes (or the export is missing or empty), the function returns `null` to
 * signal that no routes should be registered for this module.
 *
 * @param config - The host configuration describing the remote module, including its remote name and instance metadata.
 * @returns A promise resolving to the list of routes exposed by the module, or `null` if the module defines no routes.
 */
async function getRoutes(
  config: ModuleHostConfig<unknown>
): Promise<LinidRoute[] | null> {
  const routes = await loadRemote<FederatedModule<LinidRoute[]>>(
    `${config.remoteName}/routes`
  );

  if (!routes?.default || routes.default.length === 0) {
    return null;
  }

  return routes.default;
}

/**
 * Fetches i18n messages from a remote module.
 *
 * @param config - Configuration object for the remote module.
 * @returns A promise that resolves to the i18n messages object. Returns an empty object if no messages are found.
 */
async function getI18nMessages(
  config: ModuleHostConfig<unknown>
): Promise<object> {
  const messages = await loadRemote<FederatedModule<object>>(
    `${config.remoteName}/i18n`
  );

  if (!messages?.default) {
    return {};
  }

  return messages.default;
}

/**
 * Converts a LinidRoute to a Vue Router RouteRecordRaw.
 *
 * Applies Nunjucks templating to paths and loads components asynchronously via Module Federation.
 *
 * @param route - The LinidRoute to convert.
 * @param config - Module host configuration for templating.
 * @returns Promise resolving to Vue Router route record.
 */
export function toRouteRecordRaw(
  route: LinidRoute,
  config: ModuleHostConfig<unknown>
): RouteRecordRaw {
  return {
    name: route.name,
    path: getNunjucksEnv().renderString(route.path, { config }),
    component: async () =>
      (await loadRemote<FederatedModule<Component>>(route.component))!.default,
    children:
      route.children?.map((child) => toRouteRecordRaw(child, config)) || [],
    meta: route.meta
      ? (renderMeta(route.meta, config) as RouteMeta)
      : undefined,
  };
}

/**
 * Recursively renders all string values in an object or array using Nunjucks templating.
 *
 * This is useful for processing route `meta` objects so that template variables (like `{{ config.basePath }}`) are
 * replaced with actual values from the module host configuration.
 *
 * @param obj - The object, array, or string to render. Can be nested.
 * @param config - The ModuleHostConfig object used as the template context.
 * @returns A new object/array/string with all strings rendered using Nunjucks.
 */
export function renderMeta(
  obj: unknown,
  config: ModuleHostConfig<unknown>
): unknown {
  if (typeof obj === 'string') {
    return getNunjucksEnv().renderString(obj, { config });
  }

  if (Array.isArray(obj)) {
    return obj.map((v) => renderMeta(v, config));
  }

  if (typeof obj === 'object' && obj !== null) {
    const result: Record<string, unknown> = {};

    for (const key in obj) {
      result[key] = renderMeta((obj as Record<string, unknown>)[key], config);
    }

    return result;
  }

  return obj; // number, boolean, etc.
}

/**
 * Executes the SETUP lifecycle phase for a remote module.
 *
 * This phase is the earliest point in the module lifecycle and is executed immediately after the module is loaded.
 *
 * Responsibilities of this phase:
 *
 * - Register the module host configuration for later lifecycle phases.
 * - Perform early validation and preparation logic inside the module.
 *
 * No application artifacts (routes, stores, etc.) should be registered during this phase.
 *
 * @param module - The remote module lifecycle implementation.
 * @param config - The host configuration associated with this module instance.
 * @param _boot - The application boot context.
 * @returns A promise resolving to the module's setup result.
 */
export async function setup(
  module: RemoteModule<unknown>,
  config: ModuleHostConfig<unknown>,
  _boot: BootFileParams
) {
  registerModuleHostConfiguration(config);

  return module.setup();
}

/**
 * Executes the CONFIGURE lifecycle phase for a remote module.
 *
 * During this phase, the module is configured with host-provided settings and may contribute application-level
 * artifacts such as routes.
 *
 * Responsibilities of this phase:
 *
 * - Load and register module routes, if any are exposed.
 * - Pass validated host configuration to the module.
 *
 * This phase is executed after SETUP and before INITIALIZE.
 *
 * @param module - The remote module lifecycle implementation.
 * @param config - The host configuration associated with this module instance.
 * @param boot - The application boot context, used here to register routes.
 * @returns A promise resolving to the module's configuration result.
 */
export async function configure(
  module: RemoteModule<unknown>,
  config: ModuleHostConfig<unknown>,
  boot: BootFileParams
) {
  const routes = await getRoutes(config);

  if (routes) {
    routes
      .map((route) => toRouteRecordRaw(route, config))
      .forEach((routeRecord) => {
        if (routeRecord.name && boot.router.hasRoute(routeRecord.name)) {
          boot.router.removeRoute(routeRecord.name);
        }
        boot.router.addRoute(routeRecord);
      });
  }

  const i18nMessages = renameKeys(
    await getI18nMessages(config),
    (key: string) => getNunjucksEnv().renderString(key, { config })
  );

  if (i18nMessages) {
    const i18n = getI18nInstance();

    Object.keys(i18n.global.messages.value).forEach((lang: string) => {
      const messages = merge(
        // @ts-expect-error `i18nMessages` is a dynamically loaded object indexed by locale.
        // The index signature is not known at compile time.
        i18nMessages[lang],
        // @ts-expect-error Same typing issue: `messages` is inferred as `{}` without a string index signature by vue-i18n.
        i18n.global.messages.value[lang]
      );

      i18n.global.setLocaleMessage(lang, messages);
    });
  }

  return module.configure(config);
}

/**
 * Executes the INITIALIZE lifecycle phase for a remote module.
 *
 * During this phase, the module should initialize its core functionality and register any required runtime resources.
 *
 * Typical responsibilities include:
 *
 * - Registering Pinia stores.
 * - Initializing services or SDKs.
 * - Allocating long-lived resources.
 *
 * This phase is executed after CONFIGURE and before READY.
 *
 * @param module - The remote module lifecycle implementation.
 * @param config - The host configuration associated with this module instance.
 * @param _boot - The application boot context.
 * @returns A promise resolving to the module's initialization result.
 */
export async function initialize(
  module: RemoteModule<unknown>,
  config: ModuleHostConfig<unknown>,
  _boot: BootFileParams
) {
  return module.initialize(config);
}

/**
 * Executes the READY lifecycle phase for a remote module.
 *
 * This phase signals that the module is fully initialized and ready for interaction.
 *
 * At this point:
 *
 * - All modules have completed INITIALIZE.
 * - Shared application services are available.
 *
 * Use this phase for:
 *
 * - Emitting ready events.
 * - Performing final validation.
 *
 * @param module - The remote module lifecycle implementation.
 * @param config - The host configuration associated with this module instance.
 * @param _boot - The application boot context.
 * @returns A promise resolving to the module's ready result.
 */
export async function ready(
  module: RemoteModule<unknown>,
  config: ModuleHostConfig<unknown>,
  _boot: BootFileParams
) {
  return module.ready(config);
}

/**
 * Executes the POST_INIT lifecycle phase for a remote module.
 *
 * This phase is executed after all modules have reached READY.
 *
 * Use this phase for:
 *
 * - Cross-module integrations.
 * - Late-bound dependencies.
 * - Final application wiring that requires all modules to be available.
 *
 * @param module - The remote module lifecycle implementation.
 * @param config - The host configuration associated with this module instance.
 * @param _boot - The application boot context.
 * @returns A promise resolving to the module's post-initialization result.
 */
export async function postInit(
  module: RemoteModule<unknown>,
  config: ModuleHostConfig<unknown>,
  _boot: BootFileParams
) {
  const linidZoneStore = useLinidZoneStore();

  config.zones?.forEach(({ zone: zoneName, plugin, props }) => {
    linidZoneStore.register(zoneName, {
      plugin,
      props,
    });
  });

  return module.postInit(config);
}
