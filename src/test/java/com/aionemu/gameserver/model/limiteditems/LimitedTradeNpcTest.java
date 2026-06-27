package com.aionemu.gameserver.model.limiteditems;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LimitedTradeNpcTest {

    @Test
    void acceptsAndAppendsJdkListsOfLimitedItems() {
        LimitedItem first = new LimitedItem(1001, 10, 1, "0 0 * * * ?");
        LimitedItem second = new LimitedItem(1002, 20, 2, "0 0 * * * ?");

        LimitedTradeNpc npc = new LimitedTradeNpc(List.of(first));
        npc.putLimitedItems(List.of(second));

        assertEquals(2, npc.getLimitedItems().size());
        assertEquals(1001, npc.getLimitedItems().get(0).getItemId());
        assertEquals(1002, npc.getLimitedItems().get(1).getItemId());
    }
}
