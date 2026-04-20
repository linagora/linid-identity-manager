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

import io.github.linagora.linid.im.api.persistence.model.AccountView;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;
import org.jspecify.annotations.NonNull;

/**
 * Read-only Spring Data repository for {@link AccountView}.
 *
 * <p>Exposes only read operations: mutating methods from {@code JpaRepository} (save, delete,
 * flush...) are intentionally not inherited, since {@link AccountView} is backed by a database
 * view.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic filtering via {@code
 * spring-query-filter} specifications.
 */
public interface AccountViewRepository
    extends Repository<AccountView, UUID>, JpaSpecificationExecutor<AccountView> {


    /**
     * Finds all account views matching the given specification and pageable.
     *
     * @param specification the specification to filter account views.
     * @param pageable      the pagination information.
     * @return a page of account views matching the specification and pagination criteria.
     */
    @Override
    @NonNull
    Page<AccountView> findAll(@NonNull Specification<AccountView> specification, @NonNull Pageable pageable);

    /**
     * Finds an account view by its ID.
     *
     * @param id the UUID of the account view
     * @return an Optional containing the found account view, or empty if not found
     */
    Optional<AccountView> findById(UUID id);
}
