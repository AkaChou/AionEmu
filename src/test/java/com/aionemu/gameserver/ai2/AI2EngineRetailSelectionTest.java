package com.aionemu.gameserver.ai2;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.dataholders.RetailAiData.Operation;
import com.aionemu.gameserver.dataholders.RetailAiData.Pattern;
import com.aionemu.gameserver.dataholders.RetailAiData.Rule;
import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai.DummyAI2;
import com.aionemu.gameserver.ai.GeneralNpcAI2;
import org.junit.jupiter.api.Test;

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
}
