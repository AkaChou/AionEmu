package com.aionemu.commons.network.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.commons.utils.ExitCode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeadLockDetectorTest {

    @Test
    void restartHandlingUsesInjectedExitHandler() {
        List<Integer> statuses = new ArrayList<>();
        DeadLockDetector detector = new DeadLockDetector(60, DeadLockDetector.RESTART, statuses::add);

        detector.handleDeadlock();

        assertEquals(List.of(ExitCode.CODE_RESTART), statuses);
    }

    @Test
    void nothingHandlingDoesNotExit() {
        List<Integer> statuses = new ArrayList<>();
        DeadLockDetector detector = new DeadLockDetector(60, DeadLockDetector.NOTHING, statuses::add);

        detector.handleDeadlock();

        assertEquals(List.of(), statuses);
    }
}
