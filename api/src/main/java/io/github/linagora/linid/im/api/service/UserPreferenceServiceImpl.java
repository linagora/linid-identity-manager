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

import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.model.user.preference.UserPreferenceMapper;
import io.github.linagora.linid.im.api.model.user.preference.UserPreferenceRecord;
import io.github.linagora.linid.im.api.persistence.model.UserPreference;
import io.github.linagora.linid.im.api.persistence.repository.UserPreferenceRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link UserPreferenceService}.
 *
 * <p>Backed by {@link UserPreferenceRepository}. Every operation is scoped to the authenticated
 * user: the owning email is always read from the {@link UserPrincipal} resolved from the JWT, never
 * from client input, so a user can only ever read or modify their own preferences.</p>
 *
 * <p>The {@code save} operation is an upsert: it looks up the existing preference for the user/key
 * pair and overwrites its value if present, otherwise it persists a newly mapped entity. The
 * {@code delete} operation raises a {@code 404} {@link ApiException} when the key does not exist for
 * the user.</p>
 *
 * <p>Annotated {@code @Transactional} so each operation runs in a single transaction; the read-only
 * {@code findAll} narrows this with {@code @Transactional(readOnly = true)}.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserPreferenceServiceImpl implements UserPreferenceService {

    /**
     * Repository for user preference persistence.
     */
    private final UserPreferenceRepository userPreferenceRepository;

    /**
     * Mapper for record-to-entity and entity-to-map conversions.
     */
    private final UserPreferenceMapper userPreferenceMapper;

    @Override
    public UserPreference save(final UserPrincipal userPrincipal, final UserPreferenceRecord userPreference){

        return userPreferenceRepository.findByEmailAndKey(userPrincipal.getEmail(), userPreference.key())
                .map(existing -> {
                    existing.setValue(userPreference.value());
                    return userPreferenceRepository.save(existing);
                })
                .orElseGet(() -> userPreferenceRepository.save(
                        userPreferenceMapper.toUserPreference(userPreference, userPrincipal)));
    }

    /**
     * {@inheritDoc}
     *
     * @throws ApiException with HTTP 404 if no preference matches the given key for this user
     */
    @Override
    public void delete(final UserPrincipal userPrincipal, final String key){
        UserPreference existing = userPreferenceRepository
                .findByEmailAndKey(userPrincipal.getEmail(), key)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND.value(),
                        I18nMessage.of("error.user_preference.not_found", Map.of("key", key))
                ));
        userPreferenceRepository.delete(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPreference> findAll(final UserPrincipal userPrincipal){
        return userPreferenceRepository.findByEmail(
                userPrincipal.getEmail());
    }
}
