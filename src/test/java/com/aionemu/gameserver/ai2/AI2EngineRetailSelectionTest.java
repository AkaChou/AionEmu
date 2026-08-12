package com.aionemu.gameserver.ai2;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.dataholders.RetailAiData.Operation;
import com.aionemu.gameserver.dataholders.RetailAiData.Pattern;
import com.aionemu.gameserver.dataholders.RetailAiData.Rule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
class AI2EngineRetailSelectionTest {

	@Test
	void selectsCompleteRetailPatternsButPreservesScriptedActionItemProtocols() {
		var previous = DataManager.RETAIL_AI_DATA;
		try {
			DataManager.RETAIL_AI_DATA = null;
			assertEquals("general", AI2Engine.selectNpcAi("general", 200000, null));

			Pattern pattern = new Pattern("complete", Map.of("on_wake_up", List.of(
				new Rule(1, "DIRECT", List.of(), List.of(new Operation("do_nothing", Map.of()))))));
			DataManager.RETAIL_AI_DATA = new RetailAiData(Map.of("complete", pattern),
				Map.of(200000, new RetailAiData.Npc(200000, "test", "complete", 100, 0, 0, 360, 0,
					null, RetailAiData.PathfindFailReaction.RETURN_TO_SP, "walk", 150, 50)),
				Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
				Map.of(), Map.of(), Map.of());
			assertEquals("retail_pattern", AI2Engine.selectNpcAi("general", 200000, null));
			assertEquals("quest_use_item", AI2Engine.selectNpcAi("quest_use_item", 200000, null));
			assertEquals("quest_start_use_item", AI2Engine.selectNpcAi("quest_start_use_item", 700004, null));
			assertEquals("empyrean_blessing", AI2Engine.selectNpcAi("empyrean_blessing", 883959, null));
		} finally {
			DataManager.RETAIL_AI_DATA = previous;
		}
	}
}
