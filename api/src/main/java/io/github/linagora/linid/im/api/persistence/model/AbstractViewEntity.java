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

package io.github.linagora.linid.im.api.persistence.model;

import io.github.zorin95670.predicate.FilterType;
import io.github.zorin95670.processor.annotation.QueryFilterField;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base class for JPA entities representing database views.
 *
 * <p>Entities extending this class are mapped to database views rather than tables. This allows them to
 * be used for read-only queries while still benefiting from JPA's mapping capabilities. Concrete view
 * entities should extend this class and be annotated with {@code @Entity} and {@code @Immutable}.</p>
 */
@MappedSuperclass
@Getter
@EqualsAndHashCode
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractViewEntity {

    /**
     * Identifier of the creator of this record.
     */
    @Column(name = "created_by")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Full name of the record creator")
    private String createdBy;

    /**
     * Identifier of the last updater of this record.
     */
    @Column(name = "updated_by")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Full name of the record last updater")
    private String updatedBy;

    /**
     * Timestamp when the record was created. Managed by PostgreSQL default.
     */
    @Column(name = "insert_date", updatable = false, insertable = false)
    @FilterType(type = Date.class)
    @QueryFilterField(type = Date.class, description = "Record creation date")
    private OffsetDateTime insertDate;

    /**
     * Timestamp when the record was last updated. Managed by PostgreSQL trigger.
     */
    @Version
    @Column(name = "update_date", insertable = false)
    @FilterType(type = Date.class)
    @QueryFilterField(type = Date.class, description = "Record last update date")
    private OffsetDateTime updateDate;
}
