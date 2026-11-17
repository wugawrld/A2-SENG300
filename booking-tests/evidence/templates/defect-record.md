# One bug record
(R3 / R5)

## Rule and setup
The bug breaks R3 and R5. A non owner should get DENIED, and a rejected request shouldn't change anything that's saved.

I used `CancellationEffectsTest.nonOwnerCannotChangeStoredBooking` and didn't change it between versions. The test saves booking b1 (owner is "owner", ACTIVE, starts in 120 min, ends at 180 min) and another COMPLETED booking. Then "stranger" tries to cancel b1.


## Steps to reproduce
1. On branch `saarim` (reference), inside booking-tests run:
   `./mvnw clean test -Dtest='CancellationEffectsTest#nonOwnerCannotChangeStoredBooking'`
2. `git switch -c saarim-bug`, then from the root run `git apply --directory=booking-tests faulty-version/buggy-version.patch`, and commit.
3. Run the same command again.
4. Remove the bad line, commit, and run the same command again.
5. Run `./mvnw clean test` on the repaired version.
## Expected and actual outcomes
Expected: DENIED, nothing in storage changes, no notifications.

Actual (faulty version): it still returned DENIED and sent no notification, but b1 got saved as CANCELLED. So checking only the return value would have missed this. The test failed at line 40 where I compare storage before and after (expected ACTIVE, got CANCELLED). It was an assertion failure.

## Cause and minimal fix
The patch added `repository.save(booking.withState(BookingState.CANCELLED));` inside the non-owner branch of `BookingService.cancel`, before `return CancelStatus.DENIED;`. The fix removes that one line. Repair patch: `faulty-version/repair-from-faulty.patch`.
## Same test on reference and buggy and repaired versions
| Version | Commit | Result | Evidence |
| --- | --- | --- | --- |
| Reference | 4e2159a | 1 passed | booking-tests/evidence/runs/bug-reference |
| Faulty (committed before repair) | 92a5445 | 1 assertion failure | booking-tests/evidence/runs/bug-faulty |
| Repaired | 14d6486 | 1 passed | booking-tests/evidence/runs/bug-repaired |
## Full suite after repair
14 passed, 0 failures, 0 errors, 0 skipped at commit 14d6486. Output: booking-tests/evidence/runs/bug-repaired-full. The repair is on branch `saarim-bug`.
## Remaining risk
Running the tests again only checks the cases I wrote, using in memory storage. It wouldn't catch the same kind of bug in a spot I didn't test, like an extra save when the result is INACTIVE or TOO_LATE. A good next test would check that storage stays the same for every rejected cancel and not just DENIED.