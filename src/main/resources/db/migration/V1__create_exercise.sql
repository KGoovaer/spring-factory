-- The Exercise catalog (docs/adr/0009-exercises-live-in-a-database.md).
-- The classification columns mirror the Exercise record, which holds the
-- rules: every row is written from one and read back through one.
--
-- name_key is the lower-cased name, so names are unique ignoring case.
-- position is the catalog order.
CREATE TABLE exercise (
  id         UUID          PRIMARY KEY,
  name       VARCHAR(200)  NOT NULL,
  name_key   VARCHAR(200)  NOT NULL UNIQUE,
  bodyweight BOOLEAN       NOT NULL,
  hold       BOOLEAN       NOT NULL,
  timed      BOOLEAN       NOT NULL,
  gear       VARCHAR(200),
  position   INT           NOT NULL
);
