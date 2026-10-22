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

package io.github.linagora.linid.im.sync.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Read-only entity mapped to the {@code account_computed_rights_view} view of the linid-api database.
 *
 * <p>The view holds one row per account and per application with a script and a non-null {@code deployed_at},
 * identified by the pair ({@code act_id}, {@code app_id}), including the accounts never computed and the applications
 * never computed for an account. It is empty when no application is deployed.</p>
 *
 * <p>{@code sync-api} reads it at each step of the synchronization: it gives the list of the accounts whose payload is
 * built, and tells for each pair whether its rights must be computed.</p>
 *
 * <p>This entity is immutable and intended strictly for read operations.</p>
 */
@Entity
@Immutable
@Table(name = "account_computed_rights_view")
@IdClass(AccountApplicationPairId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountComputedRightView {

    /**
     * Identifier of the account.
     */
    @Id
    @Column(name = "act_id")
    private UUID actId;

    /**
     * External identifier of the account, used to identify it in the alerts and the logs.
     */
    @Column(name = "external_id")
    private String externalId;

    /**
     * Identifier of the application, which has a script and a non-null {@code deployed_at}.
     */
    @Id
    @Column(name = "app_id")
    private UUID appId;

    /**
     * Functional unique identifier of the application, used by the OPA call.
     */
    @Column(name = "app_code")
    private String appCode;

    /**
     * Template used to render the claims of the application from the OPA result.
     */
    @Column(name = "claims_template")
    private String claimsTemplate;

    /**
     * Payload of the account, used as OPA input. {@code null} when the payload of the account was never built.
     */
    @Column(name = "payload", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    /**
     * Checksum of the payload of the account, stored with the result of the computation. {@code null} when the
     * payload of the account was never built.
     */
    @Column(name = "payload_checksum")
    private String payloadChecksum;

    /**
     * Checksum of the script of the application, stored with the result of the computation.
     */
    @Column(name = "script_checksum")
    private String scriptChecksum;

    /**
     * Checksum of the claims template of the application, stored with the result of the computation.
     */
    @Column(name = "claims_template_checksum")
    private String claimsTemplateChecksum;

    /**
     * Whether the rights of the pair must be computed: true when the account has a payload, and the pair was never
     * computed or its payload, script or claims template checksum differs from the last successful computation.
     */
    @Column(name = "to_compute")
    private boolean toCompute;
}
