package com.aionemu.gameserver.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 启动阶段基类的包装语义测试：成功一次性执行、幂等、失败记录与重试。
 * Wrapper-semantics tests for the startup-stage base class: single run, idempotence,
 * failure recording and retry.
 */
class GameStartupStageTest {

    @Test
    void startRunsStageOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        RecordingStage stage = new RecordingStage(events, null);

        stage.start();
        stage.start();

        assertTrue(stage.isLoaded());
        assertEquals(List.of("run"), events);
        assertTrue(stage.getLoadTimeMillis() >= 0);
        assertNull(stage.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("stage failed");
        RecordingStage stage = new RecordingStage(events, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, stage::start);

        assertSame(failure, thrown);
        assertSame(failure, stage.getLastFailure());
        assertFalse(stage.isLoaded());
        assertTrue(stage.getLoadTimeMillis() >= 0);

        stage.start();

        assertTrue(stage.isLoaded());
        assertEquals(List.of("run", "run"), events);
        assertNull(stage.getLastFailure());
    }

    private static final class RecordingStage extends GameStartupStage {

        private final List<String> events;
        private final RuntimeException firstFailure;
        private int runs;

        private RecordingStage(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        protected void runStage() {
            events.add("run");
            if (++runs == 1 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }
}
