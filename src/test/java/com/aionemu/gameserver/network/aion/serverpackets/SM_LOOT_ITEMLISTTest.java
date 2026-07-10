package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.DropNpc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class SM_LOOT_ITEMLISTTest {

    @Test
    void constructorKeepsPublicAndPlayerOwnedDropsOnly() throws Exception {
        DropItem publicDrop = dropForPlayer(0);
        DropItem playerDrop = dropForPlayer(100);
        DropItem otherPlayerDrop = dropForPlayer(200);
        Player player = player(100);

        SM_LOOT_ITEMLIST packet = new SM_LOOT_ITEMLIST(new DropNpc(1),
                new LinkedHashSet<>(Set.of(publicDrop, playerDrop, otherPlayerDrop)), player);

        Collection<?> dropItems = dropItems(packet);
        assertEquals(2, dropItems.size());
        assertTrue(dropItems.contains(publicDrop));
        assertTrue(dropItems.contains(playerDrop));
    }

    @Test
    void constructorTreatsNullDropSetAsEmpty() throws Exception {
        SM_LOOT_ITEMLIST packet = new SM_LOOT_ITEMLIST(new DropNpc(1), null, player(100));

        assertTrue(dropItems(packet).isEmpty());
    }

    private static DropItem dropForPlayer(int playerObjId) throws Exception {
        DropItem dropItem = new ObjenesisStd().newInstance(DropItem.class);
        Field playerObjIds = DropItem.class.getDeclaredField("playerObjIds");
        playerObjIds.setAccessible(true);
        playerObjIds.set(dropItem, new ArrayList<Integer>());
        dropItem.setPlayerObjId(playerObjId);
        return dropItem;
    }

    private static Player player(int objectId) throws Exception {
        Player player = new ObjenesisStd().newInstance(Player.class);
        Field objectIdField = AionObject.class.getDeclaredField("objectId");
        objectIdField.setAccessible(true);
        objectIdField.set(player, objectId);
        return player;
    }

    private static Collection<?> dropItems(SM_LOOT_ITEMLIST packet) throws Exception {
        Field dropItems = SM_LOOT_ITEMLIST.class.getDeclaredField("dropItems");
        dropItems.setAccessible(true);
        return (Collection<?>) dropItems.get(packet);
    }
}
