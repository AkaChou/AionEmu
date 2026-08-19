package com.aionemu.gameserver.questEngine.task;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.questEngine.definition.QuestLureCompletion;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 锁定诱导 NPC 的到达完成策略及玩家死亡归因。
 * Locks lure-NPC arrival completion strategies and player death attribution.
 */
class LuredNpcCheckTaskTest {
	@Test
	void deleteCompletionKeepsThe1157RespawnThenDeleteContract() {
		List<String> calls = new ArrayList<>();
		TestNpc npc = npc(new RecordingNpcController(calls));

		LuredNpcCheckTask.completeNpc(npc, player(), QuestLureCompletion.DELETE);

		assertEquals(List.of("schedule-respawn", "delete"), calls);
	}

	@Test
	void killCompletionCallsOnDieWithTheLuringPlayer() {
		List<String> calls = new ArrayList<>();
		RecordingNpcController controller = new RecordingNpcController(calls);
		TestNpc npc = npc(controller);
		Player player = player();

		LuredNpcCheckTask.completeNpc(npc, player, QuestLureCompletion.KILL);

		assertEquals(List.of("kill"), calls);
		assertSame(player, controller.killer);
	}

	private static Player player() {
		return new ObjenesisStd().newInstance(Player.class);
	}

	private static TestNpc npc(NpcController controller) {
		TestNpc npc = new ObjenesisStd().newInstance(TestNpc.class);
		npc.controller = controller;
		return npc;
	}

	/** Test NPC with an explicitly recording controller. / 使用记录控制器的测试 NPC。 */
	private static final class TestNpc extends Npc {
		private NpcController controller;

		private TestNpc() {
			super(0, null, null, (NpcTemplate) null);
		}

		@Override
		public NpcController getController() {
			return controller;
		}
	}

	/** Records only lure completion side effects. / 只记录诱导完成副作用。 */
	private static final class RecordingNpcController extends NpcController {
		private final List<String> calls;
		private Creature killer;

		private RecordingNpcController(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public Future<?> scheduleRespawn() {
			calls.add("schedule-respawn");
			return null;
		}

		@Override
		public void onDelete() {
			calls.add("delete");
		}

		@Override
		public void onDie(Creature lastAttacker) {
			calls.add("kill");
			killer = lastAttacker;
		}
	}
}
