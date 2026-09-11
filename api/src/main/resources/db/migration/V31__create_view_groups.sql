CREATE VIEW groups_view AS
SELECT g.grp_id,
       g.code,
       g.label,
       g.parent_id,
       parent.label                                                    AS parent_label,
       g.description,
       g.email,
       g.oun_id,
       ou.name                                                         AS organizational_unit_name,
       g.app_id,
       app.name                                                        AS application_name,
       g.extra_parameters,
       NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '') AS created_by,
       NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '') AS updated_by,
       g.insert_date,
       g.update_date
FROM groups g
         LEFT OUTER JOIN groups parent ON parent.grp_id = g.parent_id
         LEFT OUTER JOIN organizational_units ou ON ou.oun_id = g.oun_id
         LEFT OUTER JOIN applications app ON app.app_id = g.app_id
         LEFT OUTER JOIN accounts creator ON creator.act_id = g.created_by
         LEFT OUTER JOIN accounts updater ON updater.act_id = g.updated_by;

COMMENT ON VIEW groups_view IS 'Read-only view exposing group records with the label of the parent group, the name of the associated organizational unit and application, and createdBy/updatedBy resolved to the full name (firstname + lastname) of the referenced account.';

COMMENT ON COLUMN groups_view.grp_id IS 'Group unique identifier (UUID).';
COMMENT ON COLUMN groups_view.code IS 'Functional unique identifier of the group.';
COMMENT ON COLUMN groups_view.label IS 'Human-readable label of the group.';
COMMENT ON COLUMN groups_view.parent_id IS 'Optional identifier of the parent group.';
COMMENT ON COLUMN groups_view.parent_label IS 'Label of the parent group. Resolved via LEFT OUTER JOIN on groups.grp_id; NULL when the group has no parent.';
COMMENT ON COLUMN groups_view.description IS 'Optional free-text description of the group.';
COMMENT ON COLUMN groups_view.email IS 'Optional email address of the group.';
COMMENT ON COLUMN groups_view.oun_id IS 'Optional identifier of the organizational unit associated with the group.';
COMMENT ON COLUMN groups_view.organizational_unit_name IS 'Name of the associated organizational unit. Resolved via LEFT OUTER JOIN on organizational_units.oun_id; NULL when the group has no organizational unit.';
COMMENT ON COLUMN groups_view.app_id IS 'Optional identifier of the application associated with the group.';
COMMENT ON COLUMN groups_view.application_name IS 'Name of the associated application. Resolved via LEFT OUTER JOIN on applications.app_id; NULL when the group has no application.';
COMMENT ON COLUMN groups_view.extra_parameters IS 'JSONB column containing custom attributes and metadata defined by the deployment. Intended for customer-specific or integration-specific extensions that are not part of the standard data model.';
COMMENT ON COLUMN groups_view.created_by IS 'Full name ("firstname lastname") of the account that created this record. Resolved via LEFT OUTER JOIN on accounts.act_id; NULL if the referenced account no longer exists.';
COMMENT ON COLUMN groups_view.updated_by IS 'Full name ("firstname lastname") of the account that last updated this record. Resolved via LEFT OUTER JOIN on accounts.act_id; NULL if the referenced account no longer exists.';
COMMENT ON COLUMN groups_view.insert_date IS 'Date and time when the group record was created (UTC).';
COMMENT ON COLUMN groups_view.update_date IS 'Date and time when the group record was last updated (UTC).';
