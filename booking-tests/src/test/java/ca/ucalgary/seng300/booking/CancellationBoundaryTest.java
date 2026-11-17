package ca.ucalgary.seng300.booking;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/** R4 study package allocated to Divakar; exactly 120 minutes is allowed. */
class CancellationBoundaryTest {
    @ParameterizedTest(name = "{0} minutes => {1}")
    @CsvSource({"-1,TOO_LATE", "0,TOO_LATE", "119,TOO_LATE", "120,CANCELLED", "121,CANCELLED"})
    void enforcesCancellationBoundary(long minutes, CancelStatus expected) {
        TestFixtures f = new TestFixtures();
        Booking original = f.seed("b1", "R204", "owner", minutes, minutes + 60, BookingState.ACTIVE);
        Booking unrelated = f.seed("other", "OTHER", "another", 300, 360, BookingState.ACTIVE);
        assertEquals(expected, f.service.cancel("b1", "owner"));
        if (expected == CancelStatus.CANCELLED) {
            Booking cancelled = original.withState(BookingState.CANCELLED);
            assertEquals(List.of(cancelled, unrelated), f.repository.all());
            assertEquals(List.of(cancelled), f.notifier.requests());
        } else {
            assertEquals(List.of(original, unrelated), f.repository.all());
            assertTrue(f.notifier.requests().isEmpty());
        }
    }
}
