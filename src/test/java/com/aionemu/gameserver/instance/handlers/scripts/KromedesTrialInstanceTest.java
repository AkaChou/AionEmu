package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.Race;

class KromedesTrialInstanceTest {

	private static final Path SOURCE = Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/KromedesTrialInstance.java");
	private static final Path RETAIL_PATTERNS = Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_lf4_minho.xml");
	private static final Path AI_AREAS = Path.of(
			"src/main/resources/aion/definitions/compact/ai/ai-areas.xml");
	private static final Path AI_STRINGS = Path.of(
			"src/main/resources/aion/definitions/compact/ai/ai-strings.xml");
	private static final Path NPC_AI = Path.of(
			"src/main/resources/aion/definitions/compact/ai/npc-ai.xml");
	private static final Path NPC_SKILLS = Path.of(
			"src/main/resources/aion/definitions/compact/skills/npc-skills.xml");
	private static final Path SPAWNS = Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300230000_Kromede's_Trial.xml");

	@Test
	void usesTheRetailTransformationForEachRace() {
		assertEquals(19220, KromedesTrialInstance.transformationFor(Race.ELYOS));
		assertEquals(19270, KromedesTrialInstance.transformationFor(Race.ASMODIANS));
	}

	@Test
	void finalBossSelectionUsesRetailAngryJudgeMessageChain() throws IOException {
		String source = Files.readString(SOURCE);
		assertFalse(source.contains("onInstanceCreate(WorldMapInstance instance)"));
		assertFalse(source.contains("spawn(217005"));
		assertFalse(source.contains("spawn(217006"));
		assertFalse(source.contains("Rnd."));
		assertFalse(source.contains("GameThreadPoolServices"));

		String spawns = Files.readString(SPAWNS);
		assertTrue(spawns.contains("<spawn npc_id=\"217006\">"));
		assertTrue(spawns.contains("x=\"668.567871\" y=\"774.373657\" z=\"216.88036\" h=\"60\""));

		String patterns = Files.readString(RETAIL_PATTERNS);
		int angryPattern = patterns.indexOf("<name>Cromede_Named_Angry</name>");
		int nextPattern = patterns.indexOf("<npc_ai_pattern>", angryPattern + 1);
		assertTrue(angryPattern >= 0);
		assertTrue(nextPattern > angryPattern);
		String body = patterns.substring(angryPattern, nextPattern);
		assertTrue(body.contains("<message_type>6404</message_type>"));
		assertTrue(body.contains("<npc_nameid>IDCromede_2up_Named_Judge_38_An</npc_nameid>"));
		assertTrue(body.contains("<despawn_self/>"));
	}

	@Test
	void onDieDoesNotSpawnPrivateClassTreasure() throws IOException {
		String source = Files.readString(SOURCE);
		String onDie = methodBody(source, "public void onDie(Npc npc)");

		assertFalse(source.contains("spawnClassTreasure"));
		for (String foreignChest : new String[] { "211861", "212333", "212335", "212338" }) {
			assertFalse(source.contains(foreignChest));
		}
	}

	@Test
	void woundedNpcSpawnsUseRetailPatterns() throws IOException {
		String source = Files.readString(SOURCE);
		for (String privateSpawn : new String[] { "spawn(217001", "spawn(217003", "spawn(217004" }) {
			assertFalse(source.contains(privateSpawn));
		}

		String patterns = Files.readString(RETAIL_PATTERNS);
		for (String retailSpawn : new String[] { "IDCromede_2up_Trigger_Wound_Wife_38_An",
			"IDCromede_2up_Trigger_Wound_AssiJudge_38_An", "IDCromede_2up_Trigger_Wound_Torture_38_An" }) {
			assertTrue(patterns.contains("<npc_nameid>" + retailSpawn + "</npc_nameid>"));
		}
	}

	@Test
	void kaligaTreasuryHintUsesRetailSensoryScript() throws IOException {
		String source = Files.readString(SOURCE);
		String onDie = methodBody(source, "public void onDie(Npc npc)");
		assertFalse(onDie.contains("case 216999"));
		assertFalse(source.contains("announceKaligaTreasury"));
		assertFalse(source.contains("removeSilverBladeRotan"));
		assertFalse(source.contains("1111370"));

		String patterns = Files.readString(RETAIL_PATTERNS);
		assertTrue(patterns.contains("<name>IDCromede_SensoryArea_BossDoor</name>"));
		assertTrue(patterns.contains("<on_user_enter_sensory_area>"));
		assertTrue(patterns.contains("<send_system_msg_by_user_indicator><user>USERI_EVENT_MAKER</user>"
			+ "<string_id>STR_QUEST_SAY_IDCromede_004</string_id></send_system_msg_by_user_indicator>"));

		String areas = Files.readString(AI_AREAS);
		assertTrue(areas.contains("<area world_id=\"300230000\" world_name=\"idcromede\" "
			+ "name=\"IDCromede_SensoryArea_BossDoor\" bottom=\"213.045425\" top=\"227.045425\">"));
		for (String point : new String[] { "561.241028\" y=\"756.814941", "561.440308\" y=\"782.309692",
			"588.809021\" y=\"782.268372", "589.051575\" y=\"756.332642" }) {
			assertTrue(areas.contains("x=\"" + point + "\""));
		}
		assertTrue(Files.readString(AI_STRINGS)
			.contains("<string name=\"STR_QUEST_SAY_IDCromede_004\" id=\"1111370\"/>"));
		assertTrue(Files.readString(SPAWNS)
			.contains("x=\"566.783936\" y=\"762.207581\" z=\"219.712234\""));
	}

	@Test
	void questOwnsRetailMoviesAndFinalBossCompletion() throws IOException {
		String source = Files.readString(SOURCE);
		String onDie = methodBody(source, "public void onDie(Npc npc)");
		String onEnterZone = methodBody(source, "public void onEnterZone(Player player, ZoneInstance zone)");

		assertFalse(onDie.contains("217005"));
		assertFalse(onDie.contains("217006"));
		assertFalse(source.contains("sendMovie(player, 455)"));
		assertFalse(onEnterZone.contains("KALIGA_DUNGEONS_300230000"));
		assertFalse(onEnterZone.contains("sendMovie(player, 454)"));
	}

	@Test
	void relicsUseRetailDeathAndCleansePatternChain() throws IOException {
		String source = Files.readString(SOURCE);
		assertFalse(source.contains("handleUseItemFinish"));
		assertFalse(source.contains("19247"));
		assertFalse(source.contains("19248"));

		String npcAi = Files.readString(NPC_AI);
		assertTrue(npcAi.contains("<npc id=\"282093\" name=\"IDCromede_Relic_Blue\" ai=\"Cromede_Relic2\""));
		assertTrue(npcAi.contains("<npc id=\"282095\" name=\"IDCromede_Relic_Red\" ai=\"Cromede_Relic1\""));
		assertTrue(npcAi.contains("<npc id=\"282084\" name=\"IDCromede_Invisible_NPC2\" ai=\"Cromede_Relic1_Noshow\""));
		assertTrue(npcAi.contains("<npc id=\"282085\" name=\"IDCromede_Invisible_NPC3\" ai=\"Cromede_Relic2_Noshow\""));

		String patterns = Files.readString(RETAIL_PATTERNS);
		assertTrue(patterns.contains("<npc_nameid>IDCromede_Invisible_NPC2</npc_nameid>"));
		assertTrue(patterns.contains("<npc_nameid>IDCromede_Invisible_NPC3</npc_nameid>"));
		assertTrue(patterns.contains("<name>Cromede_Relic1_Noshow</name><event_handlers><on_wake_up>"));
		assertTrue(patterns.contains("<name>Cromede_Relic2_Noshow</name><event_handlers><on_wake_up>"));

		String skills = Files.readString(NPC_SKILLS);
		assertTrue(skills.contains("<skill name=\"Cromede_CurePhysical_Nr\" id=\"19273\""));
		assertTrue(skills.contains("<skill name=\"Cromede_CureMental_Nr\" id=\"19274\""));
	}

	@Test
	void coverageLocksRemainingPlayerStateBridges() throws IOException {
		String source = Files.readString(SOURCE);
		assertTrue(source.contains("applyEffectDirectly(transformation, player, player"));
		assertTrue(source.contains("sendMovie(player, 453)"));
		assertTrue(source.contains("instances/kromedeTrial.xhtml"));
			assertFalse(source.contains("handleUseItemFinish"));
			assertTrue(source.contains("case 700835:"));
			assertTrue(source.contains("sendMovie(player, 462)"));
			for (String itemId : new String[] { "185000101", "185000102", "185000109" }) {
				assertTrue(source.contains("decreaseByItemId(" + itemId), itemId);
			}
			for (String itemId : new String[] { "164000140", "164000141", "164000142", "164000143" }) {
				assertFalse(source.contains("decreaseByItemId(" + itemId), itemId);
			}
			assertTrue(methodBody(source, "public void onLeaveInstance(Player player)").contains("removeItems(player)"));
			assertFalse(methodBody(source, "public void onPlayerLogOut(Player player)").contains("removeItems(player)"));
			String items = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/items/item/item_misc_templates.xml"));
			for (int itemId = 164000140; itemId <= 164000143; itemId++) {
				assertTrue(itemTemplateBlock(items, itemId).contains("ownership_world=\"300230000\""),
					Integer.toString(itemId));
			}
			for (int itemId : new int[] { 185000101, 185000102, 185000109 }) {
				assertFalse(itemTemplateBlock(items, itemId).contains("ownership_world"), Integer.toString(itemId));
			}

		String ownership = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
			.filter(line -> line.contains("id=\"300230000\"")).findFirst().orElseThrow();
		assertTrue(ownership.contains("retail static spawns, Pattern, sensory area and quests own combat, wounded NPCs, boss selection, treasury hint and final movies/completion"));
			assertTrue(ownership.contains("Pattern owns the 282093/282095 relic death and 282084/282085 cleanse chain; handler owns entry transformation/movie/UI, stone-door delete, manor movie/hint, normal-leave ownerless-key cleanup and logout/leave effect cleanup"));
	}

	private static String methodBody(String source, String signature) {
		int signatureStart = source.indexOf(signature);
		assertTrue(signatureStart >= 0, signature + " must exist");
		int bodyStart = source.indexOf('{', signatureStart);
		assertTrue(bodyStart >= 0, signature + " must have a method body");

		int depth = 0;
		for (int i = bodyStart; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				depth--;
				if (depth == 0) {
					return source.substring(bodyStart + 1, i);
				}
			}
		}
		throw new AssertionError(signature + " method body was not closed");
	}

	private static String itemTemplateBlock(String items, int itemId) {
		int start = items.indexOf("<item_template id=\"" + itemId + "\"");
		int openingEnd = items.indexOf('>', start);
		return items.charAt(openingEnd - 1) == '/' ? items.substring(start, openingEnd)
			: items.substring(start, items.indexOf("</item_template>", openingEnd));
	}
}
