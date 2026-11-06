## Verification checks

Run before you report a change as complete:

1. `mvn -B verify` (compile, unit and integration tests)
2. `npm run check` (the above plus the markdown format check)

Maven is the build tool; Java 21, Spring Boot 3. Node exists only for the
Sandcastle workflows in `.sandcastle/`, husky and prettier.

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
