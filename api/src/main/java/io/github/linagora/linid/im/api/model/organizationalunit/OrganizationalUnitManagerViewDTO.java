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

package io.github.linagora.linid.im.api.model.organizationalunit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a manager of an organizational unit, with the identity attributes of
 * the manager account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Manager of an organizational unit, with the identity attributes of the manager account")
public class OrganizationalUnitManagerViewDTO {

    /**
     * Unique identifier of the manager record.
     */
    @Schema(description = "Unique identifier of the manager record", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    /**
     * Identifier of the manager account.
     */
    @Schema(description = "Identifier of the manager account", example = "6f1c2a3b-4d5e-4f60-8a7b-9c0d1e2f3a4b")
    private UUID accountId;

    /**
     * First name of the manager.
     */
    @Schema(description = "First name of the manager", example = "John")
    private String firstname;

    /**
     * Last name of the manager.
     */
    @Schema(description = "Last name of the manager", example = "Doe")
    private String lastname;

    /**
     * Email address of the manager account.
     */
    @Schema(description = "Email address of the manager account", example = "john.doe@example.com")
    private String email;

    /**
     * Full name of the account that added the manager.
     */
    @Schema(description = "Full name of the account that added the manager", example = "Jane Smith")
    private String createdBy;

    /**
     * Full name of the account that last updated the manager record.
     */
    @Schema(description = "Full name of the account that last updated the manager record", example = "Jane Smith")
    private String updatedBy;

    /**
     * Date and time when the manager was added.
     */
    @Schema(description = "Date and time when the manager was added")
    private OffsetDateTime insertDate;

    /**
     * Date and time when the manager record was last updated.
     */
    @Schema(description = "Date and time when the manager record was last updated")
    private OffsetDateTime updateDate;
}
