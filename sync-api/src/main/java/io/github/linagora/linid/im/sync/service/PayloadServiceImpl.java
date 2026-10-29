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
import io.github.linagora.linid.im.sync.model.payload.PayloadItem;
import io.github.linagora.linid.im.sync.persistence.model.AccountIdentity;
import io.github.linagora.linid.im.sync.persistence.repository.SourceRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Default implementation of {@link PayloadService}, reading the sources with {@link SourceRepository} and rendering the
 * {@code mapping} with {@link JinjaService}.
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
     * Configuration holding the sources and the template of the payloads.
     */
    private final SyncDataProperties properties;

    /**
     * Repository reading the rows of a source.
     */
    private final SourceRepository sourceRepository;

    /**
     * Service rendering the template of the payloads.
     */
    private final JinjaService jinjaService;

    /**
     * Mapper parsing the rendered template.
     */
    private final JsonMapper jsonMapper;

    /**
     * Creates the service building the payloads from the configured template.
     *
     * @param properties       the configuration holding the sources and the template of the payloads
     * @param sourceRepository the repository reading the rows of a source
     * @param jinjaService     the service rendering the template
     * @param jsonMapper       the mapper parsing the rendered template
     */
    public PayloadServiceImpl(
        final SyncDataProperties properties,
        final SourceRepository sourceRepository,
        final JinjaService jinjaService,
        final JsonMapper jsonMapper) {
        this.properties = properties;
        this.sourceRepository = sourceRepository;
        this.jinjaService = jinjaService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public List<PayloadItem> build(final List<AccountIdentity> accounts) {
        checkMapping();
        Map<String, Map<UUID, List<Map<String, Object>>>> rowsBySource =
            readSources(accounts.stream().map(AccountIdentity::actId).toList());
        List<PayloadItem> items = new ArrayList<>();
        for (AccountIdentity account : accounts) {
            try {
                Map<String, Object> payload = render(context(rowsBySource, account.actId()));
                items.add(new PayloadItem(account.actId(), account.externalId(), payload));
            } catch (RuntimeException e) {
                AlertService.payloadError("account=" + account.externalId(), e);
            }
        }
        return items;
    }

    /**
     * Checks the template by rendering it with an empty context, where each source gives {@code null} through
     * {@code tojson}, so that a valid template gives a JSON object.
     *
     * @throws IllegalStateException when the template is invalid
     */
    private void checkMapping() {
        try {
            render(Map.of());
        } catch (RuntimeException e) {
            AlertService.invalidMapping("property=" + MAPPING_PROPERTY, e);
            throw new IllegalStateException("Invalid template " + MAPPING_PROPERTY, e);
        }
    }

    /**
     * Reads every source of the configuration once for the given accounts, and groups their rows by account.
     *
     * @param actIds the identifiers of the accounts
     * @return the rows of each source by source name, then by {@code act_id}, an account without row in a source being
     *     absent from its map
     * @throws IllegalStateException when a source cannot be read
     */
    private Map<String, Map<UUID, List<Map<String, Object>>>> readSources(final Collection<UUID> actIds) {
        Map<String, Map<UUID, List<Map<String, Object>>>> rowsBySource = new HashMap<>();
        properties.sources().forEach((source, settings) ->
            rowsBySource.put(source, readSource(source, settings.accountIdColumn(), actIds)));
        return rowsBySource;
    }

    /**
     * Reads the rows of a source for the given accounts, and groups them by account.
     *
     * @param source          the name of the source
     * @param accountIdColumn the column of the source holding the {@code act_id} of the account
     * @param actIds          the identifiers of the accounts
     * @return the rows of the source by {@code act_id}
     * @throws IllegalStateException naming the source, after logging the {@code sourceUnreadable} alert
     */
    private Map<UUID, List<Map<String, Object>>> readSource(
        final String source,
        final String accountIdColumn,
        final Collection<UUID> actIds) {
        try {
            return sourceRepository.findAllByAccountIds(source, accountIdColumn, actIds).stream()
                .collect(Collectors.groupingBy(row -> (UUID) row.get(accountIdColumn)));
        } catch (RuntimeException e) {
            AlertService.sourceUnreadable("source=" + source, e);
            throw new IllegalStateException("Unable to read the source " + source, e);
        }
    }

    /**
     * Builds the context of the template for an account: each source is a variable named after it, holding the
     * columns of the row of the account, empty when the account has no row in the source.
     *
     * @param rowsBySource the rows of each source, grouped by account
     * @param actId        the identifier of the account
     * @return the variables of the template
     * @throws IllegalStateException when the account has several rows in a source
     */
    private static Map<String, Object> context(
        final Map<String, Map<UUID, List<Map<String, Object>>>> rowsBySource,
        final UUID actId) {
        Map<String, Object> context = new HashMap<>();
        rowsBySource.forEach((source, rowsByAccount) -> {
            List<Map<String, Object>> rows = rowsByAccount.getOrDefault(actId, List.of());
            if (rows.size() > 1) {
                throw new IllegalStateException("The account has " + rows.size() + " rows in the source " + source);
            }
            context.put(source, rows.stream().findFirst().orElse(Map.of()));
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
