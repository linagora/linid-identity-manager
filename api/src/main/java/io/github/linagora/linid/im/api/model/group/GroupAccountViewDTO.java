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

package io.github.linagora.linid.im.api.model.group;

import io.github.linagora.linid.im.api.persistence.model.AccountStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing an account attached to a group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Account attached to a group")
public class GroupAccountViewDTO {

    /**
     * Unique identifier of the account.
     */
    @Schema(description = "Unique identifier of the account", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    /**
     * External system identifier of the account.
     */
    @Schema(description = "External system identifier of the account", example = "user1")
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
     * Email address of the account.
     */
    @Schema(description = "Email address of the account", example = "john.doe@example.com")
    private String email;

    /**
     * Computed account status (ACTIVE, SUSPENDED or INACTIVE).
     */
    @Schema(description = "Computed account status", example = "ACTIVE")
    private AccountStatusEnum status;

    /**
     * Additional deployment-specific attributes associated with the account.
     */
    @Schema(description = "Additional deployment-specific attributes of the account stored as JSON")
    private Map<String, Object> extraParameters;

    /**
     * Additional deployment-specific attributes associated with the relationship between the account
     * and the group.
     */
    @Schema(
        description = "Additional deployment-specific attributes associated with "
            + "the account-to-group relationship stored as JSON"
    )
    private Map<String, Object> relationExtraParameters;

    /**
     * Full name of the account that attached the account to the group.
     */
    @Schema(description = "Full name of the account that attached the account to the group", example = "John Doe")
    private String createdBy;

    /**
     * Full name of the account that last updated the relationship.
     */
    @Schema(description = "Full name of the account that last updated the relationship", example = "John Doe")
    private String updatedBy;

    /**
     * Date and time when the account was attached to the group.
     */
    @Schema(description = "Date and time when the account was attached to the group")
    private OffsetDateTime insertDate;

    /**
     * Date and time when the relationship was last updated.
     */
    @Schema(description = "Date and time when the relationship was last updated")
    private OffsetDateTime updateDate;
}
