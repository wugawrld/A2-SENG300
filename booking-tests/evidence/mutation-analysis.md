# Mutation analysis

Detailed reasoning for the mutants recorded in `templates/mutations.csv`. Counts and statuses come from the PIT reports listed under each run. PIT targets `BookingService` only, as configured in `pom.xml`.

The three mutants discussed in the shared report are M1, M2 and M5. M3 is kept as an extra notification example.

## PIT runs

| Run | Commit | Tests | Killed | No coverage | Survived | Score | Report |
|---|---|---|---|---|---|---|---|
| First run (R4 branch) | 10d2d46 | 21 | 15 | 5 | 0 | 75% | `evidence/runs/divakar-r4/20261116-191122/pit/` |
| After adding `CancellationPermissionTest` | 6eb1158 | 42 | 15 | 5 | 0 | 75% | `evidence/runs/r3-permission/20261116-193118/pit/` |
| Final run on `main` | a1c3514 | 60 | 20 | 0 | 0 | 100% | `evidence/runs/divakar-final-pit/pit/` |

PIT generated the same 20 mutants in every run. There were no survivors, timeouts, memory errors or run errors in any run. Each folder holds the full HTML report and `mutations.xml`.

## From 75% to 100%

The first two runs scored 15 of 20 (75%). Test strength was already 100%, because all 15 mutants that the tests reached were killed. The 5 that were not killed were uncovered: they sit in `create` on lines 30 to 34, the room conflict loop and its `CONFLICT` return. No test at those commits reached that loop with a booking in storage, because the creation tests that seed a booking return `INVALID_TIME` first.

`RoomConflictTest` was merged after the second run. In the final run all 5 are KILLED, and PIT lists the same killing test for each: `RoomConflictTest.classifiesIntervalBoundaries` invocation 7, which requests [239,300) against an ACTIVE booking at [180,240) in the same room and expects `CONFLICT`.

| Line | Mutator | First run | Final run |
|---|---|---|---|
| 30 | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE | KILLED |
| 31 | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE | KILLED |
| 32 | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE | KILLED |
| 33 | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE | KILLED |
| 34 | NullReturnValsMutator | NO_COVERAGE | KILLED |

The other 15 mutants were KILLED in all three runs. So the gain came from new tests reaching code that had no tests, not from stronger assertions on M1, M2 or M3. Adding the R3 tests alone did not change the score, because the `cancel` mutants were already killed by other classes.

Final score: 20 of 20 generated mutants killed, 100%.

## M1: boundary change on line 51 (R4)

- **Location:** `BookingService.java:51`, `if (minutes < 120)`.
- **Change:** `ConditionalsBoundaryMutator` turns `<` into `<=`.
- **Behaviour:** an owner cancelling an ACTIVE booking exactly 120 minutes before start gets `TOO_LATE`. R4 says exactly 120 is allowed.
- **Status:** KILLED in all three runs.
- **Detection:** PIT lists `SaveFailureTest.unarmedRepositoryAllowsTheSameValidCancellation` as the killing test because it failed first. `CancellationBoundaryTest.enforcesCancellationBoundary` at 120 detects it too: `assertEquals(expected, f.service.cancel("b1", "owner"))` expects `CANCELLED` and gets `TOO_LATE`.
- **Why 119 and 121 are not enough:** both versions return `TOO_LATE` at 119 and `CANCELLED` at 121. Only the 120 case tells them apart.
- **Action:** none. The existing assertion already detects it.

## M2: ownership guard removed on line 44 (R3)

- **Location:** `BookingService.java:44`, `if (!booking.ownerId().equals(callerId))`.
- **Change:** `RemoveConditionalMutator_EQUAL_ELSE` replaces the equality check with `false`, so the `DENIED` return on line 45 is never reached.
- **Behaviour:** a non-owner is treated like the owner. On an ACTIVE booking at 120 minutes or more, the stranger gets `CANCELLED`, the booking is saved as CANCELLED and a notification is requested. Below 120 the stranger gets `TOO_LATE`. On a CANCELLED or COMPLETED booking the stranger gets `INACTIVE`. R3 requires `DENIED` in every one of these cases.
- **Status:** KILLED in all three runs.
- **Detection, first run:** `CancellationSequencesTest.denialThenOwnerCancellationPreservesTheValidPath`. Its first step expects `DENIED` for the stranger.
- **Detection, second run:** `CancellationPermissionTest.nonOwnerIsDeniedBeforeStateAndTime`, invocation 10 (CANCELLED booking at 121 minutes). The assertion is `assertEquals(CancelStatus.DENIED, f.service.cancel("b1", "stranger"))`. The mutant returns `INACTIVE` for that case.
- **Detection, final run:** `CancellationEffectsTest.nonOwnerCannotChangeStoredBooking`, which also expects `DENIED` for a stranger. PIT stops at the first failing test, so the listed test changes as classes are added.
- **All 15 cases detect it:** the mutant never returns `DENIED`, so the status assertion fails for every state and time combination, not only the one PIT stopped at.
- **Saved data check:** for ACTIVE at 120 and 121, `assertEquals(List.of(original), f.repository.all())` would also fail, because the mutant saves the booking as CANCELLED. This matters for a bug that returns `DENIED` but still saves the change. The status assertion alone would miss that one. The stored data assertion catches it.
- **Action:** none. Already detected, so no test was changed and no improvement is claimed.

## M3: notification call removed on line 56 (R5)

- **Location:** `BookingService.java:56`, `notifier.cancellationRequested(cancelled)`.
- **Change:** `VoidMethodCallMutator` removes the call.
- **Behaviour:** a valid cancellation still returns `CANCELLED` and still saves the booking as CANCELLED, but no notification is requested. R5 requires exactly one request with the cancelled booking.
- **Status:** KILLED in all three runs.
- **Detection:** `CancellationBoundaryTest.enforcesCancellationBoundary`, invocation 5 (121 minutes), in every run. The assertion is `assertEquals(List.of(cancelled), f.notifier.requests())`. The mutant leaves the list empty.
- **Why the other assertions are not enough:** the response assertion and the stored data assertion both pass on this mutant. A test that checked only the return value and storage would report a pass while no notification was sent.
- **Action:** none. Already detected.

## M5 Overlap check on line 33 (R2)

* Location and change. `RemoveConditionalMutator_EQUAL_ELSE` makes the final overlap check, `existing.start().isBefore(end)`, fail.
* First run at `10d2d46`. PIT marked this `NO_COVERAGE` because the tests did not reach this overlap decision.
* Final run at `a1c3514`. PIT marked it `KILLED` and listed `RoomConflictTest.classifiesIntervalBoundaries` case 7.
* Assertion. The test requests [239, 300) while the same room has an active booking at [180, 240). The mutant allows the overlapping request. `assertEquals(expected, result.status())` catches it because the expected status is `CONFLICT`.
* The new room conflict cases reached this decision. The initial and final reports identify the same mutation.

[First PIT XML](runs/divakar-r4/20261116-191122/pit/mutations.xml) · [Final PIT XML](runs/divakar-final-pit/pit/mutations.xml)

## What this shows beyond coverage

JaCoCo reports line 44 as covered as soon as any test calls `cancel`. That says the line ran, not that its result was checked. PIT changing the guard and a test failing shows that an assertion depends on the ownership check.
