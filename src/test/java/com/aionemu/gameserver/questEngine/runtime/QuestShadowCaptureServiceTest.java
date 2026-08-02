package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.EvidenceRef;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.killNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setStatus;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 批次 5b：运维闭环装配验证。真实 QuestEngine 事件分发 + capture 采集 + 候选定义差分 +
 * {@link QuestShadowReportWriter} 原子落盘，经 {@link QuestShadowCaptureService}
 * 一次物理事件闭合成可消费的 shadow 批报告；clean 证明候选定义与旧 handler 行为在
 * route/variables 维度一致。stop 恢复 no-op 桥，旧执行不被改变。
 */
class QuestShadowCaptureServiceTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;
	private static final int NPC_ID = 210000;
	private static final EvidenceRef EVIDENCE = new EvidenceRef("test", "shadow", "service");

	@TempDir
	Path tempDir;

	private QuestEngine engine;
	private QuestShadowCaptureService service;
	private Player player;
	private NpcData originalNpcData;

	@BeforeEach
	void setUp() throws Exception {
		engine = QuestEngine.getInstance();
		engine.clear();
		// NpcData 未在单测加载：注入空索引，registerCanAct 走 null 分支而不是 NPE。
		originalNpcData = DataManager.NPC_DATA;
		NpcData fake = new ObjenesisStd().newInstance(NpcData.class);
		setField(NpcData.class, fake, "npcData", new IntObjectHashMap<>());
		DataManager.NPC_DATA = fake;
		player = playerWithState(QuestStatus.START, 0);
	}

	@AfterEach
	void tearDown() throws Exception {
		engine.setShadowCapture(null);
		engine.clear();
		DataManager.NPC_DATA = originalNpcData;
	}

	@Test
	void installedServiceCapturesRealDispatchAndPersistsCleanReport() throws Exception {
		service = newService();
		service.install(engine);
		engine.addQuestHandler(new KillHandler(QUEST_ID));

		engine.onKill(new QuestEnv(npc(), player, 0, 0));
		QuestShadowBatchReport report = service.drainAndPersist();

		assertEquals(1, report.actualInvocations(), "真实事件应采集到唯一 owner");
		assertEquals(Set.of(QUEST_ID), report.coveredOwners());
		assertTrue(report.complete(), "候选目录与旧注册 owner 全集应完整覆盖");
		assertTrue(report.clean(), "候选定义与旧 handler 行为应无 typed 差异");
		Path reportPath = tempDir.resolve("unified-shadow-batch.json");
		assertEquals(QuestShadowReportWriter.SCHEMA_VERSION, QuestShadowReportWriter.readSchemaVersion(reportPath));
		assertTrue(reportPath.toFile().length() > 0, "报告应原子落盘非空");
	}

	@Test
	void stopPersistsRemainingBatchThenFailsClosedAndAllowsReinstall() throws Exception {
		service = newService();
		service.install(engine);
		engine.addQuestHandler(new KillHandler(QUEST_ID));

		engine.onKill(new QuestEnv(npc(), player, 0, 0));
		QuestShadowBatchReport stopped = service.stop();

		assertFalse(service.installed());
		assertEquals(1, stopped.actualInvocations(), "stop 应落盘剩余批次");
		Path reportPath = tempDir.resolve("unified-shadow-batch.json");
		assertEquals(QuestShadowReportWriter.SCHEMA_VERSION, QuestShadowReportWriter.readSchemaVersion(reportPath));
		// 卸载后旧 handler 仍真实执行，但所有 service 入口 fail-closed
		playerWithState0();
		engine.onKill(new QuestEnv(npc(), player, 0, 0));
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(QUEST_ID).getStatus(),
				"stop 后旧 handler 仍真实执行");
		assertThrows(IllegalStateException.class, () -> service.drainAndPersist(), "未安装时 drain 应 fail-closed");
		assertThrows(IllegalStateException.class, () -> service.stop(), "未安装时 stop 应 fail-closed");
		// 可重新安装，装配不是一次性
		playerWithState0();
		service.install(engine);
		assertTrue(service.installed());
		engine.onKill(new QuestEnv(npc(), player, 0, 0));
		QuestShadowBatchReport again = service.drainAndPersist();
		assertTrue(again.complete(), "重装后采集链路应再次工作");
	}

	private QuestShadowCaptureService newService() throws Exception {
		CompiledQuestDefinition definition = quest(QUEST_ID)
				.evidence(EVIDENCE)
				.node("start", project(QuestStatus.START, java.util.Map.of()))
				.node("reward", project(QuestStatus.REWARD, java.util.Map.of()))
				.on(killNpc(NPC_ID)).when(statusIs(QuestStatus.START)).then(setStatus(QuestStatus.REWARD))
				.goTo("reward").compile();
		return new QuestShadowCaptureService(new ImmutableQuestCatalog(List.of(definition)),
				Set.of(QUEST_ID), tempDir.resolve("unified-shadow-batch.json"));
	}

	private void playerWithState0() throws Exception {
		QuestStateList states = new QuestStateList();
		states.addQuest(QUEST_ID, new QuestState(QUEST_ID, QuestStatus.START, 0, 0, (Timestamp) null, null, null));
		setField(Player.class, player, "questStateList", states);
	}

	/** 真实 legacy owner：register 绑定 NPC 击杀，onKill 推进状态（不发协议包）。 */
	private static final class KillHandler extends QuestHandler {
		private KillHandler(int questId) {
			super(questId);
		}

		@Override
		public void register() {
			qe.registerQuestNpc(NPC_ID).addOnKillEvent(getQuestId());
		}

		@Override
		public boolean onKillEvent(QuestEnv env) {
			QuestState state = env.getPlayer().getQuestStateList().getQuestState(getQuestId());
			if (state != null && state.getStatus() == QuestStatus.START) {
				state.setStatus(QuestStatus.REWARD);
				return true;
			}
			return false;
		}
	}

	private static Npc npc() throws Exception {
		NpcTemplate template = new ObjenesisStd().newInstance(NpcTemplate.class);
		setField(NpcTemplate.class, template, "npcId", NPC_ID);
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		setField(VisibleObject.class, npc, "objectTemplate", template);
		return npc;
	}

	private static Player playerWithState(QuestStatus status, int packedVariables) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		setField(AionObject.class, player, "objectId", PLAYER_ID);
		QuestStateList states = new QuestStateList();
		states.addQuest(QUEST_ID, new QuestState(QUEST_ID, status, packedVariables, 0, (Timestamp) null, null, null));
		setField(Player.class, player, "questStateList", states);
		return player;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
