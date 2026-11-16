package ca.ucalgary.seng300.booking;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CancellationSequencesTest {
    // ensuring repeat cancellations for the same booking only results in one notification and no change to stored bookings
    @Test void successfulCancellationThenRepeatHasOnlyOneNotification() {
        TestFixtures f = new TestFixtures(); // initializing new simulated variables (service, repository, notifier)

        // repository of booking that will be cancelled
        Booking original = f.seed("b1", "R204", "owner", 120, 180, BookingState.ACTIVE);
        Booking other = f.seed("other", "OTHER", "another", 300, 360, BookingState.ACTIVE); // unrelated booking that won't be affected by cancellation requests
        Booking expected = original.withState(BookingState.CANCELLED); // setting expected value for the booking we want to cancel.

        // first cancel by the booking owner results in a CANCELLED status
        assertEquals(CancelStatus.CANCELLED, f.service.cancel("b1", "owner"));
        assertEquals(List.of(expected, other), f.repository.all()); // booking b1 is cancelled while booking other is not affected
        assertEquals(List.of(expected), f.notifier.requests()); // only one notification has been sent for the cancellation request

        // second cancel by the booking owner results in no additional notifications or status changes
        assertEquals(CancelStatus.INACTIVE, f.service.cancel("b1", "owner")); // cancelling a cancelled booking reports INACTIVE
        assertEquals(List.of(expected, other), f.repository.all()); // repository is not altered by the repeat attempt
        assertEquals(List.of(expected), f.notifier.requests()); // only the original notification was sent. no additional notifs were created
    }

    // booking is first cancelled by an unauthorized user followed by the real owner cancelling.
    // initial attempt is denied without affecting the rest of the bookings and owner is still able to cancel
    @Test void denialThenOwnerCancellationPreservesTheValidPath() {
        TestFixtures f = new TestFixtures(); // initializing new simulated variables (service, repository, notifier)

        // repository of booking that will be cancelled by the OWNER
        Booking original = f.seed("b1", "R204", "owner", 120, 180, BookingState.ACTIVE);

        // unauthorized user STRANGER attempts to cancel OWNER'S booking. this request should be denied
        assertEquals(CancelStatus.DENIED, f.service.cancel("b1", "stranger"));
        assertEquals(List.of(original), f.repository.all()); // other bookings are unaffected
        assertTrue(f.notifier.requests().isEmpty()); // no notifications are sent

        // OWNER attempts to cancel their own booking
        assertEquals(CancelStatus.CANCELLED, f.service.cancel("b1", "owner")); // this should succeed, irrelevant of the STRANGER's cancel request
        Booking expected = original.withState(BookingState.CANCELLED); // booking should now be cancelled
        assertEquals(List.of(expected), f.repository.all()); // repository reflects the cancelled booking
        assertEquals(List.of(expected), f.notifier.requests()); // only one notification sent for the successful booking cancellation
    }
}
