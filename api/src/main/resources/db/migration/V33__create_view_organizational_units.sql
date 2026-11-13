CREATE OR REPLACE VIEW organizational_units_view AS
SELECT child.oun_id,
       child.name,
       child.type,
       child.extra_parameters,
       relations.parent_id AS parent_id,
       parents.parent_names,
       parents.parents,
       s.suspension_period,
       s.suspension_reason,
       s.suspension_subreason,
       s.suspension_comment,
       s.reactivation_comment,
       CASE
           WHEN s.suspension_period IS NOT NULL
               AND lower(s.suspension_period) IS NOT NULL
               AND now() >= lower(s.suspension_period)
               AND (
                    upper(s.suspension_period) IS NULL
                        OR now() <= upper(s.suspension_period)
                    )
               THEN TRUE
           ELSE FALSE
           END AS is_suspended,
       CASE
           WHEN s.suspension_period IS NOT NULL
               AND lower(s.suspension_period) IS NOT NULL
               AND now() >= lower(s.suspension_period)
               AND (
                    upper(s.suspension_period) IS NULL
                        OR now() <= upper(s.suspension_period)
                    )
               THEN 'SUSPENDED'
           ELSE 'ACTIVE'
           END AS status,
       NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
       NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
       child.insert_date,
       child.update_date,
       roles.role_names,
       (
           WITH RECURSIVE ancestors AS (
               SELECT child.oun_id AS oun_id,
                      ARRAY []::UUID[] AS path
               UNION ALL
               SELECT r.parent_id,
                      r.parent_id || a.path
               FROM ancestors a
                        JOIN organizational_unit_relations r
                             ON r.child_id = a.oun_id
               -- Stops the recursion on a hierarchy that already loops.
               WHERE r.parent_id <> child.oun_id
                 AND NOT r.parent_id = ANY (a.path)
           )
           SELECT COALESCE(JSONB_AGG(TO_JSONB(a.path) ORDER BY a.path), '[]'::JSONB)
           FROM ancestors a
           WHERE NOT EXISTS (
               SELECT 1
               FROM organizational_unit_relations r
               WHERE r.child_id = a.oun_id
               -- OFFSET 0 forces an index lookup here instead of a full table scan for every unit.
               OFFSET 0
           )
       ) AS hierarchy_paths
FROM organizational_units child
         LEFT JOIN organizational_unit_status s
                   ON s.oun_id = child.oun_id
         LEFT JOIN organizational_unit_relations relations
                   ON relations.child_id = child.oun_id
         LEFT JOIN accounts creator
                   ON creator.act_id = child.created_by
         LEFT JOIN accounts updater
                   ON updater.act_id = child.updated_by
         LEFT JOIN LATERAL (
    SELECT STRING_AGG(parent.name, ', ' ORDER BY parent.name) AS parent_names,
           JSONB_AGG(
                   JSONB_BUILD_OBJECT(
                           'id', r.our_id,
                           'parent', r.parent_id
                   )
                   ORDER BY parent.name
           ) AS parents
    FROM organizational_unit_relations r
             JOIN organizational_units parent
                  ON parent.oun_id = r.parent_id
    WHERE r.child_id = child.oun_id
    ) parents ON TRUE
         LEFT JOIN LATERAL (
             SELECT STRING_AGG(DISTINCT role.name, ', ' ORDER BY role.name) AS role_names
             FROM organizational_unit_accounts oua
                      JOIN roles role
                           ON role.rol_id = oua.rol_id
             WHERE oua.oun_id = child.oun_id
         ) roles ON TRUE;

COMMENT ON VIEW organizational_units_view IS 'Aggregated view exposing organizational units along with their parent organizational units, the hierarchy paths of their ancestors and their suspension status (suspension period, current-state status reason / sub-reason / comment and a computed is_suspended flag and a computed status) and the functional roles held in them.';

COMMENT ON COLUMN organizational_units_view.oun_id IS 'Primary key (UUID) of the organizational unit.';
COMMENT ON COLUMN organizational_units_view.name IS 'Human-readable name of the organizational unit.';
COMMENT ON COLUMN organizational_units_view.type IS 'Type of the organizational unit.';
COMMENT ON COLUMN organizational_units_view.extra_parameters IS 'JSONB column containing custom attributes and metadata defined by the deployment. Intended for customer-specific or integration-specific extensions that are not part of the standard data model.';
COMMENT ON COLUMN organizational_units_view.parent_id IS 'Identifier of the parent organizational unit. One row is returned for each parent relationship.';
COMMENT ON COLUMN organizational_units_view.parent_names IS 'Comma-separated names of all parent organizational units associated with this organizational unit.';
COMMENT ON COLUMN organizational_units_view.suspension_period IS 'Time range during which the organizational unit is suspended. NULL when no suspension is configured. An open-ended suspension (NULL upper bound) is treated as a permanent suspension.';
COMMENT ON COLUMN organizational_units_view.suspension_reason IS 'High-level reason code explaining the suspension.';
COMMENT ON COLUMN organizational_units_view.suspension_subreason IS 'More detailed classification of the suspension reason.';
COMMENT ON COLUMN organizational_units_view.suspension_comment IS 'Free-text comment providing additional context about the suspension.';
COMMENT ON COLUMN organizational_units_view.reactivation_comment IS 'Free-text comment providing additional context about a reactivation.';
COMMENT ON COLUMN organizational_units_view.is_suspended IS 'Computed flag: TRUE when the current instant falls within the suspension period (including suspensions with no upper bound), FALSE otherwise.';
COMMENT ON COLUMN organizational_units_view.status IS 'Computed lifecycle status of the organizational unit. SUSPENDED when the organizational unit is currently suspended, ACTIVE otherwise.';
COMMENT ON COLUMN organizational_units_view.parents IS 'JSON array containing the parent organizational units associated with this organizational unit.';
COMMENT ON COLUMN organizational_units_view.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN organizational_units_view.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN organizational_units_view.insert_date IS 'Date and time when the account record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN organizational_units_view.update_date IS 'Date and time when the account record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN organizational_units_view.role_names IS 'Comma-separated names of the distinct functional roles held by the accounts attached to the organizational unit, in alphabetical order. NULL when no account holds a role in it.';
COMMENT ON COLUMN organizational_units_view.hierarchy_paths IS 'JSON array of every hierarchy path of this organizational unit. Each path is an array of the identifiers of its ancestors, ordered from the top-most ancestor (a unit without parent, normally the root) down to its parent. The paths are sorted by their identifiers. A unit without parent, such as the root or the child of a deleted unit, gets a single empty path: [[]]. A unit whose ancestors all loop, so that it reaches no unit without parent, gets no path: [].';
