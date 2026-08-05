package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.quest.InventoryItem;
import com.aionemu.gameserver.model.templates.quest.InventoryItems;
import com.aionemu.gameserver.model.templates.quest.QuestCategory;
import com.aionemu.gameserver.model.templates.quest.QuestRepeatCycle;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.List;

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

	@Test
	void timeBasedNpcFactionQuestStillRequiresTheActiveFactionQuest() throws Exception {
		QuestTemplate template = templateWithNpcFaction(true);
		Player player = playerWithEmptyInventory();
		player.setNpcFactions(new MissingNpcFaction());
		PlayerQuestStartEligibilityPort port = new PlayerQuestStartEligibilityPort(playerId -> player,
			questId -> template, ignored -> false, ignored -> true);

		QuestStartEligibility result = port.snapshot(PLAYER_ID, QUEST_ID);

		assertFalse(result.eligible());
		assertEquals("NPC_FACTION_QUEST_NOT_ACTIVE", result.reason());
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

	private static QuestTemplate templateWithNpcFaction(boolean timeBased) throws Exception {
		QuestTemplate template = new QuestTemplate();
		setField(QuestTemplate.class, template, "id", QUEST_ID);
		setField(QuestTemplate.class, template, "minlevelPermitted", 0);
		setField(QuestTemplate.class, template, "category", QuestCategory.TASK);
		setField(QuestTemplate.class, template, "npcFactionId", 4);
		if (timeBased) {
			setField(QuestTemplate.class, template, "repeatCycle", List.of(QuestRepeatCycle.ALL));
		}
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

	private static final class MissingNpcFaction extends NpcFactions {
		private MissingNpcFaction() {
			super(null);
		}

		@Override
		public com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction getNpcFactionById(int id) {
			return null;
		}
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
