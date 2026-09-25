# Shared UI kit and error-handling convention

**Issue:** [#8] - Shared UI kit and error-handling convention
**Branch:** `feat/ui-kit` (from `main` after [#5] merged)
**Status:** Plan approved by the owner on 25 September 2026, including the log size. Implemented and verified locally; human acceptance and teammate review are pending.

What the feature does is in [#8], and what each class means is in its Javadoc. This plan records what was decided and what is still open.

## Goal and scope

Give both role surfaces one way to show errors, confirmations and success messages, one application-wide stylesheet, a few shared display components, and one rule for which exceptions reach the user and how they are logged.

Non-goals, from [#8]: feature-specific screens and components. No service, repository or model behaviour changes.

## Current state

- Errors are reported three ways: `Alert`s built separately in `Arbiter` and `WorkspaceSetupDialog`, and inline red labels in `AuthScreen`.
- `AppShell` and `AuthScreen` hard-code colours and fonts with `setStyle`. There is no stylesheet.
- Nothing is logged. New workspaces already get a `logs/` folder, which `WorkspacePaths` describes as holding application logs.
- An exception nobody catches on the JavaFX thread is printed to the console and the user sees nothing.

## Proposed changes

All new code is in `arbiter.ui.shared`, which both roles already use ([#5]).

- **`Styles`** owns the one stylesheet, `src/main/resources/arbiter/ui/shared/arbiter.css`, and the names of its style classes. `Styles.apply(Scene)` and `Styles.apply(DialogPane)` attach it. `Arbiter` applies it to the one `Scene`, and `Dialogs` to every dialog, since a dialog has its own scene.
- **`Components`** builds the basic display pieces the existing screens already repeat: a page title, hint text, inline error text, an empty-state placeholder, and a centred form. `AppShell`, `AuthScreen` and `Arbiter`'s placeholders use them, and their `setStyle` calls go.
- **`Dialogs`** is the only place a JavaFX dialog is built: `showError`, `showSuccess`, `confirm` (with a named confirm button) and `choose` (the wizard's create-or-open question). Each is owned by the calling window, titled "Arbiter" and styled. `showError` also logs the error.
- **`ErrorMessages`** decides what a user reads. An exception declared in an `arbiter` package (`AuthException`, `WorkspaceException`, `SourceException`, `JsonStoreException` and future service exceptions) already carries a message written for the user, so it is shown as it is. Anything else is a programming error, so the user gets a generic message and the details go to the log. This relies on a package rule rather than a list of types, so a new service's exception is covered without being registered.
- **`DiagnosticLog`** is the only code that logs, using `java.util.logging` from the JDK. It records a fixed action name written by a developer, plus the exception's type, message, causes and stack. It never takes field values, so a password or source text can reach the log only if code puts it into an exception message, which the convention forbids. While a workspace is open, the log also goes to `<workspace>/logs/arbiter.0.log`, rotated at 1 MiB across three files. The file is attached after the workspace lock is taken and detached when it is released, so only the instance holding the lock writes it. Before a workspace is open, only the console gets the log.
- **Uncaught exceptions** on the JavaFX thread go to `Dialogs.showError` with a generic heading, so they are logged and the user is told. `Arbiter` installs this handler, and a default handler logs failures on any other thread.
- **Screens adopt the convention.** `WorkspaceSetupDialog` and `Arbiter` use `Dialogs`. `AuthScreen` keeps its inline message for credentials the user can correct on the form, and sends storage failures to `Dialogs.showError`, so they are logged.

### The convention

- Services throw exceptions declared in `arbiter` packages, whose messages are written for the user and contain no credential, hash, salt or source text. Programming errors stay JDK exceptions.
- A screen catches only the exceptions of the action it runs. It shows them with `Dialogs.showError(owner, heading, error)`, or inline with `ErrorMessages.of(error)` when the user can fix the input on the same form. Anything else propagates to the uncaught handler.
- Only `DiagnosticLog` logs. Nothing prints to `System.out` or `System.err` or calls `printStackTrace`, and nothing styles a node inline.

### Automated enforcement

`UiConventionTest` adds ArchUnit rules over the shipped classes: only `Dialogs` constructs a `javafx.scene.control.Dialog`, only `DiagnosticLog` uses `java.util.logging` or `System.Logger`, no code calls `Node.setStyle` or `Throwable.printStackTrace`, and no code reads `System.out` or `System.err`.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| `java.util.logging` | SLF4J with Logback | The JDK logger is enough for one local log file; architecture prefers the standard library where it suffices |
| Arbiter-package exceptions are user-facing | A shared base exception class | A new base class would have to live below `arbiter.workspace` and would change every existing exception's hierarchy |
| Log in the workspace's `logs/` folder | A per-user log in the home folder | The workspace already reserves `logs/`, and a failure is easier to find next to the data it concerns |
| ArchUnit rules for the convention | A written rule only | "Every screen uses the shared convention" is checkable, and ArchUnit is already a test dependency |
| Logic in plain classes tested without JavaFX | UI tests with TestFX and Monocle | CI's Linux runner has no display, and no new dependency is needed for what is left |

## Risks and open decisions

- **Log retention.** 1 MiB across three files keeps at most about 3 MiB per workspace. The owner accepted this size on 25 September 2026.
- **A shared workspace's log is shared.** Both roles use one workspace ([rule 11](../docs/UserFlows.md#3-rules-both-tracks-share)), so the log can hold failures from either role. It holds no user input, only action names, exception types and messages, and paths.
- **Exception messages carry paths.** `JsonStoreException` and `WorkspaceException` name workspace paths, which the user chose and may be shown. They are not credentials.
- **Inline form errors are not logged.** A mistyped password is not a diagnostic event. Storage failures on the same form are logged through `Dialogs`.
- **The dialogs and components need JavaFX to run,** so they are covered by the ArchUnit rules and a human check rather than unit tests.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| Every screen can show errors, confirmations and success messages through the shared convention | `Dialogs` offers all three, and existing screens use it. `UiConventionTest` fails if any other class builds a dialog. Human check: open a folder that is not a workspace (error), create a workspace (confirmation) |
| One stylesheet is applied application-wide | `StylesheetTest` loads `arbiter.css` and checks that each class name in `Styles` has a rule. `UiConventionTest` forbids inline `setStyle`. Human check: the shell, login and dialogs share the stylesheet's look |
| Errors shown to users are clear while diagnostic detail is logged without exposing credentials or sensitive values | `ErrorMessagesTest`: an Arbiter exception shows its own message, and anything else the generic one. `DiagnosticLogTest`: the workspace log gets the action, type and stack; a failed login with a known password never writes the password; nothing is written after detaching |
| The convention is recorded in the developer documentation | "Errors and logging" in `docs/DeveloperGuide.md`, and the UI rules in `context/architecture.md` |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar` |

## Implementation and verification

- Added `Styles` (with `src/main/resources/arbiter/ui/shared/arbiter.css`), `Components`, `Dialogs`, `ErrorMessages` and `DiagnosticLog` to `arbiter.ui.shared`. `Arbiter`, `AppShell`, `AuthScreen` and `WorkspaceSetupDialog` now use them, and no `setStyle`, hand-built `Alert` or placeholder layout remains outside the kit. No service, repository or model file changed.
- The rotating log's current file is `arbiter.0.log` rather than `arbiter.log`, because `java.util.logging` numbers rotated files.
- `AuthScreen` now distinguishes credential errors, shown inline, from storage failures, shown in a logged dialog; before, both went to the inline label.
- Added `ErrorMessagesTest`, `DiagnosticLogTest`, `StylesheetTest` and `UiConventionTest` (18 tests). A mutation run proved each one bites. A temporary class that set an inline style, built an `Alert`, printed to `System.out` and used `java.util.logging` failed all four convention rules. A stray `.stray` stylesheet rule failed the stylesheet check. A login message temporarily including the password failed the password check. All three changes were then reverted.
- Documentation: "Errors and logging" in `docs/DeveloperGuide.md` (plus a decisions-table row), the UI rules in `context/architecture.md`, and one line in `docs/UserGuide.md` saying where the log is.
- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 208 JUnit tests, none skipped, both Checkstyle tasks, and the release jar, which contains `arbiter/ui/shared/arbiter.css`.
- Not run: the GUI. The dialogs, stylesheet look and log file in a real session need the human acceptance checks below.

### Human acceptance checks

1. Launch: the create-or-open question is titled "Arbiter". Cancelling it exits.
2. Open a folder that is not a workspace: the error dialog shows "That workspace could not be opened" with the reason, then returns to the question.
3. Create a workspace: the confirmation names the folder and has a "Create workspace" button. Afterwards `logs/arbiter.0.log` exists in the workspace.
4. Owner setup: mismatched or too-short passwords show a red inline message.
5. Log in with a wrong password: red inline message. The password does not appear in `logs/arbiter.0.log`.
6. Shell: a grey top bar with a bold "Arbiter", and the placeholder shows a heading and grey hint text.
7. Close the app: `logs/` holds no `.lck` file.

## Open questions

- None. The User Guide line saying where the log is was approved with the plan.

[#5]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5
[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
