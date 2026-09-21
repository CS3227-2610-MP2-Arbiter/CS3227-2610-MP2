---
title: Arbiter
---

# Arbiter

Arbiter is an offline Java desktop app for teams that label data.

## The two roles

- **Annotators** work a blind queue: one item at a time, choose a label, add a rationale, flag anything unclear. They never see each other's answers.
- **Adjudicators** import a corpus, split it into batches, assign annotators, review annotations and decide unresolved items. They own the label taxonomy and export the finished dataset with provenance for each item's current decision.

## How it works

An adjudicator imports a corpus and automatically divides it into batches by item count. Each split goes to several annotators, with assignments fixed once created. They work forward through one current draft at a time, permanently submitting each answer or unusable-item report before advancing; there is no return or correction loop. Single-select labels resolve by strict majority or adjudication of disputes, numeric scale ratings are averaged, and detection uses one complete submitted box set selected by the adjudicator. CSV/JSON/COCO exports include submitted evidence and each item's current decision. The [shared rules](UserFlows.md#3-rules-both-tracks-share) define valid contributors, freeze setup after assignment and seal completed projects.

See the [Glossary](Glossary.md) for the terms used throughout, and [User Flows](UserFlows.md) for the shape of both flows and the rules they share. The step-by-step behaviour is in the GitHub issues those pages link to.

## Documentation

- [User guide](UserGuide.md)
- [Glossary](Glossary.md)
- [User flows and product context](UserFlows.md)
- [Developer guide](DeveloperGuide.md)
- [Agentic SE reflections](Reflections.md)
- [Source code](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2)
- [Issue backlog](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/milestone/7)
