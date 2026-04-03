/*
 * Copyright (C) 2020-2026 Linagora
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

package io.github.linagora.linid.im.api.i18n.service;

import io.github.linagora.linid.im.api.i18n.collector.I18nMergeCollector;
import io.github.linagora.linid.im.api.i18n.loader.I18nSourceLoader;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.i18n.I18nService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

/**
 * Implementation of the {@link I18nService} that loads and provides i18n translations from multiple
 * sources in a defined order.
 *
 * <p>
 * This service merges translations from different loaders (e.g., plugin, external, internal) and provides access to
 * language-specific translation maps.
 *
 * <p>
 * It implements {@link CommandLineRunner} to initialize the merged i18n data on application startup.
 */
@Service
public class I18nServiceImpl implements I18nService, CommandLineRunner {

    /**
     * Ordered list of loader types (e.g., "plugin", "external", "internal") to
     * determine the priority of translation sources
     * during merging.
     */
    private final List<String> orders;

    /**
     * Available i18n source loaders, each capable of loading translations for a given type.
     */
    private final List<I18nSourceLoader> loaders;

    /**
     * Merged i18n translations from all loaders, organized by language code.
     */
    private Map<String, Map<String, String>> languages;

    /**
     * Constructs the I18nServiceImpl with the configured loader order and available
     * loaders.
     *
     * @param orders  the ordered list of loader types, from highest to lowest
     *                priority.
     * @param loaders the list of all available {@link I18nSourceLoader} beans.
     */
    @Autowired
    public I18nServiceImpl(
            final @Value("#{'${i18n.merge.order:plugin,external,internal}'.split(',')}") List<String> orders,
            final List<I18nSourceLoader> loaders) {
        this.orders = orders;
        this.loaders = loaders;
    }

    @Override
    public List<String> getLanguages() {
        return languages.keySet().stream().toList();
    }

    @Override
    public Map<String, String> getTranslations(final String language) {
        if (languages.containsKey(language)) {
            return languages.get(language);
        }

        throw new ApiException(HttpStatus.NOT_FOUND.value(), I18nMessage.of(
                "error.router.unknown.language",
                Map.of("language", language)));
    }

    @Override
    public String translate(final I18nMessage message) {
        return this.translate("en", message);
    }

    @Override
    public String translate(final String language, final I18nMessage message) {
        var templates = languages.getOrDefault(language, Map.of());

        if (!templates.containsKey(message.key())) {
            return String.format("Unknown key \"%s\" for language \"%s\".", message.key(), language);
        }

        var template = templates.getOrDefault(message.key(), "error.i18n.invalid");
        var context = new HashMap<>(message.context());

        context.put("I18N_LANGUAGE", language);
        context.put("I18N_KEY", message.key());

        return new StringSubstitutor(context, "{", "}").replace(template);
    }

    @Override
    public void run(final String... args) {
        languages = orders.stream()
                .map(type -> loaders.stream()
                        .filter(loader -> loader.supports(type))
                        .findAny())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(I18nSourceLoader::load)
                .collect(I18nMergeCollector.toCollect());
    }
}
