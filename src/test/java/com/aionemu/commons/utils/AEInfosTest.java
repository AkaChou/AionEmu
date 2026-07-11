package com.aionemu.commons.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AEInfosTest {

    @Test
    void memoryInfoHasNoBoxDecorations() {
        for (String line : AEInfos.getMemoryInfo()) {
            assertFalse(line.startsWith("+----"), line);
            assertFalse(line.equals("|    |"), line);
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
