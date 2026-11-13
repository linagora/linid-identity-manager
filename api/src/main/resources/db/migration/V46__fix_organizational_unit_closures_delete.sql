-- =========================================================
-- REBUILD THE ANCESTOR PATHS OF ORGANIZATIONAL UNITS
-- =========================================================

CREATE OR REPLACE FUNCTION organizational_unit_closures_rebuild(organizational_unit_ids UUID[])
    RETURNS VOID AS
$$
BEGIN
    DELETE FROM organizational_unit_closures
    WHERE descendant_id = ANY (organizational_unit_ids)
      AND depth > 0;

    INSERT INTO organizational_unit_closures (ancestor_id, descendant_id, depth)
    WITH RECURSIVE paths AS (
        SELECT parent_id AS ancestor_id,
               child_id  AS descendant_id,
               1         AS depth
        FROM organizational_unit_relations
        WHERE child_id = ANY (organizational_unit_ids)
        UNION
        SELECT r.parent_id,
               p.descendant_id,
               p.depth + 1
        FROM paths p
                 JOIN organizational_unit_relations r ON r.child_id = p.ancestor_id
    )
    SELECT ancestor_id, descendant_id, MIN(depth)
    FROM paths
    GROUP BY ancestor_id, descendant_id;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION organizational_unit_closures_rebuild(UUID[]) IS 'Recomputes from organizational_unit_relations every ancestor path of the given organizational units, keeping the shortest depth when several paths join the same ancestor.';

-- =========================================================
-- DELETE RELATION → REBUILD IMPACTED PATHS
-- =========================================================

CREATE OR REPLACE FUNCTION organizational_unit_closures_delete()
    RETURNS TRIGGER AS
$$
BEGIN
    -- Only the ancestors of the child and of its descendants can change.
    PERFORM organizational_unit_closures_rebuild(ARRAY(
        SELECT descendant_id
        FROM organizational_unit_closures
        WHERE ancestor_id = OLD.child_id
    ));

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION organizational_unit_closures_delete() IS 'Trigger function that rebuilds the ancestor paths of the child organizational unit and of its descendants after a relation is deleted, keeping the paths still supported by another parent.';

-- =========================================================
-- REPAIR THE PATHS LEFT BY THE FORMER DELETE TRIGGER
-- =========================================================

SELECT organizational_unit_closures_rebuild(ARRAY(SELECT oun_id FROM organizational_units));
