package com.aionemu.gameserver.ai2;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.dataholders.RetailAiData.Operation;
import com.aionemu.gameserver.dataholders.RetailAiData.Pattern;
import com.aionemu.gameserver.dataholders.RetailAiData.Rule;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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
			List<String> fissureRoomAis = List.of(
				"IDTransform_TransRoom_01", "IDTransform_TransRoom_02", "IDTransform_TransRoom_03",
				"IDTransform_TransRoom_04");
			for (int i = 0; i < fissureRoomAis.size(); i++) {
				String ai = fissureRoomAis.get(i);
				assertEquals(ai, AI2Engine.selectNpcAi(ai, 245396 + i, null));
			}

			Pattern pattern = new Pattern("complete", Map.of("on_wake_up", List.of(
				new Rule(1, "DIRECT", List.of(), List.of(new Operation("do_nothing", Map.of()))))));
			List<Integer> bossSummonNpcIds = List.of(857869, 857870, 857871, 857872, 857885, 857886, 857887, 857888,
				857889, 857890, 857891, 857892, 857893, 857894, 857895, 857896);
			Map<Integer, RetailAiData.Npc> retailNpcs = new HashMap<>();
			retailNpcs.put(200000, new RetailAiData.Npc(200000, "test", "complete", 100, 0, 0, 360, 0,
				null, RetailAiData.PathfindFailReaction.RETURN_TO_SP, "walk", 150, 50));
			for (int npcId : bossSummonNpcIds) {
				retailNpcs.put(npcId, new RetailAiData.Npc(npcId, "summon-control", "complete", 100, 0, 0, 360, 7,
					null, RetailAiData.PathfindFailReaction.RETURN_TO_SP, "run", 200, 40));
			}
			DataManager.RETAIL_AI_DATA = new RetailAiData(Map.of("complete", pattern),
				retailNpcs,
				Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
				Map.of(), Map.of(), Map.of());
			assertEquals("retail_pattern", AI2Engine.selectNpcAi("general", 200000, null));
			assertEquals("quest_use_item", AI2Engine.selectNpcAi("quest_use_item", 200000, null));
			assertEquals("quest_start_use_item", AI2Engine.selectNpcAi("quest_start_use_item", 700004, null));
			assertEquals("empyrean_blessing", AI2Engine.selectNpcAi("empyrean_blessing", 883959, null));
			for (int npcId : bossSummonNpcIds) {
				assertEquals("IDEternity_01_Boss_Summon", AI2Engine.selectNpcAi("IDEternity_01_Boss_Summon", npcId, null));
			}
			for (String ai : List.of(
				"Mechaturerk", "anikiki", "betrayer_icaronix", "bighorn_wendigo", "blood_fungus_skinwalker",
				"colossal_forest_flavia", "coral_corask", "crimson_crested_slink", "devious_manduri_beacon",
				"dynamic_iluma_monster", "dynamic_norsvold_monster", "forest_of_life_brohum_changeling",
				"frostgullet_kirrin", "frosty_petrahulk", "gatorback_skilex", "giant_razorback_frillneck",
				"hidden_swamp_bufo", "hugehorn_wendigo", "masked_manduri_monkey_king",
				"masquerading_desert_gehkros", "mine_mage", "molting_honey_klaw", "mysterious_moonlight_brax",
				"nightbloom_gargonops_shifter", "plateau_gihla_chameleon", "progo_klaw_chameleon",
				"razor_clawed_forest_cloke", "rejuvinating_wave_wave_tauric", "roughhorn_wendigo",
				"ruthless_wave_tauric", "skulking_forsaken_zaif", "spirit_forest_worg_morpher",
				"thickhorn_wendigo", "valley_torr_crumbler", "venerable_sea_giant", "warrior_monument",
				"whiptail_metamorph", "young_roundshell_spiner", "IDEternity_01_Boss_Summon")) {
				assertEquals(ai, AI2Engine.selectNpcAi(ai, 200000, null));
			}
		} finally {
			DataManager.RETAIL_AI_DATA = previous;
		}
	}
}
