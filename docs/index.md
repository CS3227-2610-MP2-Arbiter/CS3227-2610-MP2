---
title: Arbiter
---

# Arbiter

Arbiter is an offline Java desktop app for teams that label data.

## The two roles

- **Annotators** work a blind queue of plain-text items, submitting either one label or an integer scale rating for each item. They never see another annotator's work or the resolved answer.
- **The adjudicator** owns the workspace. They create annotator accounts, set up text-classification projects and their taxonomies, assign the work, monitor basic progress, settle label disputes and export the dataset.

## How it works

An adjudicator registers a plain-text corpus and splits it into batches. Each batch goes to several annotators, so every item collects several independent annotations. Arbiter resolves what it can automatically and the adjudicator decides the remaining label disputes. The result exports as a dataset with compact provenance for each current decision.

See the [Glossary](Glossary.md) for the terms used throughout, and [User Flows](UserFlows.md) for the shape of both flows and the rules they share. The step-by-step behaviour is in the GitHub issues those pages link to.

## Documentation

- [User guide](UserGuide.md)
- [Glossary](Glossary.md)
- [User flows and product context](UserFlows.md)
- [Developer guide](DeveloperGuide.md)
- [Agentic SE reflections](Reflections.md)
- [Source code](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2)
- [Issue backlog](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/milestone/7)
