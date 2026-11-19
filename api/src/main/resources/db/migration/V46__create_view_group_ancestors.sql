CREATE VIEW group_ancestors_view AS
WITH RECURSIVE ancestors AS (
    SELECT g.grp_id,
           g.parent_id AS ancestor_id,
           ARRAY [g.grp_id] AS path
    FROM groups g
    WHERE g.parent_id IS NOT NULL

    UNION ALL

    SELECT a.grp_id,
           g.parent_id,
           a.path || g.grp_id
    FROM ancestors a
             JOIN groups g ON g.grp_id = a.ancestor_id
    WHERE g.parent_id IS NOT NULL
      -- Stops the recursion on a hierarchy that already loops.
      AND NOT g.parent_id = ANY (a.path)
)
SELECT ancestor_id::text || grp_id::text AS id,
       grp_id,
       ancestor_id
FROM ancestors;

COMMENT ON VIEW group_ancestors_view IS 'Read-only recursive view exposing every (group, ancestor) pair of the group hierarchy, one row per ancestor at any depth. Used to detect cycles before attaching a group to a parent.';

COMMENT ON COLUMN group_ancestors_view.id IS 'Synthetic row identifier: concatenation of ancestor_id and grp_id. Unique per (group, ancestor) pair.';
COMMENT ON COLUMN group_ancestors_view.grp_id IS 'Group unique identifier (UUID).';
COMMENT ON COLUMN group_ancestors_view.ancestor_id IS 'Identifier of a group found up the parent chain of grp_id, at any depth.';
