package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.npcFaction.ENpcFactionQuestState;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestPlayerEmotion;
import com.aionemu.gameserver.questEngine.definition.RepeatPolicy;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Real player-effect boundary checks for authoritative interaction targets. */
class PlayerQuestEffectPortTest {
	@Test
	void appliesAndRemovesEffectsThroughTheProductionBoundary() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		List<String> calls = new java.util.ArrayList<>();
		PlayerQuestEffectPort port = new PlayerQuestEffectPort(playerId -> player,
			new PlayerQuestEffectPort.EffectOperations() {
				@Override
				public void apply(Player target, int skillId, int durationMillis) {
					calls.add("apply:" + skillId + ":" + durationMillis + ":" + (target == player));
				}

				@Override
				public void remove(Player target, int effectId) {
					calls.add("remove:" + effectId + ":" + (target == player));
				}
			});
		QuestSnapshot snapshot = new QuestSnapshot(7, 14114, QuestStatus.START, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(14114, QuestStatus.START, 1, List.of(), List.of());

		port.applyEffect(snapshot, plan, 8197, 0);
		port.removeEffect(snapshot, plan, 8197);

		assertEquals(List.of("apply:8197:0:true", "remove:8197:true"), calls);
	}

	@Test
	void playerEmotionFailsClosedWithoutAnAuthoritativeInteractionObject() throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerQuestEffectPort port = new PlayerQuestEffectPort(playerId -> player);
		QuestSnapshot snapshot = new QuestSnapshot(7, 1004, QuestStatus.START, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(1004, QuestStatus.START, 0, List.of(), List.of());

		assertThrows(IllegalStateException.class,
			() -> port.playerEmotion(snapshot, plan, QuestPlayerEmotion.STAND));
	}

	@Test
	void factionLifecycleFailsClosedWhenTheActiveFactionPointsToAnotherQuest() throws Exception {
		int questId = 14114;
		int npcFactionId = 4;
		NpcFaction faction = new ObjenesisStd().newInstance(NpcFaction.class);
		setField(NpcFaction.class, faction, "id", npcFactionId);
		setField(NpcFaction.class, faction, "active", true);
		setField(NpcFaction.class, faction, "state", ENpcFactionQuestState.START);
		setField(NpcFaction.class, faction, "questId", questId + 1);
		RecordingNpcFactions factions = new RecordingNpcFactions(faction);
		Player player = new ObjenesisStd().newInstance(Player.class);
		player.setNpcFactions(factions);
		QuestMetadata metadata = factionMetadata(npcFactionId);
		PlayerQuestEffectPort port = new PlayerQuestEffectPort(playerId -> player, noOpEffects(),
			id -> id == questId ? metadata : null);
		QuestSnapshot snapshot = new QuestSnapshot(7, questId, QuestStatus.REWARD, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(questId, QuestStatus.COMPLETE, 0, List.of(), List.of());

		assertFalse(port.completeNpcFactionQuest(snapshot, plan, npcFactionId));
		assertFalse(factions.completeCalled);
		assertFalse(port.abortNpcFactionQuest(snapshot, plan, npcFactionId));
		assertFalse(factions.abortCalled);
	}

	private static QuestMetadata factionMetadata(int npcFactionId) {
		return new QuestMetadata("Faction quest", 1, 1, 80, Set.of(), "QUEST", RepeatPolicy.once(),
			Set.of(), List.of(), List.of(), List.of(), Set.of(), "", 0, 1, 1,
			false, false, false, 0, null, null, false, Set.of(), npcFactionId,
			"NONE", "NONE", 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}

	private static PlayerQuestEffectPort.EffectOperations noOpEffects() {
		return new PlayerQuestEffectPort.EffectOperations() {
			@Override
			public void apply(Player player, int skillId, int durationMillis) {
			}

			@Override
			public void remove(Player player, int effectId) {
			}
		};
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class RecordingNpcFactions extends NpcFactions {
		private final NpcFaction faction;
		private boolean completeCalled;
		private boolean abortCalled;

		private RecordingNpcFactions(NpcFaction faction) {
			super(null);
			this.faction = faction;
		}

		@Override
		public NpcFaction getNpcFactionById(int id) {
			return id == faction.getId() ? faction : null;
		}

		@Override
		public void completeQuest(int npcFactionId, boolean mentor) {
			completeCalled = true;
		}

		@Override
		public void abortQuest(int npcFactionId) {
			abortCalled = true;
		}
	}
}
