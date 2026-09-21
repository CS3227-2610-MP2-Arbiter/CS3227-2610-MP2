---
title: Arbiter
---

# Arbiter

Arbiter is an offline Java desktop app for teams that label data.

## The two roles

- **Annotators** work a blind queue: one item at a time, choose a label, add a rationale, flag anything unclear. They never see each other's answers.
- **Adjudicators** import a corpus, split it into batches, assign annotators, review annotations and decide unresolved items. They own the label taxonomy and export the finished dataset with provenance for every decision.

## How it works

An adjudicator imports a corpus and splits it into batches. Each split goes to several annotators, so every item collects several independent annotations. For classification, single-select labels resolve by strict majority or an adjudicator's decision, while numeric scale ratings are averaged. The result exports as CSV, JSON or COCO, with a provenance record explaining every decision.

See the [Glossary](Glossary.md) for the terms used throughout, and [User Flows](UserFlows.md) for the shape of both flows and the rules they share. The step-by-step behaviour is in the GitHub issues those pages link to.

## Documentation

- [User guide](UserGuide.md)
- [Glossary](Glossary.md)
- [User flows and product context](UserFlows.md)
- [Developer guide](DeveloperGuide.md)
- [Agentic SE reflections](Reflections.md)
- [Source code](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2)
- [Issue backlog](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/milestone/7)
