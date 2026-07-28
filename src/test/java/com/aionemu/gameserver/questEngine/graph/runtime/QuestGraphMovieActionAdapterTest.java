package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncQuestStatusAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphMovieActionAdapter.PlayMovieCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

class QuestGraphMovieActionAdapterTest {

	/** 验证直接协议成功占用稳定幂等键，重复调用不再次播放。 / Verifies direct protocol success claims the stable idempotency key and prevents replay. */
	@Test
	void directDeliveryIsIdempotent() {
		AtomicInteger deliveries = new AtomicInteger();
		AtomicInteger retries = new AtomicInteger();
		AtomicReference<PlayMovieCommand> command = new AtomicReference<>();
		QuestGraphMovieActionAdapter adapter = new QuestGraphMovieActionAdapter(7, value -> {
			command.set(value);
			deliveries.incrementAndGet();
			return ActionResult.APPLIED;
		}, value -> {
			retries.incrementAndGet();
			return ActionResult.FAILED;
		});
		ActionInvocation invocation = invocation(7, new PlayMovieAction(913), "movie-key");

		assertEquals(ActionResult.APPLIED, adapter.execute(invocation));
		assertEquals(ActionResult.ALREADY_APPLIED, adapter.execute(invocation));
		assertEquals(1, deliveries.get());
		assertEquals(0, retries.get());
		assertEquals(new PlayMovieCommand(1, 7, 913, "movie-key"), command.get());
		assertEquals(1, adapter.size());
	}

	/** 验证发送异常或失败进入同一 typed retry command，retry 未接管时保持 FAILED 且可再次尝试。 / Verifies send exceptions or failures use the same typed retry command and remain retryable when not accepted. */
	@Test
	void failedDeliveryUsesExplicitRetryPort() {
		AtomicInteger retries = new AtomicInteger();
		QuestGraphMovieActionAdapter recovered = new QuestGraphMovieActionAdapter(7, command -> {
			throw new IllegalStateException("connection failed");
		}, command -> {
			retries.incrementAndGet();
			return ActionResult.APPLIED;
		});
		assertEquals(ActionResult.APPLIED, recovered.execute(invocation(7, new PlayMovieAction(913), "retry-key")));
		assertEquals(1, retries.get());

		AtomicInteger deliveries = new AtomicInteger();
		QuestGraphMovieActionAdapter failed = new QuestGraphMovieActionAdapter(7, command -> {
			deliveries.incrementAndGet();
			return ActionResult.REJECTED;
		}, command -> ActionResult.FAILED);
		ActionInvocation invocation = invocation(7, new PlayMovieAction(913), "failed-key");
		assertEquals(ActionResult.FAILED, failed.execute(invocation));
		assertEquals(ActionResult.FAILED, failed.execute(invocation));
		assertEquals(2, deliveries.get());
		assertEquals(0, failed.size());
	}

	/** 验证 owner mismatch、未知 action 和会话 cleanup 合同。 / Verifies owner mismatch, unknown-action, and session-cleanup contracts. */
	@Test
	void rejectsWrongOwnerOrActionAndClearsSessionKeys() {
		AtomicInteger deliveries = new AtomicInteger();
		QuestGraphMovieActionAdapter adapter = new QuestGraphMovieActionAdapter(7, command -> {
			deliveries.incrementAndGet();
			return ActionResult.APPLIED;
		}, command -> ActionResult.FAILED);
		assertEquals(ActionResult.FAILED, adapter.execute(invocation(8, new PlayMovieAction(913), "wrong-owner")));
		assertEquals(ActionResult.FAILED, adapter.execute(invocation(7, new SyncQuestStatusAction(), "wrong-action")));
		ActionInvocation valid = invocation(7, new PlayMovieAction(913), "session-key");
		assertEquals(ActionResult.APPLIED, adapter.execute(valid));
		adapter.clear();
		assertEquals(0, adapter.size());
		assertEquals(ActionResult.APPLIED, adapter.execute(valid));
		assertEquals(2, deliveries.get());
	}

	private static ActionInvocation invocation(int playerId, com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action action,
			String key) {
		return new ActionInvocation(action, 1, 0, QuestStatus.START,
			new DialogEvent("event", playerId, 1_700_000_000_000L, 100, "QUEST_SELECT"),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, key);
	}
}
