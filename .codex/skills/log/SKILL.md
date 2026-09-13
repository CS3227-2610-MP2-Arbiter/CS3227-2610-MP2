---
name: log
description: Record Arbiter development prompts, agent actions, corrections and results in summary logs for human verification and reflection.
---

# log

**Input:** The available conversation, tool results and changed files for a development task.

1. Create or update `logs/YYYY-MM-DD-<task>.md`, preserving earlier entries. Summarize the user's requests and follow-up corrections in order.
2. Record the skills used and their versions when known, key decisions, changes, actual checks, failures and recovery steps. Use a commit reference or note uncommitted skill edits rather than inventing a version.
3. Separate observed results from assumptions and work still pending. Include useful paths or command evidence and note any missing conversation history.
4. Mark the summary as awaiting human verification unless a human has explicitly verified it. Record verification details when supplied; keep substantive skill lessons available for `docs/Reflections.md`.

**Done when:** The log reflects the available interaction and evidence, with its verification status clear and its path reported.
