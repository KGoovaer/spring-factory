## Verification checks

Run before you report a change as complete:

1. `mvn -B verify` (compile, unit and integration tests, then the frontend's
   typecheck, lint, Vitest tests and build)
2. `npm run check` (the above plus the markdown format check)

Maven is the build tool; Java 21, Spring Boot 3. The React frontend in
`frontend/` (TypeScript, Vite) is built by Maven in `prepare-package` into
`target/classes/static`, so the jar serves it. Node is also used for the
Sandcastle workflows in `.sandcastle/`, husky and prettier.

Frontend commands, from `frontend/` (after `npm ci` there):

- `npm run dev`: Vite dev server on 5173, proxying `/api` to Spring on 8080
  (start that with `mvn spring-boot:run`).
- `npm run check`: typecheck, ESLint and Vitest, without Maven.
- `npm run test -- --watch`: Vitest in watch mode.

Behaviour lives in deep modules: a package with a small public API, tested
through that API. See [CODING_STANDARDS.md](./CODING_STANDARDS.md) and
[GLOSSARY.md](./GLOSSARY.md). Use the glossary's words in code and tickets.

The pre-commit hook compiles the project and checks markdown. Do not bypass it
(`--no-verify`) or edit `.husky/` or `scripts/pre-commit.sh`.

## Agent skills

### Issue tracker

Issues and PRDs live as GitHub issues on the `origin` remote, managed with the
`gh` CLI. See `docs/agents/issue-tracker.md`.

### Software factory

Workflows in `.github/workflows/agent-*.yml` run Sandcastle scripts from
`.sandcastle/agent-workflows/`. Labels `agent:implement`, `agent:review` etc.
trigger them. See the `software-factory` skill.
