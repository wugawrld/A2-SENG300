package ca.ucalgary.seng300.booking;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class CancellationPermissionTest {

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

}