package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;

/**
 * 服务端收尾关窗（原版 {@code 0x5d8}）：关客户端窗口的同时必须结束 NPC 的对话态——向已装配 AI 的
 * NPC 补发 {@code DIALOG_FINISH}（与客户端 {@code CM_CLOSE_DIALOG} 同链）。
 * <p>
 * 缺这一步时行进中的对话 NPC **停在半路永不恢复**：开门时 {@code TalkEventHandler.onSimpleTalk}
 * 把行进 NPC 置 {@code AISubState.TALK} 并设玩家为目标，移动 tick 随即把它判为「已到达」
 * （{@code AbstractAI#isDestinationReached}）并 {@code abortMove()}，而恢复巡逻的唯一常规入口就是
 * {@code DIALOG_FINISH}（2026-10-06 实机 NPC 203111 Spiros，quest 14110 关窗后不再巡逻）。
 * <p>
 * The server-side dialog close must also end the NPC's talk state by firing {@code DIALOG_FINISH},
 * exactly like the client-close tail; otherwise a walking dialog NPC stays paused forever (live
 * NPC 203111, 2026-10-06).
 */
class DialogServiceTest {

	/** 真机 NPC 203111 = Spiros（带路线 LF1A_8_NpcPath_N_Spiros 的巡逻对话 NPC）。 */
	private static final int PATROL_DIALOG_NPC = 203111;

	@Test
	void closeDialogFiresDialogFinishOnTheAttachedAi() {
		Player player = NativeTalkFixture.player();
		Npc npc = NativeTalkFixture.npc(PATROL_DIALOG_NPC);
		RecordingAi2 ai = attachAi(npc);
		NativeTalkFixture.clearPackets(player);

		DialogService.closeDialog(npc, player);

		assertEquals(List.of("dialogFinish:" + player.getObjectId()), ai.events,
			"服务端关窗必须向 NPC AI 补发 DIALOG_FINISH（否则巡逻停在半路）");
		NativeTalkFixture.assertCloseDialog(player);
	}

	@Test
	void closeDialogResolvesTheNpcFromTheInteractionObjectId() {
		Player player = NativeTalkFixture.player();
		Npc npc = NativeTalkFixture.npc(PATROL_DIALOG_NPC);
		player.setKnownlist(new com.aionemu.gameserver.world.knownlist.KnownList(player));
		player.getKnownList().getKnownObjects().put(npc.getObjectId(), npc);
		RecordingAi2 ai = attachAi(npc);
		NativeTalkFixture.clearPackets(player);

		DialogService.closeDialog(player, npc.getObjectId());

		assertEquals(List.of("dialogFinish:" + player.getObjectId()), ai.events,
			"按交互目标 objectId 解析到 NPC 后同样要结束对话态");
		NativeTalkFixture.assertCloseDialog(player);
	}

	@Test
	void closeDialogSkipsTheAiWhenTheNpcHasNone() {
		Player player = NativeTalkFixture.player();
		Npc npc = NativeTalkFixture.npc(PATROL_DIALOG_NPC);
		NativeTalkFixture.clearPackets(player);

		// 未装配 AI 的 NPC 不得让收尾路径懒建 dummy AI，也不得抛错。
		// A creature without an AI must not make the teardown path lazily attach the dummy AI.
		DialogService.closeDialog(npc, player);

		NativeTalkFixture.assertCloseDialog(player);
	}

	/** 装配记录式 AI：真机形态是 AI 处于 WALKING（巡逻中开门）时被置 TALK 子状态。 /
	 * Attaches a recording AI in the live shape: WALKING (patrolling) when the dialog opened. */
	private static RecordingAi2 attachAi(Npc npc) {
		RecordingAi2 ai = new RecordingAi2();
		ai.setOwner(npc);
		ai.setStateIfNot(AIState.WALKING);
		setAi2(npc, ai);
		return ai;
	}

	private static void setAi2(Npc npc, RecordingAi2 ai) {
		try {
			Field field = Creature.class.getDeclaredField("ai2");
			field.setAccessible(true);
			field.set(npc, ai);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	/** 记录 {@code DIALOG_FINISH} 的空转 AI（AITemplate 的空实现事件面，不产生额外副作用）。 /
	 * A recording no-op AI: the DIALOG_FINISH handler is the only overridden event face. */
	private static final class RecordingAi2 extends NpcAI2 {
		private final List<String> events = new ArrayList<>();

		@Override
		protected void handleDialogFinish(Player player) {
			events.add("dialogFinish:" + player.getObjectId());
		}
	}
}
