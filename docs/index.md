---
title: Arbiter
---

# Arbiter

Arbiter is an offline Java desktop app for teams that label data.

## The two roles

- **Annotators** work a blind queue: one item at a time, choose a label by their best judgement even when the item is ambiguous, optionally add a rationale, and flag only problems with the source material. The adjudicator settles the disagreements that result.
- **The adjudicator** owns the workspace. They create annotator accounts, set up projects and their label taxonomy, assign the work, review flags, settle disputes, choose detection answers and export the finished dataset.

## How it works

An adjudicator imports a corpus and splits it into batches. Each batch goes to several annotators, so every item collects several independent annotations. Arbiter resolves what it can automatically and the adjudicator decides the rest. The result exports as a dataset with provenance for each decision.

See the [Glossary](Glossary.md) for the terms used throughout, and [User Flows](UserFlows.md) for the shape of both flows and the rules they share. The step-by-step behaviour is in the GitHub issues those pages link to.

## Documentation

- [User guide](UserGuide.md)
- [Glossary](Glossary.md)
- [User flows and product context](UserFlows.md)
- [Developer guide](DeveloperGuide.md)
- [Agentic SE reflections](Reflections.md)
- [Source code](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2)
- [Issue backlog](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/milestone/7)
