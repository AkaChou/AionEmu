package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.quest.InventoryItem;
import com.aionemu.gameserver.model.templates.quest.InventoryItems;
import com.aionemu.gameserver.model.templates.quest.QuestCategory;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PlayerQuestStartEligibilityPortTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 987654;
	private static final int REQUIRED_ITEM_ID = 182400001;

	@Test
	void missingRequiredInventoryItemFailsClosedWithoutLegacyWarningSideEffects() throws Exception {
		QuestTemplate template = templateRequiringItem();
		Player player = playerWithEmptyInventory();
		PlayerQuestStartEligibilityPort port = new PlayerQuestStartEligibilityPort(playerId -> player,
			questId -> template, ignored -> false, ignored -> true);

		QuestStartEligibility result = port.snapshot(PLAYER_ID, QUEST_ID);

		assertFalse(result.eligible());
		assertEquals("REQUIRED_INVENTORY_ITEM_MISSING", result.reason());
	}

	private static QuestTemplate templateRequiringItem() throws Exception {
		QuestTemplate template = new QuestTemplate();
		setField(QuestTemplate.class, template, "id", QUEST_ID);
		setField(QuestTemplate.class, template, "minlevelPermitted", 0);
		setField(QuestTemplate.class, template, "category", QuestCategory.TASK);
		InventoryItem required = new InventoryItem();
		setField(InventoryItem.class, required, "itemId", REQUIRED_ITEM_ID);
		InventoryItems inventoryItems = new InventoryItems();
		inventoryItems.getInventoryItem().add(required);
		setField(QuestTemplate.class, template, "inventoryItems", inventoryItems);
		return template;
	}

	private static Player playerWithEmptyInventory() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		setField(Player.class, player, "playerCommonData", new PlayerCommonData(PLAYER_ID));
		setField(Player.class, player, "questStateList", new QuestStateList());
		PlayerStorage inventory = new PlayerStorage(StorageType.CUBE);
		inventory.setOwner(player);
		setField(Player.class, player, "inventory", inventory);
		return player;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
