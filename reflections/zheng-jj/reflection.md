# Agentic SE reflections: zheng-jj (Zheng Jiongjie)

Most of my work on Arbiter was specification and process rather than code. we had a rough brainstorm, turned it into a backlog, and then spent the rest of the project keeping the docs and the issues in agreement as decisions changed.

## How I customised the agent

What I'd stress most are the negative instructions. `create-pull-request` may commit and push only with authorization; `review` must never weaken a test to make it pass. Those mattered more than the positive steps, because they bound the damage when the agent got something wrong. A skill that only describes the happy path falls apart the first time the agent is confident and incorrect.

## Three skills in detail

### 1. `clarify-requirements` — refusing to guess

**What it does.** It turns a request into requirements with an owner, affected roles, a goal, non-goals and observable acceptance criteria, then finds or drafts the matching GitHub issue.

**Why it's interesting.** It asks about conflicts that affect scope or correctness, but states a reasonable assumption for minor details. That's a threshold rather than a rule to ask about everything, and getting the threshold right is what keeps it useful instead of annoying.

**What it produced.** I handed the agent a rough brainstorm — unstructured notes about two roles, a blind queue, and a wishlist that ended in a bare "ORMs for storage" — and asked it to tidy the requirements and split them into issues. It produced 45 issues with consistent labels, dependencies and acceptance criteria. More importantly, it did not quietly resolve the ambiguities. Three of them became explicit spike issues that end in a written decision rather than code, including "how does a team share data while staying offline", which was a real tension between the offline premise and the team premise.

**The failure mode.** The agent will happily produce a confident answer to a question nobody answered. The spikes worked because they forced that into the open. Later on, decisions like when earnings vest and what counts as a disagreement were just as load-bearing, and they only surfaced because I went back and asked. A skill that forces ambiguity into the open beats one that resolves it quietly.

### 2. `log` — making the work reviewable

**What it does.** It writes `logs/<user>/<NNN>-<name>.md` with four fixed sections: the original request; follow-ups, corrections and reflection; agent responses and outcomes; and verification.

**Why it's interesting.** The verification section separates what was actually checked from what was assumed or skipped. That distinction is the whole point. A log that says "tests pass" without naming which tests, or that leaves out the checks that _couldn't_ run, is worse than no log at all.

**How I know it works.** We revised the structure against real failures. The turn-by-turn version was repetitive and buried the decisions, so we moved to task-level summaries. We tried recording the tester's name and dropped it as bookkeeping. We removed date metadata. Every one of those changes came from reading an actual log and finding it unhelpful.

**What it cost.** Writing a log is real work, and it's tempting to skip. I skipped it once — when creating all 45 issues — and had to reconstruct the entry afterwards from issue metadata, because the conversation was gone. The reconstruction is visibly weaker: it records what happened but not why, and the prompts and corrections are unrecoverable. That's the strongest argument I have for the skill.

### 3. `maintain-docs` — keeping derived artefacts in step

**What it does.** It updates the guides, the architecture context and the product site to match accepted behaviour, and checks that shared facts stay consistent across pages.

**Why it's interesting.** Most of my rework came from decisions made in one place and referenced in several. A change to vesting touched `docs/UserFlows.md`, five GitHub issues and the logs. This skill is what turns "update the docs" from a vague hope into a checklist.

**Where it still falls short.** It has no sibling yet for syncing _issues_ with the spec. Several rounds of that went in by hand, and it's a repeatable task with a fixed shape. If I did this again, I'd add that skill.

## Building the annotator track with the agent

The second half of my work was implementation: source resolution ([#10]), the test harness and blindness test ([#11]), the UI kit ([#8]), and the whole annotator track — My splits ([#12]), the queue ([#13]), the answer controls ([#14]), Submit & next ([#17]) and progress ([#18]). Here the agent did nearly all the typing, and my role shifted to deciding, approving and checking.

### What I delegated

For each issue the agent ran the whole chain: a plan, the code, tests derived from the acceptance criteria, `./gradlew check shadowJar`, a log and a pull-request draft. I kept four things for myself — approving the plan, approving every commit, push and PR, answering product questions, and merging.

A couple of concrete examples:

- **The blindness test ([#11]).** The rule "fails if any annotator-facing code can reach another annotator's answers" became an ArchUnit walk over the compiled classes, method by method. It's proven against fixture classes that each leak in a different way.
- **The screenshots for the annotator guide ([#22]).** The session couldn't capture my screen, so the agent rendered the real screens from a seeded workspace using JavaFX snapshots.

### What worked

- **Tests the agent wrote before implementing caught its own bugs.** The first draft of the #11 fixture unboxed a null `Integer` for every unassigned split, and eight fixture tests failed on the first run. The fix was in the fixture; no test was weakened.
- **Mutation checks made the tests trustworthy.** After each feature the agent removed the rule under test and confirmed its tests failed — dropping the ownership check in the queue, the duplicate-submission guards, and `submit` from the blindness test's trusted list. A test suite that passes proves very little until you've watched it fail for the right reason.
- **Stacked pull requests.** #14, #17 and #18 went up as three PRs, each built on the one before, and rebasing them onto new work on `main` was mechanical. When #26 tightened the test fixture, a trial merge in a throwaway worktree showed exactly which 12 tests would break, before anything was pushed.

### What didn't work

- **The first attempt at #11 was poor, and I only noticed by reading it.** It duplicated hashing that #10 was about to provide, checked blindness per class instead of per method (which would flag legitimate code as soon as a service mixed scoped and unscoped reads), described APIs in its plan that it never built, and marked its own log "Human verified" when I hadn't verified it. I had it throw the branch away and start again.
- **My own verification missed a steady stream of design problems that review caught.** Whimsyturtle left 31 review comments across my eight feature PRs, and every one led to a change. The same patterns kept coming back:
  - **The same fact decided in two places.** A card took its status text from the stored status but its button label from the answer count. "Finished" was defined once by status and once by the absence of a next file. The current file was stored twice.
  - **Duplicating work that had just landed.** I built #14 before #26 existed, so it carried its own taxonomy record and number parser. Once #26 merged, both duplicated what #26 had added.
  - **Behaviour nobody had tried on screen.** The queue page could push Submit & next off the bottom, and the rating field showed an error while you were still typing "-2".
  - **Imagined scenarios in comments and docs.** The code and the User Guide described the queue moving on "in another window". That can't happen — the workspace lock allows one instance, and it has one window.
- **The agent broke the delivery rule once, and misreported it.** It committed review fixes locally before I'd approved them, and wrote "The owner approved delivery" into a log. It caught the false line before pushing and told me, but the rule exists precisely because the agent will do this.
- **A product insight the agent missed entirely.** Showing annotators the file path can leak labels (`label_x/item_1.txt`), and so can the source resolver's error messages, which name the path. Whimsyturtle raised it; after that the agent carried it into later work without being asked.

### How I checked AI-generated work

Every change ran JUnit and Checkstyle locally, then CI on Linux, macOS, Windows and an Apple Silicon jar check. Every plan listed how each acceptance criterion would be verified. Every log separated what was checked from what wasn't — "the GUI was not run by the agent" appears in nearly every log from this phase. That line is the honest weak spot. As far as the logs show, nobody ran the annotator screens by hand before they merged, and several review comments were things a single run would have shown.

On how much got rewritten: every feature PR in this phase needed at least one round of fixes after review, and #11 was rewritten from scratch. The fixes were usually small, but only a human reading the diff would have found them.

## What the agent handled well

- **Bulk, structured generation.** 45 issues with consistent labelling and dependency wiring.
- **Mechanical editing at scale.** Applying one decision across a spec, several issues and the docs, then re-scanning for stale wording. It's more thorough at this than I am.

## Where the agent created work

- **Editing long documents — the single largest source of rework.** Multi-line edits to `docs/UserFlows.md` repeatedly produced duplicated headings, orphaned sentence fragments, clipped bullets and broken section numbering, each needing a re-read to catch. Its strength is generating coherent text; its weakness is surgically modifying existing text. **Reading the result back is not optional.**
- **Assuming rather than checking.** It reported a document missing when it was already on disk, and was about to create a duplicate.
- **Sandboxed operations failing silently.** Git writes and network calls failed under the workspace sandbox in ways that looked like logic errors.

## What I would change

- **Make every document edit verifiable.** Follow each multi-line edit with a structural check — heading order, numbering sequence, no duplicate lines. Nearly all my rework would have been caught by that one step.
- **Write the log when the task happens.** Reconstruction recovers what happened, not why, and the reflection content is exactly what doesn't survive.
- **Add a skill for syncing the spec with the GitHub issues.**
- **Re-check what a blocking issue added the moment it lands.** Building ahead of a dependency is fine, but when it merges, its types and helpers should replace mine before review, not because of it.
- **One home per fact in the code, not only in the docs.** Several review comments were the same value decided in two places. A review checklist item for derived state ("where else is this decided?") would have caught them.
- **Run the screen before asking for review.** A two-minute manual pass would have caught the cut-off button and the mid-typing error.
- **Keep approvals mechanical.** The agent treats "approved" loosely. A rule that every commit, push and PR needs an explicit yes worked, but only because I kept enforcing it.

## What I learned about designing a single agent

The skills that worked had three things in common: a **narrow trigger** (it's unambiguous when to use them), a **declared output shape** (you can tell whether they succeeded) and an **explicit stop condition** (they know when to hand back). Vague skills drifted.

The agent is fast, tireless and **unaware of its own mistakes**. It produces plausible output from an unverified premise, so my job is to check the premise and then the output. The most useful conventions were the ones that forced intermediate state into the open — a plan before implementation, a verification section naming what wasn't checked. Basic agentic SE turns out to be less about what the agent does unattended and more about designing the handoffs so that being wrong is cheap.

The implementation phase sharpened that. The agent writes code that compiles, passes its own tests and reads well. What it doesn't do on its own is ask "is this already decided somewhere else?" or "what would this look like on screen?". Those questions came from review and from me. The mutation checks and the explicit "not verified" lines were the most valuable conventions, because they turned "it passes" into "it passes, and here's what nobody has checked yet".

[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#12]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/12
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#18]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/18
[#22]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/22
