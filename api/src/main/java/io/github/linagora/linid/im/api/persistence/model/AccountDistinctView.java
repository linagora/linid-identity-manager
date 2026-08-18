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

import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
import io.hypersistence.utils.hibernate.type.range.Range;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Entity providing enriched, read-only account information, mapped to the {@code accounts_view} database view.
 *
 * <p>In addition to the account's identity information, this view exposes organizational unit membership,
 * account validity and suspension periods, lifecycle timestamps, status information, and the reasons and
 * comments associated with account state changes.
 *
 * <p>Audit information such as {@code createdBy}, {@code updatedBy}, {@code insertDate}, and {@code updateDate}
 * is inherited from {@link AbstractViewEntity}.
 */
@Entity
@Table(name = "accounts_view")
@Data
@Immutable
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AccountDistinctView extends AbstractViewEntity {

    /**
     * Unique identifier of the account (UUID).
     */
    @Id
    @Column(name = "act_id")
    private UUID id;

    /**
     * External identifier (e.g. OIDC sub or external system ID).
     */
    @Column(name = "external_id", nullable = false)
    private String externalId;

    /**
     * Last name of the account holder.
     */
    @Column(name = "lastname", nullable = false)
    private String lastname;

    /**
     * First name of the account holder.
     */
    @Column(name = "firstname", nullable = false)
    private String firstname;

    /**
     * Email address associated with the account.
     */
    @Column(name = "email", nullable = false)
    private String email;

    /**
     * Names of the organizational units to which the account belongs, represented as a comma-separated list.
     */
    @Column(name = "organizational_units", nullable = false)
    private String organizationalUnits;

    /**
     * Time range during which the account is considered valid. {@code null} when no status row exists.
     */
    @Type(PostgreSQLRangeType.class)
    @Column(name = "validity_period", columnDefinition = "tstzrange")
    private Range<ZonedDateTime> validityPeriod;

    /**
     * Time range during which the account is suspended. {@code null} when no suspension is configured.
     */
    @Type(PostgreSQLRangeType.class)
    @Column(name = "suspension_period", columnDefinition = "tstzrange")
    private Range<ZonedDateTime> suspensionPeriod;

    /**
     * Timestamp when the account was activated or reactivated.
     * {@code null} until the account is activated.
     */
    @Column(name = "activation_at")
    private OffsetDateTime activationAt;

    /**
     * High-level reason code explaining the suspension. {@code null} when not provided.
     */
    @Column(name = "suspension_reason")
    private String suspensionReason;

    /**
     * More detailed classification of the suspension reason. {@code null} when not provided.
     */
    @Column(name = "suspension_subreason")
    private String suspensionSubreason;

    /**
     * Free-text comment providing additional context about the suspension.
     * {@code null} when not provided.
     */
    @Column(name = "suspension_comment")
    private String suspensionComment;

    /**
     * High-level reason code explaining the deactivation. {@code null} when not provided.
     */
    @Column(name = "deactivation_reason")
    private String deactivationReason;

    /**
     * More detailed classification of the deactivation reason. {@code null} when not provided.
     */
    @Column(name = "deactivation_subreason")
    private String deactivationSubreason;

    /**
     * Free-text comment providing additional context about the deactivation.
     * {@code null} when not provided.
     */
    @Column(name = "deactivation_comment")
    private String deactivationComment;

    /**
     * Free-text comment providing additional context about the reactivation.
     * {@code null} when not provided.
     */
    @Column(name = "reactivation_comment")
    private String reactivationComment;

    /**
     * Computed account status: {@code ACTIVE}, {@code SUSPENDED} or {@code INACTIVE}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatusEnum status;

    /**
     * Integer number of calendar days before the validity period's upper bound.
     * Can be negative. {@code null} when the validity period has no upper bound.
     */
    @Column(name = "days_before_deactivation")
    private Integer daysBeforeDeactivation;

    /**
     * Creates an account view with its audit information, identity information, organizational unit membership,
     * lifecycle information, status, and state change details.
     *
     * <p>This constructor is intentionally provided with all view properties as parameters to allow
     *  {@code spring-query-filter} to instantiate the entity when creating filtered query projections.
     *
     * @param createdBy the identifier of the user or system that created the account.
     * @param updatedBy the identifier of the user or system that last updated the account.
     * @param insertDate the timestamp when the account was created.
     * @param updateDate the timestamp when the account was last updated.
     * @param id the unique identifier of the account.
     * @param externalId the external identifier of the account, such as an OIDC subject or an identifier from
     *                   an external system.
     * @param lastname the last name of the account holder.
     * @param firstname the first name of the account holder.
     * @param email the email address associated with the account.
     * @param organizationalUnits the names of the organizational units to which the account belongs,
     *                             represented as a comma-separated list.
     * @param validityPeriod the time range during which the account is considered valid.
     * @param suspensionPeriod the time range during which the account is suspended.
     * @param activationAt the timestamp when the account was activated or reactivated.
     * @param suspensionReason the high-level reason code explaining the account suspension.
     * @param suspensionSubreason the detailed classification of the account suspension reason.
     * @param suspensionComment the free-text comment providing additional context about the account suspension.
     * @param deactivationReason the high-level reason code explaining the account deactivation.
     * @param deactivationSubreason the detailed classification of the account deactivation reason.
     * @param deactivationComment the free-text comment providing additional context about the account deactivation.
     * @param reactivationComment the free-text comment providing additional context about the account reactivation.
     * @param status the computed current status of the account.
     * @param daysBeforeDeactivation the number of calendar days remaining before the upper bound of the validity
     *                               period; may be negative when the validity period has expired.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public AccountDistinctView(final String createdBy,
                               final String updatedBy,
                               final OffsetDateTime insertDate,
                               final OffsetDateTime updateDate,
                               final UUID id,
                               final String externalId,
                               final String lastname,
                               final String firstname,
                               final String email,
                               final String organizationalUnits,
                               final Range<ZonedDateTime> validityPeriod,
                               final Range<ZonedDateTime> suspensionPeriod,
                               final OffsetDateTime activationAt,
                               final String suspensionReason,
                               final String suspensionSubreason,
                               final String suspensionComment,
                               final String deactivationReason,
                               final String deactivationSubreason,
                               final String deactivationComment,
                               final String reactivationComment,
                               final AccountStatusEnum status,
                               final Integer daysBeforeDeactivation) {
        super(createdBy, updatedBy, insertDate, updateDate);
        this.id = id;
        this.externalId = externalId;
        this.lastname = lastname;
        this.firstname = firstname;
        this.email = email;
        this.organizationalUnits = organizationalUnits;
        this.validityPeriod = validityPeriod;
        this.suspensionPeriod = suspensionPeriod;
        this.activationAt = activationAt;
        this.suspensionReason = suspensionReason;
        this.suspensionSubreason = suspensionSubreason;
        this.suspensionComment = suspensionComment;
        this.deactivationReason = deactivationReason;
        this.deactivationSubreason = deactivationSubreason;
        this.deactivationComment = deactivationComment;
        this.reactivationComment = reactivationComment;
        this.status = status;
        this.daysBeforeDeactivation = daysBeforeDeactivation;
    }
}
