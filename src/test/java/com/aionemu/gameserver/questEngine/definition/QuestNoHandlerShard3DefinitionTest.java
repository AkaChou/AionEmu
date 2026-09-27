package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Verdict for the no-handler shard-3 quests 29634 / 30208 / 30565 / 30760. */
class QuestNoHandlerShard3DefinitionTest {

	@Test
	void directoryCompilesAllFourRestoredOwners() {
		/* 四行已由真端表驱动（XML 退役），改从生产视图断言可编译。 */
		for (int questId : new int[] {29634, 30208, 30565, 30760}) {
			assertTrue(ProductionQuestDefinitions.definition(questId) != null,
				() -> "quest " + questId + " must compile from the production view");
		}
	}

	@Test
	void scaredSkurvsCarriesRetailHuntChainAndSharedTenKillCount() throws Exception {
		CompiledQuestDefinition compiled = definition("29634.xml");
		QuestMetadata meta = compiled.definition().metadata();
		/* P5-1：真端元数据无英文标题（仓内客户端解包为韩文），回落 "Q"+id；字符串 id 经
		   quest_name_string_ids.tsv 完好（Q29634 -> 1800422），英文名属 L10N 数据边界。
		   Retail metadata carries no English title (Korean client unpack), so the name falls
		   back to Q+id; the string id itself stays intact via the name-id registry. */
		assertEquals("Q29634", meta.name());
		assertEquals(1800422, meta.displayNameId());
		assertEquals(45, meta.minLevel());
		assertEquals(Set.of("ASMODIANS"), meta.permittedRaces());
		assertEquals("IMPORTANT", meta.category());
		/* 真端元数据未声明 finished-29633 前置（finished_quest_cond 缺席；29633 的 con_quest 只指
		   链的下一环），旧 XML 的前置来自链推断——按真端为空锁定，客户端链门控负责展示。
		   Retail metadata declares no finished-29633 condition (only the chain's forward con_quest);
		   the legacy prerequisite was chain-inferred — lock the retail-empty shape. */
		assertEquals(List.of(), meta.startConditions());
		assertEquals(List.of(new QuestReward("EXP", 0, 6242224L),
			new QuestReward("SELECTABLE_ITEM", 110101862, 1L),
			new QuestReward("SELECTABLE_ITEM", 110301854, 1L),
			new QuestReward("SELECTABLE_ITEM", 110301855, 1L),
			new QuestReward("SELECTABLE_ITEM", 110551182, 1L),
			new QuestReward("SELECTABLE_ITEM", 110601652, 1L)), meta.rewards());

		/* P5-1：真端击杀网格（a0..a10，var0 = 共享 10 杀计数，四个 retail 怪各一条逐 id 击杀边）；
		   旧双变量形（var0 行号 + var1 计数 + priority 0/1 边）随退役一并退出。
		   Grid since P5-1: a0..a10 with a shared 10-kill var0 and one per-id kill edge per mob. */
		Set<Integer> npcIds = compiled.definition().transitions().stream()
			.filter(t -> "a0".equals(t.sourceNode()) && "a1".equals(t.targetNode()))
			.map(QuestTransition::event).filter(e -> e instanceof QuestEvent.KillNpc)
			.map(e -> ((QuestEvent.KillNpc) e).npcId())
			.collect(Collectors.toSet());
		assertEquals(Set.of(214371, 214372, 214440, 214441), npcIds);
		for (QuestTransition edge : compiled.definition().transitions().stream()
				.filter(t -> "a0".equals(t.sourceNode()) && "a1".equals(t.targetNode())).toList()) {
			assertEquals(List.of(), edge.conditions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				edge.afterCommit());
		}
		assertEquals(10, varsOf(compiled, "reward").get("var0"));
		/* P0c-8b 判例：真端确认段 = 8..23 全段 16 条；五个可选项路径各带一个 selectable 发放，
		   其余确认 id 只发固定奖励。 */
		List<List<QuestAction>> completions = completionActions(compiled);
		assertEquals(16, completions.size());
		int selectablePaths = 0;
		for (List<QuestAction> path : completions) {
			assertEquals(new QuestAction.GrantReward("EXP", 0, 6242224, QuestRewardAmountMode.QUEST_BASE), path.get(0));
			assertEquals(new QuestAction.CompleteQuest(0), path.getLast());
			if (path.size() == 3) {
				selectablePaths++;
			}
		}
		assertEquals(5, selectablePaths, "five selectable rewards must keep their grant paths");
	}

	@Test
	void truthHurtsGivesWorkItemAtAcceptAndDeletesDrakanAfterSetReward() throws Exception {
		CompiledQuestDefinition compiled = definition("30208.xml");
		QuestMetadata meta = compiled.definition().metadata();
		assertEquals("[Group] The Truth Hurts", meta.name());
		assertEquals(1114308, meta.displayNameId());
		assertEquals(53, meta.minLevel());
		assertEquals(Set.of("ELYOS"), meta.permittedRaces());
		assertEquals("QUEST", meta.category());
		assertTrue(meta.cannotShare());
		assertEquals(List.of(new QuestStartCondition("finished", 30207, 0)), meta.startConditions());
		assertEquals(List.of(new QuestReward("EXP", 0, 6517414L),
			new QuestReward("ITEM", 186000098, 1L)), meta.rewards());

		// Accepting gives the summon ceremony work item quest_30208a (182209610).
		// started 节点内另有 1008/31 停留过渡，接取路径以 source="unaccepted" 区分。
		List<QuestTransition> acceptRoutes = compiled.definition().transitions().stream()
			.filter(t -> "unaccepted".equals(t.sourceNode())
				&& t.event() instanceof QuestEvent.TalkToNpc
				&& ((QuestEvent.TalkToNpc) t.event()).npcId() == 798941
				&& t.targetNode().equals("started")).toList();
		assertEquals(2, acceptRoutes.size());
		for (QuestTransition accept : acceptRoutes) {
			assertTrue(accept.actions().contains(new QuestAction.GiveItem(182209610, 1)));
		}

		// Faithful respondent Utra (799506) SET_REWARD deletes itself and moves to reward.
		QuestTransition ceremony = compiled.definition().transitions().stream()
			.filter(t -> t.event().equals(new QuestEvent.TalkToNpc(799506, 10255))).findFirst().orElseThrow();
		assertEquals("reward", ceremony.targetNode());
		assertTrue(ceremony.afterCommit().contains(new AfterCommitAction.DeleteInteractionNpc(true)));

		// Fixed reward completes on npc 798941 through the 8..23 dialog range.
		List<List<QuestAction>> completions = completionActions(compiled);
		assertEquals(16, completions.size());
		for (List<QuestAction> path : completions) {
			assertEquals(List.of(
				new QuestAction.GrantReward("EXP", 0, 6517414, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 186000098, 1),
				new QuestAction.CompleteQuest(0)), path);
		}
	}

	@Test
	void reunitingTheReiansIsAPureDialogTwoStepChain() throws Exception {
		CompiledQuestDefinition compiled = definition("30565.xml");
		QuestMetadata meta = compiled.definition().metadata();
		/* 同 29634：真端元数据无英文标题，回落 Q+id；字符串 id 完好。 */
		assertEquals("Q30565", meta.name());
		assertEquals(1801111, meta.displayNameId());
		assertEquals(65, meta.minLevel());
		assertEquals(Set.of("ASMODIANS"), meta.permittedRaces());
		assertEquals("IMPORTANT", meta.category());
		assertTrue(meta.prerequisites().isEmpty());
		assertEquals(List.of(new QuestReward("GOLD", 0, 299160L),
			new QuestReward("EXP", 0, 4432902L),
			new QuestReward("ITEM", 186000469, 210L)), meta.rewards());

		/* P5-1：真端 30565 是单步报告对话（Ekios 805156 接取；Garnon 804879 QUEST_SELECT 显示
		   客户端完成页 10002、1009 进领奖）——旧 s1/s2 两步阶梯随退役退出。
		   Retail 30565 is a single report dialog: Garnon's QUEST_SELECT shows the client page and
		   1009 enters reward; the legacy s1/s2 ladder retired with the XML. */
		assertTrue(hasDialog(compiled, 805156, 1002, "unaccepted", "started"));
		assertTrue(hasDialog(compiled, 805156, 20000, "unaccepted", "started"));
		assertTrue(hasDialog(compiled, 804879, 31, "started", "started"));
		QuestTransition report = compiled.definition().transitions().stream()
			.filter(t -> "started".equals(t.sourceNode()) && "reward".equals(t.targetNode())
				&& t.event().equals(new QuestEvent.TalkToNpc(804879, 1009)))
			.findFirst().orElseThrow();
		assertEquals(List.of(), report.conditions());

		// Fixed gold/exp/medal reward completes through the 8..23 dialog range.
		List<List<QuestAction>> completions = completionActions(compiled);
		assertEquals(16, completions.size());
		for (List<QuestAction> path : completions) {
			assertEquals(List.of(
				new QuestAction.GrantReward("GOLD", 0, 299160, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("EXP", 0, 4432902, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.GrantReward("ITEM", 186000469, 210),
				new QuestAction.CompleteQuest(0)), path);
		}
	}

	@Test
	void petrifiedHeroSpawnsHakelanAfterUsingTheStatue() throws Exception {
		CompiledQuestDefinition compiled = definition("30760.xml");
		QuestMetadata meta = compiled.definition().metadata();
		assertEquals("[Group] Petrified Hero of the Asmodians", meta.name());
		assertEquals(1137131, meta.displayNameId());
		assertEquals(57, meta.minLevel());
		assertEquals(Set.of("ASMODIANS"), meta.permittedRaces());
		assertEquals("QUEST", meta.category());
		assertTrue(meta.cannotShare());
		assertEquals(List.of(new QuestStartCondition("finished", 30759, 0)), meta.startConditions());
		assertEquals(List.of(new QuestReward("EXP", 0, 7086913L),
			new QuestReward("SELECTABLE_ITEM", 164000066, 26L),
			new QuestReward("SELECTABLE_ITEM", 164000121, 26L),
			new QuestReward("SELECTABLE_ITEM", 164000070, 26L)), meta.rewards());

		// Using the Asmodian Hero's Statue (701499, USE_OBJECT dialog -1) spawns
		// Hakelan (800458) at the player and moves to reward.
		QuestTransition statue = compiled.definition().transitions().stream()
			.filter(t -> t.event().equals(new QuestEvent.TalkToNpc(701499, -1))).findFirst().orElseThrow();
		assertEquals("reward", statue.targetNode());
		AfterCommitAction.SpawnNpc spawn = statue.afterCommit().stream()
			.filter(a -> a instanceof AfterCommitAction.SpawnNpc)
			.map(a -> (AfterCommitAction.SpawnNpc) a).findFirst().orElseThrow();
		assertEquals("hakelan", spawn.slot());
		assertEquals(800458, spawn.templateId());
		assertInstanceOf(QuestSpawnLocation.PlayerPosition.class, spawn.location());

		// Three selectable scroll rewards complete on Hank (804871): dialog 8/9/10 各一条完成路线。
		List<List<QuestAction>> completions = completionActions(compiled);
		assertEquals(3, completions.size());
		assertTrue(completions.stream().anyMatch(p -> p.contains(new QuestAction.GrantReward("ITEM", 164000066, 26))));
		assertTrue(completions.stream().anyMatch(p -> p.contains(new QuestAction.GrantReward("ITEM", 164000121, 26))));
		assertTrue(completions.stream().anyMatch(p -> p.contains(new QuestAction.GrantReward("ITEM", 164000070, 26))));
	}

	private static boolean hasDialog(CompiledQuestDefinition compiled, int npcId, int dialogId,
		String source, String target) {
		return compiled.definition().transitions().stream()
			.anyMatch(t -> t.event().equals(new QuestEvent.TalkToNpc(npcId, dialogId))
				&& t.sourceNode().equals(source) && t.targetNode().equals(target));
	}

	private static Map<String, Integer> varsOf(CompiledQuestDefinition compiled, String label) {
		return compiled.definition().nodes().stream().filter(n -> n.label().equals(label))
			.findFirst().orElseThrow().projection().variables();
	}

	private static List<List<QuestAction>> completionActions(CompiledQuestDefinition compiled) {
		return compiled.definition().transitions().stream()
			.filter(t -> t.targetNode().equals("complete"))
			.map(QuestTransition::actions).toList();
	}

	private CompiledQuestDefinition definition(String file) {
		// Shard3 行已由真端表驱动（退役），改从生产视图取定义；file 形如 "29634.xml"。
		// The shard-3 rows are retail-driven since retirement; load via the production view.
		return ProductionQuestDefinitions.definition(Integer.parseInt(file.substring(0, file.length() - 4)));
	}

	private InputStream resource(String path) {
		InputStream input = getClass().getResourceAsStream(path);
		if (input == null) {
			throw new IllegalStateException("missing resource " + path);
		}
		return input;
	}
}
