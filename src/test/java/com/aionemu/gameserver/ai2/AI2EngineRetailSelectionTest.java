package com.aionemu.gameserver.ai2;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.dataholders.RetailAiData.Operation;
import com.aionemu.gameserver.dataholders.RetailAiData.Pattern;
import com.aionemu.gameserver.dataholders.RetailAiData.Rule;
import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai.DummyAI2;
import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai.RetailPatternAI2;
import com.aionemu.gameserver.instance.handlers.scripts.danuarReliquary.DanuarReliquaryInstance;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
class AI2EngineRetailSelectionTest {

	@Test
	void selectsAnyCompleteRetailPatternAndKeepsFallbackOtherwise() {
		var previous = DataManager.RETAIL_AI_DATA;
		try {
			DataManager.RETAIL_AI_DATA = null;
			assertEquals("general", AI2Engine.selectNpcAi("general", 200000, null));

			Pattern pattern = new Pattern("complete", Map.of("on_wake_up", List.of(
				new Rule(1, "DIRECT", List.of(), List.of(new Operation("do_nothing", Map.of()))))));
			DataManager.RETAIL_AI_DATA = new RetailAiData(Map.of("complete", pattern),
				Map.of(200000, new RetailAiData.Npc(200000, "test", "complete", 0, 0, 360, 0,
					null, RetailAiData.PathfindFailReaction.RETURN_TO_SP, "walk", 150, 50)),
				Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
				Map.of(), Map.of(), Map.of());
			assertEquals("retail_pattern", AI2Engine.selectNpcAi("general", 200000, null));
		} finally {
			DataManager.RETAIL_AI_DATA = previous;
		}
	}

	@Test
	void unsupportedReasonReportsFirstStructuralGapOrNull() {
		// 缺失 Pattern 必须返回原因，而非 null。
		assertEquals("missing pattern", RetailPatternAI2.unsupportedReason(null));

		Pattern unsupportedEvent = new Pattern("bad_event", Map.of("on_nonexistent_event", List.of(
			new Rule(1, "DIRECT", List.of(), List.of(new Operation("do_nothing", Map.of()))))));
		assertEquals("unsupported event on_nonexistent_event",
			RetailPatternAI2.unsupportedReason(unsupportedEvent));

		Pattern unsupportedAction = new Pattern("bad_action", Map.of("on_wake_up", List.of(
			new Rule(1, "DIRECT", List.of(), List.of(new Operation("definitely_not_an_action", Map.of()))))));
		assertEquals("unsupported action definitely_not_an_action in on_wake_up",
			RetailPatternAI2.unsupportedReason(unsupportedAction));

		Pattern multipleGaps = new Pattern("multiple_gaps", Map.of(
			"on_killed_by_user", List.of(new Rule(1, "DIRECT", List.of(),
				List.of(new Operation("give_money", Map.of())))),
			"on_attacked", List.of(new Rule(1, "DIRECT", List.of(),
				List.of(new Operation("give_abysspoint", Map.of()))))));
		assertEquals("unsupported action give_abysspoint in on_attacked",
			RetailPatternAI2.unsupportedReason(multipleGaps));
	}

	@Test
	void unsupportedReasonIdentifiesMissingNpcSkillSlot() {
		var previous = DataManager.RETAIL_AI_DATA;
		try {
			Pattern pattern = new Pattern("missing_skill", Map.of("on_battle_timer", List.of(
				new Rule(1, "PLANNED", List.of(), List.of(new Operation("use_skill", Map.of(
					"target", "OBJI_SELF", "skill", "SKILLI_INDEX_0", "skill_level", "0")))))));
			DataManager.RETAIL_AI_DATA = new RetailAiData(Map.of("missing_skill", pattern),
				Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
				Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

			assertEquals("missing NPC skill SKILLI_INDEX_0", RetailPatternAI2.unsupportedReason(pattern, null));
		} finally {
			DataManager.RETAIL_AI_DATA = previous;
		}
	}

	@Test
	void selectNpcAiKeepsFallbackAndReportsGapForUnsupportedPattern() {
		var previous = DataManager.RETAIL_AI_DATA;
		try {
			Pattern unsupported = new Pattern("bad_action", Map.of("on_wake_up", List.of(
				new Rule(1, "DIRECT", List.of(), List.of(new Operation("definitely_not_an_action", Map.of()))))));
			DataManager.RETAIL_AI_DATA = new RetailAiData(Map.of("bad_action", unsupported),
				Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
				Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
			// 不支持的 Pattern 必须回退到 fallback，且不抛异常（缺口报告走去重 warn）。
			assertEquals("general", AI2Engine.selectNpcAi("general", 200000, null));
		} finally {
			DataManager.RETAIL_AI_DATA = previous;
		}
	}

	@Test
	void keepsHandlerOwnedDanuarActorsOnTheirScriptAi() {
		var previous = DataManager.RETAIL_AI_DATA;
		try {
			Pattern pattern = new Pattern("complete", Map.of("on_message", List.of(
				new Rule(1, "DIRECT", List.of(), List.of(new Operation("do_nothing", Map.of()))))));
			DataManager.RETAIL_AI_DATA = new RetailAiData(Map.of("complete", pattern), Map.of(
				231304, new RetailAiData.Npc(231304, "boss", "complete", 0, 0, 360, 0,
					null, RetailAiData.PathfindFailReaction.RETURN_TO_SP, "walk", 150, 50),
				284383, new RetailAiData.Npc(284383, "clone", "complete", 0, 0, 360, 0,
					null, RetailAiData.PathfindFailReaction.RETURN_TO_SP, "walk", 150, 50)),
				Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
				Map.of(), Map.of(), Map.of(), Map.of());
			TestWorldMapInstance instance = new ObjenesisStd().newInstance(TestWorldMapInstance.class);
			instance.setInstanceHandler(new DanuarReliquaryInstance());

			assertEquals("cursed_queen_modor", AI2Engine.selectNpcAi("cursed_queen_modor", 231304,
				testNpc(231304, 301110000, instance)));
			assertEquals("retail_pattern", AI2Engine.selectNpcAi("aggressive", 284383,
				testNpc(284383, 301110000, instance)));
		} finally {
			DataManager.RETAIL_AI_DATA = previous;
		}
	}

	@Test
	void replacesRemovedRetailFallbackWithRegisteredGenericAi() {
		var previous = DataManager.RETAIL_AI_DATA;
		try {
			DataManager.RETAIL_AI_DATA = null;
			AI2Engine engine = new AI2Engine();
			engine.registerAI(GeneralNpcAI2.class);
			engine.registerAI(AggressiveNpcAI2.class);
			engine.registerAI(DummyAI2.class);

			assertEquals("general", engine.selectRegisteredNpcAi("hyperion", null));
		} finally {
			DataManager.RETAIL_AI_DATA = previous;
		}
	}

	private static TestNpc testNpc(int npcId, int worldId, WorldMapInstance instance) {
		TestNpc npc = new ObjenesisStd().newInstance(TestNpc.class);
		npc.npcId = npcId;
		npc.worldId = worldId;
		npc.template = new NpcTemplate();
		npc.position = new WorldPosition(worldId) {
			@Override
				public WorldMapInstance getWorldMapInstanceOrNull() {
					return instance;
				}
		};
		return npc;
	}

	private static final class TestNpc extends Npc {
		private int npcId;
		private int worldId;
		private NpcTemplate template;
		private WorldPosition position;

		private TestNpc() {
			super(0, null, null, null);
		}

		@Override public int getNpcId() { return npcId; }
		@Override public int getWorldId() { return worldId; }
		@Override public NpcTemplate getObjectTemplate() { return template; }
		@Override public WorldPosition getPosition() { return position; }
	}

	private static final class TestWorldMapInstance extends WorldMapInstance {
		private TestWorldMapInstance() { super(null, 0); }
		@Override public MapRegion getRegion(float x, float y, float z) { return null; }
		@Override protected MapRegion createMapRegion(int regionId) { return null; }
		@Override protected void initMapRegions() { }
		@Override public boolean isPersonal() { return false; }
		@Override public int getOwnerId() { return 0; }
	}
}
