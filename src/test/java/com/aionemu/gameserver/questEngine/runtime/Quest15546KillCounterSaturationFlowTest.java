package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 15546《[每日]雷欧娜的委托》顺序链的段饱和合同（定义来自生产驱动，真端四段顺序链）。
 * Saturation contract for the sequential-chain daily 15546 (production-driver definition, the
 * retail four-stage chain): while later stages are closed, a kill of an already saturated stage
 * must not change quest state and must not announce a quest update to the client.
 * <p>回归背景：客户端每段计数上限 4（{@code Progress(SECTION_n<4)}）。顺序链的击杀边只从
 * "首个未满段"推进——段满后同段再击杀不再命中任何路线，不会提交空事务、不会因
 * {@code sync-quest-state PACKET_ONLY} 下发 {@code SM_QUEST_ACTION}（旧并行网格的"收口路线"
 * 假更新问题在链形下结构性消失）。</p>
 * <p>Regression background: each stage caps at 4. Chain kill edges only advance the first
 * unfinished stage, so post-saturation kills match no route — no empty transaction, no spurious
 * quest-update packet (the legacy parallel grid's closing-route double-announce disappears
 * structurally in the chain shape).</p>
 */
class Quest15546KillCounterSaturationFlowTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 15546;
	/** 星光精灵 T_ 变体：Iluma 生产刷怪数据里真实刷新的第 1 段目标（逐段登记表并入）。 */
	private static final int ELEMENTAL_LIGHT_NPC_ID = 241656;
	/** 第 2 段（达鲁）目标，用于验证第 1 段满后下一段继续计数。 */
	private static final int DARU_NPC_ID = 241664;
	/** 第 3 段（波波库）目标，用于验证前段进行中后段永不提前计数。 */
	private static final int POPOKU_NPC_ID = 241676;
	private static final int STAGE_CEILING = 4;

	@Test
	void fourKillsSaturateTheFirstStageAndAnnounceProgress() throws Exception {
		Fixture fixture = new Fixture();
		for (int kill = 1; kill <= STAGE_CEILING; kill++) {
			final int progress = kill;
			fixture.afterCommit.clear();
			assertHandled(fixture.dispatchKill(ELEMENTAL_LIGHT_NPC_ID));
			assertEquals(progress, fixture.counter("var0"), "SECTION_1 progress after kill " + kill);
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				fixture.afterCommit, "kill " + kill + " must announce the counter progress");
		}
	}

	@Test
	void extraKillsOfASaturatedStageDoNotAnnounceAQuestUpdate() throws Exception {
		Fixture fixture = new Fixture();
		for (int kill = 1; kill <= STAGE_CEILING; kill++) {
			assertHandled(fixture.dispatchKill(ELEMENTAL_LIGHT_NPC_ID));
		}
		// 第 2 段仍未计数：任务既不能完成，也不该因为第 1 段的超额击杀下发任何状态更新。
		assertEquals(0, fixture.counter("var1"));
		int packedAfterCeiling = fixture.packedVariables.get();

		fixture.afterCommit.clear();
		QuestEventRouter.DispatchResult result = fixture.dispatchKill(ELEMENTAL_LIGHT_NPC_ID);

		assertFalse(result.handled(), () -> "an extra kill must not match any kill route: " + result);
		assertEquals(packedAfterCeiling, fixture.packedVariables.get(),
			"an extra kill must not rewrite the saturated counters");
		assertEquals(List.of(), fixture.afterCommit,
			"an extra kill must not send a quest-state update");
	}

	@Test
	void theNextStageStillCountsWhileLaterStagesNeverCountEarly() throws Exception {
		Fixture fixture = new Fixture();
		// 第 1 段进行中：第 3 段目标不得提前计数（链式 SECTION 门控）。
		// While stage 1 runs, a stage-3 target must never count early (the chained SECTION gate).
		assertFalse(fixture.dispatchKill(POPOKU_NPC_ID).handled(),
			"a later-stage kill must not match any route before its stage opens");
		for (int kill = 1; kill <= STAGE_CEILING; kill++) {
			assertHandled(fixture.dispatchKill(ELEMENTAL_LIGHT_NPC_ID));
		}
		fixture.afterCommit.clear();
		assertHandled(fixture.dispatchKill(DARU_NPC_ID));

		assertEquals(1, fixture.counter("var1"), "SECTION_2 must keep counting");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			fixture.afterCommit, "real progress must still be announced");
	}

	/** 生产 IR + 假端口组成的击杀夹具。 / Kill fixture built from the production IR and fake ports. */
	private static final class Fixture {
		private final CompiledQuestDefinition definition;
		private final AtomicReference<QuestStatus> status = new AtomicReference<>(QuestStatus.START);
		private final AtomicInteger packedVariables = new AtomicInteger();
		private final List<AfterCommitAction> afterCommit = new ArrayList<>();
		private final QuestProductionDispatcher dispatcher;

		private Fixture() throws Exception {
			this.definition = ProductionQuestDefinitions.definition(QUEST_ID);
			QuestEventPort eventPort = (connection, playerId, questId, event) ->
				new QuestSnapshot(playerId, questId, status.get(), packedVariables.get(), Map.of())
					.withStartEligibility(QuestStartEligibility.allowed());
			QuestStatePort statePort = new QuestStatePort() {
				@Override
				public void apply(Connection connection, int playerId, QuestMutationPlan plan) {
					status.set(plan.nextStatus());
					packedVariables.set(plan.nextPackedVariables());
				}

				@Override
				public void publish(int playerId, QuestMutationPlan plan) {
				}
			};
			this.dispatcher = new QuestProductionDispatcher(
				new ImmutableQuestCatalog(List.of(definition)),
				new QuestExecutionCoordinator(new PlayerSerialExecutor()),
				eventPort, counterIncrementPort(), statePort,
				(action, snapshot, plan) -> afterCommit.add(action),
				Quest15546KillCounterSaturationFlowTest::connection, ignored -> { },
				new QuestRuntimeMetricsCollector());
		}

		private QuestEventRouter.DispatchResult dispatchKill(int npcId) {
			return dispatcher.dispatch(new QuestEvent.KillNpc(npcId), PLAYER_ID, QUEST_ID,
				QuestDispatchContract.EXCLUSIVE);
		}

		private int counter(String field) {
			return definition.definition().progressLayout().unpack(packedVariables.get()).get(field);
		}
	}

	/** 击杀路线只允许携带计数增量；其余动作由状态端口应用，这里不应出现物品/货币类动作。 */
	private static QuestActionPort counterIncrementPort() {
		return new QuestActionPort() {
			@Override
			public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) {
			}

			@Override
			public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
					List<QuestAction> actions) {
				actions.forEach(action -> assertInstanceOf(QuestAction.IncrementVariable.class, action,
					() -> "kill routes must only carry counter increments: " + action));
				return QuestTransactionParticipant.none();
			}
		};
	}

	private static Connection connection() {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
			new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
				case "getAutoCommit" -> true;
				case "setAutoCommit", "commit", "rollback", "close" -> null;
				default -> method.getReturnType() == boolean.class ? false : null;
			});
	}

	private static void assertHandled(QuestEventRouter.DispatchResult result) {
		result.owners().stream().map(QuestEventRouter.OwnerResult::failure)
			.filter(java.util.Objects::nonNull).findFirst().ifPresent(failure -> {
				throw failure;
			});
		assertTrue(result.handled(), result::toString);
	}
}
