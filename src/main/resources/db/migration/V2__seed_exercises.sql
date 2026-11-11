-- The seeded Exercises, with the fixed ids of docs/adr/0001-exercise-ids-are-uuids.md:
-- the Kettlebell Exercises first, then the Bodyweight ones. V1 and V2 run
-- together on an empty table, so there is nothing a seed could collide with.
INSERT INTO exercise (id, name, name_key, bodyweight, hold, timed, gear, position) VALUES
  ('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01', 'Swing',                 'swing',                 FALSE, FALSE, FALSE, NULL,        1),
  ('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c02', 'Goblet squat',          'goblet squat',          FALSE, FALSE, FALSE, NULL,        2),
  ('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c03', 'Clean and press',       'clean and press',       FALSE, FALSE, FALSE, NULL,        3),
  ('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c04', 'Turkish get-up',        'turkish get-up',        FALSE, FALSE, FALSE, NULL,        4),
  ('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05', 'Snatch',                'snatch',                FALSE, FALSE, FALSE, NULL,        5),
  ('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c09', 'Clean to waiter press', 'clean to waiter press', FALSE, FALSE, FALSE, NULL,        6),
  ('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0a', 'Split stance row',      'split stance row',      FALSE, FALSE, FALSE, NULL,        7),
  ('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06', 'Push-up',               'push-up',               TRUE,  FALSE, FALSE, NULL,        8),
  ('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c07', 'Plank',                 'plank',                 TRUE,  TRUE,  FALSE, NULL,        9),
  ('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c08', 'Sit-up',                'sit-up',                TRUE,  FALSE, FALSE, NULL,        10),
  ('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0b', 'Walking plank',         'walking plank',         TRUE,  FALSE, FALSE, NULL,        11),
  ('8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0c', 'Skipping',              'skipping',              TRUE,  FALSE, TRUE,  'Jump rope', 12);
