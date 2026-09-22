# Arbiter agent instructions

- The development process is in [context/swe.md](context/swe.md), and project skills live in `.codex/skills/`.

## One home per fact

Every fact lives in exactly one place. Everywhere else, link to it or cite it (for example "rule 7" or "#28") with at most a one-sentence summary; never restate it. This applies to issue bodies and Javadoc as well as documentation.

| Fact | Home |
| --- | --- |
| What Arbiter is and what each role does | [docs/index.md](docs/index.md) |
| Domain terms and what each enum value means | [docs/Glossary.md](docs/Glossary.md) |
| Rules shared across features | Section 3 of [docs/UserFlows.md](docs/UserFlows.md) |
| Behaviour of one feature | That feature's GitHub issue |
| Setup and build commands | "Setting up" in [docs/DeveloperGuide.md](docs/DeveloperGuide.md) |
| Design rationale | "Design" in [docs/DeveloperGuide.md](docs/DeveloperGuide.md) |
| Where the code enforces rules | [context/architecture.md](context/architecture.md) |
| What a class, field or method means | Its Javadoc |
| A task's decisions and open questions | Its plan in `plans/` |

- Before adding a fact, check its home. Update the home rather than adding a copy elsewhere.
- If two copies of a fact disagree, do not choose between them: raise the contradiction with the human.
- Leave counts of files, classes, tables or methods out of documentation; the code is their home.
