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

package io.github.linagora.linid.im.sync.service;

import io.github.linagora.linid.im.sync.config.SyncDataProperties;
import io.github.linagora.linid.im.sync.exception.PayloadException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Default implementation of {@link PayloadService}, rendering the {@code mapping} with {@link JinjaService} and
 * computing the checksums with {@link ChecksumService}.
 */
@Service
public class PayloadServiceImpl implements PayloadService {

    /**
     * Property of the template of the payloads, named in the alerts.
     */
    private static final String MAPPING_PROPERTY = "sync-data.job.mapping";

    /**
     * Type of a payload, a JSON object.
     */
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    /**
     * Configuration holding the template of the payloads.
     */
    private final SyncDataProperties properties;

    /**
     * Service rendering the template of the payloads.
     */
    private final JinjaService jinjaService;

    /**
     * Service computing the checksums of the payloads.
     */
    private final ChecksumService checksumService;

    /**
     * Mapper parsing the rendered template.
     */
    private final JsonMapper jsonMapper;

    /**
     * Writer serializing a payload as compact JSON, with its keys sorted at every level. The arrays keep their order.
     */
    private final ObjectWriter sortedWriter;

    /**
     * Creates the service building the payloads from the configured template.
     *
     * @param properties      the configuration holding the template of the payloads
     * @param jinjaService    the service rendering the template
     * @param checksumService the service computing the checksums of the payloads
     * @param jsonMapper      the mapper parsing the rendered template and serializing the payloads
     */
    public PayloadServiceImpl(
        final SyncDataProperties properties,
        final JinjaService jinjaService,
        final ChecksumService checksumService,
        final JsonMapper jsonMapper) {
        this.properties = properties;
        this.jinjaService = jinjaService;
        this.checksumService = checksumService;
        this.jsonMapper = jsonMapper;
        this.sortedWriter = jsonMapper.writer(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    @Override
    public void checkMapping() {
        try {
            render(Map.of());
        } catch (RuntimeException e) {
            AlertService.invalidMapping("property=" + MAPPING_PROPERTY, e);
            throw new IllegalStateException("Invalid template " + MAPPING_PROPERTY, e);
        }
    }

    @Override
    public Map<String, Object> build(final Map<String, List<Map<String, Object>>> rowsBySource) {
        Map<String, Object> context = context(rowsBySource);
        try {
            return render(context);
        } catch (RuntimeException e) {
            throw new PayloadException(e.getMessage(), e);
        }
    }

    @Override
    public String checksum(final Map<String, Object> payload) {
        return checksumService.compute(sortedWriter.writeValueAsString(payload));
    }

    /**
     * Builds the context of the template for an account: each source is a variable named after it, holding the
     * columns of the row of the account, empty when the account has no row in the source.
     *
     * @param rowsBySource the rows of the account in each source, by source name
     * @return the variables of the template
     * @throws PayloadException when the account has several rows in a source
     */
    private static Map<String, Object> context(final Map<String, List<Map<String, Object>>> rowsBySource) {
        Map<String, Object> context = new HashMap<>();
        rowsBySource.forEach((source, sourceRows) -> {
            if (sourceRows.size() > 1) {
                throw new PayloadException("The account has " + sourceRows.size() + " rows in the source " + source);
            }
            context.put(source, sourceRows.stream().findFirst().orElse(Map.of()));
        });
        return context;
    }

    /**
     * Renders the template with the given context, and parses it as a JSON object.
     *
     * @param context the variables of the template
     * @return the payload
     * @throws IllegalArgumentException when the rendered text is {@code null}, which Jackson parses without error
     */
    private Map<String, Object> render(final Map<String, Object> context) {
        Map<String, Object> payload = jsonMapper.readValue(
            jinjaService.render(properties.mapping(), context), PAYLOAD_TYPE);
        if (payload == null) {
            throw new IllegalArgumentException("The template is not rendered as a JSON object");
        }
        return payload;
    }
}
