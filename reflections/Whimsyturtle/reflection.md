# Agentic SE reflections: Whimsyturtle

My work on Arbiter was mostly **designing the agent itself**: the skills in `.codex/skills/`, the
workflow in `context/swe.md`, and the conventions the logs follow. So my reflections are about getting
a skill set to actually function, rather than about any one feature.

## How I customised the agent

We did not build one general-purpose assistant. We split the work into eight skills, each with a
declared input, a short list of steps and explicit completion criteria: `clarify-requirements`,
`write-plan`, `implement-feature`, `write-test`, `review`, `maintain-docs`, `create-pull-request` and
`log`.

The rule I settled on was **one skill per human-visible decision point**. A skill exists where a human
approves something, or where output has to match a fixed shape. That is why `create-pull-request`
exists but "write a commit message" does not: the PR is a deliverable with a required structure, a
commit message is not.

They were **refined through use, not designed up front**. Several began as prose and were shortened
after I found the agent over-reading them; `context/swe.md` was deliberately kept short so the detail
stays in the skills, where it is actually consulted.

## Three skills in detail

### 1. `log` - the one that made the others auditable

**What it does.** Writes `logs/<user>/<NNN>-<name>.md`, numbered per person, with four fixed sections:
original request; follow-ups, corrections and reflection; agent responses and outcomes; verification.

**Why it is interesting.** It exists purely so the other seven can be checked. Every log is marked
"Awaiting human verification" until a human says otherwise, and the verification section separates what
was actually checked from what was assumed or skipped.

**How I know it works.** This skill went through the most revision, which is itself the evidence. It
started turn-by-turn and was repetitive, hiding the decisions - so it became task-level. I later asked
for a hybrid: keep the original request distinct from later developments, retain explicit reflection on
mistakes and pushback, summarise across the whole interaction, and drop duplicated sections and
mandatory skill metadata. I also cut date metadata. **Each change came from reading a real log and
finding it unhelpful** - about as direct a feedback loop as a skill can have.

**A correction I made.** The agent initially treated log organisation as part of skill consolidation. It
was a separate concern, so those entries moved into their own log. The lesson: the boundary between
"defining the skill" and "using the skill" is a real boundary, and conflating them made both worse.

### 2. `review` - verification that cannot be talked out of

**What it does.** Inspects a diff against the requirements, runs the relevant checks (for code,
`./gradlew check shadowJar` on JDK 25) and reports findings with severity.

**Why it is interesting.** The instruction I am most glad I wrote: it must distinguish a genuine pass
from a skipped task, a cached result, a `NO-SOURCE` or a check that could not run, and must never hide
a failure or weaken a test. Without that, "tests pass" is unfalsifiable - a run where nothing executed
looks identical to one where everything succeeded.

**Where it earned its keep.** It forced scope discipline too. On documentation-only tasks it makes the
agent state that Gradle was not run *and why*, rather than silently omitting it. That turns a gap into
a recorded decision someone can disagree with.

**What I would change.** It runs a fixed command for code changes. It would be better if it derived the
minimum relevant check from the changed paths, so a docs edit and a repository change did not each need
arguing.

### 3. `write-plan` - the cheapest place to be wrong

**What it does.** Reuses or creates a branch, reads the current code and `context/architecture.md`,
then writes `plans/<task>.md` mapping each acceptance criterion to how it will be verified.

**Why it is interesting.** It forces the *how will we know* question before any implementation exists.
Planning identifies what verification is needed; `write-test` writes the tests later during
implementation. Separating those two was a correction worth making - the agent initially blurred them,
which produced plans that read like test suites.

**The correction.** I pushed back on vague issue and branch terminology and on inconsistent ordering of
existing versus new resources, and asked whether planning actually examines current code. It now
reuses or creates a branch and inspects the code and architecture first. The lesson is that a planning
skill with no grounding in the current tree produces confident plans that do not fit.

## What the agent handled well

- **Turning critique into structure.** I gave it a series of objections - too verbose, wrong boundary,
  vague terminology - and it converted them into concrete rule changes rather than apologising and
  moving on.
- **Consistency enforcement across files.** Once a convention was stated, it applied it everywhere and
  re-scanned to confirm nothing was missed.

## Where the agent created work

- **Over-documenting.** It repeated log-organisation rules into `context/swe.md` when they belonged
  only in the log skill, and added a requirement for the tester to record their name that was pure
  bookkeeping. Both had to be removed. The pattern: **the agent adds structure unless told the boundary
  of the change.**
- **Uneven detail.** Simplifying one stage left it inconsistent with the others, which took another
  round to fix. Keeping an overview at a single level of detail is a genuine discipline.

## What I would change

- **State the blast radius of every request.** Most of my corrections were the agent editing more than
  was asked.
- **Narrow `review` to derive checks from changed paths.**
- **Keep general docs short.** Detail belongs in the skill that is consulted, not the overview that is
  skimmed.

## What I learned about designing a single agent

The skills that worked had three properties: a **narrow trigger**, a **declared output shape** and an
**explicit stop condition**. `create-pull-request` has all three. Vague skills drifted.

Negative instructions were more valuable than positive ones: "never weaken a test to make it pass" and
"commit only when authorized" prevented the failures I would least want to discover late.

Basic agentic SE is less about what the agent can do unattended and more about **designing the handoffs
so that being wrong is cheap**. The plan before implementation, the verification section naming what was
not checked, and the log that a human must confirm - those are the load-bearing parts, not any single
skill's wording.