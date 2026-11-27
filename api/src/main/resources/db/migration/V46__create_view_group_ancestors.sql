CREATE VIEW group_ancestors_view AS
SELECT c.ancestor_id::text || c.descendant_id::text AS id,
       c.descendant_id                              AS grp_id,
       c.ancestor_id
FROM group_closures c
WHERE c.depth > 0;

COMMENT ON VIEW group_ancestors_view IS 'Read-only view over group_closures exposing every (group, ancestor) pair of the group hierarchy, one row per ancestor at any depth, without the group itself. Used to detect cycles before attaching a group to a parent.';

COMMENT ON COLUMN group_ancestors_view.id IS 'Synthetic row identifier: concatenation of ancestor_id and grp_id. Unique per (group, ancestor) pair.';
COMMENT ON COLUMN group_ancestors_view.grp_id IS 'Group unique identifier (UUID).';
COMMENT ON COLUMN group_ancestors_view.ancestor_id IS 'Identifier of a group found up the parent chain of grp_id, at any depth.';
