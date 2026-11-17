# SENG 300 Software Testing

**Group 8 Shared Report | Parts 1 through 6 | Java 17**

AbduMalek Ben Issa (30240712), Sardar Waheed (30096015), Raman Raut (30247548), Divakar Gaba (30242369), Saarim Ali (30211679), Selina Vuong (30241862), and Abdullah Warraich (30245056).

## Part 1 Test plan

We planned the tests using rules R1 through R5. For creation we covered starts before, at, and after now, end times at or before the start, and room bookings that are separate, touching, or overlapping. For cancellation we used a decision table for the caller, booking state, and minutes until the booking starts. The test map records the starting data, action, expected response, saved bookings, and notifications for each case. The plan and its 57 cases were committed at `6fdf688` before the new test classes were added to this repo. [Test plan](../booking%2Dtests/evidence/templates/test%2Dplan.md) · [Test map](../booking%2Dtests/evidence/templates/test%2Dmap.csv)

We planned two sequences. An owner who cancels twice gets `CANCELLED` and then `INACTIVE`, with one notification overall. A stranger gets `DENIED`, then the owner can cancel the same booking. The later plan revision added six cases that show which cancellation rule comes first. At 119 minutes a stranger gets `DENIED`, and an owner of an inactive booking gets `INACTIVE`. [Plan revision](https://github.com/wugawrld/A2%2DSENG300/commit/6eb1158)

## Part 2 Automated tests

`CreationTimeTest.acceptsFirstFutureMinuteAndPersistsFullBooking` checks the first valid start and the saved booking. `RoomConflictTest.classifiesIntervalBoundaries` checks which intervals can share a room. `CancellationPermissionTest.nonOwnerIsDeniedBeforeStateAndTime` and `CancellationBoundaryTest.enforcesCancellationBoundary` check who can cancel and when. `CancellationSequencesTest` checks both planned sequences. `CancellationEffectsTest.nonOwnerCannotChangeStoredBooking` checks the effects of a denial. `SaveFailureTest` covers the failed save. [Java test files](../booking%2Dtests/src/test/java/ca/ucalgary/seng300/booking/)

Each test uses fresh data and a fixed clock. We check the returned status, stored bookings, and notification requests. Checking only for `DENIED` would miss a request that still changes the saved booking. All 60 tests passed at `c0ecb43` with zero failures, errors, or skips. From the booking tests folder run `./mvnw clean verify` on macOS or Linux, or <code>.\mvnw.cmd &#45;B clean verify</code> on Windows. [Test output](../booking%2Dtests/evidence/runs/selina%2Dpart3%2Dfinal/test%2Doutput.txt)

## Part 3 Coverage

<table>
  <thead><tr><th>JaCoCo measure</th><th>Starter 6fdf688</th><th>Final c0ecb43</th></tr></thead>
  <tbody>
    <tr><td>BookingService lines</td><td>22/31 (71%)</td><td>31/31 (100%)</td></tr>
    <tr><td>BookingService branches</td><td>7/20 (35%)</td><td>20/20 (100%)</td></tr>
    <tr><td>cancel() branches</td><td>4/6</td><td>6/6</td></tr>
  </tbody>
</table>

* **Gap 1.** The starter missed the `INACTIVE` return at line 48. `CancellationPermissionTest.ownerOfInactiveBookingIsRejectedBeforeTime` covers it and checks that storage stays the same and no notification is requested.
* **Gap 2.** The starter missed the `TOO_LATE` return at line 52. `CancellationBoundaryTest.enforcesCancellationBoundary` covers it at 119 minutes and checks the same effects.

The permission tests cover denial for a stranger and rejection of an inactive booking. The boundary tests use an active booking owned by the caller and cover both sides of the time check. Together they exercise six decision outcomes in `cancel()`, matching the six JaCoCo branches. Coverage shows which paths ran. The assertions tell us whether the results and side effects were correct. [Coverage gaps](../booking%2Dtests/evidence/templates/coverage%2Dgaps.csv) · [Starter JaCoCo](../booking%2Dtests/evidence/runs/starter%2Dcoverage/jacoco/index.html) · [Final JaCoCo](../booking%2Dtests/evidence/runs/selina%2Dpart3%2Dfinal/jacoco/index.html)

## Part 4 Mutation testing

The first PIT run at `10d2d46` killed 15 of 20 mutants, for 75%. The other five had no coverage. The final run at `a1c3514` killed all 20, for 100%. In both runs there were zero survivors and zero tool errors. The room conflict tests reached the five previously uncovered mutants. For example, the overlapping case expects `CONFLICT` and catches the changed overlap check on line 33.

* **M1** changes the boundary from `< 120` to `<= 120`. At exactly 120 minutes the boundary test expects `CANCELLED` and the mutant returns `TOO_LATE`.
* **M2** removes the ownership guard. The permission test expects `DENIED` for a stranger and catches the wrong response.
* **M5** changes the last overlap check on line 33. It had `NO_COVERAGE` at `10d2d46`. At `a1c3514`, `RoomConflictTest.classifiesIntervalBoundaries` case 7 tried a booking from 239 to 300 against one already active from 180 to 240 in the same room. The mutant allows the booking. `assertEquals(expected, result.status())` expects `CONFLICT` and catches it.

From the booking tests folder run <code>mvn &#45;B test&#45;compile org.pitest:pitest&#45;maven:mutationCoverage</code>. [First PIT report](../booking%2Dtests/evidence/runs/divakar%2Dr4/20261116%2D191122/pit/index.html) · [Final PIT report](../booking%2Dtests/evidence/runs/divakar%2Dfinal%2Dpit/pit/index.html) · [Mutation records](../booking%2Dtests/evidence/templates/mutations.csv) · [Run log](../booking%2Dtests/evidence/templates/run%2Dlog.csv)

## Part 5 Bug repair

`CancellationEffectsTest.nonOwnerCannotChangeStoredBooking` expects `DENIED`, unchanged storage, and zero notifications. The faulty version returned `DENIED` and still saved the booking as cancelled. The fix removes that extra save. The regression test passed at `4e2159a`, failed the storage assertion at `92a5445`, and passed after the repair at `14d6486`. At the repair checkpoint the full suite passed all 14 tests. The tests use an in memory repository, so real database rollback remains unverified. [Defect record](../booking%2Dtests/evidence/templates/defect%2Drecord.md) · [Repair patch](../faulty%2Dversion/repair%2Dfrom%2Dfaulty.patch) · [Repaired run](../booking%2Dtests/evidence/runs/bug%2Drepaired%2Dfull/output.txt)

The focused command was <code>./mvnw clean test &#45;Dtest='CancellationEffectsTest#nonOwnerCannotChangeStoredBooking'</code>. The full run used `./mvnw clean test`.

## Part 6 Failed save experiment

For Option B, `SaveFailureTest.failedSavePropagatesWithoutChangingDataOrNotifying` makes the repository throw during a valid cancellation. It checks that the original `RepositoryException` reaches the caller, the bookings stay the same, and no notification is requested. The successful case `unarmedRepositoryAllowsTheSameValidCancellation` checks that the same request works when saving is allowed. All three cases passed at `075b229` with <code>./mvnw &#45;Dtest=SaveFailureTest test</code>. [Save failure evidence](../booking%2Dtests/evidence/runs/abdullah%2Dsave%2Dfailure/20261116%2D172035/)

**Learning record.** We used the supplied API guide to decide what should happen when a save fails during a valid cancellation: the error reaches the caller, nothing changes, and no notification is sent. Adding the starting bookings also uses save, so our test version of the repository saves normally during setup and only fails once we switch it on, right before the cancel call. It throws before storing anything, and we check that the caller gets the exact same exception, not just any error. Our first recorded run went wrong because it reused files compiled with a newer Java version, so we deleted the build folder and ran it again on Java 17. We then added a second test where saving works. It shows the request itself is valid, so the failed save is the only thing stopping it. The mutation results showed this second test also catches a missing notification and the 120 minute boundary change. We learned to check the result, the stored bookings and the notifications together, because one can be wrong while the others look fine. Our in memory repository leaves real database rollback unverified. A useful next test would make saving fail on a valid cancellation at 122 minutes with another booking already stored. We would check that the exception reaches the caller, both bookings stay the same, and no notification is sent. Sources: [API guide](../booking%2Dtests/API.md), JUnit 5 User Guide.
