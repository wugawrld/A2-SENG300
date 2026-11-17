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

    
}
