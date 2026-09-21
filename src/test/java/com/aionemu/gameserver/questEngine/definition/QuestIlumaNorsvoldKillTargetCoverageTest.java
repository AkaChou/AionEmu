package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aion 5.8 Iluma(210100000)/Norsvold(220110000) 击杀任务的"客户端变体族"覆盖门禁。
 * Coverage gate for the Aion 5.8 Iluma/Norsvold kill quests: every variant the Aion 5.8 client counts
 * for a hunting step must be registered by the quest, and each registered family must keep at least one
 * target that the production spawn data actually places in an active map.
 *
 * <p>回归背景：15546《雷欧娜的委托》只登记了世界中不刷新的基础模板（240475/240483/240495/240497），
 * 而客户端对话与计数条目的目标是同族的 T_ 变体（241656/241664/241676/241678 等），
 * 因此 {@code QuestEngine.onKill} 拿不到该任务的 onKill owner，击杀不会下发到任务、进度不更新。
 * Regression background: quest 15546 registered only the base templates that are never spawned, so the
 * spawned {@code T_} variants of the same family never reached the quest owner and the counters stayed 0.
 *
 * <p>契约快照 {@code iluma-norsvold-kill-target-contract.tsv} 由客户端 {@code quest_monster.csv}
 * 的怪物名单经 npc_template 名称解析后生成，见 {@code .agents/summary/quest-15546-kill-progress/}。
 */
class QuestIlumaNorsvoldKillTargetCoverageTest {
	private static final String CONTRACT_RESOURCE = "/quest/iluma-norsvold-kill-target-contract.tsv";
	/** 快照规模：低于该值说明基线被误删或生成脚本漏了任务。 / Guard against silent baseline shrink. */
	private static final int EXPECTED_CONTRACT_ROWS = 45;
	/** 15546/25546：四族各 4 杀的目标集合与实际刷新变体。 */
	private static final Set<Integer> LEONA_VARIANTS = Set.of(
		240475, 240476, 241656, 241657,
		240483, 240484, 241664, 241665,
		240495, 240496, 241676, 241677,
		240497, 240498, 241678, 241679);
	private static final Set<Integer> LEONA_SPAWNED = Set.of(241656, 241657, 241664, 241665, 241676, 241677, 241678, 241679);
	private static final Path STATIC_DATA = Path.of("src/main/resources/aion/data/static_data");
	private static final int FOUR_COUNTER_QUESTS_REQUIRED_KILLS = 16;
	/** 客户端计数上限（{@code Progress(SECTION_n<4)}）：满值后的击杀不得再命中任何路线。 */
	private static final int FOUR_COUNTER_QUEST_CEILING = 4;

	@Test
	void killTargetsMatchTheReviewedClientVariantContract() {
		Map<Integer, Set<Integer>> contract = contractSnapshot();
		assertEquals(EXPECTED_CONTRACT_ROWS, contract.size(),
			"kill-target contract snapshot must keep its reviewed coverage");
		for (Map.Entry<Integer, Set<Integer>> entry : contract.entrySet()) {
			int questId = entry.getKey();
			assertEquals(entry.getValue(), killTargets(load(questId)),
				() -> "quest " + questId + " must register exactly the reviewed Iluma/Norsvold variant set");
		}
	}

	@Test
	void everyReviewedQuestKeepsTargetsThatTheActiveMapSpawns() {
		Set<Integer> spawned = spawnedNpcIdsInActiveMaps();
		for (Map.Entry<Integer, Set<Integer>> entry : contractSnapshot().entrySet()) {
			Set<Integer> live = new LinkedHashSet<>(entry.getValue());
			live.retainAll(spawned);
			assertFalse(live.isEmpty(),
				() -> "quest " + entry.getKey() + " registers no target that the production spawn data places in an active map");
		}
	}

	@Test
	void spawnedVariantsOfLeonaAndArundFavorAreRoutedAndCounted() {
		Set<Integer> spawned = spawnedNpcIdsInActiveMaps();
		Set<Integer> leonaLive = new LinkedHashSet<>(LEONA_VARIANTS);
		leonaLive.retainAll(spawned);
		assertEquals(LEONA_SPAWNED, leonaLive,
			"15546 must count exactly the four T_ families that Iluma spawns");
		for (int questId : List.of(15546, 25546)) {
			CompiledQuestDefinition compiled = load(questId);
			Set<Integer> live = new LinkedHashSet<>(contractSnapshot().get(questId));
			live.retainAll(spawned);
			assertFalse(live.isEmpty(), () -> "quest " + questId + " has no spawned target");
			for (int npcId : live) {
				assertTrue(routesKill(compiled, npcId),
					() -> "quest " + questId + " has no kill route for spawned variant " + npcId);
			}
			assertEquals(Set.of("var1", "var2", "var3", "var4"), QuestKillCounterSimulator.killCounterFields(compiled),
				() -> "quest " + questId + " must keep four independent client counters");
			assertEquals(FOUR_COUNTER_QUESTS_REQUIRED_KILLS, QuestKillCounterSimulator.requiredKills(compiled),
				() -> "quest " + questId + " must complete after four kills per family");
		}
	}

	/** 任务 IR 中所有击杀路线登记的 NPC：KillNpc 与 KillNpcSet 一并展开。 */
	private static Set<Integer> killTargets(CompiledQuestDefinition compiled) {
		Set<Integer> targets = new LinkedHashSet<>();
		for (QuestTransition transition : compiled.definition().transitions()) {
			if (transition.event() instanceof QuestEvent.KillNpc(int npcId)) {
				targets.add(npcId);
			} else if (transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)) {
				targets.addAll(npcIds);
			}
		}
		return targets;
	}

	/** 从全新 START 快照出发，至少有一条击杀转换能匹配该 NPC 击杀事实。 */
	private static boolean routesKill(CompiledQuestDefinition compiled, int npcId) {
		return matchesAnyKillRoute(compiled, zeroCounters(compiled), npcId);
	}

	/**
	 * 计数器已饱和（等于客户端上限）时的额外击杀不得命中任何路线，否则会提交一笔状态未变化的空事务
	 * 并下发一次任务状态更新，客户端把这次重复通知显示成"任务更新"。
	 * An extra kill of a saturated counter must not match any route: a matched-but-identical plan still commits and
	 * pushes a quest-state update, which the client renders as an unsolicited "quest updated" notice.
	 */
	@Test
	void saturatedCountersRejectExtraKillsWithoutMatchingAnyRoute() {
		for (int questId : List.of(15546, 25546)) {
			CompiledQuestDefinition compiled = load(questId);
			for (String field : List.of("var1", "var2", "var3", "var4")) {
				Map<String, Integer> variables = zeroCounters(compiled);
				variables.put(field, FOUR_COUNTER_QUEST_CEILING);
				Set<Integer> targets = counterKillTargets(compiled, field);
				assertEquals(4, targets.size(),
					() -> "quest " + questId + " must keep four variants for " + field);
				for (int npcId : targets) {
					assertFalse(matchesAnyKillRoute(compiled, variables, npcId),
						() -> "quest " + questId + " must not match an extra kill of saturated " + field
							+ " via npc " + npcId);
				}
			}
		}
	}

	/** 所有计数器归零的可提交状态。/ Committable state with every counter at zero. */
	private static Map<String, Integer> zeroCounters(CompiledQuestDefinition compiled) {
		Map<String, Integer> variables = new LinkedHashMap<>();
		for (BitField field : compiled.definition().progressLayout().fields()) {
			variables.put(field.name(), 0);
		}
		return variables;
	}

	/** 由自环击杀路线计数（set/increment）的字段 -> 该族的全部目标 NPC。 */
	private static Set<Integer> counterKillTargets(CompiledQuestDefinition compiled, String field) {
		Set<Integer> targets = new LinkedHashSet<>();
		for (QuestTransition transition : compiled.definition().transitions()) {
			if (!"started".equals(transition.sourceNode()) || !"started".equals(transition.targetNode())) {
				continue;
			}
			boolean touchesField = transition.actions().stream().anyMatch(action ->
				(action instanceof QuestAction.SetVariable set && set.field().equals(field))
					|| (action instanceof QuestAction.IncrementVariable increment && increment.field().equals(field)));
			if (!touchesField) {
				continue;
			}
			if (transition.event() instanceof QuestEvent.KillNpc(int npcId)) {
				targets.add(npcId);
			} else if (transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)) {
				targets.addAll(npcIds);
			}
		}
		return targets;
	}

	/** 给定快照下是否有任意击杀转换能匹配该 NPC 击杀事实。 */
	private static boolean matchesAnyKillRoute(CompiledQuestDefinition compiled, Map<String, Integer> variables,
			int npcId) {
		QuestDefinition definition = compiled.definition();
		QuestSnapshot snapshot = new QuestSnapshot(1, definition.id(), QuestStatus.START,
			definition.progressLayout().pack(variables), Map.of());
		QuestEvent event = new QuestEvent.KillNpc(npcId);
		List<QuestTransition> ordered = new ArrayList<>(definition.transitions());
		ordered.sort(Comparator.comparing(transition -> transition.priority() == null
			? Integer.MAX_VALUE : transition.priority()));
		return ordered.stream()
			.anyMatch(transition -> QuestMutationPlanner.plan(compiled, snapshot, event, transition).isPresent());
	}

	/** 生产刷怪数据中所有落在活跃地图上的 NPC 模板 ID。 */
	private static Set<Integer> spawnedNpcIdsInActiveMaps() {
		Set<Integer> activeMaps = activeMapIds();
		Set<Integer> spawned = new LinkedHashSet<>();
		try (Stream<Path> files = Files.walk(STATIC_DATA.resolve("spawns"))) {
			for (Path file : files.filter(path -> path.toString().endsWith(".xml")).toList()) {
				String text = Files.readString(file, StandardCharsets.UTF_8);
				Matcher map = Pattern.compile("spawn_map map_id=\"(\\d+)\"").matcher(text);
				if (!map.find() || !activeMaps.contains(Integer.parseInt(map.group(1)))) {
					continue;
				}
				Matcher npc = Pattern.compile("npc_id=\"(\\d+)\"").matcher(text);
				while (npc.find()) {
					spawned.add(Integer.parseInt(npc.group(1)));
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return spawned;
	}

	/** world_maps.xml 中未被注释的可用地图；注释掉的 600200000(Lakrum) 等不参与判定。 */
	private static Set<Integer> activeMapIds() {
		try {
			String raw = Files.readString(STATIC_DATA.resolve("world_maps.xml"), StandardCharsets.UTF_8);
			Matcher map = Pattern.compile("<map id=\"(\\d+)\"").matcher(raw.replaceAll("(?s)<!--.*?-->", ""));
			Set<Integer> ids = new LinkedHashSet<>();
			while (map.find()) {
				ids.add(Integer.parseInt(map.group(1)));
			}
			assertFalse(ids.isEmpty(), "world_maps.xml must declare active maps");
			return ids;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Map<Integer, Set<Integer>> contractSnapshot() {
		try (InputStream input = Objects.requireNonNull(
				QuestIlumaNorsvoldKillTargetCoverageTest.class.getResourceAsStream(CONTRACT_RESOURCE), CONTRACT_RESOURCE);
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			Map<Integer, Set<Integer>> rows = new LinkedHashMap<>();
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("quest_id")) {
					continue;
				}
				String[] columns = trimmed.split("\t");
				if (columns.length != 2) {
					throw new AssertionError("malformed contract row: " + trimmed);
				}
				Set<Integer> targets = Arrays.stream(columns[1].trim().split(" "))
					.filter(token -> !token.isEmpty())
					.map(Integer::parseInt)
					.collect(Collectors.toCollection(LinkedHashSet::new));
				rows.put(Integer.parseInt(columns[0].trim()), targets);
			}
			return rows;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
				QuestIlumaNorsvoldKillTargetCoverageTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
