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

package io.github.linagora.linid.im.sync.persistence.repository;

import io.github.linagora.linid.im.sync.persistence.model.AccountApplicationPairId;
import io.github.linagora.linid.im.sync.persistence.model.AccountComputedRightView;
import io.github.linagora.linid.im.sync.persistence.model.AccountSummary;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.repository.Repository;

/**
 * Read-only Spring Data repository for {@link AccountComputedRightView}.
 *
 * <p>Exposes only read operations: mutating methods from {@code JpaRepository} (save, delete, flush...) are
 * intentionally not inherited, since {@link AccountComputedRightView} is backed by a database view.</p>
 */
public interface AccountComputedRightViewRepository
    extends Repository<AccountComputedRightView, AccountApplicationPairId> {

    /**
     * Finds the next page of the accounts of the view, each one once, except the technical account of {@code sync-api},
     * ordered by identifier.
     *
     * <p>The page starts after the given account, not at an offset: an account deleted since the previous page does not
     * shift the next ones. The {@link AccountSummary} projection only selects the identifiers of the accounts and the
     * checksums of their payloads, so that the distinct applies to them and not to the whole rows: an account has at
     * most one payload, so it is selected once.</p>
     *
     * <p>The technical account gets no payload, so its pairs are never {@code to_compute} and are never selected for
     * the computation of the rights.</p>
     *
     * @param technicalAccountId the identifier of the technical account of {@code sync-api}
     * @param actId              the last account of the previous page, the lowest UUID for the first page
     * @param limit              the maximum number of accounts of the page
     * @return the next accounts with the checksums of their payloads, including the accounts without payload yet,
     *     empty when no application is deployed or when every account was read
     */
    List<AccountSummary> findDistinctByActIdNotAndActIdGreaterThanOrderByActIdAsc(
        UUID technicalAccountId, UUID actId, Limit limit);

    /**
     * Tells whether an account is still in the view.
     *
     * <p>The payload build reads the accounts by pages: an account deleted since its page was read is no longer in
     * the view, and inserting its payload would fail on the foreign key.</p>
     *
     * @param actId the identifier of the account
     * @return {@code true} when the account is still in the view
     */
    boolean existsByActId(UUID actId);

    /**
     * Finds the next pairs of the cycle whose rights must be computed, ordered by account then application.
     *
     * <p>With a keyset position, the window starts after the pair of the position: the pairs written since the
     * previous window are no longer {@code to_compute}, and an offset would skip as many pairs.</p>
     *
     * <p>The {@code actId} bound is implied by the position, but is not redundant: Spring Data writes the keyset as
     * {@code act_id > ? OR (act_id = ? AND app_id > ?)}, which PostgreSQL cannot use on the index of the accounts, so
     * each window would scan again the accounts already read. With this bound, the scan starts at the account of the
     * position.</p>
     *
     * @param actId    the account of the position, the lowest UUID for the first window
     * @param position the position of the last pair read, {@link ScrollPosition#keyset()} for the first window
     * @param limit    the maximum number of pairs of the window
     * @return the next pairs whose {@code to_compute} is true
     */
    Window<AccountComputedRightView> findByToComputeTrueAndActIdGreaterThanEqualOrderByActIdAscAppIdAsc(
        UUID actId, ScrollPosition position, Limit limit);

    /**
     * Finds the next pairs of one account whose rights must be computed, ordered by application.
     *
     * @param actId    the identifier of the account
     * @param position the position of the last pair read, {@link ScrollPosition#keyset()} for the first window
     * @param limit    the maximum number of pairs of the window
     * @return the next pairs of the account whose {@code to_compute} is true
     */
    Window<AccountComputedRightView> findByActIdAndToComputeTrueOrderByAppIdAsc(
        UUID actId, ScrollPosition position, Limit limit);

    /**
     * Finds the next pairs of one application whose rights must be computed, ordered by account.
     *
     * <p>The {@code actId} bound starts the scan at the account of the position, like for the pairs of the cycle: the
     * keyset also compares the account then the application, the composite identifier being added to the order.</p>
     *
     * @param appId    the identifier of the application
     * @param actId    the account of the position, the lowest UUID for the first window
     * @param position the position of the last pair read, {@link ScrollPosition#keyset()} for the first window
     * @param limit    the maximum number of pairs of the window
     * @return the next pairs of the application whose {@code to_compute} is true
     */
    Window<AccountComputedRightView> findByAppIdAndToComputeTrueAndActIdGreaterThanEqualOrderByActIdAsc(
        UUID appId, UUID actId, ScrollPosition position, Limit limit);
}
