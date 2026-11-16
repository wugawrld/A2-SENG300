package ca.ucalgary.seng300.booking;

import static ca.ucalgary.seng300.booking.TestFixtures.at;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CreationTimeTest {

    @ParameterizedTest(name = "start={0}, end={1} must be invalid")
    @CsvSource({
        "-1, 10",
        "0, 10",
        "1, 1",
        "2, 1",
        "-2, -1",
        "0, 0"
    })
    void rejectsInvalidTimesWithoutEffects(long startMinutes, long endMinutes) {
        TestFixtures fixtures = new TestFixtures();

        // keep another booking in storage so we can check that it stays the same
        Booking otherBooking = fixtures.seed(
            "other", "OTHER", "other-owner", 180, 240, BookingState.ACTIVE
        );

        Instant start = at(startMinutes);
        Instant end = at(endMinutes);
        CreateResult result = fixtures.service.create("R204", "owner", start, end);

        assertEquals(CreateStatus.INVALID_TIME, result.status());
        assertNull(result.bookingId());
        assertEquals(List.of(otherBooking), fixtures.repository.all());
        assertTrue(fixtures.notifier.requests().isEmpty());
    }

    @Test
    void acceptsFirstFutureMinuteAndPersistsFullBooking() {
        TestFixtures fixtures = new TestFixtures();

        // one minute after the fixed clock is the first valid start
        Instant start = at(1);
        Instant end = at(2);
        CreateResult result = fixtures.service.create("R204", "owner", start, end);

        assertEquals(CreateStatus.CREATED, result.status());
        assertNotNull(result.bookingId());

        // check the saved booking as well as the result returned by create
        Booking expectedBooking = new Booking(
            result.bookingId(), "R204", "owner", start, end, BookingState.ACTIVE
        );
        assertEquals(List.of(expectedBooking), fixtures.repository.all());
        assertTrue(fixtures.notifier.requests().isEmpty());
    }

    @Test
    void invalidTimeTakesPrecedenceOverAnOverlap() {
        TestFixtures fixtures = new TestFixtures();
        Booking existingBooking = fixtures.seed(
            "b1", "R204", "owner", -10, 180, BookingState.ACTIVE
        );

        // the request overlaps another booking, but starting at 0 is invalid
        CreateResult result = fixtures.service.create("R204", "owner", at(0), at(60));

        assertEquals(CreateStatus.INVALID_TIME, result.status());
        assertNull(result.bookingId());
        assertEquals(List.of(existingBooking), fixtures.repository.all());
        assertTrue(fixtures.notifier.requests().isEmpty());
    }
}
