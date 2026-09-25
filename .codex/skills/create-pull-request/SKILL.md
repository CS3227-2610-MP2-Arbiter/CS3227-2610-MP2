---
name: create-pull-request
description: Prepare or update a GitHub pull request for an Arbiter change, including verification evidence and acceptance status.
---

# create-pull-request

**Input:** The task branch, related issue, final changes, verification results and human acceptance status.

1. Inspect the branch diff against the intended base and check for an existing pull request. Identify any uncommitted task changes before delivery.
2. Write a concise title and description covering the problem, resulting behavior, related issue, verification, deviations and remaining limitations. Use an existing repository template when available.
3. Prepare the complete description before publishing. Follow the explicit delivery approval rule in `context/swe.md` before committing, pushing, or creating or updating the pull request. If approval is absent or publication is unavailable, return the prepared text and exact remaining steps.
4. Check available CI and review status for the final revision. Keep incomplete acceptance or verification explicit and use draft status where appropriate. Shared contracts need both owners' review; merging remains a human action.

**Done when:** The pull request URL or ready-to-use draft is provided, with verification, acceptance and review status accurately reported.
