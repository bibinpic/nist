CREATE RECURSIVE VIEW cr_managers(id, manager_id, username, depth, idpath, path, cycle) AS (
         SELECT u.id,
            u.manager_id,
            u.username,
            1,
            ARRAY[u.id] AS "array",
            ARRAY[u.username] AS "array",
            false AS bool
           FROM people u
        UNION ALL
         SELECT u.id,
            u.manager_id,
            u.username,
            cm.depth + 1,
            cm.idpath || u.id,
            cm.path || u.username,
            u.username = ANY (cm.path)
           FROM people u,
            cr_managers cm
          WHERE u.id = cm.manager_id AND NOT cm.cycle
        )
 SELECT cr_managers.id,
    cr_managers.manager_id,
    cr_managers.username,
    cr_managers.depth,
    cr_managers.idpath,
    cr_managers.path,
    cr_managers.cycle
   FROM cr_managers;