package ca.ucalgary.seng300.booking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

/**
 * R3 + R5: This is for saved data and notifications
 * I check the return value and whats saved in storage,
 * and what notifications were requested.
 */
public class CancellationEffectsTest {
    
    /**
     * a non owner trying to cancel should be denied
     * and nothing in the storage should change.
     */
    @Test 
    void nonOwnerCannotChangeStoredBooking() {
        // fresh repository
        TestFixtures f = new TestFixtures();
        // the booking i try to cancel
        f.seed("b1", "R204", "owner", 120, 180, BookingState.ACTIVE);
        // an unrelated booking to make sure it also stays teh same
        f.seed("other", "OTHER", "another", 300, 360, BookingState.COMPLETED);

        // snapshot before the request
        List<Booking> before = f.repository.all();

        // "stranger" is not an owner so it must be denied
        assertEquals(CancelStatus.DENIED,f.service.cancel("b1", "stranger"));

        // storage must be the same as before 
        assertEquals(before,f.repository.all(), "DENIED must not change ant stored booking");

        // a rejected request has no notifcication
        assertTrue(f.notifier.requests().isEmpty());
    }

    /**
     *  R4 + R5: the owner cancels exactly 120 minutes before start which is allowed
     *  the booking msut be saved as cancelled and exactly one notification sent.
     */
    @Test 
    void successfulCancellationPersistsAndNotifiesExactlyOnce() {
        TestFixtures f = new TestFixtures();
        // booking to cancels the starts in 120 minutes
        Booking original = f.seed("b1", "R204", "owner", 120,180, BookingState.ACTIVE);

        // unrelated booking that shouldnt change
        Booking other = f.seed("other", "OTHER", "another", 300, 360, BookingState.CANCELLED);

        // owner cancels so it should succeed
        assertEquals(CancelStatus.CANCELLED, f.service.cancel("b1", "owner"));

        //what the booking should look like
        Booking expected = original.withState(BookingState.CANCELLED);

        // target is canceled so other booking us untouched
        assertEquals(List.of(expected, other), f.repository.all());

        //Exactly one notification and its for the right booking 
        assertEquals(List.of(expected), f.notifier.requests(), "Exact count and complete payload");

    }


   
}
