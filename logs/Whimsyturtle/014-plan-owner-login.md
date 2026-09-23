# Plan workspace owner and login

Status: Owner UI acceptance confirmed; teammate review pending.

## Original request

Start issue #7 under `context/swe.md`, keep salted password hashing simple for a course project. Ask the owner about material design decisions.

## Follow-ups, corrections, and reflection

- The owner chose a minimum password length of 8 characters without composition rules and a minimal working setup/login/logout screen until #5 supplies role navigation. The owner then asked whether salt plus plain SHA-256 could simplify storage and requested sample PBKDF2 code before approval. The plan now emphasizes a two-operation JDK helper. After seeing the sample and a short explanation of `SecureRandom` salt generation, the owner approved the plan, including the username character policy.
- During manual acceptance, the owner reported that leaving the username blank showed a truncated validation error with an ellipsis. The UI now wraps labels in the shared auth form; the owner confirmed the visual retest passed.
- The owner asked to split the work into smaller review groups and stage the first group. The plan records the review order; only the password-storage helper, its test, and its exception type were staged first.
- The owner committed and pushed the first group as `e1f9ef3`, then asked to stage the next group. Only the authentication behavior and storage-boundary files were staged for that review.
- During review of the staged auth group, the owner chose printable ASCII passwords alongside the existing ASCII username rule to avoid Unicode handling. The service, UI wording, and tests were revised, and the staged auth group was refreshed after verification.
- The owner flagged the ambiguous `USERNAME` pattern constant; it was renamed to `USERNAME_PATTERN` in the staged service file.
- The owner reconsidered generic failures for disabled accounts and proposed revealing status only after a correct password. That change was added to #7 and implemented with a regression check for both password outcomes.
- The owner then questioned whether spaces should be allowed in passwords and suggested a regex. After reviewing current guidance, the agent explained that spaces are safe; the owner chose ASCII letters and digits only for this project. The issue, screen, service, and tests were revised.
- The owner retained the case-insensitive username rule in #7, committed the authentication review group, and asked to stage the UI group next. Only `Arbiter` and `AuthScreen` were staged.
- The owner committed the UI group as `037976e` and asked to stage the task record. Only this log and `plans/owner-login.md` were prepared for that review.
- At #7 planning time, merged #6 used JSON while rule 11 and architecture/design guides still said SQLite. This contradiction was raised and later corrected in the #6 documentation follow-up.

## Agent responses and outcomes

- Applied `clarify-requirements`, `write-plan`, `implement-feature`, `write-test`, `review`, and `log`.
- Retrieved [#7](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/7), [#31](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/31), and [#23](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/23). The local GitHub CLI could not reach GitHub inside the sandbox, so read-only issue calls used the approved escalation path.
- Created branch `feat/owner-login` and drafted [the plan](../../plans/owner-login.md). The normal branch command could not write `.git`; the approved escalation path succeeded.
- Implemented sole-owner bootstrap, salted password storage, login/logout, in-memory current user, authorization check, and minimal screens. The reviewer found a Unicode length boundary issue, which was resolved before the owner later chose an ASCII-only policy.
- Updated [#7](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/7) to record the owner's later ASCII-only credential decision. During delivery, six evidence-backed criteria were checked first; the owner then confirmed the UI scenarios and the remaining two were checked.
- After the owner committed the review groups, published [draft PR #65](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/pull/65) against `main` with verification and owner acceptance recorded.

## Verification

- The exact issue criteria, JSON store, user model/repository, wizard, build, and JUnit patterns informed the implementation. Focused auth tests passed. JDK 25 `.\gradlew.bat check shadowJar --offline --no-daemon` passed with JUnit, both Checkstyle tasks, and the release jar.
- A sandboxed Gradle run could not create `C:\.gradle`'s lock, so the approved unsandboxed path was used. The first incremental compile could not locate the unchanged `SnapshotPublisher.java`; `clean compileJava` recovered. The first full check found three formatting violations, all corrected before the passing rerun.
- The owner confirmed the requested UI scenarios passed. Teammate review remains pending under `context/swe.md`; the documentation update and draft PR are complete.
- After the error-label fix, `.\gradlew.bat check shadowJar --offline --no-daemon` passed again. JavaFX layout still needs the owner's visual confirmation.
- After the ASCII credential revision, the full command passed again. One initial run failed only test Checkstyle indentation; the tests themselves passed, and the rerun passed after the formatting correction.
- After the disabled-login revision, focused auth tests and the full command passed. The first full run found two test Checkstyle line-wrap violations; the assertions were reformatted and the rerun passed.
- After the alphanumeric-password revision, focused auth tests, Checkstyle, and `.\gradlew.bat check shadowJar --offline --no-daemon` passed.
- On the pushed head, JDK 25 `.\gradlew.bat check shadowJar --offline --no-daemon` passed with up-to-date tasks, and [Java CI](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/actions/runs/35844859347) passed across its platform and Apple Silicon jobs. The published PR body and issue checklist were re-read and verified.
