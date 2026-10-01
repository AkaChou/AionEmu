package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.BitField;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 临时探针：转储指定 DataDriven 任务的 IR（用完即删）。 / Scratch probe, delete after use. */
class ZzIrDumpProbeTest {

	private static RetailDataDrivenTable table;
	private static RetailQuestXmlTable retailTable;
	private static RetailNpcNameIndex npcIndex;
	private static RetailItemNameIndex itemIndex;
	private static RetailClientSummaryRows clientSummaryRows;
	private static RetailClientRewardNpcs clientRewardNpcs;
	private static RetailClientHandinPages clientHandinPages;
	private static RetailClientHandinNpcSets clientHandinNpcSets;
	private static RetailQuestUseItemNpcs interactionObjects;
	private static RetailClientKillTargets clientKillTargets;
	private static RetailClientHuntProgressRows clientHuntProgressRows;
	private static RetailEnterAreaZoneResolution enterAreaZoneResolution;
	private static RetailClientDialogExits clientDialogExits;
	private static RetailQuestAreaIndex clientQuestAreas;
	private static Map<String, Integer> randomRewards;
	private static Map<Integer, Integer> nameIds;

	@BeforeAll
	static void loadFixtures() throws Exception {
		try (InputStream input = open("/aion/data/static_data/quest/retail/data_driven_quest.xml")) {
			table = RetailDataDrivenTable.load(input);
		}
		try (InputStream input = open("/aion/data/static_data/quest/retail/quest.xml")) {
			retailTable = RetailQuestXmlTable.load(input);
		}
		clientSummaryRows = RetailClientSummaryRows.defaultSummaryRows();
		clientHandinPages = RetailClientHandinPages.defaultHandinPages();
		clientKillTargets = RetailClientKillTargets.defaultKillTargets();
		clientHuntProgressRows = RetailClientHuntProgressRows.defaultHuntProgressRows();
		enterAreaZoneResolution = RetailEnterAreaZoneResolution.defaultZoneResolution();
		clientDialogExits = RetailClientDialogExits.defaultExits();
		clientRewardNpcs = RetailClientRewardNpcs.defaultRewardNpcs();
		clientHandinNpcSets = RetailClientHandinNpcSets.defaultSets();
		try (InputStream input = open("/aion/definitions/compact/ai/ai-areas.xml")) {
			clientQuestAreas = RetailQuestAreaIndex.load(input);
		}
		npcIndex = RetailNpcNameIndex.build(openAll("/aion/data/static_data/npcs/", NPC_TEMPLATES()),
			RetailQuestAiNameGroupsFixture.streams());
		interactionObjects = RetailQuestUseItemNpcs.fromIds(npcIndex.questUseItemNpcIds());
		itemIndex = RetailItemNameIndex.build(openAll("/aion/data/static_data/items/item/",
			listXmlNames("/aion/data/static_data/items/item/")));
		randomRewards = randomRewardIds();
		nameIds = nameIds();
	}

	@Test
	void dumpRequestedQuests() throws Exception {
		String requested = System.getProperty("zz.dumpIds", "14252,15324,24252");
		StringBuilder out = new StringBuilder();
		for (String token : requested.split(",")) {
			int questId = Integer.parseInt(token.trim());
			out.append("===== quest ").append(questId).append(" =====\n");
			var entry = table.find(questId).orElse(null);
			if (entry == null) {
				out.append("no DataDriven row\n");
				continue;
			}
			out.append("huntStages=").append(entry.huntStages().size())
				.append(" allHunt=").append(entry.allHunt())
				.append(" allPvp=").append(entry.allPvp())
				.append(" acquire=").append(entry.acquireCategory())
				.append(" reward=").append(entry.rewardNpc()).append('\n');
			var metadata = RetailQuestMetadataCompiler.compile(retailTable.find(questId).orElseThrow(),
				npcIndex, itemIndex, randomRewards, nameIds);
			var outcome = RetailDataDrivenDefinitionCompiler.compile(entry, itemIndex, npcIndex, metadata,
				clientRewardNpcs, clientHandinNpcSets, clientQuestAreas, clientDialogExits, clientSummaryRows,
				clientHandinPages, interactionObjects, clientKillTargets, clientHuntProgressRows,
				enterAreaZoneResolution);
			out.append("accepted=").append(outcome.accepted())
				.append(" code=").append(outcome.rejectionCode())
				.append(" detail=").append(outcome.detail()).append('\n');
			if (outcome.accepted()) {
				dump(out, outcome.definition().definition());
			}
		}
		Path path = Path.of(System.getProperty("zz.out", "target/zz-ir-dump.txt"));
		Files.createDirectories(path.toAbsolutePath().getParent());
		Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
		System.out.println(out);
	}

	private static void dump(StringBuilder out, QuestDefinition definition) {
		out.append("-- layout --\n");
		for (BitField field : definition.progressLayout().fields()) {
			out.append("  ").append(field.name()).append(" offset=").append(field.offset())
				.append(" width=").append(field.width()).append(" min=").append(field.minValue())
				.append(" max=").append(field.maxValue()).append('\n');
		}
		out.append("-- nodes --\n");
		definition.nodes().forEach(node -> out.append("  ").append(node).append('\n'));
		out.append("-- transitions --\n");
		definition.transitions().forEach(transition -> out.append("  ").append(transition).append('\n'));
	}

	private static List<String> NPC_TEMPLATES() {
		return List.of("npc_template_200000_216188.xml", "npc_template_216189_235748.xml",
			"npc_template_235749_247606.xml", "npc_template_247607_270057.xml", "npc_template_270058_286320.xml",
			"npc_template_286321_800030.xml", "npc_template_800031_834289.xml", "npc_template_834290_885645.xml");
	}

	private static InputStream open(String resource) {
		InputStream input = ZzIrDumpProbeTest.class.getResourceAsStream(resource);
		if (input == null) {
			throw new IllegalStateException("missing resource " + resource);
		}
		return input;
	}

	private static List<InputStream> openAll(String dir, List<String> names) {
		List<InputStream> streams = new ArrayList<>();
		for (String name : names) {
			streams.add(open(dir + name));
		}
		return streams;
	}

	private static List<String> listXmlNames(String dir) throws Exception {
		var url = ZzIrDumpProbeTest.class.getResource(dir);
		java.io.File[] files = new java.io.File(url.toURI()).listFiles((directory, name) -> name.endsWith(".xml"));
		return java.util.Arrays.stream(files).map(java.io.File::getName).sorted().toList();
	}

	private static Map<Integer, Integer> nameIds() throws Exception {
		Map<Integer, Integer> ids = new HashMap<>();
		try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(
			open("/aion/data/static_data/quest/retail/quest_name_string_ids.tsv"), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t");
				ids.put(Integer.parseInt(parts[0].substring("STR_QUEST_NAME_Q".length())), Integer.parseInt(parts[1]));
			}
		}
		return ids;
	}

	private static Map<String, Integer> randomRewardIds() throws Exception {
		Map<String, Integer> ids = new HashMap<>();
		var document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
			.parse(open("/aion/data/static_data/quest/legacy/quest_random_rewards.xml"));
		var nodes = document.getDocumentElement().getElementsByTagName("quest_random_reward");
		for (int index = 0; index < nodes.getLength(); index++) {
			var element = (org.w3c.dom.Element) nodes.item(index);
			ids.put(element.getElementsByTagName("name").item(0).getTextContent().trim(),
				Integer.parseInt(element.getElementsByTagName("id").item(0).getTextContent().trim()));
		}
		return ids;
	}
}
