---
title: Smoke Checklist
---

# Smoke Checklist

A manual end-to-end check of the retained text-classification workflow ([#41]), run before a release and after any change that crosses both roles. Each step names what to do and what you should see; how each screen works is in the [User Guide](UserGuide.md), and the rules cited are in section 3 of [User Flows](UserFlows.md#3-rules-both-tracks-share).

Part A builds everything by hand from a fresh workspace. Part B opens a seeded demo workspace to check the outcomes that need three annotators or many answers. Record every run in [Results](#results).

## The demo corpus and seeded workspace

The demo corpus is in `src/demo/resources/arbiter/demo/corpus/`: six product reviews in `reviews/` and six answers to support questions in `answers/`. The file names are neutral, so they never hint at a label.

The command in [Setting up](DeveloperGuide.md#setting-up) seeds a demo workspace. It never changes how Arbiter normally starts, and it builds the workspace through the same services the screens use, signed in as each account, so it can only reach states the app can reach.

Every demo account's password is `demo1234`.

| Account | Role |
| --- | --- |
| `owner` | Adjudicator |
| `alice`, `bob`, `carol` | Annotators |

| Project | Kind and format | *k* | Assigned | State after seeding |
| --- | --- | --- | --- | --- |
| Product reviews | `SINGLE` (positive, negative, mixed), CSV | 3 | alice, bob, carol | Every file answered. `review-01`, `review-03` resolve to positive and `review-02`, `review-05` to negative by majority. `review-04` and `review-06` are disputes |
| Answer helpfulness | `SCALE` 1 to 5, JSON | 2 | alice, bob | alice rated all six; bob rated the first four in his queue, which resolve to the mean of the two ratings. Two files wait for bob |

## Part A: from a fresh workspace

1. **Owner setup.** Launch Arbiter and create a new workspace in an empty folder, with an owner account. You land on Projects after signing in.
2. **Accounts.** On Accounts, create two annotators. Both are listed as active annotators.
3. **Project.** Create a `SINGLE` project with the CSV format.
4. **Source import.** Copy the six files in `reviews/` into the workspace's `media/` folder. On the project page, choose **Add files...** and select all six. All six are listed.
5. **Taxonomy.** Add the labels `positive`, `negative` and `mixed`.
6. **Split.** Generate splits of 6 files and confirm. One split holds all six.
7. **Assignment.** Assign both annotators to the split with *k* = 2. The split's **Annotators** column shows 2 of 2, and the taxonomy and file list can no longer be changed (rule 3).
8. **Annotation.** Sign out and sign in as the first annotator. My splits shows only their split. Answer every file, choosing `positive` for the first file and `negative` for the second. Restart Arbiter partway through: the split resumes at the first unanswered file (rule 18).
9. **Majority and dispute.** Sign in as the second annotator and answer every file, choosing `positive` for the first file and `mixed` for the second. As the owner, Projects shows fewer unresolved files; Disputes lists the second file and any other file you answered differently.
10. **Manual resolution.** Open a dispute, pick a label and choose **Save**. It moves to the decided files.
11. **CSV export.** Choose **Export** and confirm. `exports/project-<id>.csv` lists every file with its answer and method (`MAJORITY` or `ADJUDICATED`), each annotator's answer, and `UNRESOLVED` for any dispute left open (rule 15).
12. **Content-hash failure.** Edit one registered file in `media/` and save it. As the owner, **Export** refuses and names the changed file, and a dispute on that file cannot be saved. If that file is next in an annotator's queue, their split says it cannot be read instead of showing its text (rule 21). Restore the file's original text: export works again.

## Part B: the seeded workspace

1. **Open.** Seed a new demo workspace, launch Arbiter, open the workspace and sign in as `owner`.
2. **Configurable *k*.** The **Annotators** column shows 3 of 3 for the Product reviews split and 2 of 2 for the Answer helpfulness split.
3. **Majority and disputes.** Product reviews shows two unresolved files. Its Disputes screen lists `review-04` and `review-06`, each with three different labels and no annotator names. Resolve one of them.
4. **Scale averaging and annotation.** Sign in as `bob`. Answer helpfulness shows 4 of 6 files submitted; rate the last two. As `owner`, Answer helpfulness now has no unresolved files.
5. **Exports.** Export both projects. The CSV shows `review-01` and `review-03` as positive and `review-02` and `review-05` as negative by `MAJORITY`, your decision as `ADJUDICATED` and the other dispute as `UNRESOLVED`. The JSON gives each answer the mean of its two ratings with method `AUTO_SCALE`, such as 1.5 for `answer-02` and 4.5 for `answer-03`.
6. **Blindness.** Sign in as `alice`. My splits shows only her two splits, and nothing shows another annotator's answers or a resolved label (rule 1).

## Results

| Date | Commit | Run by | Platform | Part A | Part B | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Not yet run | | | | | | |

[Back to home](index.md)

[#41]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/41
