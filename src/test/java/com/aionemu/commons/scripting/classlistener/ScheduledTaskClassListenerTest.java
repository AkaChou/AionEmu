package com.aionemu.commons.scripting.classlistener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ScheduledTaskClassListenerTest {

    @Test
    void scheduledTaskListenerResolvesCronServiceOncePerOperation() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/aionemu/commons/scripting/classlistener/ScheduledTaskClassListener.java"
        ));

        assertTrue(source.contains("CronService cronService = getCronService();"));
        assertTrue(source.contains("return CronService.requireCurrent();"));
        assertFalse(source.contains("CronService.getInstance()"));
        assertFalse(source.contains("getCronService().schedule"));
        assertFalse(source.contains("getCronService().getRunnables()"));
        assertFalse(source.contains("getCronService().cancel"));
    }
}
