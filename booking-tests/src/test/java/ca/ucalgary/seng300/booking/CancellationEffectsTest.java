package ca.ucalgary.seng300.booking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ca.ucalgary.seng300.booking.TestFixtures.*;

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


    /**
     * R2 + R5 : Creating bookings should never send a notification
     */
    @Test void creatingTwoBookingsDoesNotRequestCancellationNotifications() {
        TestFixtures f = new TestFixtures();

        // two bookings in the same room
        CreateResult first = f.service.create("R204", "owner", at(180), at(240));
        CreateResult second = f.service.create("R204", "owner",at(240), at(300));

        //both should be made
        assertEquals(CreateStatus.CREATED, first.status());
        assertEquals(CreateStatus.CREATED, second.status());
        
        // both should have an id and they should be different
        assertNotNull(first.bookingId());
        assertNotNull(second.bookingId());
        assertNotEquals(first.bookingId(), second.bookingId());

        //storage should hold two active bookings
        assertEquals(List.of(
            new Booking(first.bookingId(), "R204", "owner", at(180), at(240), BookingState.ACTIVE),
            new Booking(second.bookingId(), "R204", "owner", at(240), at(300), BookingState.ACTIVE)),
            f.repository.all());

        // Creating bookings must not request any notification
        assertTrue(f.notifier.requests().isEmpty());
    }
}
