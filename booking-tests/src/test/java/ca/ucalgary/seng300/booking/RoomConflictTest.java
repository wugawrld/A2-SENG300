package ca.ucalgary.seng300.booking;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static ca.ucalgary.seng300.booking.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** tests room conflicts and time boundaries. */
class RoomConflictTest {
    @ParameterizedTest(name = "[{0},{1}) => {2}")
    @CsvSource({"120,179,CREATED", "120,180,CREATED", "120,181,CONFLICT",
        "180,240,CONFLICT", "190,230,CONFLICT", "170,250,CONFLICT",
        "239,300,CONFLICT", "240,300,CREATED", "241,300,CREATED"})
    // checks separate, touching, and overlapping bookings.
    void classifiesIntervalBoundaries(long start, long end, CreateStatus expected) {
        TestFixtures f = new TestFixtures();
        Booking existing = f.seed("b1", "R204", "first", 180, 240, BookingState.ACTIVE);
        CreateResult result = f.service.create("R204", "second", at(start), at(end));
        assertEquals(expected, result.status());
        // a rejected request must leave existing bookings unchanged.
        if (expected == CreateStatus.CONFLICT) {
            assertNull(result.bookingId());
            assertEquals(List.of(existing), f.repository.all());
        } else {
            assertNotNull(result.bookingId());
            Booking created = new Booking(result.bookingId(), "R204", "second", at(start), at(end), BookingState.ACTIVE);
            assertEquals(List.of(existing, created), f.repository.all());
        }
        assertTrue(f.notifier.requests().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"ACTIVE,OTHER", "CANCELLED,R204", "COMPLETED,R204", "CANCELLED,OTHER", "COMPLETED,OTHER"})
    // different rooms and inactive bookings should not block creation.
    void differentRoomsAndInactiveBookingsDoNotConflict(BookingState state, String room) {
        TestFixtures f = new TestFixtures();
        Booking existing = f.seed("b1", room, "first", 180, 240, state);
        CreateResult result = f.service.create("R204", "second", at(180), at(240));
        assertEquals(CreateStatus.CREATED, result.status());
        assertNotNull(result.bookingId());
        Booking created = new Booking(result.bookingId(), "R204", "second", at(180), at(240), BookingState.ACTIVE);
        assertEquals(List.of(existing, created), f.repository.all());
        assertTrue(f.notifier.requests().isEmpty());
    }
    // checks that the search continues past an unrelated booking.
    @Test void findsConflictAfterANonConflictingBooking() {
        TestFixtures f = new TestFixtures();
        f.seed("unrelated", "OTHER", "first", 180, 240, BookingState.ACTIVE);
        f.seed("conflict", "R204", "first", 180, 240, BookingState.ACTIVE);
        List<Booking> before = f.repository.all();
        CreateResult result = f.service.create("R204", "second", at(190), at(200));
        assertEquals(CreateStatus.CONFLICT, result.status());
        assertNull(result.bookingId());
        assertEquals(before, f.repository.all());
        assertTrue(f.notifier.requests().isEmpty());
    }
}