package com.aionemu.gameserver.questEngine;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestShadowBatchRunner;
import com.aionemu.gameserver.questEngine.runtime.QuestShadowCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real {@link QuestEngine} entry-point integration for the production shadow
 * assembly: after {@code setShadowCapture} a physical dispatch produces typed
 * shadow bindings; detaching restores the no-op bridge; a missing handler or a
 * failing capture never changes the legacy dispatch result.
 */
class QuestEngineShadowIntegrationTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;
	private static final int NPC_ID = 210000;

	private QuestEngine engine;
	private QuestShadowCapture capture;
	private Player player;
	private NpcData originalNpcData;

	@BeforeEach
	void setUp() throws Exception {
		engine = QuestEngine.getInstance();
		engine.clear();
		// NpcData 未在单测加载:注入空索引,registerCanAct 走 null 分支而不是 NPE。
		originalNpcData = DataManager.NPC_DATA;
		NpcData fake = new ObjenesisStd().newInstance(NpcData.class);
		setField(NpcData.class, fake, "npcData", new IntObjectHashMap<>());
		DataManager.NPC_DATA = fake;
		capture = new QuestShadowCapture();
		engine.setShadowCapture(capture);
		player = playerWithState(QuestStatus.START, 0);
	}

	@AfterEach
	void tearDown() {
		engine.setShadowCapture(null);
		engine.clear();
		DataManager.NPC_DATA = originalNpcData;
	}

	@Test
	void assembledCaptureBindsRealDispatchInvocation() throws Exception {
		engine.addQuestHandler(new KillHandler(QUEST_ID));
		QuestEnv env = new QuestEnv(npc(), player, 0, 0);

		boolean handled = engine.onKill(env);

		assertTrue(handled);
		assertEquals(1, capture.envelopes().size());
		QuestShadowBatchRunner.Envelope envelope = capture.envelopes().get(0);
		assertEquals(PLAYER_ID, envelope.snapshots().get(QUEST_ID).playerId());
		assertEquals(Set.of(QUEST_ID), envelope.observation().owners().keySet());
		// 快照冻结在 handler 变异之前
		assertEquals(QuestStatus.START, envelope.snapshots().get(QUEST_ID).status());
		// handler 实际推进了状态
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(QUEST_ID).getStatus());
	}

	@Test
	void detachedCaptureRestoresNoopBridgeAndStopsProducingBindings() throws Exception {
		engine.setShadowCapture(null);
		engine.addQuestHandler(new KillHandler(QUEST_ID));
		QuestEnv env = new QuestEnv(npc(), player, 0, 0);

		boolean handled = engine.onKill(env);

		assertTrue(handled);
		assertEquals(0, capture.envelopes().size());
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(QUEST_ID).getStatus());
	}

	@Test
	void dispatchWithoutHandlerKeepsLegacyResultUnchanged() throws Exception {
		QuestEnv env = new QuestEnv(npc(), player, 0, 0);

		boolean handled = engine.onKill(env);

		assertTrue(handled);
		assertEquals(0, capture.envelopes().size());
	}

	/** 真实 legacy owner:register 绑定 NPC 击杀,onKill 推进状态(不发协议包)。 */
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
		states.addQuest(QUEST_ID, new QuestState(QUEST_ID, status, packedVariables, 0,
				(Timestamp) null, null, null));
		setField(Player.class, player, "questStateList", states);
		return player;
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
