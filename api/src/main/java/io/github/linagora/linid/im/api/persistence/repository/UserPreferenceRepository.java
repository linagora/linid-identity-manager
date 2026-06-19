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

package io.github.linagora.linid.im.api.persistence.repository;

import io.github.linagora.linid.im.api.persistence.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UserPreference}.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic filtering
 * via {@code spring-query-filter} specifications.</p>
 */
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID>,
        JpaSpecificationExecutor<UserPreference> {

    /**
     * Retrieves all {@link UserPreference} entries belonging to the given user.
     *
     * @param email the email address identifying the user whose preferences are fetched
     * @return a list of every {@link UserPreference} for that user,
     *     or an empty list if the user has no preferences
     */
    List<UserPreference> findByEmail(String email);

    /**
     * Retrieves a single {@link UserPreference} matching both the user and the preference key.
     *
     * <p>Relies on the unique ({@code email}, {@code key}) constraint, so at most one
     * result can exist. Used by the upsert logic to decide whether to update an existing
     * preference or create a new one.</p>
     *
     * @param email the email address identifying the user
     * @param key   the preference key to look up
     * @return an {@link Optional} containing the matching {@link UserPreference} if present,
     *     or {@link Optional#empty()} if no preference exists for that user and key
     */
    Optional<UserPreference> findByEmailAndKey(String email, String key);

}
