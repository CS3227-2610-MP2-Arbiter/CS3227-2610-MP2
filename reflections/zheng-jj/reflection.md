# Agentic SE reflections: zheng-jj

My work on Arbiter was mostly specification and process: turning a rough brainstorm into a backlog,
then keeping the docs and the issues in agreement as decisions changed. So my reflections are about
the agent as a **writer and maintainer of documents**, not as a code generator.

## How I customised the agent

The eight skills in `.codex/skills/` each have a declared input, steps and completion criteria. The
principle was **one skill per human-visible decision point** - a skill exists where a human approves
something or where output must match a fixed shape. That is why there is a `create-pull-request` skill
but no "write a commit message" skill.

The part I would stress is the **negative** instructions. `create-pull-request` commits and pushes only
with authorization; `review` never weakens a test to make it pass. Those mattered more than the
positive steps, because they bound the damage when the agent was wrong.

## Three skills in detail

### 1. `clarify-requirements` - refusing to guess

**What it does.** Turns a request into requirements with an owner, affected roles, goal, non-goals and
observable acceptance criteria, then finds or drafts the matching GitHub issue.

**Why it is interesting.** It asks about conflicts that affect scope or correctness but states
reasonable assumptions for minor details. That is a threshold, not a rule to ask about everything, and
getting the threshold right is what keeps it useful rather than annoying.

**What it produced.** This was my largest single win. I gave the agent a rough brainstorm -
unstructured notes about two roles, a blind queue, a wishlist ending in a bare "ORMs for storage" - and
asked it to clean up the requirements and split them into issues. It produced 45 issues with consistent
labels, dependencies and acceptance criteria. More importantly it did **not** silently resolve the
ambiguities: three became explicit spike issues ending in a written decision rather than code,
including "how does a team share data while staying offline", which was a real tension between the
offline premise and the team premise.

**The failure mode.** The agent will produce a confident answer to a question nobody answered. The
spikes worked because they made that visible. Later decisions - when earnings vest, what counts as a
disagreement - were equally load-bearing and only surfaced because I went back and asked. A skill that
forces ambiguity into the open beats one that resolves it quietly.

### 2. `log` - making the work reviewable

**What it does.** Writes `logs/<user>/<NNN>-<name>.md` with four fixed sections: original request;
follow-ups, corrections and reflection; agent responses and outcomes; verification.

**Why it is interesting.** The verification section separates what was actually checked from what was
assumed or skipped. That distinction is the whole value - a log saying "tests pass" without naming
which tests, or omitting the checks that *could not* run, is worse than no log.

**How I know it works.** The structure was revised against real failures: turn-by-turn logs were
repetitive and buried the decisions, so we moved to task-level summaries; we tried recording the
tester's name and dropped it as bookkeeping; we removed date metadata. Each change came from reading an
actual log and finding it unhelpful.

**What it cost.** Writing a log is real work and it is tempting to skip. I skipped it once - when
creating all 45 issues - and had to reconstruct the entry later from issue metadata because the
conversation was gone. The reconstruction is visibly weaker: it records what happened but not why, and
the prompts and corrections are unrecoverable. That is the strongest argument for the skill.

### 3. `maintain-docs` - keeping derived artefacts in step

**What it does.** Updates the guides, architecture context and product site to match accepted
behaviour, and checks shared facts stay consistent across pages.

**Why it is interesting.** Most of my rework came from decisions made in one place and referenced in
several: a change to vesting touched `docs/UserFlows.md`, five GitHub issues and the logs. This skill is
what turns "update the docs" from a vague hope into a checklist.

**Where it still falls short.** It does not yet have a sibling for syncing *issues* with the spec.
Several rounds went into that by hand, and it is a repeatable task with a fixed shape. If I repeated
this, I would add that skill.

## What the agent handled well

- **Bulk, structured generation.** 45 issues with consistent labelling and dependency wiring.
- **Mechanical editing at scale.** Applying one decision across a spec, several issues and the docs,
  then re-scanning for stale wording. It is more thorough at this than I am.

## Where the agent created work

- **Editing long documents - the single largest source of rework.** Multi-line edits to
  `docs/UserFlows.md` repeatedly produced duplicated headings, orphaned sentence fragments, clipped
  bullets and broken section numbering, each needing a re-read to catch. Its strength is generating
  coherent text; its weakness is surgically modifying existing text. **Reading the result back is not
  optional.**
- **Assuming rather than checking.** It reported a document missing when it was already on disk and
  prepared to create a duplicate.
- **Sandboxed operations failing silently.** Git writes and network calls failed under the workspace
  sandbox in ways that looked like logic errors.

## What I would change

- **Make every document edit verifiable.** Follow each multi-line edit with a structural check -
  heading order, numbering sequence, no duplicate lines. Nearly all my rework would have been caught
  by that one step.
- **Write the log when the task happens.** Reconstruction recovers what happened, not why, and the
  reflection content is exactly what does not survive.
- **Add a skill for syncing the spec with the GitHub issues.**

## What I learned about designing a single agent

The skills that worked had three properties: a **narrow trigger** (unambiguous when to use them), a
**declared output shape** (you can tell if it succeeded), and an **explicit stop condition** (it knows
when to hand back). Vague skills drifted.

The agent is fast, tireless and **unaware of its own mistakes**. It produces plausible output from an
unverified premise, so my job is to check the premise and then the output. The most useful conventions
were the ones forcing intermediate state into the open - a plan before implementation, a verification
section naming what was not checked. Basic agentic SE is less about what the agent does unattended and
more about designing the handoffs so that being wrong is cheap.