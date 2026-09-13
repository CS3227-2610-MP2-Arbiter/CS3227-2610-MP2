---
title: Agentic SE Reflections
---

# Agentic SE Reflections

We built Arbiter with a single AI agent customised through task-specific skills in `.codex/skills/`.
This document reflects on what that actually did for us: which tasks the agent handled well, where it
needed correcting, and what we would change next time. It is written from the logs in `logs/`, which
were produced by the agent and verified by us.

## How we customised the agent

We did not build one general-purpose assistant. We split the work into eight skills, each with a
declared input, a short list of steps, and explicit completion criteria:
`clarify-requirements`, `write-plan`, `implement-feature`, `write-test`, `review`,
`maintain-docs`, `create-pull-request` and `log`.

The principle we settled on was **one skill per human-visible decision point**. A skill exists where a
human needs to approve something or where output has to match a fixed shape. That is why there is a
`create-pull-request` skill but no "write a commit message" skill: the PR is a deliverable with a
required structure, while a commit message is not.

Each skill also states what it must **not** do. `create-pull-request` commits, pushes and publishes
only with authorization. `review` never weakens a test to make it pass and never hides a failure.
Those negative instructions turned out to matter more than the positive ones, because they bound the
damage when the agent was wrong.

The skills were refined through use rather than designed up front. Several started as prose and were
shortened after we found the agent over-reading them; `context/swe.md` was deliberately kept short so
the detail stays in the skills where it is actually consulted.

## Three skills in detail

### 1. `log` - making the agent's work reviewable

**What it does.** Writes a task-level summary to `logs/<github-user>/<NNN>-<name>.md`, numbered per
person, with a fixed four-section structure: original request; follow-ups, corrections and reflection;
agent responses and outcomes; verification.

**Why it is interesting.** This skill exists purely to make the other seven auditable. Every log is
marked "Awaiting human verification" unless a human says otherwise, and the verification section
separates what was actually checked from what was assumed or skipped. That distinction is the whole
value: a log that said "tests pass" without saying which tests, or that omitted the checks that
*could not* run, would be worse than no log.

**How we know it works.** The structure survived several revisions driven by real failures. The first
logs were turn-by-turn, which was repetitive and hid the decisions; we changed to task-level
summaries. We tried recording the tester's name and dropped it as unnecessary bookkeeping. We removed
date metadata. Each change came from reading an actual log and finding it unhelpful, which is about as
direct a feedback loop as a skill can have.

**What it cost.** Writing a log is real work, and it is tempting to skip. Twice we did skip it - once
when creating all 45 GitHub issues - and had to reconstruct the entry later from issue metadata
because the conversation was gone. The reconstruction is visibly weaker than a contemporaneous log:
it records what happened but not why, and we cannot recover the prompts or the corrections. That is
the strongest argument we have for the skill.

### 2. `review` - verification that cannot be talked out of

**What it does.** Inspects a diff against the requirements, then runs the relevant checks - for code
changes, `./gradlew check shadowJar` on JDK 25 - and reports findings with severity.

**Why it is interesting.** The instruction we are most glad we wrote is that it must distinguish a
genuine pass from a skipped task, a cached result, a `NO-SOURCE` or a check that could not run, and
must never hide a failure or weaken a test. Without that, "tests pass" is unfalsifiable: a run where
nothing executed looks identical to a run where everything succeeded.

**Where it earned its keep.** It forced scope discipline too. In documentation-only tasks the skill
makes the agent state that Gradle was not run *and why*, rather than silently omitting it. That turns
a gap into a recorded decision someone can disagree with.

**What we would change.** It currently runs a fixed command for code changes. It would be better if it
derived the minimum relevant check from the changed paths, so a documentation edit and a repository
change did not have to be argued separately each time.

### 3. `clarify-requirements` - refusing to guess

**What it does.** Turns a request into requirements with an owner, affected roles, goal, non-goals and
observable acceptance criteria, then finds or drafts the matching GitHub issue.

**Why it is interesting.** Its most valuable behaviour is that it asks about missing or conflicting
requirements that affect scope or correctness, while stating reasonable assumptions for minor details.
That is a threshold, not a rule to ask about everything, and getting the threshold right is what keeps
the skill useful rather than annoying.

**What it produced.** Our largest single win came from here. We gave the agent a rough brainstorm -
unstructured notes about two roles, a blind queue, and a wishlist ending in a bare "ORMs for storage" -
and asked it to clean up the requirements and split them into issues. It produced 45 issues with
consistent labels, dependencies and acceptance criteria. More importantly, it did not silently
resolve the ambiguities: three of them became explicit spike issues ending in a written decision
rather than code, including "how does a team share data while staying offline", which was a genuine
tension between the offline premise and the team premise.

**The failure mode.** The agent is willing to produce a coherent-sounding answer to a question nobody
answered. The spikes worked because they made that visible. Several later decisions - when earnings
vest, what counts as a disagreement - were equally load-bearing and were only surfaced because we
went back and asked. A skill that forces ambiguity into the open is worth more than one that resolves
it quietly.

## What the agent handled well

- **Bulk, structured generation.** 45 issues with consistent labelling and dependency wiring, and
  long documents with many cross-references, both of which are tedious and error-prone by hand.
- **Mechanical editing at scale.** Applying one decision across a spec, several issues and the
  supporting docs, then re-scanning to confirm no stale wording survived. It is thorough at this in a
  way that humans are not.
- **Reconstruction and auditing.** Finding every reference to a moved file, and confirming after a
  change that nothing still pointed at the old path.

## Where the agent created work

- **Editing long documents.** This was the single largest source of rework. Multi-line edits to
  `docs/UserFlows.md` repeatedly produced duplicated headings, orphaned sentence fragments,
  clipped bullet lines and broken section numbering, each of which had to be found and repaired by
  re-reading the file. The agent's strength is generating coherent text and its weakness is
  surgically modifying existing text. Reading the result back is not optional.
- **Assuming rather than checking.** It claimed a document was missing when it was already on disk,
  and prepared to create a duplicate. The fix is a habit, not a rule: confirm the current state before
  creating anything.
- **Sandboxed operations failing silently.** Git writes and network calls failed under the workspace
  sandbox in ways that looked like logic errors. This cost time until we recognised the pattern.

## What we would change

- **Make document edits verifiable.** Every multi-line edit should be followed by a structural check -
  heading order, numbering sequence, no duplicated lines - rather than trusting the edit. Most of our
  rework would have been caught by that one step.
- **Narrow `review` to derive its checks from changed paths**, as noted above.
- **Write the log when the task happens, not after.** Our one missing log is the clearest lesson in
  this project: reconstruction from artefacts recovers what happened but not why, and the reflection
  content the skill asks for is exactly what does not survive.
- **Add a skill for syncing derived artefacts.** Several rounds went into keeping `docs/UserFlows.md`
  and the GitHub issues consistent after each decision. That is a repeatable task with a fixed shape
  and it deserves its own skill.
- **Record decisions closer to where they are used.** We removed a local `issues/` directory in favour
  of GitHub, which was right, but it left decisions scattered across issue comments. A single
  decision log would have made them easier to find and to challenge.

## What we learned about designing a single agent

The skills that worked best had three properties: a **narrow trigger** (it is unambiguous when to use
them), a **declared output shape** (you can tell whether it succeeded), and an **explicit stop
condition** (it knows when to hand back to a human). `create-pull-request` has all three. Vague skills
drifted.

Negative instructions were more valuable than positive ones. "Never weaken a test to make it pass"
and "commit only when authorized" prevented the failures we would least want to discover late.

The agent is best treated as fast, tireless and **unaware of its own mistakes**. It will produce
plausible output from an unverified premise, so the human's job is to check the premise and then
the output. Our most useful conventions were the ones that forced intermediate state into the open -
a plan before implementation, a verification section that names what was not checked. Basic agentic SE
is less about what the agent can do unattended and more about designing the handoffs so that being
wrong is cheap.

[Back to home](index.md)