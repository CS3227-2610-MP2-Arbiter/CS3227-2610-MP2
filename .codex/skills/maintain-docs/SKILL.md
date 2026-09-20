---
name: maintain-docs
description: Update Arbiter guides, architecture context and product site content to match accepted behavior or agreed process changes.
---

# maintain-docs

**Input:** An accepted behavior change or agreed process change, with relevant implementation and verification evidence.

1. Identify the affected documentation: `docs/UserGuide.md`, `docs/DeveloperGuide.md`, `docs/index.md`, `README.md` and project context as applicable.
2. Check descriptions against the implementation. Update role-specific instructions, setup, examples and limitations without presenting planned features as available.
3. Keep each shared fact in one home:
   - `docs/index.md` for what Arbiter is and what each role does.
   - `docs/Glossary.md` for the domain terms.
   - The Developer Guide's "Setting up" section for setup and build instructions.
   - The Developer Guide's "Design" section for the design and its rationale.

   `README.md`, `context/` and the other pages in `docs/` link to that home instead of restating it:
   - Give at most a one-sentence summary.
   - `context/architecture.md` keeps only the code-level rules the design implies.

   Add acknowledgements for reused material where needed.
4. Check affected links and commands; preview pages when layout changes. Report any instructions or platform behavior that could not be verified. Publishing is a separate delivery action.

**Done when:** Relevant documentation matches the agreed change, with updated files and verification limits reported.
