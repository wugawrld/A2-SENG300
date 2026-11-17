# Mutation analysis

Detailed reasoning for the mutants recorded in `templates/mutations.csv`. Counts and statuses come from the PIT reports listed under each run. PIT targets `BookingService` only, as configured in `pom.xml`.

## PIT runs so far

| Run | Commit | Tests | Killed | No coverage | Survived | Report |
|---|---|---|---|---|---|---|
| First run (R4 branch) | 10d2d46 | 21 | 15 | 5 | 0 | `evidence/runs/divakar-r4/20261116-191122/pit/` |
| After adding `CancellationPermissionTest` | 6eb1158 | 42 | 15 | 5 | 0 | `evidence/runs/r3-permission/20261116-193118/pit/` |

PIT generated 20 mutants in both runs. Mutation score is 15 of 20, or 75%. Test strength is 15 of 15 covered mutants, or 100%. There were no timeouts, memory errors or run errors.

The 5 uncovered mutants are all in `create`, on lines 30 to 34 (the room conflict loop and its `CONFLICT` return). No merged test reaches the conflict loop with a booking in storage (the creation tests that seed a booking return `INVALID_TIME` first), so that code never runs. These belong to R2 and should be reached once `RoomConflictTest` is merged. Adding the R3 tests did not change the score, because the `cancel` mutants were already killed by other classes.

## M1: boundary change on line 51 (R4)

- **Location:** `BookingService.java:51`, `if (minutes < 120)`.
- **Change:** `ConditionalsBoundaryMutator` turns `<` into `<=`.
- **Behaviour:** an owner cancelling an ACTIVE booking exactly 120 minutes before start gets `TOO_LATE`. R4 says exactly 120 is allowed.
- **Status:** KILLED in both runs.
- **Detection:** PIT lists `SaveFailureTest.unarmedRepositoryAllowsTheSameValidCancellation` as the killing test because it failed first. `CancellationBoundaryTest.enforcesCancellationBoundary` at 120 detects it too: `assertEquals(expected, f.service.cancel("b1", "owner"))` expects `CANCELLED` and gets `TOO_LATE`.
- **Why 119 and 121 are not enough:** both versions return `TOO_LATE` at 119 and `CANCELLED` at 121. Only the 120 case tells them apart.
- **Action:** none. The existing assertion already detects it.

## M2: ownership guard removed on line 44 (R3)

- **Location:** `BookingService.java:44`, `if (!booking.ownerId().equals(callerId))`.
- **Change:** `RemoveConditionalMutator_EQUAL_ELSE` replaces the equality check with `false`, so the `DENIED` return on line 45 is never reached.
- **Behaviour:** a non-owner is treated like the owner. On an ACTIVE booking at 120 minutes or more, the stranger gets `CANCELLED`, the booking is saved as CANCELLED and a notification is requested. Below 120 the stranger gets `TOO_LATE`. On a CANCELLED or COMPLETED booking the stranger gets `INACTIVE`. R3 requires `DENIED` in every one of these cases.
- **Status:** KILLED in both runs.
- **Detection, first run:** `CancellationSequencesTest.denialThenOwnerCancellationPreservesTheValidPath`. Its first step expects `DENIED` for the stranger.
- **Detection, second run:** `CancellationPermissionTest.nonOwnerIsDeniedBeforeStateAndTime`, invocation 10 (CANCELLED booking at 121 minutes). The assertion is `assertEquals(CancelStatus.DENIED, f.service.cancel("b1", "stranger"))`. The mutant returns `INACTIVE` for that case.
- **All 15 cases detect it:** the mutant never returns `DENIED`, so the status assertion fails for every state and time combination, not only the one PIT stopped at.
- **Saved data check:** for ACTIVE at 120 and 121, `assertEquals(List.of(original), f.repository.all())` would also fail, because the mutant saves the booking as CANCELLED. This matters for a bug that returns `DENIED` but still saves the change. The status assertion alone would miss that one. The stored data assertion catches it.
- **Action:** none. Already detected, so no test was changed and no improvement is claimed.

## What this shows beyond coverage

JaCoCo reports line 44 as covered as soon as any test calls `cancel`. That says the line ran, not that its result was checked. PIT changing the guard and a test failing shows that an assertion depends on the ownership check.
