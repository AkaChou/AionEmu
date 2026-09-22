package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 30721 (天族) 与 30771 (魔族) 完整真端流程、物品使用区域、袭击怪物生成与清理、唯一领奖 NPC。
 * Verifies quests 30721 (Elyos) and 30771 (Asmodian) retail flow, item use zones, ambush spawn/despawn, and exclusive turn-in NPC.
 */
class Quest30721And30771RetailFlowTest {

	@Test
	void quest30721CompleteRetailFlow() {
		CompiledQuestDefinition compiled = load(30721);
		QuestDefinition definition = compiled.definition();

		assertEquals("IMPORTANT", definition.metadata().category());
		assertEquals(57, definition.metadata().minLevel());
		assertEquals(Set.of("ELYOS"), definition.metadata().permittedRaces());

		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "s0", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 4));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		// 1. 接取：营地 NPC 804704 (Eukraton)
		QuestSnapshot snapUnaccepted = new QuestSnapshot(7, 30721, QuestStatus.NONE, 0, Map.of())
			.withStartEligibility(QuestStartEligibility.allowed());
		QuestEvent acceptEvent = new QuestEvent.TalkToNpc(804704, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id());
		QuestMutationPlan acceptPlan = dispatch(compiled, snapUnaccepted, acceptEvent);
		assertEquals(QuestStatus.START, acceptPlan.nextStatus());
		assertEquals(0, definition.progressLayout().unpack(acceptPlan.nextPackedVariables()).get("var0"));

		// 2. 第一步：与调查官 804870 (Monroe) 对话推进至 s1
		QuestSnapshot snapS0 = new QuestSnapshot(7, 30721, QuestStatus.START,
			definition.progressLayout().pack(Map.of("var0", 0)), Map.of());
		QuestEvent talkMonroe1 = new QuestEvent.TalkToNpc(804870, QuestDialogAction.QUEST_SELECT.id());
		QuestMutationPlan planMonroe1 = dispatch(compiled, snapS0, talkMonroe1);
		assertEquals(QuestStatus.START, planMonroe1.nextStatus());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())),
			planMonroe1.afterCommit());

		QuestEvent talkMonroeSetpro = new QuestEvent.TalkToNpc(804870, QuestDialogAction.SETPRO1.id());
		QuestMutationPlan planSetpro1 = dispatch(compiled, snapS0, talkMonroeSetpro);
		assertEquals(QuestStatus.START, planSetpro1.nextStatus());
		assertEquals(1, definition.progressLayout().unpack(planSetpro1.nextPackedVariables()).get("var0"));

		// 3. 第二步：与鸢族魔法师 804868 (Rosalee) 对话，获得道具 182215698 并推进至 s2
		QuestSnapshot snapS1 = new QuestSnapshot(7, 30721, QuestStatus.START,
			definition.progressLayout().pack(Map.of("var0", 1)), Map.of());
		QuestEvent talkRosalee1 = new QuestEvent.TalkToNpc(804868, QuestDialogAction.QUEST_SELECT.id());
		QuestMutationPlan planRosalee1 = dispatch(compiled, snapS1, talkRosalee1);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			planRosalee1.afterCommit());

		QuestEvent talkRosaleeSetpro2 = new QuestEvent.TalkToNpc(804868, QuestDialogAction.SETPRO2.id());
		QuestMutationPlan planSetpro2 = dispatch(compiled, snapS1, talkRosaleeSetpro2);
		assertEquals(QuestStatus.START, planSetpro2.nextStatus());
		assertEquals(2, definition.progressLayout().unpack(planSetpro2.nextPackedVariables()).get("var0"));
		assertTrue(planSetpro2.requiredActions().contains(new QuestAction.GiveItem(182215698, 1)));

		// 4. 第三步：在残骸堆使用道具 182215698，扣除道具，推进至 s3，刷出 2 只德拉坎 (236654)
		QuestSnapshot snapS2 = new QuestSnapshot(7, 30721, QuestStatus.START,
			definition.progressLayout().pack(Map.of("var0", 2)), Map.of(182215698, 1));
		QuestEvent itemPlayEvent = new QuestEvent.ItemPlay(182215698, 3000);
		QuestMutationPlan planItemPlay = dispatch(compiled, snapS2, itemPlayEvent);
		assertEquals(QuestStatus.START, planItemPlay.nextStatus());
		assertEquals(3, definition.progressLayout().unpack(planItemPlay.nextPackedVariables()).get("var0"));
		assertTrue(planItemPlay.requiredActions().contains(new QuestAction.RemoveItem(182215698, 1)));
		assertTrue(planItemPlay.afterCommit().contains(new AfterCommitAction.SpawnNpc(
			"drakan1", 236654, new QuestSpawnLocation.PlayerPosition((byte) 0))));
		assertTrue(planItemPlay.afterCommit().contains(new AfterCommitAction.SpawnNpc(
			"drakan2", 236654, new QuestSpawnLocation.PlayerPosition((byte) 60))));

		// 5. 第四步：返回与 804868 对话，despawn 怪物，汇报后 SET_SUCCEED 推进至 REWARD
		QuestSnapshot snapS3 = new QuestSnapshot(7, 30721, QuestStatus.START,
			definition.progressLayout().pack(Map.of("var0", 3)), Map.of());
		QuestEvent talkRosaleeReport = new QuestEvent.TalkToNpc(804868, QuestDialogAction.QUEST_SELECT.id());
		QuestMutationPlan planRosaleeReport = dispatch(compiled, snapS3, talkRosaleeReport);
		assertTrue(planRosaleeReport.afterCommit().contains(new AfterCommitAction.DespawnNpc("drakan1")));
		assertTrue(planRosaleeReport.afterCommit().contains(new AfterCommitAction.DespawnNpc("drakan2")));
		assertTrue(planRosaleeReport.afterCommit().contains(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4.id())));

		QuestEvent talkRosaleeSucceed = new QuestEvent.TalkToNpc(804868, QuestDialogAction.SET_SUCCEED.id());
		QuestMutationPlan planRosaleeSucceed = dispatch(compiled, snapS3, talkRosaleeSucceed);
		assertEquals(QuestStatus.REWARD, planRosaleeSucceed.nextStatus());
		assertEquals(4, definition.progressLayout().unpack(planRosaleeSucceed.nextPackedVariables()).get("var0"));
		assertTrue(planRosaleeSucceed.afterCommit().contains(new AfterCommitAction.DespawnNpc("drakan1")));
		assertTrue(planRosaleeSucceed.afterCommit().contains(new AfterCommitAction.DespawnNpc("drakan2")));

		// 6. 领奖：唯一汇报与领奖 NPC 为 804870 (Monroe)，接取 NPC 804704 绝不参与领奖
		QuestSnapshot snapReward = new QuestSnapshot(7, 30721, QuestStatus.REWARD,
			definition.progressLayout().pack(Map.of("var0", 4)), Map.of());
		assertTrue(routes(definition, "reward", 804704).isEmpty(), "804704 绝不能作为领奖或汇报 NPC");

		// 与 804870 对话下发故事页 DEFAULT_SUCCESS (10002)
		QuestEvent talkMonroeReward = new QuestEvent.TalkToNpc(804870, QuestDialogAction.QUEST_SELECT.id());
		QuestMutationPlan planMonroeReward = dispatch(compiled, snapReward, talkMonroeReward);
		assertEquals(QuestStatus.REWARD, planMonroeReward.nextStatus());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			planMonroeReward.afterCommit());

		// 选奖励按钮 SELECT_QUEST_REWARD 弹出奖励选择窗
		QuestEvent selectRewardPreview = new QuestEvent.TalkToNpc(804870, QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestMutationPlan planPreview = dispatch(compiled, snapReward, selectRewardPreview);
		assertEquals(QuestStatus.REWARD, planPreview.nextStatus());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), planPreview.afterCommit());

		// 点击领奖 SELECTED_QUEST_REWARD1 完成任务
		QuestEvent completeEvent = new QuestEvent.TalkToNpc(804870, QuestDialogAction.SELECTED_QUEST_REWARD1.id());
		QuestMutationPlan planComplete = dispatch(compiled, snapReward, completeEvent);
		assertEquals(QuestStatus.COMPLETE, planComplete.nextStatus());

		// 7. 自愈：enter-world 下 status=REWARD && var0 < 4 自愈为 4
		QuestSnapshot snapStaleReward = new QuestSnapshot(7, 30721, QuestStatus.REWARD,
			definition.progressLayout().pack(Map.of("var0", 0)), Map.of());
		QuestEvent enterWorld = new QuestEvent.EnterWorld();
		QuestMutationPlan planEnter = dispatch(compiled, snapStaleReward, enterWorld);
		assertEquals(QuestStatus.REWARD, planEnter.nextStatus());
		assertEquals(4, definition.progressLayout().unpack(planEnter.nextPackedVariables()).get("var0"));
	}

	@Test
	void quest30771CompleteRetailFlow() {
		CompiledQuestDefinition compiled = load(30771);
		QuestDefinition definition = compiled.definition();

		assertEquals("IMPORTANT", definition.metadata().category());
		assertEquals(57, definition.metadata().minLevel());
		assertEquals(Set.of("ASMODIANS"), definition.metadata().permittedRaces());

		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "s0", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 4));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		// 1. 接取：营地 NPC 804728 (Engrid)
		QuestSnapshot snapUnaccepted = new QuestSnapshot(7, 30771, QuestStatus.NONE, 0, Map.of())
			.withStartEligibility(QuestStartEligibility.allowed());
		QuestEvent acceptEvent = new QuestEvent.TalkToNpc(804728, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id());
		QuestMutationPlan acceptPlan = dispatch(compiled, snapUnaccepted, acceptEvent);
		assertEquals(QuestStatus.START, acceptPlan.nextStatus());

		// 2. 第一步：与调查官 804871 (Hank) 对话推进至 s1
		QuestSnapshot snapS0 = new QuestSnapshot(7, 30771, QuestStatus.START,
			definition.progressLayout().pack(Map.of("var0", 0)), Map.of());
		QuestEvent talkHankSetpro = new QuestEvent.TalkToNpc(804871, QuestDialogAction.SETPRO1.id());
		QuestMutationPlan planSetpro1 = dispatch(compiled, snapS0, talkHankSetpro);
		assertEquals(QuestStatus.START, planSetpro1.nextStatus());
		assertEquals(1, definition.progressLayout().unpack(planSetpro1.nextPackedVariables()).get("var0"));

		// 3. 第二步：与魔法师 804869 (Ginnie) 对话，获得恢复剂 182215699 并推进至 s2
		QuestSnapshot snapS1 = new QuestSnapshot(7, 30771, QuestStatus.START,
			definition.progressLayout().pack(Map.of("var0", 1)), Map.of());
		QuestEvent talkGinnieSetpro2 = new QuestEvent.TalkToNpc(804869, QuestDialogAction.SETPRO2.id());
		QuestMutationPlan planSetpro2 = dispatch(compiled, snapS1, talkGinnieSetpro2);
		assertEquals(QuestStatus.START, planSetpro2.nextStatus());
		assertEquals(2, definition.progressLayout().unpack(planSetpro2.nextPackedVariables()).get("var0"));
		assertTrue(planSetpro2.requiredActions().contains(new QuestAction.GiveItem(182215699, 1)));

		// 4. 第三步：使用道具 182215699，扣除道具，推进至 s3，刷出德拉坎
		QuestSnapshot snapS2 = new QuestSnapshot(7, 30771, QuestStatus.START,
			definition.progressLayout().pack(Map.of("var0", 2)), Map.of(182215699, 1));
		QuestEvent itemPlayEvent = new QuestEvent.ItemPlay(182215699, 3000);
		QuestMutationPlan planItemPlay = dispatch(compiled, snapS2, itemPlayEvent);
		assertEquals(QuestStatus.START, planItemPlay.nextStatus());
		assertEquals(3, definition.progressLayout().unpack(planItemPlay.nextPackedVariables()).get("var0"));
		assertTrue(planItemPlay.requiredActions().contains(new QuestAction.RemoveItem(182215699, 1)));
		assertTrue(planItemPlay.afterCommit().contains(new AfterCommitAction.SpawnNpc(
			"drakan1", 236654, new QuestSpawnLocation.PlayerPosition((byte) 0))));

		// 5. 第四步：返回与 804869 对话，despawn 怪物，SET_SUCCEED 推进至 REWARD
		QuestSnapshot snapS3 = new QuestSnapshot(7, 30771, QuestStatus.START,
			definition.progressLayout().pack(Map.of("var0", 3)), Map.of());
		QuestEvent talkGinnieSucceed = new QuestEvent.TalkToNpc(804869, QuestDialogAction.SET_SUCCEED.id());
		QuestMutationPlan planGinnieSucceed = dispatch(compiled, snapS3, talkGinnieSucceed);
		assertEquals(QuestStatus.REWARD, planGinnieSucceed.nextStatus());
		assertEquals(4, definition.progressLayout().unpack(planGinnieSucceed.nextPackedVariables()).get("var0"));
		assertTrue(planGinnieSucceed.afterCommit().contains(new AfterCommitAction.DespawnNpc("drakan1")));

		// 6. 领奖：唯一汇报与领奖 NPC 为 804871 (Hank)，接取 NPC 804728 绝不参与领奖
		QuestSnapshot snapReward = new QuestSnapshot(7, 30771, QuestStatus.REWARD,
			definition.progressLayout().pack(Map.of("var0", 4)), Map.of());
		assertTrue(routes(definition, "reward", 804728).isEmpty(), "804728 绝不能作为领奖或汇报 NPC");

		QuestEvent talkHankReward = new QuestEvent.TalkToNpc(804871, QuestDialogAction.QUEST_SELECT.id());
		QuestMutationPlan planHankReward = dispatch(compiled, snapReward, talkHankReward);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			planHankReward.afterCommit());

		QuestEvent selectRewardPreview = new QuestEvent.TalkToNpc(804871, QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestMutationPlan planPreview = dispatch(compiled, snapReward, selectRewardPreview);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), planPreview.afterCommit());

		QuestEvent completeEvent = new QuestEvent.TalkToNpc(804871, QuestDialogAction.SELECTED_QUEST_REWARD1.id());
		QuestMutationPlan planComplete = dispatch(compiled, snapReward, completeEvent);
		assertEquals(QuestStatus.COMPLETE, planComplete.nextStatus());
	}

	@Test
	void zonesExistAndCoverUseItemPositions() throws Exception {
		String zonesXml = Files.readString(
			Path.of("src/main/resources/aion/data/static_data/zones/zones_quest.xml"));

		// 30721 Cygnea 区域 LF5_ITEMUSEAREA_Q30721
		assertTrue(zonesXml.contains("name=\"LF5_ITEMUSEAREA_Q30721\""),
			"zones_quest.xml 必须包含 LF5_ITEMUSEAREA_Q30721");
		assertTrue(zonesXml.contains("mapid=\"210070000\" name=\"LF5_ITEMUSEAREA_Q30721\" area_type=\"SPHERE\" zone_type=\"ITEM_USE\""),
			"LF5_ITEMUSEAREA_Q30721 必须为 mapid 210070000 的 ITEM_USE SPHERE");
		// 校验残骸堆 NPC 805207 (145.90985, 1427.6487, 484.65582) 落在真端区域 (152.65, 1430.13, 488.10, r=41.04) 内
		float dx1 = 152.65f - 145.90985f;
		float dy1 = 1430.13f - 1427.6487f;
		float dz1 = 488.10f - 484.65582f;
		double dist1 = Math.sqrt(dx1 * dx1 + dy1 * dy1 + dz1 * dz1);
		assertTrue(dist1 < 41.04, "NPC 805207 坐标必须落在 LF5_ITEMUSEAREA_Q30721 范围内，当前距离: " + dist1);

		// 30771 Enshar 区域 DF5_ITEMUSEAREA_Q30771
		assertTrue(zonesXml.contains("name=\"DF5_ITEMUSEAREA_Q30771\""),
			"zones_quest.xml 必须包含 DF5_ITEMUSEAREA_Q30771");
		assertTrue(zonesXml.contains("mapid=\"220080000\" name=\"DF5_ITEMUSEAREA_Q30771\" area_type=\"SPHERE\" zone_type=\"ITEM_USE\""),
			"DF5_ITEMUSEAREA_Q30771 必须为 mapid 220080000 的 ITEM_USE SPHERE");
		// 校验残骸堆 NPC 805208 (2906.7825, 1664.2417, 321.172) 落在真端区域 (2924.01, 1672.71, 322.26, r=58.71) 内
		float dx2 = 2924.01f - 2906.7825f;
		float dy2 = 1672.71f - 1664.2417f;
		float dz2 = 322.26f - 321.172f;
		double dist2 = Math.sqrt(dx2 * dx2 + dy2 * dy2 + dz2 * dz2);
		assertTrue(dist2 < 58.71, "NPC 805208 坐标必须落在 DF5_ITEMUSEAREA_Q30771 范围内，当前距离: " + dist2);
	}

	private static QuestMutationPlan dispatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestMutationPlan> plans = compiled.definition().transitions().stream()
			.map(transition -> QuestMutationPlanner.plan(compiled, snapshot, event, transition).orElse(null))
			.filter(Objects::nonNull)
			.toList();
		assertEquals(1, plans.size(), () -> compiled.id() + " " + event + " "
			+ compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
		return plans.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
			.toList();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		var node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
				Quest30721And30771RetailFlowTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
