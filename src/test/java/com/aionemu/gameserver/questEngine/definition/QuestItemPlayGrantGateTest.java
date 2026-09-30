package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真端驱动定义的"演出道具可得"门禁：ItemPlay 步消费的道具必须在同一份定义里被授予（GiveItem）或掉落，
 * 否则链会在演出步死亡（玩家拿不到道具却要"使用"它）。
 * <p>
 * 排查口径：遍历保留清单里所有 RETAIL_TABLE 行，逐任务收集 {@code metadata().drops()} 与所有
 * {@code GiveItem} 动作得到"可得集合"，再对每条 {@link QuestEvent.ItemPlay} 断言其 itemId 在集合内。
 * 真端凭证模型是接取即发（planner 负责完成/放弃回收），采集物由掉落覆盖；两者之外的道具来源
 * （例如上一环任务的奖励）需要在 {@link #REGISTERED_SOURCES} 里逐条登记。
 * <p>
 * Gate: every item consumed by an {@link QuestEvent.ItemPlay} step must be granted ({@code GiveItem}) or
 * dropped somewhere in the same retail-driven definition, otherwise the chain dead-ends on the play step.
 * Sources outside the retail credential model must be registered per quest in {@link #REGISTERED_SOURCES}.
 */
class QuestItemPlayGrantGateTest {

	/** 保留清单：RETAIL_TABLE 行 = 真端驱动的任务。 / The retention manifest: RETAIL_TABLE rows are retail-driven. */
	private static final String RETENTION = "/aion/data/static_data/quest/retail/retail-xml-retention.tsv";

	/** 登记的外部来源（quest_id -> 说明）；当前为空。 / Registered outside sources (quest_id -> reason); empty today. */
	private static final java.util.Map<Integer, String> REGISTERED_SOURCES = java.util.Map.of();

	@Test
	void everyItemPlayStepConsumesAnObtainableItem() throws Exception {
		List<Integer> questIds = retailDrivenQuestIds();
		List<String> problems = new ArrayList<>();
		int playEdges = 0;
		for (int questId : questIds) {
			QuestDefinition definition;
			try {
				definition = ProductionQuestDefinitions.definition(questId).definition();
			} catch (RuntimeException e) {
				problems.add(questId + ": 生产视图不可读 " + e.getMessage());
				continue;
			}
			Set<Integer> obtainable = new HashSet<>();
			definition.metadata().drops().forEach(drop -> obtainable.add(drop.itemId()));
			for (QuestTransition transition : definition.transitions()) {
				for (QuestAction action : transition.actions()) {
					if (action instanceof QuestAction.GiveItem give) {
						obtainable.add(give.itemId());
					}
				}
			}
			for (QuestTransition transition : definition.transitions()) {
				if (!(transition.event() instanceof QuestEvent.ItemPlay play)) {
					continue;
				}
				playEdges++;
				if (!obtainable.contains(play.itemId()) && !REGISTERED_SOURCES.containsKey(questId)) {
					problems.add(questId + ": itemplay item " + play.itemId() + " 在定义内无授予也无掉落（"
						+ transition.sourceNode() + " -> " + transition.targetNode() + "，work items="
						+ definition.metadata().questWorkItems() + "）");
				}
			}
		}
		assertTrue(playEdges > 0, "演出步扫描必须真的看到条目（口径失效）");
		assertTrue(problems.isEmpty(), () -> "ItemPlay 道具不可得：" + problems.stream().limit(20).toList());
	}

	private static List<Integer> retailDrivenQuestIds() throws Exception {
		List<Integer> ids = new ArrayList<>();
		try (var in = QuestItemPlayGrantGateTest.class.getResourceAsStream(RETENTION)) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank() || line.startsWith("#")) {
					continue;
				}
				String[] cells = line.split("\t");
				if (cells.length > 1 && cells[1].equals("RETAIL_TABLE")) {
					ids.add(Integer.parseInt(cells[0]));
				}
			}
		}
		return ids;
	}
}
