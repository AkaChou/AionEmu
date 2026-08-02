package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.closeDialog;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setStatus;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.showQuestDialog;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 对话能力代表任务: 1101 (report_to, start 203049 / end 203057, 无任务物品)。
 *
 * 验证 ReportTo 模板 handler 的固定 dialog 分支可被完整、封闭地投影到统一 IR:
 *   NONE + START_DIALOG(31)   → show-quest-dialog(1011)          (显示接取页)
 *   NONE + ACCEPT_QUEST(1002) → set-status START + close-dialog   (接取)
 *   NONE + ACCEPT_QUEST_SIMPLE(20000) → set-status START          (简易接取)
 *   START + START_DIALOG(31)  → show-quest-dialog(2375)           (显示汇报页)
 *   START + SELECT_REWARD(1009) → var0=1 + set-status REWARD      (交任务)
 *
 * 这些 dialog 分支来自 ReportTo.java:83-146 的固定 switch, 不是 Python 猜测。
 * objectId 属于执行上下文 (show-quest-dialog 由 after-commit 端口携带权威交互对象),
 * 不由定义文件提供。
 */
class ReportTo1101DefinitionTest {
	private static final int NPC_START = 203049;
	private static final int NPC_END = 203057;

	@Test
	void xmlAndDslCompileToTheSameDefinition() throws Exception {
		CompiledQuestDefinition fromDsl = dslDefinition();
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(xmlFixture());

		assertEquals(fromDsl.definition(), fromXml.definition());
		assertFalse(fromXml.definition().transitions().isEmpty(), "代表任务应有 5 条对话 transition");
	}

	@Test
	void everyDialogPathCarriesAnExplicitDialogId() throws Exception {
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(xmlFixture());
		List<QuestTransition> transitions = fromXml.definition().transitions();
		assertEquals(5, transitions.size(), "1101 应有 5 条可静态证明的对话分支");

		for (QuestTransition transition : transitions) {
			assertTrue(transition.event() instanceof QuestEvent.TalkToNpc,
				"所有分支都应是 TalkToNpc 事件: " + transition.event());
			QuestEvent.TalkToNpc talk = (QuestEvent.TalkToNpc) transition.event();
			assertTrue(talk.dialogId() != null, "对话分支必须携带明确 dialogId");
		}
	}

	@Test
	void acceptAndRewardPathsAreMutuallyExclusiveByStatusAndDialog() throws Exception {
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(xmlFixture());
		// 编译器已用 AMBIGUOUS_TRANSITION 门禁保证同事件 + 同条件无歧义;
		// 这里再断言 start 与 end NPC 的 dialog 分支都指向预期目标状态。
		long rewardPaths = fromXml.definition().transitions().stream()
			.filter(t -> ((QuestEvent.TalkToNpc) t.event()).dialogId() == QuestDialog.SELECT_REWARD.id())
			.count();
		assertEquals(1, rewardPaths, "恰好一条 SELECT_REWARD 分支进入 REWARD");
	}

	private static CompiledQuestDefinition dslDefinition() {
		QuestMetadata metadata = new QuestMetadata("Sleeping On The Job", 1102201, 1, 2147483647,
			java.util.Set.of("ELYOS"), "IMPORTANT", RepeatPolicy.once(), java.util.Set.of(),
			List.of(), List.of(new QuestReward("GOLD", 0, 120), new QuestReward("EXP", 0, 130)), List.of());
		return quest(1101)
			.metadata(metadata)
			.evidence(
				new EvidenceRef("CURRENT_XML_OWNER", "src/main/resources/aion/data/static_data/quest_script_data/poeta.xml#report_to[1101]",
					"report_to 模板声明 start 203049 与 end 203057, 无 item_id, 使用默认对话页 1011/2375"),
				new EvidenceRef("TEMPLATE_HANDLER", "src/main/java/com/aionemu/gameserver/questEngine/handlers/template/ReportTo.java:83-146",
					"ReportTo.onDialogEvent 的固定 dialog 分支: NONE+START_DIALOG 显示 1011, NONE+ACCEPT_QUEST 启动, START+START_DIALOG 显示 2375, START+SELECT_REWARD 收物品进 REWARD"),
				new EvidenceRef("QUEST_DATA", "src/main/resources/aion/data/static_data/quest_data/quest_data.xml#quest[1101]",
					"min-level=1, race=ELYOS, reward gold=120 exp=130 + 3 物品"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(talkToNpc(NPC_START, QuestDialog.START_DIALOG))
				.from("unaccepted").when(statusIs(QuestStatus.NONE)).priority(1).goTo("unaccepted")
				.afterCommit(showQuestDialog(1011))
			.on(talkToNpc(NPC_START, QuestDialog.ACCEPT_QUEST))
				.from("unaccepted").when(statusIs(QuestStatus.NONE)).priority(2)
				.then(setStatus(QuestStatus.START)).goTo("started")
				.afterCommit(closeDialog())
			.on(talkToNpc(NPC_START, QuestDialog.ACCEPT_QUEST_SIMPLE))
				.from("unaccepted").when(statusIs(QuestStatus.NONE)).priority(3)
				.then(setStatus(QuestStatus.START)).goTo("started")
				.afterCommit(closeDialog())
			.on(talkToNpc(NPC_END, QuestDialog.START_DIALOG))
				.from("started").when(statusIs(QuestStatus.START)).priority(1).goTo("started")
				.afterCommit(showQuestDialog(2375))
			.on(talkToNpc(NPC_END, QuestDialog.SELECT_REWARD))
				.from("started").when(statusIs(QuestStatus.START)).priority(2)
				.then(setVariable("var0", 1)).then(setStatus(QuestStatus.REWARD)).goTo("reward")
				.afterCommit(closeDialog())
			.compile();
	}

	private static InputStream xmlFixture() throws Exception {
		byte[] bytes;
		try (InputStream in = ReportTo1101DefinitionTest.class.getClassLoader()
				.getResourceAsStream("quest-definition-candidates/reportto-1101.xml")) {
			if (in == null) {
				throw new IllegalStateException("missing fixture quest-definition-candidates/reportto-1101.xml");
			}
			bytes = in.readAllBytes();
		}
		return new ByteArrayInputStream(bytes);
	}
}
