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

package io.github.linagora.linid.im.api.model.account;

import io.github.linagora.linid.im.api.model.common.PeriodDTO;
import io.github.linagora.linid.im.api.persistence.model.AccountStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing an account in API responses,
 * with enriched createdBy and updatedBy fields as full names instead of UUIDs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Account view data transfer object with enriched creator and updater information")
public class AccountViewDTO {
    /**
     * Unique identifier of the account.
     */
    @Schema(description = "Unique identifier of the account", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    /**
     * External identifier (e.g. OIDC sub or external system ID).
     */
    @Schema(description = "External identifier (e.g. OIDC sub)", example = "ext-001")
    private String externalId;

    /**
     * Last name of the account holder.
     */
    @Schema(description = "Last name of the account holder", example = "Doe")
    private String lastname;

    /**
     * First name of the account holder.
     */
    @Schema(description = "First name of the account holder", example = "John")
    private String firstname;

    /**
     * Email address associated with the account.
     */
    @Schema(description = "Email address of the account", example = "john.doe@example.com")
    private String email;

    /**
     * Time range during which the account is considered valid.
     */
    @Schema(description = "Time range during which the account is considered valid")
    private PeriodDTO validityPeriod;

    /**
     * Time range during which the account is suspended.
     */
    @Schema(description = "Time range during which the account is suspended")
    private PeriodDTO suspensionPeriod;

    /**
     * Timestamp when the account was activated or reactivated.
     */
    @Schema(description = "Activation timestamp", example = "2025-02-01T00:00:00Z")
    private OffsetDateTime activationAt;

    /**
     * High-level reason code explaining the suspension.
     */
    @Schema(description = "High-level suspension reason code", example = "ONBOARDING")
    private String suspensionReason;

    /**
     * More detailed classification of the suspension reason.
     */
    @Schema(description = "Detailed classification of the suspension reason", example = "FIRST_ACTIVATION")
    private String suspensionSubreason;

    /**
     * Free-text comment providing additional context about the suspension.
     */
    @Schema(description = "Free-text suspension comment", example = "Suspended pending investigation")
    private String suspensionComment;

    /**
     * High-level reason code explaining the deactivation.
     */
    @Schema(description = "High-level deactivation reason code", example = "OFFBOARDING")
    private String deactivationReason;

    /**
     * More detailed classification of the deactivation reason.
     */
    @Schema(description = "Detailed classification of the deactivation reason", example = "CONTRACT_END")
    private String deactivationSubreason;

    /**
     * Free-text comment providing additional context about the deactivation.
     */
    @Schema(description = "Free-text deactivation comment", example = "Deactivated after contract termination")
    private String deactivationComment;

    /**
     * Free-text comment providing additional context about the reactivation.
     */
    @Schema(description = "Free-text reactivation comment", example = "Reactivated after KYC approval")
    private String reactivationComment;

    /**
     * Computed account status (ACTIVE, SUSPENDED or INACTIVE).
     */
    @Schema(description = "Computed account status", example = "ACTIVE")
    private AccountStatusEnum status;

    /**
     * Integer number of calendar days before the validity period's upper bound.
     */
    @Schema(description = "Days before the validity period upper bound; can be negative; null when unbounded",
        example = "30")
    private Integer daysBeforeDeactivation;

    /**
     * Identifier of the creator of this record.
     */
    @Schema(description = "Full name of the record last creator", example = "John Doe")
    private String createdBy;

    /**
     * Identifier of the last updater of this record.
     */
    @Schema(description = "Full name of the record last updater", example = "John Doe")
    private String updatedBy;

    /**
     * Timestamp when the record was created.
     */
    @Schema(description = "Record creation date")
    private OffsetDateTime insertDate;

    /**
     * Timestamp when the record was last updated.
     */
    @Schema(description = "Record last update date")
    private OffsetDateTime updateDate;
}
