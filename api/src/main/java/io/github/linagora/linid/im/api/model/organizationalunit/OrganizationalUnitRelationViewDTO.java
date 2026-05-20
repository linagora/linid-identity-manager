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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a direct hierarchical relationship between two organizational units.
 * <p>This entity defines a parent-child association within the organizational
 * structure graph. Each relation represents a direct edge in the directed
 * acyclic graph (DAG) used to model organizational hierarchies.
 *
 * <p>The entity is mapped to the {@code organizational_unit_relations} table.
 * A unique constraint at the database level guarantees that the same
 * parent-child relationship cannot be inserted multiple times.
 *
 * <p>This table acts as the source of truth for the hierarchy structure,
 * while transitive relationships are maintained separately in the
 * closure table.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "OrganizationalUnitRelationView",
    description = "Represents a direct parent-child relationship between two "
        + "organizational units within the hierarchy graph."
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganizationalUnitRelationViewDTO implements Serializable {

    /**
     * Unique identifier of the organizational unit relation, auto-generated as UUID.
     */
    @Schema(description = "Unique identifier of the organizational unit relation",
        example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    /**
     * Identifier of the parent organizational unit.
     */
    @Schema(description = "Identifier of the parent organizational unit",
        example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID parent;
}
