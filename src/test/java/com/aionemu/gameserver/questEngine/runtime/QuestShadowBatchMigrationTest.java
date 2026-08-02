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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
 * 批次 8：shadow observation 采集链路到迁移工具消费端的契约验证。
 *
 * <p>真实 QuestEngine 事件分发 + 旧 handler 执行 + capture 采集 + {@link QuestShadowCaptureService}
 * {@code drainAndPersist} 原子落盘 → 产出 {@code unified-shadow-batch.json}。本测试只覆盖 Java 侧：
 * 落盘原子、schemaVersion 可读、payload 包含迁移工具消费端读取的字段（{@code actualInvocations}/
 * {@code complete}/{@code clean}/{@code differenceCounts}）、部分批次不误报 {@code complete}，以及坏
 * payload 由 writer 读取 fail-closed。消费端还会拒绝与当前全量 owner inventory 不一致的局部
 * expectedOwners；该合并门禁由
 * {@code docs/quest/tools/run-shadow-batch-integration.sh} 端到端装配验证。</p>
 */
class QuestShadowBatchMigrationTest {
	private static final String FIXTURE_PROPERTY = "shadow.batch.fixture.path";
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;
	private static final int OTHER_ID = 1002;
	private static final int NPC_ID = 210000;
	private static final EvidenceRef EVIDENCE = new EvidenceRef("test", "shadow", "migration");

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
	void capturedBatchPersistsToConsumablePayload() throws Exception {
		service = newService(Set.of(QUEST_ID, OTHER_ID), fixturePath());
		service.install(engine);
		engine.addQuestHandler(new KillHandler(QUEST_ID));

		engine.onKill(new QuestEnv(npc(), player, 0, 0));
		QuestShadowBatchReport report = service.drainAndPersist();
		Path fixture = fixturePath();
		assertTrue(Files.exists(fixture), "drainAndPersist 应原子落盘批次文件");
		assertEquals(QuestShadowReportWriter.SCHEMA_VERSION, QuestShadowReportWriter.readSchemaVersion(fixture),
			"落盘 payload 应携带 schemaVersion 且可读");

		// 部分批次：真实事件只采集到 1 个 owner，expected 为 2 → 不得误报 complete/clean
		assertEquals(1, report.actualInvocations());
		assertFalse(report.complete());
		assertFalse(report.clean());
		// 迁移工具消费端读取的字段契约
		String payload = Files.readString(fixture, StandardCharsets.UTF_8);
		assertTrue(payload.contains("\"actualInvocations\":1"), "消费端需读取 actualInvocations");
		assertTrue(payload.contains("\"complete\":false"), "消费端需读取 complete");
		assertTrue(payload.contains("\"clean\":false"), "消费端需读取 clean");
		assertTrue(payload.contains("\"differenceCounts\":"), "消费端需读取 differenceCounts");
	}

	@Test
	void corruptedPersistedPayloadFailsClosed() throws Exception {
		// 独立损坏路径，避免覆写共享 fixture（两个测试方法共享同一 -Dshadow.batch.fixture.path）
		Path corrupted = tempDir.resolve("corrupted-batch.json");
		service = newService(Set.of(QUEST_ID), corrupted);
		service.install(engine);
		engine.addQuestHandler(new KillHandler(QUEST_ID));
		engine.onKill(new QuestEnv(npc(), player, 0, 0));
		service.drainAndPersist();
		assertTrue(Files.exists(corrupted), "drainAndPersist 应原子落盘");

		// 落盘后被破坏的 payload 不能成为 shadow 样本证据
		Files.writeString(corrupted, "{truncated", StandardCharsets.UTF_8);
		assertThrows(IllegalArgumentException.class, () -> QuestShadowReportWriter.readSchemaVersion(corrupted),
			"截断 payload 应 fail-closed");
	}

	private Path fixturePath() {
		String configured = System.getProperty(FIXTURE_PROPERTY);
		if (configured != null && !configured.isBlank()) {
			return Path.of(configured);
		}
		return tempDir.resolve("unified-shadow-batch.json");
	}

	private QuestShadowCaptureService newService(Set<Integer> expectedOwners, Path reportPath) throws Exception {
		CompiledQuestDefinition definition = quest(QUEST_ID)
				.evidence(EVIDENCE)
				.node("start", project(QuestStatus.START, java.util.Map.of()))
				.node("reward", project(QuestStatus.REWARD, java.util.Map.of()))
				.on(killNpc(NPC_ID)).when(statusIs(QuestStatus.START)).then(setStatus(QuestStatus.REWARD))
				.goTo("reward").compile();
		return new QuestShadowCaptureService(new ImmutableQuestCatalog(List.of(definition)),
				expectedOwners, reportPath);
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
