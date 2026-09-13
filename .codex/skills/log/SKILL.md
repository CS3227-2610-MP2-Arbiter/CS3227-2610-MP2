---
name: log
description: Record Arbiter development requests, responses, corrections and verification in concise task-level logs for human review and reflection.
---

# log

**Input:** The available conversation, tool results and changed files for a development task.

1. Create or update `logs/<github-user>/<NNN>-<log-name>.md`. Use `Whimsyturtle` or `zheng-jj` as the folder name based on the user's GitHub username; ask if unclear. Number new logs independently within each user's folder, starting at `001` and using the highest existing prefix plus one, padded to three digits. Use a descriptive title of at most ten words and derive `<log-name>` from it in lowercase kebab case. Update the existing log for a follow-up to the same task; create a new numbered log for a distinct task.
2. Use this structure:

   ```markdown
   # Title

   Status: Awaiting human verification.

   ## Original request

   ## Follow-ups, corrections, and reflection

   ## Agent responses and outcomes

   ## Verification
   ```

3. Summarize the coherent task rather than each conversational turn. Keep the original request distinct from substantive later requests, misunderstandings, pushback, concessions, corrections and lessons. Record each fact once in the section where it is most useful. Summarize important agent decisions, actions and results from across the interaction. Include paths, commands, failures, recovery steps, skill versions and other evidence only when they materially help a human understand or verify the work. State when relevant conversation history is unavailable.
4. Separate observed results from assumptions and pending work. Keep the summary concise, mark it as awaiting human verification unless a human explicitly verifies it, and record supplied verification details. Preserve substantive lessons for possible inclusion in `reflections/<github-user>/reflections.md`.

**Done when:** The log concisely captures the task, its evolution, the agent's work and the available verification without duplication, and its status and path are reported.
