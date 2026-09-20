# Workspace setup and file layout

**Issue:** [#9](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/9) - Workspace setup and file layout
**Branch:** `feat/workspace-setup`
**Status:** Implemented, awaiting human review.

## Result

`./gradlew check` passes: 27 tests, 0 failures, Checkstyle clean. `arbiter.workspace` holds the paths, metadata, recent list, codec and service; `arbiter.ui.shared.WorkspaceSetupDialog` is the wizard, and `arbiter.Arbiter` opens the remembered workspace or shows the wizard.

## Goal

Add a first-run wizard that creates a workspace folder with a fixed layout, and reopens it on later launches. Everything that persists data depends on knowing where the workspace is, so this lands before the schema.

## Non-goals

- Schema, repositories and migrations ([#6](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6)). This issue creates an empty `arbiter.db` so the layout is complete, but nothing opens it.
- Authentication ([#7](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/7)), media import ([#25](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25)) and media resolution ([#10](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10)).
- The app shell. This issue runs before [#5](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5) and does not build navigation.

## Decisions

1. **All path names live in one class.** `WorkspacePaths` resolves every path inside a workspace and is the only place folder and file names are written down.
2. **Two versions are recorded.** `workspaceVersion` covers the layout and metadata format; `schemaVersion` covers the database and belongs to [#6](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6). Keeping them separate means the database can migrate without the layout changing, and the other way round.
3. **The recent list derives its order from the list itself.** Paths are stored newest-first and "last opened" is the first entry, so there is no second field to drift out of step.
4. **The recent list lives outside any workspace**, in `~/.arbiter/recent-workspaces.json`, because it records which workspaces exist and so cannot live inside one. The base directory is injected so tests never touch the real user home.
5. **JSON is read and written by a small in-package codec** rather than a new dependency. Both files are small and fixed-schema, and the only real hazard is escaping, which is tested directly. If this is the wrong trade, switching to a library is contained to one class.
6. **The wizard lives in `arbiter.ui.shared`.** It is first-run shell behaviour, which is where the architecture puts that layer. It cannot use the [#8](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8) UI kit because that issue has not shipped.

## Open questions

- **The single-writer lock.** `context/architecture.md` lists the lock under `arbiter.workspace`, but this issue's scope does not mention it. It is left out here and recorded as a question, because the mechanism (a file lock held for the session) is a decision worth taking deliberately rather than as a side effect of the folder wizard.
- **UI ownership.** `arbiter.ui.shared` is [zheng-jj](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5)'s package. The wizard is a new file rather than a change to an existing one, but [#5](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5) will rework the same entry point.
- **Wiring into the entry point.** `Arbiter.start` is currently a placeholder that [#5](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5) replaces. The change here is deliberately small so the conflict is trivial.

## Proposed changes

- `arbiter.workspace`: `WorkspacePaths`, `WorkspaceMetadata`, `WorkspaceService`, `RecentWorkspaces`, `Json`, and two exceptions.
- `arbiter.ui.shared.WorkspaceSetupDialog`: the first-run wizard.
- `arbiter.Arbiter`: open the remembered workspace if there is one, otherwise show the wizard.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| The wizard creates the full layout and is skipped on later runs | Unit test: `create` makes every folder and file; `openRemembered` returns the workspace on a second start without creating anything |
| Metadata records the workspace version and schema version | Unit test reads the written `workspace.json` back and asserts both |
| Multiple workspaces, last opened remembered | Unit test: two workspaces are created, the newer is remembered, the older is still openable |
| A newer workspace fails clearly and is left untouched | Unit test: a workspace stamped with a higher version throws, and its files are byte-identical afterwards |
| Paths come from a single shared object | All paths are produced by `WorkspacePaths`; asserted path by path |
| `./gradlew check` passes | `./gradlew check` |

The JavaFX wizard is not unit-tested: it needs a toolkit, and every acceptance criterion above is decided in `arbiter.workspace`. The wizard is a thin adapter over `WorkspaceService`.

## Risks

- The hand-written codec is the main technical risk, since a bad parse could corrupt a workspace. It is covered by direct tests, including Windows paths with backslashes.
- Writing to the wrong folder is unrecoverable for a user who picks an existing directory. `create` refuses a folder that already holds a workspace, and the wizard confirms before writing.
