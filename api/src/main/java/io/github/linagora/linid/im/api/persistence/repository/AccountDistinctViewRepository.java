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

import io.github.linagora.linid.im.api.persistence.model.AccountDistinctView;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only Spring Data repository for {@link AccountDistinctView}.
 *
 * <p>This repository is backed by a database view and therefore intentionally exposes only
 * read operations. It does not extend {@code JpaRepository}, preventing write operations such
 * as {@code save}, {@code delete}, or {@code flush} from being available through this repository.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic filtering and pagination using
 * JPA {@link Specification specifications}.
 */
public interface AccountDistinctViewRepository
    extends Repository<AccountDistinctView, UUID>, JpaSpecificationExecutor<AccountDistinctView> {

    /**
     * Retrieves a page of account views matching the given specification.
     *
     * @param specification the specification used to filter the account views.
     * @param pageable the pagination and sorting information.
     * @return a page containing the matching account views.
     */
    @Override
    @NonNull
    Page<AccountDistinctView> findAll(
        @NonNull Specification<AccountDistinctView> specification,
        @NonNull Pageable pageable
    );

    /**
     * Retrieves an account view by its identifier.
     *
     * @param id the identifier of the account view.
     * @return an {@link Optional} containing the matching account view, or an empty
     * {@link Optional} if no account view exists with the given identifier.
     */
    Optional<AccountDistinctView> findFirstById(UUID id);
}
