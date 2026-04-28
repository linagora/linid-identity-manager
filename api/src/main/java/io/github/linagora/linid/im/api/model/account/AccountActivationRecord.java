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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * Request payload for the {@code PUT /accounts/{id}/status/activation} endpoint.
 *
 * <p>Business rules (enforced server-side):
 * <ul>
 *   <li>The current {@code activationAt} on the account must be {@code null}.</li>
 *   <li>The validity period and its start must be set.</li>
 *   <li>The validity period's start must be less than or equal to {@code now()}.</li>
 *   <li>The provided {@code activationAt} must be greater than or equal to the validity period's start.</li>
 *   <li>The provided {@code activationAt} must be less than or equal to {@code now()}.</li>
 * </ul>
 *
 * @param activationAt the requested activation timestamp
 */
@Schema(description = "Request payload for activating an account")
public record AccountActivationRecord(
    @NotNull
    @Schema(description = "Activation timestamp; must be >= validityPeriod.start",
        example = "2025-02-01T00:00:00Z")
    OffsetDateTime activationAt) {
}
