package com.aionemu.gameserver.model.assemblednpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class AssembledNpcTest {

    @Test
    void acceptsJdkListOfAssembledParts() {
        AssembledNpcPart part = new AssembledNpcPart(7, null);

        AssembledNpc npc = new AssembledNpc(11, 22, 30, List.of(part));
        List<AssembledNpcPart> assembledParts = npc.getAssembledParts();

        assertEquals(11, npc.getRouteId());
        assertEquals(22, npc.getMapId());
        assertEquals(1, assembledParts.size());
        assertEquals(7, assembledParts.get(0).getObject());
    }
}
