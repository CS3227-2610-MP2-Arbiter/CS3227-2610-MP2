---
title: Arbiter
---

# Arbiter

Arbiter is an offline Java desktop app for teams that label text.

A label from one person carries that person's mistakes, and people who can see each other's answers stop judging independently. Arbiter collects several blind, independent answers for every item and turns them into one dataset in which every answer comes with the evidence behind it.

## The two roles

- **Annotators** work a blind queue of plain-text items, giving each one a label or an integer rating without ever seeing anyone else's work ([rule 1](UserFlows.md#3-rules-both-tracks-share)).
- **The adjudicator** owns the workspace and runs each project, from creating accounts and assigning the work to exporting the dataset.

## How it works

1. **Set up.** The adjudicator creates a project of one of two kinds, `SINGLE` (choose one label) or `SCALE` (give an integer rating), with CSV or JSON as its output format ([rule 4](UserFlows.md#3-rules-both-tracks-share)). They then register its plain-text files and define its labels or rating range.
2. **Assign.** Arbiter splits the files into batches, and each batch goes to *k* annotators, so every item collects *k* independent answers.
3. **Submit.** Annotators answer one item at a time, and **Submit & next** records each answer permanently: nobody can change it afterwards ([rule 18](UserFlows.md#3-rules-both-tracks-share)).
4. **Resolve.** Arbiter resolves each item it can from the answers, and the adjudicator settles each remaining [dispute](Glossary.md) ([rule 10](UserFlows.md#3-rules-both-tracks-share)).
5. **Export.** The adjudicator exports the dataset in the project's format, with compact provenance for each current decision ([rule 15](UserFlows.md#3-rules-both-tracks-share)).

The [Glossary](Glossary.md) defines the terms used throughout, and [User Flows](UserFlows.md) shows both flows and the rules they share. The step-by-step behaviour is in the GitHub issues those pages link to.

## Documentation

- [Running a release](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2#running-a-release)
- [User guide](UserGuide.md)
- [Glossary](Glossary.md)
- [User flows and product context](UserFlows.md)
- [Developer guide](DeveloperGuide.md)
- [Smoke checklist](SmokeChecklist.md)
- [Agentic SE reflections](Reflections.md)
- [Source code](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2)
- [Issue backlog](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/milestone/7)
