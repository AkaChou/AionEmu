package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.EmotionId;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DialogBindingMode;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DialogTargetKind;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EmotionTarget;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Event;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SendDialogAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SendEmotionAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphDialogProtocolAdapter.DialogCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphDialogProtocolAdapter.DialogCommandTargetKind;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphDialogProtocolAdapter.EmotionCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemDialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemUseEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

/**
 * 验证 BOUND/UNBOUND 对话协议投影与失败关闭。
 * Verifies BOUND/UNBOUND dialog protocol projection and fail-closed handling.
 */
class QuestGraphDialogProtocolAdapterTest {

	/**
	 * 验证 BOUND 与 UNBOUND 分别投影，且错误 owner/动作失败。
	 * Verifies BOUND and UNBOUND project distinctly and wrong owner/action fails.
	 */
	@Test
	void projectsBoundAndUnboundDialogsAndRejectsMismatch() {
		AtomicReference<DialogCommand> last = new AtomicReference<>();
		QuestGraphDialogProtocolAdapter adapter = new QuestGraphDialogProtocolAdapter(7, command -> {
			last.set(command);
			return ActionResult.APPLIED;
		});

		assertEquals(ActionResult.APPLIED,
			adapter.execute(invocation(7, new SendDialogAction(1352, DialogBindingMode.BOUND), "bound")));
		assertEquals(DialogBindingMode.BOUND, last.get().binding());
		assertEquals(1352, last.get().dialogId());
		assertEquals(1, last.get().questId());
		assertEquals(990072, last.get().objectId());
		assertEquals(DialogCommandTargetKind.NPC, last.get().targetKind());

		assertEquals(ActionResult.APPLIED,
			adapter.execute(invocation(7, new SendDialogAction(1352, DialogBindingMode.UNBOUND), "unbound")));
		assertEquals(DialogBindingMode.UNBOUND, last.get().binding());

		assertEquals(ActionResult.APPLIED, adapter.execute(invocation(7, new SendDialogAction(5), "default-bound")));
		assertEquals(DialogBindingMode.BOUND, last.get().binding());

		assertEquals(ActionResult.FAILED,
			adapter.execute(invocation(8, new SendDialogAction(5, DialogBindingMode.BOUND), "wrong-owner")));
		assertEquals(ActionResult.FAILED, adapter.execute(invocation(7, new PlayMovieAction(913), "wrong-action")));
		ActionInvocation missingObjectId = new ActionInvocation(new SendDialogAction(5), 1, 0, QuestStatus.START,
			new DialogEvent("missing-object", 7, 1_700_000_000_000L, 203072, "STEP_TO_1"),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, "missing-object");
		assertEquals(ActionResult.FAILED, adapter.execute(missingObjectId));
	}

	/** 验证只有显式零 NPC 身份可投影无目标窗口，其他目标种类与 objectId 必须成对。 / Verifies only explicit zero NPC identity projects a targetless window. */
	@Test
	void projectsTypedNoTargetDialogAndRejectsIdentityMismatches() {
		AtomicReference<DialogCommand> last = new AtomicReference<>();
		QuestGraphDialogProtocolAdapter adapter = new QuestGraphDialogProtocolAdapter(7, command -> {
			last.set(command);
			return ActionResult.APPLIED;
		});
		DialogEvent event = new DialogEvent("no-target", 7, 1_700_000_000_000L, 0, 0, "ACCEPT_QUEST");
		ActionInvocation noTarget = new ActionInvocation(new SendDialogAction(1003), 1, 0, QuestStatus.START,
			event, new Event(EventType.DIALOG, 0, "ACCEPT_QUEST", DialogTargetKind.NO_TARGET),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, "no-target-dialog");

		assertEquals(ActionResult.APPLIED, adapter.execute(noTarget));
		assertEquals(0, last.get().objectId());
		assertEquals(DialogCommandTargetKind.NO_TARGET, last.get().targetKind());
		assertEquals(ActionResult.FAILED, adapter.execute(new ActionInvocation(new SendDialogAction(1003), 1, 0,
			QuestStatus.START, event, RepeatDeadlineResolution.NOT_APPLICABLE, null, "unproven-no-target")));
		assertThrows(IllegalArgumentException.class,
			() -> new DialogCommand(1, 7, 0, 1003, DialogBindingMode.BOUND, DialogCommandTargetKind.NPC, "npc"));
		assertThrows(IllegalArgumentException.class,
			() -> new DialogCommand(1, 7, 990072, 1003, DialogBindingMode.BOUND, DialogCommandTargetKind.NO_TARGET, "none"));
	}

	/** 验证物品使用与一次性物品对话授权都把真实物品实例绑定到窗口协议。 / Verifies item-use and authorized item-dialog events bind the real item object. */
	@Test
	void projectsDialogsToAuthoritativeItemObjectIdentity() {
		AtomicReference<DialogCommand> last = new AtomicReference<>();
		QuestGraphDialogProtocolAdapter adapter = new QuestGraphDialogProtocolAdapter(7, command -> {
			last.set(command);
			return ActionResult.APPLIED;
		});
		long occurredAt = 1_700_000_000_000L;
		ActionInvocation itemUse = new ActionInvocation(new SendDialogAction(4), 1, 0, QuestStatus.NONE,
			new ItemUseEvent("item-use", 7, occurredAt, 182200501, 990501),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, "item-use-dialog");
		assertEquals(ActionResult.APPLIED, adapter.execute(itemUse));
		assertEquals(990501, last.get().objectId());
		assertEquals(DialogCommandTargetKind.ITEM, last.get().targetKind());

		ActionInvocation itemDialog = new ActionInvocation(new SendDialogAction(1003), 1, 0, QuestStatus.START,
			new ItemDialogEvent("item-dialog", 7, occurredAt, 1, 182200501, 990502, "ACCEPT_QUEST", 11),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, "item-dialog-response");
		assertEquals(ActionResult.APPLIED, adapter.execute(itemDialog));
		assertEquals(990502, last.get().objectId());
	}

	/** 验证玩家/NPC 表情都绑定同一 DIALOG objectId 快照，缺失快照失败关闭。 / Verifies player/NPC emotes bind the same DIALOG object-id snapshot. */
	@Test
	void projectsTypedEmotionTargetsAndRejectsMissingDialogIdentity() {
		AtomicReference<EmotionCommand> last = new AtomicReference<>();
		QuestGraphDialogProtocolAdapter adapter = new QuestGraphDialogProtocolAdapter(7, command -> ActionResult.FAILED,
			command -> {
				last.set(command);
				return ActionResult.APPLIED;
			});

		assertEquals(ActionResult.APPLIED,
			adapter.execute(invocation(7, new SendEmotionAction(EmotionTarget.PLAYER, EmotionId.STAND, true), "player-emote")));
		assertEquals(EmotionTarget.PLAYER, last.get().target());
		assertEquals(EmotionId.STAND, last.get().emotion());
		assertEquals(203072, last.get().npcId());
		assertEquals(990072, last.get().npcObjectId());
		assertEquals(ActionResult.APPLIED,
			adapter.execute(invocation(7, new SendEmotionAction(EmotionTarget.DIALOG_NPC, EmotionId.PANIC, true), "npc-emote")));
		assertEquals(EmotionTarget.DIALOG_NPC, last.get().target());
		assertEquals(EmotionId.PANIC, last.get().emotion());

		ActionInvocation missingObjectId = new ActionInvocation(
			new SendEmotionAction(EmotionTarget.PLAYER, EmotionId.STAND, true), 1, 0, QuestStatus.START,
			new DialogEvent("missing-object", 7, 1_700_000_000_000L, 203072, "STEP_TO_1"),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, "missing-emote-object");
		assertEquals(ActionResult.FAILED, adapter.execute(missingObjectId));
		assertEquals(ActionResult.FAILED,
			adapter.execute(invocation(8, new SendEmotionAction(EmotionTarget.PLAYER, EmotionId.STAND, true), "wrong-owner")));
	}

	/** 验证直接发送失败必须由 typed retry/outbox 接管，否则保持 FAILED 且可重试。 / Verifies direct failures require typed retry/outbox acceptance and otherwise remain retryable. */
	@Test
	void failedDeliveryUsesExplicitRetryPorts() {
		AtomicInteger direct = new AtomicInteger();
		AtomicReference<DialogCommand> retriedDialog = new AtomicReference<>();
		AtomicReference<EmotionCommand> retriedEmotion = new AtomicReference<>();
		QuestGraphDialogProtocolAdapter accepted = new QuestGraphDialogProtocolAdapter(7, command -> {
			direct.incrementAndGet();
			throw new IllegalStateException("offline");
		}, command -> {
			retriedDialog.set(command);
			return ActionResult.APPLIED;
		}, command -> ActionResult.FAILED, command -> {
			retriedEmotion.set(command);
			return ActionResult.ALREADY_APPLIED;
		});

		ActionInvocation dialog = invocation(7, new SendDialogAction(1352), "dialog-retry");
		assertEquals(ActionResult.APPLIED, accepted.execute(dialog));
		assertEquals(ActionResult.ALREADY_APPLIED, accepted.execute(dialog));
		assertEquals("dialog-retry", retriedDialog.get().idempotencyKey());
		ActionInvocation emotion = invocation(7,
			new SendEmotionAction(EmotionTarget.PLAYER, EmotionId.STAND, true), "emotion-retry");
		assertEquals(ActionResult.ALREADY_APPLIED, accepted.execute(emotion));
		assertEquals("emotion-retry", retriedEmotion.get().idempotencyKey());
		assertEquals(1, direct.get());
		assertEquals(2, accepted.size());

		AtomicInteger failedDeliveries = new AtomicInteger();
		QuestGraphDialogProtocolAdapter failed = new QuestGraphDialogProtocolAdapter(7, command -> {
			failedDeliveries.incrementAndGet();
			return ActionResult.REJECTED;
		}, command -> ActionResult.FAILED, command -> ActionResult.FAILED, command -> ActionResult.FAILED);
		ActionInvocation unaccepted = invocation(7, new SendDialogAction(5), "unaccepted");
		assertEquals(ActionResult.FAILED, failed.execute(unaccepted));
		assertEquals(ActionResult.FAILED, failed.execute(unaccepted));
		assertEquals(2, failedDeliveries.get());
		assertEquals(0, failed.size());
	}

	private static ActionInvocation invocation(int playerId,
			com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action action, String key) {
		return new ActionInvocation(action, 1, 0, QuestStatus.START,
			new DialogEvent("dialog", playerId, 1_700_000_000_000L, 203072, 990072, "STEP_TO_1"),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, key);
	}
}
