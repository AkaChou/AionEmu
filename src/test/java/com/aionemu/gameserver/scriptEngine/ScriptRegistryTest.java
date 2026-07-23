package com.aionemu.gameserver.scriptEngine;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScriptRegistryTest {

	@Test
	void registerAndLookupScriptNpcByNpcId() {
		ScriptRegistry registry = new ScriptRegistry();
		assertNull(registry.getScriptNpc(200000));

		ScriptNpc npc = new TestScriptNpc(200000);
		registry.registerScriptNpc(npc);

		assertSame(npc, registry.getScriptNpc(200000));
		assertEquals(1, registry.scriptNpcCount());
	}

	@Test
	void registerAndLookupScriptQuestByQuestId() {
		ScriptRegistry registry = new ScriptRegistry();
		assertNull(registry.getScriptQuest(1000));

		ScriptQuest quest = new TestScriptQuest(1000);
		registry.registerScriptQuest(quest);

		assertSame(quest, registry.getScriptQuest(1000));
		assertEquals(1, registry.scriptQuestCount());
	}

	@Test
	void duplicateRegistrationOverwrites() {
		ScriptRegistry registry = new ScriptRegistry();
		ScriptNpc first = new TestScriptNpc(200000);
		ScriptNpc second = new TestScriptNpc(200000);
		registry.registerScriptNpc(first);
		registry.registerScriptNpc(second);

		assertSame(second, registry.getScriptNpc(200000));
		assertEquals(1, registry.scriptNpcCount());
	}

	@Test
	void nullRegistrationsAreIgnored() {
		ScriptRegistry registry = new ScriptRegistry();
		registry.registerScriptNpc(null);
		registry.registerScriptQuest(null);

		assertEquals(0, registry.scriptNpcCount());
		assertEquals(0, registry.scriptQuestCount());
	}

	@Test
	void clearRemovesAllBindings() {
		ScriptRegistry registry = new ScriptRegistry();
		registry.registerScriptNpc(new TestScriptNpc(200000));
		registry.registerScriptQuest(new TestScriptQuest(1000));
		assertNotNull(registry.getScriptNpc(200000));

		registry.clear();

		assertNull(registry.getScriptNpc(200000));
		assertNull(registry.getScriptQuest(1000));
		assertEquals(0, registry.scriptNpcCount());
		assertEquals(0, registry.scriptQuestCount());
	}

	private static final class TestScriptNpc implements ScriptNpc {
		private final int npcId;

		TestScriptNpc(int npcId) {
			this.npcId = npcId;
		}

		@Override
		public int getNpcId() {
			return npcId;
		}

		@Override
		public void onDialogStart(Player player) {
		}

		@Override
		public void onSeePlayer(Player player) {
		}

		@Override
		public void onDied(Creature killer) {
		}
	}

	private static final class TestScriptQuest implements ScriptQuest {
		private final QuestHandler handler;

		TestScriptQuest(int questId) {
			this.handler = new QuestHandler(questId) {
				@Override
				public void register() {
				}
			};
		}

		@Override
		public QuestHandler getHandler() {
			return handler;
		}
	}
}
