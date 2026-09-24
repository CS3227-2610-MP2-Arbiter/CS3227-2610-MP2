# Workspace setup and file layout

Status: Human verified.

## Original request

- After [#4] was implemented but not yet merged, the user said to take [#9] next: "the first issue that others would depend on".
- [#9] was chosen because it was one of only three issues with no open blocker, and it blocks [#6], [#10] and [#25].

## Follow-ups, corrections, and reflection

- [#9] is assigned to Whimsyturtle, not to zheng-jj. The user directed the work anyway; it is raised here and in the pull request so the assigned owner can object before this is merged rather than after.
- [#9] has no dependency on [#4], so this branch was taken from `main` rather than stacked on `feat/shared-domain-model`, and can merge independently of it.
- The issue's scope does not mention the single-writer lock, although `context/architecture.md` lists it under `arbiter.workspace`. It was left out and recorded as an open question, because the mechanism is a decision worth taking deliberately rather than smuggling in with the folder wizard.
- Two test failures came from the tests, not the code: `RecentWorkspaces.remember` normalises a path to absolute so entries compare reliably from any working directory, and the tests asserted the relative path they had passed in. The code was left as it was and the tests corrected, with a new test pinning the normalisation down.
- Two Gradle runs failed with `AccessDeniedException` deleting `build/` and `build/test-results/`. This is the sandbox refusing recursive deletes, not a defect: the folders were removed after checking they resolved inside the workspace, and the runs then succeeded.
- The first version of `WorkspaceSetupDialog` had a `start` method taking a callback, which made the entry point awkward and could hand a null to the callback. It was rewritten to return the workspace, and `Arbiter` now closes the window when the user cancels.

## Agent responses and outcomes

- Created branch `feat/workspace-setup` from `main` and wrote `plans/workspace-setup.md`.
- Added `arbiter.workspace`: `WorkspacePaths` (the only place the layout is written down), `WorkspaceMetadata`, `RecentWorkspaces`, `WorkspaceService`, a small JSON codec, and two exception types.
- Added `arbiter.ui.shared.WorkspaceSetupDialog`, the first-run wizard, kept deliberately thin so every acceptance criterion is decided in `arbiter.workspace` and testable without a JavaFX toolkit.
- Wired `arbiter.Arbiter.start` to open the remembered workspace, or show the wizard on first run.
- Added 27 tests across `WorkspaceServiceTest` and `JsonTest`. No JavaFX toolkit is needed to run them.

## Verification

- `./gradlew check` passes: 27 tests, 0 failures, and Checkstyle clean for both main and test sources.
- Each acceptance criterion has a direct test: the full layout is created; metadata records both versions; a second start reopens without rewriting; two workspaces stay openable and the last opened is remembered; a workspace stamped with a newer layout version fails with a message naming that version and is left byte-identical; an incomplete workspace and a non-workspace folder are both refused by name; and every path comes from `WorkspacePaths`.
- The codec is tested against a Windows path, embedded quotes, backslashes, newlines, tabs and non-ASCII characters, plus nine malformed documents that must be rejected rather than guessed at.
- Tests use a temporary directory and an injected recent-list location, so they never read or write the real `~/.arbiter`.
- Not verified: the JavaFX wizard itself. It needs a display and a toolkit, and it is a thin adapter over the tested service.
- `./gradlew check` was run on Windows only. CI covers Linux, macOS and Apple Silicon.
- The owner marked this summary human verified on 24 September 2026.

[#4]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4
[#6]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6
[#9]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/9
[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
