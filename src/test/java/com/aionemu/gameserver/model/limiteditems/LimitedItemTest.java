package com.aionemu.gameserver.model.limiteditems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LimitedItemTest {

    @Test
    void exposesJdkMapForPerPlayerBuyCounts() {
        LimitedItem item = new LimitedItem(1001, 10, 1, "0 0 * * * ?");

        Map<Integer, Integer> buyCounts = item.getBuyCount();
        item.setBuyCount(42, 3);
        item.setBuyCount(42, 9);

        assertEquals(3, buyCounts.get(42));

        item.setToDefault();

        assertTrue(buyCounts.isEmpty());
        assertEquals(10, item.getSellLimit());
    }
}
