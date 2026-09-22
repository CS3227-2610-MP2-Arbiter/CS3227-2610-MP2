# Resolve review comments on workspace setup

Status: Awaiting human verification.

## Original request

- The user had reviewed `zheng-jj`'s PR #59 for #9 and asked the agent to resolve their comments, leaving commits to them. The comments asked to:
  - use a library instead of the hand-written JSON codec, and add an agent rule against reinventing the wheel;
  - drop `WorkspaceMetadataException` in favour of `WorkspaceException`, and make its comment more general;
  - convert `WorkspacePaths` and `WorkspaceMetadata` to records;
  - remove comments that broke the one-home rule;
  - stop remembering the last workspace, and let the user choose one at each launch;
  - rename `RecentWorkspaces.FILE_NAME`, and handle `Path.of` throwing `InvalidPathException`.
- The user noted that dropping the remembered workspace went against #9, and asked for #9 to be updated to match.

## Follow-ups, corrections, and reflection

- The user then asked for the fixes to be staged one change at a time, and committed each before asking for the next:
  1. the records;
  2. Jackson;
  3. the exception merge, with a fuller `@throws` on `open` that the user had pointed to;
  4. the comment trims;
  5. removing the remembered workspace;
  6. the agent rules and the plan, which the user committed directly.
- Each commit had to build on its own, so `RecentWorkspaces` was moved to Jackson in step 2 and deleted in step 5.
- The user asked for a second agent rule preferring records. The agent excluded model classes, whose fields stay mutable for ORMLite.

## Agent responses and outcomes

- Used Jackson rather than Java's `.properties` files, and put the rule in `context/architecture.md`.
- Deleting `RecentWorkspaces` resolved the `FILE_NAME` and `Path.of` comments; no other unsafe path parsing remained.
- The wizard now asks to create or open a workspace at every launch, and returns to that choice after a cancelled or failed step.
- Updated #9, the PR #59 description and `plans/workspace-setup.md` to match. Flagged that #9's relocation criterion has no test.

## Verification

- `./gradlew check shadowJar` passed with 11 tests and no Checkstyle issues. Each staged change was also built and tested on its own before staging.
- A workspace created from the built jar wrote a readable `workspace.json` and reopened.
- #9 and the PR description matched their drafts on read-back.
- Not verified: the JavaFX wizard, which needs a display.
