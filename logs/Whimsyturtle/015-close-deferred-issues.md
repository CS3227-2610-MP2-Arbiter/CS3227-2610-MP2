# Close deferred issues as not planned

Status: Human verified.

## Original request

Close all deferred issues in this repository as not planned.

## Follow-ups, corrections, and reflection

The GitHub connector could read issues but returned `403 Resource not accessible by integration` on the first update attempt. The local `gh` token was invalid inside the sandbox; the authenticated CLI worked with escalated network access. No issue changed during the failed attempts.

## Agent responses and outcomes

Closed [#20], [#21], [#39] and [#40] with GitHub's `not_planned` state reason. Updated the status sentence in #20 and #21 because it still directed readers to keep those issues open. The previously closed scope cuts referenced by [#62] needed no action.

## Verification

Fetched each issue after the changes: all four report `closed` and `not_planned`. A live search for open issues with `Deferred` in the title returned none. No code checks were needed for issue metadata changes.

[#20]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/20
[#21]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/21
[#39]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/39
[#40]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/40
[#62]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/62
