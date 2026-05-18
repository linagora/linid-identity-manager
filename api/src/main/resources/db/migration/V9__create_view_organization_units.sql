CREATE VIEW organizational_units_view AS
SELECT child.oun_id,
       child.name,
       child.type,
       NULLIF(CONCAT_WS(' ', creator.firstname, creator.lastname), '')                    AS created_by,
       NULLIF(CONCAT_WS(' ', updater.firstname, updater.lastname), '')                    AS updated_by,
       child.insert_date,
       child.update_date,
       json_agg(json_build_object('id', relations.our_id, 'parent', relations.parent_id)) AS "parents"
FROM organizational_units child
         LEFT JOIN organizational_unit_relations relations ON relations.child_id = child.oun_id
         LEFT OUTER JOIN accounts creator ON creator.act_id = child.created_by
         LEFT OUTER JOIN accounts updater ON updater.act_id = child.updated_by
GROUP BY child.oun_id,
         child.name,
         child.type,
         child.insert_date,
         child.update_date,
         creator.firstname,
         creator.lastname,
         updater.firstname,
         updater.lastname;

COMMENT ON VIEW organizational_units_view IS 'Aggregated view exposing organizational units along with their parent organizational units.';

COMMENT ON COLUMN organizational_units_view.oun_id IS 'Primary key (UUID) of the organizational unit.';
COMMENT ON COLUMN organizational_units_view.name IS 'Human-readable name of the organizational unit.';
COMMENT ON COLUMN organizational_units_view.type IS 'Type of the organizational unit.';
COMMENT ON COLUMN organizational_units_view.parents IS 'JSON array containing the parent organizational units associated with this organizational unit.';
COMMENT ON COLUMN organizational_units_view.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN organizational_units_view.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN organizational_units_view.insert_date IS 'Date and time when the account record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN organizational_units_view.update_date IS 'Date and time when the account record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';
