# The Program overview reads one Program stats endpoint

The Program overview page (`/programs/:id`) shows every Week of a Program with
its Volume and Adherence, and every Session with its planned Volume and whether
it has a Logged Session. It gets all of that from `GET /api/programs/{id}/stats`
plus the Program's name from `GET /api/programs/{id}`: two requests, whatever
the size of the Program.

The new endpoint lives in the `stats` package, next to
`GET /api/weeks/{id}/stats`, because only `stats` may read both `program` and
`log`; `program` cannot know about Logged Sessions without a package cycle. So
`SessionResponse` stays `{id, prescriptions}`: planned Volume and the logged
flag are derived data and belong with the other derived data.

The Week row shows both planned and logged Volume, and Adherence.

## Considered options

- **A `logged` flag and `plannedVolume` on `SessionResponse`**: makes `program`
  depend on `log`, which already depends on `program`.
- **One `GET /api/sessions/{id}/log` per Session, 404 meaning "not logged"**:
  one request per Session, and a 404 that is not an error clashes with the
  frontend rule that the API client throws on non-2xx.
- **Computing Session Volume in the browser** (Sets × Reps × Bell weight):
  copies a domain rule into the UI.
- **One `GET /api/weeks/{id}/stats` per Week**: kept as is, but the page would
  need one request per Week and still lack the per-Session data.

## Frontend build

- frontend-maven-plugin builds `frontend/` in `prepare-package`, so
  `mvn verify` yields one jar with the app in it. Binding it after the tests
  keeps the pre-commit hook (`mvn -o test-compile`) free of Node. It costs a
  backend-only `mvn verify` about ten seconds; skip it with
  `-Dskip.installnodenpm -Dskip.npm` when iterating on Java alone.
- Recharts draws the planned and logged Volume per Week above the grid, as
  asked on the pull request. It is React components over SVG, so the chart is
  declared like the rest of the page and needs no imperative D3 code; the grid
  stays the table view of the same numbers.
