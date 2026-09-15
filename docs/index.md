---
title: Arbiter
---

# Arbiter

Arbiter is an offline Java desktop app for teams that label data.

## The two roles

- **Annotators** work a blind queue: one item at a time, choose a label, add a rationale, flag
  anything unclear. They never see each other's answers.
- **Adjudicators** import a corpus, split it into batches, assign annotators, and resolve the items
  where annotators disagreed. They own the label taxonomy and export the finished dataset with
  provenance for every decision.

## How it works

An adjudicator imports a corpus and splits it into batches. Each split goes to several annotators, so
every item collects several independent annotations. Where they agree, Arbiter resolves the item
automatically; where they disagree, an adjudicator decides. The result exports as COCO, YOLO, Pascal
VOC, CSV or JSON, with a provenance record explaining every decision.

See [User Flows](UserFlows.md) for the full step-by-step walkthrough.

## Documentation

- [User guide](UserGuide.md)
- [User flows and issue index](UserFlows.md)
- [Developer guide](DeveloperGuide.md)
- [Agentic SE reflections](Reflections.md)
- [Source code](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2)
- [Issue backlog](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/milestone/7)
