package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.TeleportInstancePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.TeleportPlayerAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTeleportActionAdapter.TeleportCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

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
		ActionInvocation invocation = invocation(7, new TeleportPlayerAction(210010000, 0, 1f, 2f, 3f, (byte) 90), "tp");
		assertEquals(PreflightResult.READY, adapter.preflight(invocation));
		assertEquals(ActionResult.APPLIED, adapter.execute(invocation));
		assertEquals(210010000, last.get().worldId());
		assertEquals(0, last.get().instanceId());
		assertEquals(TeleportInstancePolicy.EXPLICIT_OR_DEFAULT, last.get().instancePolicy());
		assertEquals(90, last.get().heading());
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

	/** 验证首次登记后传送失败的重试复用同一副本。 / Verifies a retry after teleport failure reuses the instance registered by the first attempt. */
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
		assertEquals(1, creates.get());
		assertEquals(2, teleports.get());
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
		return new ActionInvocation(action, 1, 0, QuestStatus.START,
			new DialogEvent("dialog", playerId, 1_700_000_000_000L, 203072, "STEP_TO_1"),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, key);
	}
}
