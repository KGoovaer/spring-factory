-- The seeded Exercises, with the fixed ids of docs/adr/0001-exercise-ids-are-uuids.md:
-- the Kettlebell Exercises first, then the Bodyweight ones.
--
-- A seed overrides a listed Exercise a Lifter added under the same name,
-- ignoring case: that row takes the seed's id and classification. Later seed
-- migrations follow this pattern (docs/adr/0009-exercises-live-in-a-database.md).
MERGE INTO exercise e
USING (VALUES
  (CAST('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01' AS UUID), 'Swing',                 FALSE, FALSE, FALSE, CAST(NULL AS VARCHAR(200)), 1),
  (CAST('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c02' AS UUID), 'Goblet squat',          FALSE, FALSE, FALSE, NULL,        2),
  (CAST('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c03' AS UUID), 'Clean and press',       FALSE, FALSE, FALSE, NULL,        3),
  (CAST('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c04' AS UUID), 'Turkish get-up',        FALSE, FALSE, FALSE, NULL,        4),
  (CAST('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05' AS UUID), 'Snatch',                FALSE, FALSE, FALSE, NULL,        5),
  (CAST('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c09' AS UUID), 'Clean to waiter press', FALSE, FALSE, FALSE, NULL,        6),
  (CAST('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0a' AS UUID), 'Split stance row',      FALSE, FALSE, FALSE, NULL,        7),
  (CAST('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06' AS UUID), 'Push-up',               TRUE,  FALSE, FALSE, NULL,        8),
  (CAST('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c07' AS UUID), 'Plank',                 TRUE,  TRUE,  FALSE, NULL,        9),
  (CAST('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c08' AS UUID), 'Sit-up',                TRUE,  FALSE, FALSE, NULL,        10),
  (CAST('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0b' AS UUID), 'Walking plank',         TRUE,  FALSE, FALSE, NULL,        11),
  (CAST('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0c' AS UUID), 'Skipping',              TRUE,  FALSE, TRUE,  'Jump rope', 12)
) AS s (id, name, bodyweight, hold, timed, gear, position)
ON e.name_key = LOWER(s.name)
WHEN MATCHED THEN UPDATE SET
  id = s.id, name = s.name, bodyweight = s.bodyweight, hold = s.hold,
  timed = s.timed, gear = s.gear, position = s.position
WHEN NOT MATCHED THEN INSERT (id, name, name_key, bodyweight, hold, timed, gear, position)
  VALUES (s.id, s.name, LOWER(s.name), s.bodyweight, s.hold, s.timed, s.gear, s.position);
