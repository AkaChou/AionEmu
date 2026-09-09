package com.aionemu.commons.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AEInfosTest {

    @Test
    void memoryInfoHasNoBoxDecorations() {
        for (String line : AEInfos.getMemoryInfo()) {
            assertFalse(line.startsWith("+----"), line);
			assertNotEquals("|    |", line, line);
            assertFalse(line.contains("...."), line);
        }
        assertTrue(AEInfos.getMemoryInfo().length >= 4);
    }

    @Test
    void cpuOsJreJvmHaveNoDotRules() {
        for (String[] block : new String[][] {
            AEInfos.getCPUInfo(), AEInfos.getOSInfo(), AEInfos.getJREInfo(), AEInfos.getJVMInfo()
        }) {
            for (String line : block) {
                assertFalse(line.startsWith("...."), line);
            }
            assertTrue(block.length >= 1);
        }
    }
}
