# Group 8 test plan

## Goal

We will use rules R1 through R5 and the [API guide](../../API.md) to plan tests for the booking service. For each request, we will check what it returns, what it saves, and whether it asks for a notification.

## Who is doing what

* **AbduMalek Ben Issa (30240712)** will work on `CreationTimeTest.java` for R1. He will coordinate the Part 1 plan, choose the input groups and boundaries, and help keep the test map and group README up to date. Sardar will review his work.
* **Sardar Waheed (30096015)** will work on `RoomConflictTest.java` for R2. In Part 2 he will explain the test setup, the fixed clock, the parameterized cases, and what the tests check. AbduMalek will review his work.
* **Raman Raut (30247548)** will work on `CancellationPermissionTest.java` for R3. For Part 1 he will write the cancellation decision table. For Part 4 he will explain the mutation that removes the ownership check. Divakar will review his work.
* **Divakar Gaba (30242369)** will work on `CancellationBoundaryTest.java` for R4. He will cover the 120 minute boundary and explain the change from `< 120` to `<= 120`, both PIT reports, the result counts, and the score. Saarim will review his work.
* **Saarim Ali (30211679)** will work on `CancellationEffectsTest.java` for R5. For Part 5 he will use the same test on the reference, faulty, and repaired versions. He will explain the fix and save the full test results. Selina will review his work.
* **Selina Vuong (30241862)** will work on `CancellationSequencesTest.java`. For Part 3 she will compare starter and final coverage, explain two gaps and the decision outcomes, and describe the limits of coverage. Abdullah will review her work.
* **Abdullah Warraich (30245056)** will work on `SaveFailureTest.java` for Part 6 Option B. He will explain the failed save, write the learning record, explain the missing notification mutation, and help check the submission. Raman will review his work.

Each person will help write tests, run their cases, and review a teammate's work. Use the [contribution record](contributions.csv) to record completed work and commit IDs.

We will review the shared report for Parts 1 through 6 and check it against the saved results. Each member will write their own Part 7 page about a boundary test they worked on, a mutation and the assertion that detects it, and one weakness with a useful next test. The limit is 250 words and one PDF page per person.

## Test setup

* Use Java 17, JUnit 5, and the supplied settings. Keep the reference service code as supplied.
* Use a fresh `TestFixtures` object for every test case, including each row in a parameterized test. For a sequence, use the same fixture for both calls.
* The fixed clock is November 1, 2026 at 10:00 UTC. `at(n)` means `n` whole minutes from that time. Zero is now. A negative number is before now.
* Use valid IDs and the repository that stores bookings in memory. Set up ACTIVE, CANCELLED, and COMPLETED bookings directly. Give them IDs that the service will not generate.
* Use the recording notifier to check notification requests. Run one request at a time. Keep tests independent and avoid sleeps.
* Assume saving and notifications work in the core tests. Check a failed save separately in Part 6 Option B.
* Leave null inputs, missing or duplicate IDs, fractions of a minute, extreme dates, login, simultaneous requests, real databases, email delivery, and the completion feature outside these tests.

## Input groups and boundaries

### R1 Booking creation times

The start must be after now. The end must be after the start. Use minute offsets from the fixed clock for these cases.

* Start 1 minute before now and end 10 minutes after now. Expect `INVALID_TIME`.
* Start now and end 10 minutes later. Expect `INVALID_TIME`.
* Start and end at minute 1. Expect `INVALID_TIME` because the times are the same.
* Start at minute 2 and end at minute 1. Expect `INVALID_TIME` because the end is earlier.
* Start 2 minutes before now and end 1 minute before now. Expect `INVALID_TIME`.
* Start and end now. Expect `INVALID_TIME`.
* Start at minute 1 and end at minute 2. Expect `CREATED` and one new ACTIVE booking.
* Start now and end at minute 60. First add an ACTIVE booking in R204 from 10 minutes before now to minute 180. The request overlaps it, but the start is invalid, so expect `INVALID_TIME` first.

For every rejected case, expect a null booking ID, the same stored bookings, and zero notification requests. Put an unrelated booking in storage for the first six cases so we can check that it stays the same. For the successful case, check that the ID is present and that the saved booking has the right ID, room, owner, start, end, and state. Expect zero notification requests.

### R2 Room conflicts

Start with an ACTIVE booking in R204 from minute 180 to minute 240. Try these requests for the same room with a different owner. A booking can start at the exact time another one ends.

* 120 to 179 ends before the existing booking. Expect `CREATED`.
* 120 to 180 touches the start. Expect `CREATED`.
* 120 to 181 overlaps the beginning. Expect `CONFLICT`.
* 180 to 240 has the same times. Expect `CONFLICT`.
* 190 to 230 is inside the existing booking. Expect `CONFLICT`.
* 170 to 250 covers the whole existing booking. Expect `CONFLICT`.
* 239 to 300 overlaps the end. Expect `CONFLICT`.
* 240 to 300 touches the end. Expect `CREATED`.
* 241 to 300 starts after the existing booking. Expect `CREATED`.

Also test an overlapping ACTIVE booking in a different room. Expect `CREATED`. An overlapping CANCELLED or COMPLETED booking should also allow creation in the same room or another room. If an unrelated booking is stored before a conflicting booking in R204, expect `CONFLICT`. This checks that the service looks past the first booking.

For `CREATED`, expect an ID and exactly one new ACTIVE booking with the right fields. For `CONFLICT`, expect a null ID and zero new bookings. In both cases, check that the existing bookings stay the same and the notifier gets zero requests.

### R3 Ownership and state

Use `owner` as the booking owner and `stranger` as someone else. Have the stranger try to cancel bookings in all three states. Try one minute after the booking starts, right when it starts, and 119, 120, and 121 minutes before it starts. Expect `DENIED` each time, with the same stored bookings and zero notifications. This checks that the service checks ownership first.

Have the owner try to cancel CANCELLED and COMPLETED bookings at 119, 120, and 121 minutes before they start. Expect `INACTIVE` each time, with the same stored bookings and zero notifications. The 119 minute case checks that the service checks the state before the time limit.

### R4 Cancellation at 120 minutes

Use an ACTIVE booking owned by the caller. Give it a length of 60 minutes and add an unrelated booking so we can check that it stays the same.

* At the start time or one minute after it, expect `TOO_LATE`. The booking stays ACTIVE and there are zero notifications.
* At 119 minutes before the start, expect `TOO_LATE`. The booking stays ACTIVE and there are zero notifications.
* At 120 minutes before the start, expect `CANCELLED`. Save the cancelled booking and request one notification.
* At 121 minutes before the start, expect `CANCELLED`. Save the cancelled booking and request one notification.

On success, the notification must contain the full cancelled booking. Check that the unrelated booking stays the same in every case. The test at exactly 120 minutes catches a change from `< 120` to `<= 120`.

### R5 Stored data and notifications

* A successful cancellation changes the selected booking to CANCELLED and requests one notification with those booking details.
* A rejected request leaves storage the same and requests zero notifications.
* Creating two bookings that touch should save two ACTIVE bookings with different IDs and the right fields. Expect zero cancellation notifications.
* Check the full saved bookings and the whole list of notification requests. A bug could return `DENIED` and still save a change to the booking.

## Cancellation decision table

Check ownership first, then state, then time. Any means that value does not affect the result for that row.

<table>
  <thead>
    <tr><th>Caller is owner</th><th>Booking state</th><th>Time before start</th><th>Result</th><th>Saved data</th><th>Notifications</th></tr>
  </thead>
  <tbody>
    <tr><td>No</td><td>Any</td><td>Any</td><td>DENIED</td><td>Same bookings</td><td>0</td></tr>
    <tr><td>Yes</td><td>CANCELLED</td><td>Any</td><td>INACTIVE</td><td>Same bookings</td><td>0</td></tr>
    <tr><td>Yes</td><td>COMPLETED</td><td>Any</td><td>INACTIVE</td><td>Same bookings</td><td>0</td></tr>
    <tr><td>Yes</td><td>ACTIVE</td><td>Less than 120 minutes</td><td>TOO_LATE</td><td>Same bookings</td><td>0</td></tr>
    <tr><td>Yes</td><td>ACTIVE</td><td>Exactly 120 minutes</td><td>CANCELLED</td><td>Only the selected state changes</td><td>1</td></tr>
    <tr><td>Yes</td><td>ACTIVE</td><td>More than 120 minutes</td><td>CANCELLED</td><td>Only the selected state changes</td><td>1</td></tr>
  </tbody>
</table>

## Two action sequences

Start each sequence with a new fixture. Add an ACTIVE booking called `b1` in R204, owned by `owner`, from minute 120 to minute 180. Check the saved bookings and the notification list after each call.

### Sequence 1 Cancel twice

Add an unrelated ACTIVE booking in another room.

1. The owner cancels `b1`. Expect `CANCELLED`. Only the state of `b1` changes. Check for one notification containing the cancelled booking.
2. The owner cancels `b1` again. Expect `INACTIVE`. Storage stays the same as after the first call, and there is still only one notification.

### Sequence 2 Try as a stranger, then as the owner

1. A stranger tries to cancel `b1`. Expect `DENIED`. The booking stays ACTIVE and otherwise unchanged. Expect zero notifications.
2. The owner cancels `b1`. Expect `CANCELLED`. Only the state of `b1` changes. Check for one notification containing the cancelled booking.

## Test files and rules

Put the core tests in the [Java test folder](../../src/test/java/ca/ucalgary/seng300/booking/) and end each filename with `Test.java`.

* **AbduMalek, `CreationTimeTest.java`**, R1 and saving a creation under R2 and R5. Use `rejectsInvalidTimesWithoutEffects`, `acceptsFirstFutureMinuteAndPersistsFullBooking`, and `invalidTimeTakesPrecedenceOverAnOverlap`.
* **Sardar, `RoomConflictTest.java`**, R2 and R5. Use `classifiesIntervalBoundaries`, `differentRoomsAndInactiveBookingsDoNotConflict`, and `findsConflictAfterANonConflictingBooking`.
* **Raman, `CancellationPermissionTest.java`**, R3 and R5. Use `nonOwnerIsDeniedBeforeStateAndTime` and `ownerOfInactiveBookingIsRejectedBeforeTime`.
* **Divakar, `CancellationBoundaryTest.java`**, R4 and R5. Use `enforcesCancellationBoundary`.
* **Saarim, `CancellationEffectsTest.java`**, R3 through R5 and the Part 5 regression test. Use `nonOwnerCannotChangeStoredBooking`, `successfulCancellationPersistsAndNotifiesExactlyOnce`, and `creatingTwoBookingsDoesNotRequestCancellationNotifications`.
* **Selina, `CancellationSequencesTest.java`**, both required sequences and R3 through R5. Use `successfulCancellationThenRepeatHasOnlyOneNotification` and `denialThenOwnerCancellationPreservesTheValidPath`.
* **Abdullah, `SaveFailureTest.java`**, Part 6 Option B and R4 and R5 for the successful control. Use `failedSavePropagatesWithoutChangingDataOrNotifying` and `unarmedRepositoryAllowsTheSameValidCancellation`.

Use parameterized tests to run the same check with different inputs. Take each expected result from the assignment rules.

The [test map](test%2Dmap.csv) lists each case and what we expect to happen. Each row has the rule, test name, starting data, action, expected result, saved data, notifications, and why the case matters. Update the map if we add a case or change a test name or input. Use the [run log](run%2Dlog.csv) to record actual test runs.
