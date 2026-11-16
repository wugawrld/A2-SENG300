package ca.ucalgary.seng300.booking;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class CancellationPermissionTest {

    /**
     * This function will create a stream that contains 15 different test cases, a mix of:
     *  - one of: active, canceled, completed
     *  - one of: -1, 0, 119, 120, 121
     *
     * @return - Stream
     */
    static Stream<Arguments> allStatesAndTimes() {
        return Stream.of(BookingState.values()).flatMap(state ->
                Stream.of(-1L, 0L, 119L, 120L, 121L).map(start -> Arguments.of(state, start)));
    } //allStatesAndTimes

    /**
     * This function will create a stream that contains 6 different test cases, a mix of:
     *  - one of: cancelled, completed
     *  - one of: 119, 120, 121
     *
     * @return - Stream
     */
    static Stream<Arguments> inactiveStatesAndTimes() {
        return Stream.of(BookingState.CANCELLED, BookingState.COMPLETED).flatMap(state ->
                Stream.of(119L, 120L, 121L).map(start -> Arguments.of(state, start)));
    } //inactiveStatesAndTimes

    /**
     * This tests whether a person who does not own a booking is asctually prevented from canelling it.
     * It should always return "DENIED"
     * @param state - the current booking state
     * @param start - booking's time
     */
    @ParameterizedTest(name = "non-owner: {0} at {1} minutes => DENIED")
    @MethodSource("allStatesAndTimes")
    void nonOwnerIsDeniedBeforeStateAndTime(BookingState state, long start) {
        TestFixtures f = new TestFixtures();
        Booking original = f.seed("b1", "R204", "owner", start, start + 60, state);
        assertEquals(CancelStatus.DENIED, f.service.cancel("b1", "stranger"));
        assertEquals(List.of(original), f.repository.all());
        assertTrue(f.notifier.requests().isEmpty());
    } //nonOwnerIsDeniedBeforeStateAndTime

    /**
     * This tests the owner trying to cancel their booking, but it's already inactive
     * It should always result in "INACTIVE"
     * @param state - the current booking state
     * @param start - booking's time
     */
    @ParameterizedTest(name = "owner: {0} at {1} minutes => INACTIVE")
    @MethodSource("inactiveStatesAndTimes")
    void ownerOfInactiveBookingIsRejectedBeforeTime(BookingState state, long start) {
        TestFixtures f = new TestFixtures();
        Booking original = f.seed("b1", "R204", "owner", start, start + 60, state);
        assertEquals(CancelStatus.INACTIVE, f.service.cancel("b1", "owner"));
        assertEquals(List.of(original), f.repository.all());
        assertTrue(f.notifier.requests().isEmpty());
    } //ownerOfInactiveBookingIsRejectedBeforeTime







}