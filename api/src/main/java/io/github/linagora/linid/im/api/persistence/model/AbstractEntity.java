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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * Base entity with audit fields shared across all persistent entities.
 *
 * <p>Provides {@code createdBy}, {@code updatedBy}, {@code insertDate}, and {@code updateDate}
 * columns that are automatically managed by the service layer and database triggers.</p>
 */
@MappedSuperclass
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractEntity {

    /**
     * Identifier of the creator of this record.
     */
    @Column(name = "created_by", nullable = false)
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Creator of the record")
    private UUID createdBy;

    /**
     * Identifier of the last updater of this record.
     */
    @Column(name = "updated_by", nullable = false)
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Last updater of the record")
    private UUID updatedBy;

    /**
     * Timestamp when the record was created. Managed by PostgreSQL default.
     */
    @Generated(event = EventType.INSERT)
    @Column(name = "insert_date", updatable = false, insertable = false)
    @FilterType(type = Date.class)
    @QueryFilterField(type = Date.class, description = "Record creation date")
    private OffsetDateTime insertDate;

    /**
     * Timestamp when the record was last updated. Managed by PostgreSQL trigger.
     */
    @Version
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "update_date", insertable = false)
    @FilterType(type = Date.class)
    @QueryFilterField(type = Date.class, description = "Record last update date")
    private OffsetDateTime updateDate;
}
