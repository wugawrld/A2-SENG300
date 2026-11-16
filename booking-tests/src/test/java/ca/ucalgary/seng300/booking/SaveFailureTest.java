package ca.ucalgary.seng300.booking;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static ca.ucalgary.seng300.booking.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** Part 6 Option B: a save that fails during an otherwise valid owner cancellation. */
class SaveFailureTest {
    /** Delegate real storage behaviour; fail only an explicitly armed save. */
    static final class ControllableRepository implements BookingRepository {
        final InMemoryBookingRepository storage = new InMemoryBookingRepository();
        final RepositoryException failure = new RepositoryException("simulated save failure");
        boolean failSaves;
        int saveAttempts;
        @Override public Booking find(String id) { return storage.find(id); }
        @Override public List<Booking> all() { return storage.all(); }
        @Override public void save(Booking booking) {
            saveAttempts++;
            if (failSaves) throw failure; // Must precede mutation of storage.
            storage.save(booking);
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {120, 121})
    void failedSavePropagatesWithoutChangingDataOrNotifying(long minutes) {
        ControllableRepository repository = new ControllableRepository();
        Booking target = new Booking("b1", "R204", "owner", at(minutes), at(minutes + 60), BookingState.ACTIVE);
        Booking other = new Booking("other", "OTHER", "another", at(300), at(360), BookingState.COMPLETED);
        repository.save(target);
        repository.save(other);
        List<Booking> before = repository.all();
        int seededAttempts = repository.saveAttempts;
        RecordingNotifier notifier = new RecordingNotifier();
        BookingService service = new BookingService(repository, notifier, Clock.fixed(NOW, ZoneOffset.UTC), () -> "unused");
        repository.failSaves = true; // Arm only after successful fixture setup.
        RepositoryException thrown = assertThrows(RepositoryException.class, () -> service.cancel("b1", "owner"));
        assertSame(repository.failure, thrown, "the original exception reaches the caller");
        assertEquals(seededAttempts + 1, repository.saveAttempts);
        assertEquals(before, repository.all());
        assertTrue(notifier.requests().isEmpty(), "no request after a failed save");
    }

    @Test void unarmedRepositoryAllowsTheSameValidCancellation() {
        ControllableRepository repository = new ControllableRepository();
        Booking target = new Booking("b1", "R204", "owner", at(120), at(180), BookingState.ACTIVE);
        repository.save(target);
        RecordingNotifier notifier = new RecordingNotifier();
        BookingService service = new BookingService(repository, notifier, Clock.fixed(NOW, ZoneOffset.UTC), () -> "unused");
        assertEquals(CancelStatus.CANCELLED, service.cancel("b1", "owner"));
        Booking expected = target.withState(BookingState.CANCELLED);
        assertEquals(2, repository.saveAttempts);
        assertEquals(List.of(expected), repository.all());
        assertEquals(List.of(expected), notifier.requests());
    }
}
