# Developer guide and architecture write-up

**Issue:** [#43] - Developer guide and architecture write-up
**Branch:** `docs/developer-guide`, from `main` at `6db6fc6`
**Status:** Plan approved by the owner on 28 September 2026, while it was being implemented at their request ("do 41 and 43"). Documentation only; teammate review is pending.

The scope and acceptance criteria live in [#43]. This plan records what the write-up covers and where each fact goes.

## Goal and scope

Bring `docs/DeveloperGuide.md` up to the focused text-classification system as merged, so a new developer can see how it is built and why, and leave `context/architecture.md` as code-level rules that link to the guide for reasons.

Non-goals, from [#43]: user instructions (the User Guide) and reflections.

## Proposed changes

- **`docs/DeveloperGuide.md`, "Design":**
  - **Architecture:** keep the layer diagram, and add a diagram of which screens call which services and what each service reaches (store, resolver, exports).
  - **Domain model:** a diagram of the model classes and how they refer to one another, with a note on the identifiers and the two answer shapes. What each field means stays in Javadoc and the Glossary.
  - **Authorization and blindness:** how a session is checked on every service call, what the shell's role routing does and does not enforce, password hashing, and the blindness boundary with its trusted entry points.
  - **Persistence:** the snapshot, the write action as the transaction boundary, integrity checks, atomic publication and the reopen-after-failure rule, the single-writer lock, the two version numbers and the current state of migrations (none yet; any other version is refused unchanged), source hashing, and submission atomicity, with a sequence diagram of **Submit & next**.
  - **Resolution and export:** where automatic and manual resolution run, how an export is read, checked against its sources and written, and a checklist for adding an output format that states v1 ships only CSV and JSON.
  - **Scope reduction ([#62]):** what v1 deferred, that the model holds no state for it, and the schema implications of later adding another source type (such as images) or task type (such as detection).
  - **Design decisions:** extend the table with the [#62] reduction, submit-only persistence, and refusing rather than migrating other data versions.
- **"Testing":** describe the strategy behind each level: the temporary JSON workspaces and seeded fixtures, the architecture tests (`AnnotatorBlindnessTest`, `UiConventionTest`), and manual acceptance.
- **Acknowledgements:** add ArchUnit, which the architecture tests use.
- **`context/architecture.md`:** check every line is a code-level rule. Add the rules the guide's new sections imply (a snapshot shape change bumps its version, how an output format is added) and link the relevant guide section instead of giving a reason inline.
- Nothing describes detection, images, flags, drafts, completion seals, analytics or a provenance browser as implemented.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| Text diagrams in fenced blocks | Mermaid | The Pages site uses Jekyll's Cayman theme, which does not render Mermaid; text renders the same on GitHub and Pages |
| Rationale in the guide, rules in `context/architecture.md` | Both in one file | [#43] and the "one home per fact" table in `AGENTS.md` require the split |

## Open decisions

None.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| Architecture and domain model with useful diagrams | Review against `arbiter.model` and the service constructors |
| Persistence, migrations, transaction boundaries, source hashing, submission atomicity | Review against `JsonStore`, `JsonIntegrity`, `WorkspaceLock`, `SourceResolver` and `AnnotationService.submit` |
| Authorization and blindness boundaries explicit | Review against `AuthService`, `ScreenRegistry` and `AnnotatorBlindnessTest` |
| Major decisions with rejected alternatives, #62 and schema implications | Review of the decisions table and the scope section |
| Export-format extension guidance without implying unsupported behaviour | Review against `ExportService` and `OutputFormat` |
| `context/architecture.md` has code-level rules only and links the guide | Line-by-line review |
| Removed components not described as implemented | Search of both files for detection, image, flag, draft, completion, seal, analytics and provenance browser |
| Pages workflow passes | The PR's `Product site` check |

## Implementation and verification

- Rewrote "Design" in `docs/DeveloperGuide.md` with the sections above, three text diagrams (screens to services, the domain model, and **Submit & next**), and three new rows in the decisions table. Added the testing strategy under "Testing" and ArchUnit to the acknowledgements. Fixed the guide's relative link to `context/swe.md`, which does not resolve on the Pages site, to its GitHub URL.
- `context/architecture.md` gains an "Authorization" section, two persistence rules (where source reads and exports happen relative to a store action, and version-and-migration), an `ExportService` pointer to the output-format checklist, and "Why" links to the guide. The hashing line now names `PasswordHasher` and PBKDF2, where it had said "PBKDF2 or bcrypt".
- Checking the draft against the code corrected four claims before they were kept:
  - `JsonIntegrity` does not check that an answer holds exactly one of a label and a rating; the services decide.
  - The resolver refuses a link that escapes `media/` rather than following it.
  - Registration, submission and manual resolution check the source inside their write action, so the rule about reading outside it applies only to showing text and writing exports.
  - `CorpusService.mediaDirectory()` returns a path without a session check, so the authorization rule covers methods that read or change records.
- A search of both files for detection, image, flag, draft, completion, seal, analytics, COCO and provenance browser finds them only as deferred or future work. Every in-page anchor and the architecture file's links to the guide resolve to existing headings.
- The `Product site` check runs on the pull request; Jekyll is not installed locally.

[#43]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/43
[#62]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/62
