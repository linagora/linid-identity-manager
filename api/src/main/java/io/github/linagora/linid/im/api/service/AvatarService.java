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

package io.github.linagora.linid.im.api.service;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service storing the avatar images of the supported entities on the filesystem.
 *
 * <p>It only handles the files: checking that the target entity exists is the responsibility of the caller, which
 * keeps this service free of any dependency on the services it would otherwise form a cycle with.</p>
 */
public interface AvatarService {

    /**
     * Stores the avatar image of an entity, replacing any existing one.
     *
     * @param entity the entity type, one of {@code accounts}, {@code applications},
     *               {@code organizational-units} or {@code groups}
     * @param id     the entity UUID
     * @param file   the uploaded image
     */
    void upload(String entity, UUID id, MultipartFile file);

    /**
     * Deletes the avatar image of an entity, if any.
     *
     * @param entity the entity type, one of {@code accounts}, {@code applications},
     *               {@code organizational-units} or {@code groups}
     * @param id     the entity UUID
     */
    void delete(String entity, UUID id);

    /**
     * Deletes the avatar image of an entity that has just been removed, if any.
     *
     * <p>Called by the services deleting an entity, so its avatar does not stay behind on the filesystem. Unlike
     * {@link #delete}, a failure is logged and swallowed: the entity is already deleted, the request must not fail
     * for a leftover file.</p>
     *
     * @param entity the entity type, one of {@code accounts}, {@code applications},
     *               {@code organizational-units} or {@code groups}
     * @param id     the entity UUID
     */
    void deleteQuietly(String entity, UUID id);
}
