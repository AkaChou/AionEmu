package com.aionemu.gameserver.model.gameobjects.player.npcFaction;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifest;

import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 阵营日常归属门禁：生产 XML 必须声明 {@code npc-faction-id}，且真的能进入该阵营的日常候选池。
 * <p>背景：{@link NpcFactions#sendDailyQuest()} 的候选池按 {@code metadata.npcFactionId()} 过滤；
 * 缺少声明的任务永远不会被推送，而 {@code PlayerQuestStartEligibilityPort} 又会因此跳过阵营校验。
 * 曾出现 218 个任务（含 3505x Alabaster Order 日常）整体缺声明的情况。
 * <p>Gate: faction dailies must declare their faction owner, and must actually land in that faction's
 * daily candidate pool.
 */
class QuestNpcFactionRetailGateTest {
	private static final String CONTRACT_RESOURCE = "/quest/quest-npc-faction-retail-contract.tsv";
	private static final String ROTATION_RESOURCE =
		"/aion/data/static_data/npc_factions/npc_factions_quest.xml";
	/** 合同快照规模：防止基线被误删或生成脚本漏项。 / Guard against silent baseline shrink. */
	private static final int EXPECTED_CONTRACT_ROWS = 287;

	/** 契约快照行：评审势力归属 + 原版星期位掩码（mon..sun 七位）。 / One reviewed row. */
	private record ContractRow(int factionId, String mask) {
	}

	private static QuestCatalog catalog() {
		return QuestDefinitionCatalogManifest.compile(
			Path.of("src/main/resources/aion/data/static_data/quest/definitions"));
	}

	@Test
	void factionDailiesDeclareExactlyTheirReviewedFaction() {
		QuestCatalog catalog = catalog();
		Map<Integer, ContractRow> contract = readings();
		assertEquals(EXPECTED_CONTRACT_ROWS, contract.size(),
			"faction contract snapshot must keep its reviewed coverage");
		int dormant = 0;
		for (Map.Entry<Integer, ContractRow> entry : contract.entrySet()) {
			int questId = entry.getKey();
			// P8 重锚（§10.3-#25）：可达行——native 车道行走 laneOf → factionId，XML 保留行走 typed
			// 元数据；两者都必须命中评审基线。既无路由也无移植定义的休眠行（until-ported，沿用
			// quest-prerequisite 契约的既有模式）必须至少持有原版 quest.xml 的阵营绑定，待覆盖后
			// 由第一分支自动转为强制。
			// P8 re-anchor: reachable rows declare through the grant lane or typed metadata and must
			// match the reviewed snapshot; dormant rows (no lane route, no ported definition) must at
			// least carry the retail quest.xml binding and auto-tighten once covered.
			com.aionemu.gameserver.questEngine.tablelane.NativeSystemGrantLane lane =
				com.aionemu.gameserver.questEngine.tablelane.NativeSystemGrantLanes.laneOf(questId);
			int declared;
			if (lane != null) {
				declared = lane.factionId(questId);
			} else {
				Optional<CompiledQuestDefinition> compiled = catalog.findExecutable(questId);
				if (compiled.isEmpty()) {
					declared = dormantRetailBinding(questId);
					dormant++;
				} else {
					declared = compiled.get().definition().metadata().npcFactionId();
				}
			}
			assertEquals(entry.getValue().factionId(), declared,
				"quest " + questId + " must declare its reviewed faction owner");
		}
		assertTrue(dormant > 0, "dormant until-ported rows must stay exercised");
		// 反向：生产目录里任何声明了阵营的任务都必须在评审基线上，禁止静默新增未评审归属。
		Set<Integer> undeclared = new TreeSet<>();
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			int factionId = compiled.definition().metadata().npcFactionId();
			if (factionId != 0 && !contract.containsKey(compiled.id())) {
				undeclared.add(compiled.id());
			}
		}
		assertEquals(Set.of(), undeclared,
			"quests declaring a faction owner must be part of the reviewed contract snapshot");
	}

	/** 休眠行的兜底证明：原版 quest.xml 仍绑定评审势力（覆盖后自动收紧）。 / Dormant-row proof. */
	private static int dormantRetailBinding(int questId) {
		String name = com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable.instance()
			.find(questId)
			.map(row -> row.text("npcfaction_name"))
			.orElse(null);
		int binding = com.aionemu.gameserver.questEngine.tablelane.NativeNpcFactionNames
			.idOf(name);
		assertTrue(binding > 0, () -> "dormant faction quest " + questId
			+ " has no retail quest.xml faction binding to keep the review anchored");
		return binding;
	}

	@Test
	void everyContractQuestLandsInItsFactionDailyPool() {
		QuestCatalog catalog = catalog();
		Map<Integer, ContractRow> contract = readings();
		Map<Integer, Set<Integer>> expectedByFaction = new TreeMap<>();
		Map<Integer, Set<Integer>> dormantByFaction = new TreeMap<>();
		for (Map.Entry<Integer, ContractRow> entry : contract.entrySet()) {
			int questId = entry.getKey();
			Set<Integer> target = poolReachable(questId, entry.getValue().factionId(), catalog)
				? expectedByFaction.computeIfAbsent(entry.getValue().factionId(),
					ignored -> new LinkedHashSet<>())
				: dormantByFaction.computeIfAbsent(entry.getValue().factionId(),
					ignored -> new LinkedHashSet<>());
			target.add(questId);
		}
		for (Map.Entry<Integer, Set<Integer>> entry : expectedByFaction.entrySet()) {
			// P8 重锚：池 = typed 候选 ∪ 发放车道候选（与 NpcFactions.sendDailyQuest 的实发组合一致；
			// 车道候选剔除仍带 typed 元数据的行，资格/星期位谓词按本门恒真口径放行——星期位真值
			// 由掩码保真门单独锁定）。休眠行不入池，一旦被路由/移植会立即因池超出评审集而变红。
			// P8 re-anchor: pool = typed candidates ∪ grant-lane candidates (the live composition);
			// weekday truth is pinned by the mask-fidelity gate. Dormant rows stay out of the pool
			// and turn red the moment they become routable without a review update.
			Set<Integer> pool = new TreeSet<>(NpcFactions.canonicalDailyQuestCandidates(
				catalog, entry.getKey(), id -> true, id -> true, id -> true));
			for (int questId : com.aionemu.gameserver.questEngine.tablelane.NativeSystemGrantLanes
					.factionRotationCandidates(entry.getKey())) {
				if (catalog.findMetadata(questId).isEmpty()) {
					pool.add(questId);
				}
			}
			assertEquals(new TreeSet<>(entry.getValue()), pool,
				"faction " + entry.getKey() + " daily pool must contain exactly its reviewed owners");
		}
		assertTrue(dormantByFaction.values().stream().mapToInt(Set::size).sum() > 0,
			"dormant rows must stay tracked per faction");
	}

	/**
	 * 与实发组合逐源对齐的可达判定：typed 元数据声明该势力，或车道已路由的 {@code _faction_} 行
	 * （装载但不可路由的行按休眠处理，until-ported）。 / Pool-source-exact reachability: typed
	 * metadata declaring the faction, or a routed {@code _faction_} lane row (loaded-but-unroutable
	 * rows count as dormant until-ported).
	 */
	private static boolean poolReachable(int questId, int factionId, QuestCatalog catalog) {
		var metadata = catalog.findMetadata(questId);
		if (metadata.isPresent()) {
			return metadata.get().npcFactionId() == factionId;
		}
		var lane = com.aionemu.gameserver.questEngine.tablelane.NativeSystemGrantLanes.laneOf(questId);
		return lane != null && lane.routes(questId)
			&& lane.grantKind(questId) == com.aionemu.gameserver.questEngine.retail.RetailGrantKind.FACTION
			&& lane.factionId(questId) == factionId;
	}

	/**
	 * 轮换表保真：每条契约行必须在轮换表有行，势力与星期位掩码逐位等于评审快照——
	 * 掩码全 0 是原版本征不轮换（P0c-3 判例），按快照冻结，禁止本地"修复"。
	 * <p>Rotation fidelity: every contract row must exist with the reviewed faction and the exact
	 * retail weekday bitmask; all-zero masks are retail-intrinsic and frozen, never "fixed" locally.
	 */
	@Test
	void rotationTableMirrorsTheReviewedMasks() {
		Map<Integer, ContractRow> contract = readings();
		Map<Integer, String> rotation = rotationRows();
		int liveMasks = 0;
		for (Map.Entry<Integer, ContractRow> entry : contract.entrySet()) {
			String row = rotation.get(entry.getKey());
			assertTrue(row != null, () -> "quest " + entry.getKey()
				+ " is reviewed but missing from the rotation table");
			String mask = row.substring(0, 7);
			int factionId = Integer.parseInt(row.substring(7));
			assertEquals(entry.getValue().factionId(), factionId,
				"quest " + entry.getKey() + " rotation row must match its declared faction");
			assertEquals(entry.getValue().mask(), mask,
				"quest " + entry.getKey() + " weekday mask must mirror the retail rotation table");
			if (!mask.equals("0000000")) {
				liveMasks++;
			}
		}
		assertTrue(liveMasks > 0, "at least one faction daily must stay rotatable");
	}

	/** 解析轮换表：quest_id -> [7 位星期掩码 mon..sun][势力 id]。 / Rows as mask + faction id. */
	private static Map<Integer, String> rotationRows() {
		Map<Integer, String> rows = new LinkedHashMap<>();
		try (InputStream input = Objects.requireNonNull(
			QuestNpcFactionRetailGateTest.class.getResourceAsStream(ROTATION_RESOURCE), ROTATION_RESOURCE)) {
			var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
			var nodes = document.getElementsByTagName("npc_faction_quest");
			for (int index = 0; index < nodes.getLength(); index++) {
				var element = (org.w3c.dom.Element) nodes.item(index);
				StringBuilder mask = new StringBuilder();
				for (String day : new String[] {"mon", "tue", "wed", "thu", "fri", "sat", "sun"}) {
					mask.append(element.getAttribute(day));
				}
				rows.put(Integer.parseInt(element.getAttribute("quest_id")),
					mask + element.getAttribute("faction_id"));
			}
		} catch (Exception e) {
			throw new AssertionError("unable to read " + ROTATION_RESOURCE, e);
		}
		return rows;
	}

	private static Map<Integer, ContractRow> readings() {
		Map<Integer, ContractRow> rows = new LinkedHashMap<>();
		try (InputStream input = Objects.requireNonNull(
			QuestNpcFactionRetailGateTest.class.getResourceAsStream(CONTRACT_RESOURCE), CONTRACT_RESOURCE);
			var reader = new java.io.BufferedReader(new java.io.InputStreamReader(input,
				java.nio.charset.StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#") || line.startsWith("quest_id")) {
					continue;
				}
				String[] cells = line.split("\t");
				rows.put(Integer.parseInt(cells[0]),
					new ContractRow(Integer.parseInt(cells[1]), cells[2]));
			}
		} catch (Exception e) {
			throw new AssertionError("unable to read " + CONTRACT_RESOURCE, e);
		}
		assertTrue(rows.size() > 0, "faction contract snapshot must not be empty");
		return rows;
	}
}
