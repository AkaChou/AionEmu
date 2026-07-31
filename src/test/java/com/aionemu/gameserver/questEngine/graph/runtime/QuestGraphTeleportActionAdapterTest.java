package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.TeleportHeadingPolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.TeleportInstancePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.TeleportPlayerAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTeleportActionAdapter.TeleportCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.TeleportPlan;

/**
 * 验证传送 adapter 的预检、成功应用与错误 owner/动作失败关闭。
 * Verifies teleport adapter preflight, successful apply, and wrong owner/action fail-closed.
 */
class QuestGraphTeleportActionAdapterTest {

	/**
	 * 验证合法传送预检就绪并应用命令。
	 * Verifies a valid teleport preflights ready and applies the command.
	 */
	@Test
	void teleportsWithAuthoritativeCoordinates() {
		AtomicReference<TeleportCommand> last = new AtomicReference<>();
		QuestGraphTeleportActionAdapter adapter = new QuestGraphTeleportActionAdapter(7, command -> {
			last.set(command);
			return ActionResult.APPLIED;
		});
		ActionInvocation invocation = invocation(7, new TeleportPlayerAction(210010000, 1, 1f, 2f, 3f, (byte) 90), "tp");
		assertEquals(PreflightResult.READY, adapter.preflight(invocation));
		assertEquals(ActionResult.APPLIED, adapter.execute(invocation));
		assertEquals(210010000, last.get().worldId());
		assertEquals(1, last.get().instanceId());
		assertEquals(TeleportInstancePolicy.EXPLICIT_OR_DEFAULT, last.get().instancePolicy());
		assertEquals(90, last.get().heading());
	}

	/** 验证动态 instance/heading 只在 PREPARED 前读取一次，执行仅消费冻结计划。 / Verifies dynamic instance/heading are read once before PREPARED and execution only consumes the frozen plan. */
	@Test
	void freezesCurrentPlayerContextBeforeExecution() {
		AtomicInteger snapshots = new AtomicInteger();
		AtomicReference<TeleportCommand> last = new AtomicReference<>();
		QuestGraphTeleportActionAdapter adapter = new QuestGraphTeleportActionAdapter(7,
			QuestGraphTeleportActionAdapter.RegisteredInstanceGateway.UNAVAILABLE,
			() -> {
				snapshots.incrementAndGet();
				return new QuestGraphTeleportActionAdapter.PlayerContext(7, 37, (byte) 44);
			}, command -> {
				last.set(command);
				return ActionResult.APPLIED;
			});
		TeleportPlayerAction action = new TeleportPlayerAction(210030000, 0, TeleportInstancePolicy.PLAYER_CURRENT,
			1643, 1500, 120, TeleportHeadingPolicy.PLAYER_CURRENT, (byte) 0);
		ActionInvocation unresolved = invocation(7, action, "frozen");

		TeleportPlan plan = adapter.preparePlan(unresolved);
		ActionInvocation frozen = invocation(7, action, "frozen", plan);
		assertEquals(PreflightResult.READY, adapter.preflight(frozen));
		assertEquals(ActionResult.APPLIED, adapter.execute(frozen));
		assertEquals(1, snapshots.get());
		assertEquals(37, last.get().instanceId());
		assertEquals(44, last.get().heading());
		assertEquals(TeleportInstancePolicy.EXPLICIT_OR_DEFAULT, last.get().instancePolicy());
	}

	@Test
	void freezesLegacyDefaultInstanceForSameAndCrossWorldTeleports() {
		AtomicReference<TeleportCommand> last = new AtomicReference<>();
		QuestGraphTeleportActionAdapter adapter = new QuestGraphTeleportActionAdapter(7,
			QuestGraphTeleportActionAdapter.RegisteredInstanceGateway.UNAVAILABLE,
			() -> new QuestGraphTeleportActionAdapter.PlayerContext(7, 210010000, 37, (byte) 44), command -> {
				last.set(command);
				return ActionResult.APPLIED;
			});

		TeleportPlayerAction sameWorld = new TeleportPlayerAction(210010000, 0, 1, 2, 3, (byte) 4);
		TeleportPlan sameWorldPlan = adapter.preparePlan(invocation(7, sameWorld, "same-world"));
		assertEquals(37, sameWorldPlan.instanceId());
		assertEquals(ActionResult.APPLIED, adapter.execute(invocation(7, sameWorld, "same-world", sameWorldPlan)));
		assertEquals(37, last.get().instanceId());

		TeleportPlayerAction crossWorld = new TeleportPlayerAction(220010000, 0, 4, 5, 6, (byte) 7);
		TeleportPlan crossWorldPlan = adapter.preparePlan(invocation(7, crossWorld, "cross-world"));
		assertEquals(1, crossWorldPlan.instanceId());
		assertEquals(ActionResult.APPLIED, adapter.execute(invocation(7, crossWorld, "cross-world", crossWorldPlan)));
		assertEquals(1, last.get().instanceId());
	}

	/** 验证仅动态朝向不会绕过 registered-instance 策略，也不要求无关的当前 instance。 / Verifies a dynamic heading preserves registered-instance routing without requiring an unrelated current instance. */
	@Test
	void currentHeadingPreservesRegisteredInstancePolicy() {
		AtomicInteger finds = new AtomicInteger();
		AtomicInteger creates = new AtomicInteger();
		AtomicReference<TeleportCommand> last = new AtomicReference<>();
		QuestGraphTeleportActionAdapter adapter = new QuestGraphTeleportActionAdapter(7,
			new QuestGraphTeleportActionAdapter.RegisteredInstanceGateway() {
				@Override
				public boolean isInstanceWorld(int worldId) {
					return worldId == 301580000;
				}

				@Override
				public int findRegistered(int worldId, int playerId) {
					finds.incrementAndGet();
					return 37;
				}

				@Override
				public int createAndRegister(int worldId, int playerId) {
					creates.incrementAndGet();
					return 41;
				}
			}, () -> new QuestGraphTeleportActionAdapter.PlayerContext(7, 0, (byte) 44), command -> {
				last.set(command);
				return ActionResult.APPLIED;
			});
		TeleportPlayerAction action = new TeleportPlayerAction(301580000, 0,
			TeleportInstancePolicy.PLAYER_REGISTERED_OR_CREATE, 1643, 1500, 120,
			TeleportHeadingPolicy.PLAYER_CURRENT, (byte) 0);

		TeleportPlan plan = adapter.preparePlan(invocation(7, action, "registered-heading"));
		assertEquals(ActionResult.APPLIED, adapter.execute(invocation(7, action, "registered-heading", plan)));
		assertEquals(1, finds.get());
		assertEquals(0, creates.get());
		assertEquals(37, last.get().instanceId());
		assertEquals(44, last.get().heading());
		assertEquals(TeleportInstancePolicy.EXPLICIT_OR_DEFAULT, last.get().instancePolicy());
	}

	/** 验证只有 PLAYER_CURRENT instance 才要求快照含正 instance id。 / Verifies only PLAYER_CURRENT instance requires a positive snapshot instance id. */
	@Test
	void currentInstanceRejectsMissingSnapshotInstance() {
		QuestGraphTeleportActionAdapter adapter = new QuestGraphTeleportActionAdapter(7,
			QuestGraphTeleportActionAdapter.RegisteredInstanceGateway.UNAVAILABLE,
			() -> new QuestGraphTeleportActionAdapter.PlayerContext(7, 0, (byte) 44), command -> ActionResult.APPLIED);
		TeleportPlayerAction action = new TeleportPlayerAction(210030000, 0, TeleportInstancePolicy.PLAYER_CURRENT,
			1643, 1500, 120, TeleportHeadingPolicy.EXPLICIT, (byte) 9);

		assertThrows(IllegalStateException.class, () -> adapter.preparePlan(invocation(7, action, "missing-instance")));
		assertThrows(IllegalArgumentException.class,
			() -> new QuestGraphTeleportActionAdapter.PlayerContext(7, -1, (byte) 44));
	}

	/** 验证动态动作缺失/篡改计划以及静态动作夹带计划均失败关闭。 / Verifies missing/tampered dynamic plans and extra static plans fail closed. */
	@Test
	void rejectsMissingTamperedAndUnexpectedTeleportPlans() {
		QuestGraphTeleportActionAdapter adapter = new QuestGraphTeleportActionAdapter(7,
			QuestGraphTeleportActionAdapter.RegisteredInstanceGateway.UNAVAILABLE,
			() -> new QuestGraphTeleportActionAdapter.PlayerContext(7, 37, (byte) 44),
			command -> ActionResult.APPLIED);
		TeleportPlayerAction dynamic = new TeleportPlayerAction(210030000, 0, TeleportInstancePolicy.PLAYER_CURRENT,
			1643, 1500, 120, TeleportHeadingPolicy.PLAYER_CURRENT, (byte) 0);
		assertEquals(PreflightResult.FAILED, adapter.preflight(invocation(7, dynamic, "missing")));
		TeleportPlan tampered = new TeleportPlan(0, 210030000, 38, 1644, 1500, 120, (byte) 44);
		assertEquals(ActionResult.FAILED, adapter.execute(invocation(7, dynamic, "tampered", tampered)));

		TeleportPlayerAction fixed = new TeleportPlayerAction(210030000, 0, 1643, 1500, 120, (byte) 44);
		TeleportPlan extra = new TeleportPlan(0, 210030000, 0, 1643, 1500, 120, (byte) 44);
		assertEquals(ActionResult.FAILED, adapter.execute(invocation(7, fixed, "extra", extra)));
	}

	/** 验证已登记副本被直接复用，不创建新副本。 / Verifies an existing registration is reused without creating another instance. */
	@Test
	void reusesRegisteredPlayerInstance() {
		AtomicInteger creates = new AtomicInteger();
		AtomicReference<TeleportCommand> last = new AtomicReference<>();
		QuestGraphTeleportActionAdapter adapter = new QuestGraphTeleportActionAdapter(7,
			new QuestGraphTeleportActionAdapter.RegisteredInstanceGateway() {
				@Override
				public boolean isInstanceWorld(int worldId) {
					return worldId == 301580000;
				}

				@Override
				public int findRegistered(int worldId, int playerId) {
					return 37;
				}

				@Override
				public int createAndRegister(int worldId, int playerId) {
					creates.incrementAndGet();
					return 41;
				}
			}, command -> {
				last.set(command);
				return ActionResult.APPLIED;
			});

		assertEquals(ActionResult.APPLIED, adapter.execute(invocation(7, registeredInstanceTeleport(), "reuse")));
		assertEquals(0, creates.get());
		assertEquals(37, last.get().instanceId());
	}

	@Test
	void durableCommandRetainsResolvedInstanceRecoveryAuthority() {
		QuestGraphTeleportOutboxTest.MemoryStore store = new QuestGraphTeleportOutboxTest.MemoryStore();
		QuestGraphTeleportOutbox outbox = new QuestGraphTeleportOutbox(7, store, task -> { },
			(command, authorization, completion) -> true, () -> 100);
		QuestGraphTeleportActionAdapter adapter = new QuestGraphTeleportActionAdapter(7,
			new QuestGraphTeleportActionAdapter.RegisteredInstanceGateway() {
				@Override
				public boolean isInstanceWorld(int worldId) {
					return true;
				}

				@Override
				public int findRegistered(int worldId, int playerId) {
					return 37;
				}

				@Override
				public int createAndRegister(int worldId, int playerId) {
					return 0;
				}
			}, QuestGraphTeleportActionAdapter.CurrentPlayerContextGateway.UNAVAILABLE, outbox);
		ActionInvocation invocation = invocation(7, registeredInstanceTeleport(), "durable-registered")
			.withJournalIdentity(4, "teleport");

		assertEquals(ActionResult.DURABLY_ACCEPTED, adapter.execute(invocation));
		var accepted = store.find(7, invocation.idempotencyKey()).command();
		assertEquals(37, accepted.instanceId());
		assertEquals(com.aionemu.gameserver.questEngine.graph.state.TeleportOutboxCommand.InstanceRecoveryMode.PLAYER_REGISTERED_OR_CREATE,
			accepted.instanceRecoveryMode());
	}

	/** 验证重试复用已登记副本，但不会用进程内缓存伪造 teleport 幂等。 / Verifies retries reuse the registered instance without faking teleport idempotency in process memory. */
	@Test
	void retryReusesNewlyRegisteredPlayerInstance() {
		AtomicInteger registered = new AtomicInteger();
		AtomicInteger creates = new AtomicInteger();
		AtomicInteger teleports = new AtomicInteger();
		QuestGraphTeleportActionAdapter adapter = new QuestGraphTeleportActionAdapter(7,
			new QuestGraphTeleportActionAdapter.RegisteredInstanceGateway() {
				@Override
				public boolean isInstanceWorld(int worldId) {
					return worldId == 301580000;
				}

				@Override
				public int findRegistered(int worldId, int playerId) {
					return registered.get();
				}

				@Override
				public int createAndRegister(int worldId, int playerId) {
					creates.incrementAndGet();
					registered.compareAndSet(0, 41);
					return registered.get();
				}
			}, command -> teleports.getAndIncrement() == 0 ? ActionResult.FAILED : ActionResult.APPLIED);

		ActionInvocation invocation = invocation(7, registeredInstanceTeleport(), "retry");
		assertEquals(ActionResult.FAILED, adapter.execute(invocation));
		assertEquals(ActionResult.APPLIED, adapter.execute(invocation));
		assertEquals(ActionResult.APPLIED, adapter.execute(invocation));
		assertEquals(1, creates.get());
		assertEquals(3, teleports.get());
	}

	/** 验证副本解析失败时失败关闭且不调用传送端点。 / Verifies resolution failure fails closed without invoking teleport. */
	@Test
	void failsClosedWhenPlayerInstanceCannotBeResolved() {
		AtomicInteger teleports = new AtomicInteger();
		QuestGraphTeleportActionAdapter adapter = new QuestGraphTeleportActionAdapter(7,
			new QuestGraphTeleportActionAdapter.RegisteredInstanceGateway() {
				@Override
				public boolean isInstanceWorld(int worldId) {
					return worldId == 301580000;
				}

				@Override
				public int findRegistered(int worldId, int playerId) {
					return 0;
				}

				@Override
				public int createAndRegister(int worldId, int playerId) {
					return 0;
				}
			}, command -> {
				teleports.incrementAndGet();
				return ActionResult.APPLIED;
			});

		assertEquals(PreflightResult.READY, adapter.preflight(invocation(7, registeredInstanceTeleport(), "preflight")));
		assertEquals(ActionResult.FAILED, adapter.execute(invocation(7, registeredInstanceTeleport(), "resolve")));
		assertEquals(0, teleports.get());
	}

	/**
	 * 验证错误 owner/动作与非法坐标失败关闭。
	 * Verifies wrong owner/action and illegal coordinates fail closed.
	 */
	@Test
	void rejectsWrongOwnerActionAndInvalidCoordinates() {
		QuestGraphTeleportActionAdapter adapter = new QuestGraphTeleportActionAdapter(7, command -> ActionResult.APPLIED);
		assertEquals(PreflightResult.FAILED,
			adapter.preflight(invocation(8, new TeleportPlayerAction(210010000, 0, 1f, 2f, 3f, (byte) 0), "wrong")));
		assertEquals(PreflightResult.FAILED,
			adapter.preflight(invocation(7, new TeleportPlayerAction(210010000, 0,
				TeleportInstancePolicy.PLAYER_REGISTERED_OR_CREATE, 1f, 2f, 3f, (byte) 0), "world")));
		assertEquals(ActionResult.FAILED, adapter.execute(invocation(7, new PlayMovieAction(913), "movie")));
		assertEquals(ActionResult.FAILED,
			adapter.execute(invocation(8, new TeleportPlayerAction(210010000, 1, 1f, 2f, 3f, (byte) 0), "owner")));
		assertThrows(IllegalArgumentException.class,
			() -> new TeleportPlayerAction(210010000, 0, Float.NaN, 2f, 3f, (byte) 0));
		assertThrows(IllegalArgumentException.class,
			() -> new TeleportPlayerAction(210010000, 1, TeleportInstancePolicy.PLAYER_REGISTERED_OR_CREATE,
				1f, 2f, 3f, (byte) 0));
	}

	private static TeleportPlayerAction registeredInstanceTeleport() {
		return new TeleportPlayerAction(301580000, 0, TeleportInstancePolicy.PLAYER_REGISTERED_OR_CREATE,
			431f, 491f, 99f, (byte) 0);
	}

	private static ActionInvocation invocation(int playerId,
			com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action action, String key) {
		return invocation(playerId, action, key, null);
	}

	private static ActionInvocation invocation(int playerId,
			com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action action, String key, TeleportPlan plan) {
		return new ActionInvocation(action, 1, 0, QuestStatus.START,
			new DialogEvent("dialog", playerId, 1_700_000_000_000L, 203072, "STEP_TO_1"),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, plan, java.util.Map.of(), key);
	}
}
