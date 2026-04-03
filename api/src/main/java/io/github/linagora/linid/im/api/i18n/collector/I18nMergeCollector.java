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

package io.github.linagora.linid.im.api.i18n.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collector;

/**
 * Utility class that provides a custom {@link java.util.stream.Collector}
 * implementation to merge multiple
 * {@code Map<String, Map<String, String>>} structures into a single one.
 *
 * <p>
 * This is particularly useful for combining language-to-translation maps from
 * different sources in an internationalization (i18n)
 * context.
 */
public final class I18nMergeCollector {

    private I18nMergeCollector() {
    }

    /**
     * Returns a {@link java.util.stream.Collector} that merges multiple
     * {@code Map<String, Map<String, String>>} into a single
     * aggregated map.
     *
     * <p>
     * For each input map, all entries are merged by language code. If a language
     * already exists in the accumulator, its
     * translations are merged (overwritten if duplicated).
     *
     * @return a collector that performs a deep merge of maps of
     *         language-to-translations.
     */
    public static Collector<Map<String, Map<String, String>>, ?, Map<String, Map<String, String>>> toCollect() {
        return Collector.of(
                HashMap::new,
                I18nMergeCollector::merge,
                I18nMergeCollector::merge);
    }

    /**
     * Merges a source map into an accumulator map. For each language key, the
     * translations are added or merged into the
     * accumulator.
     *
     * @param acc    the accumulator map that collects the merged results
     * @param inputs the input map to merge into the accumulator
     * @return the updated accumulator with merged content
     */
    public static Map<String, Map<String, String>> merge(final Map<String, Map<String, String>> acc,
            final Map<String, Map<String, String>> inputs) {
        inputs.forEach((language, translation) -> acc
                .computeIfAbsent(language, k -> new HashMap<>())
                .putAll(translation));

        return acc;
    }
}
