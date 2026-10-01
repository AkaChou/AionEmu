package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.RetailHuntLadderShape;
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
 * 15546《[每日]雷欧娜的委托》行阶梯的段饱和合同（定义来自生产驱动，真端单行四段并行形）。
 * Saturation contract for the row-ladder daily 15546 (production-driver definition, the retail
 * single row with four parallel segments).
 * <p>真端证据：{@code data_driven_quest.xml} 的 hunt 只有 {@code <data>} 一块，块内
 * {@code value0_progress_} 并列四组目标；客户端 {@code quest_monster.csv} 同侧登记
 * {@code Progress(SECTION_0==0; SECTION_1..4<4)}——四个计数并行推进（段满即封顶），行号 var0 全程保持 0，
 * 四段齐满的那一杀才把行号置 1 并进领奖。段满后同段再击杀不再命中任何路线：既不提交空事务，也不因
 * {@code sync-quest-state PACKET_ONLY} 下发 {@code SM_QUEST_ACTION}。</p>
 * <p>Retail evidence: the DD hunt row owns a single {@code <data>} block whose
 * {@code value0_progress_} lists four target groups side by side, and the client journal registers
 * {@code Progress(SECTION_0==0; SECTION_1..4<4)}. The four counters advance in parallel (each caps at
 * four) while the row index var0 stays 0; only the kill that saturates the whole row flips it to 1 and
 * enters reward. A kill of an already saturated segment matches no route — no empty transaction and no
 * spurious quest-update packet.</p>
 */
class Quest15546KillCounterSaturationFlowTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 15546;
	/** 第 1 段（星光精灵 T_ 变体，SECTION_1）目标。 / Segment 1 (SECTION_1) target. */
	private static final int ELEMENTAL_LIGHT_NPC_ID = 241656;
	/** 第 2 段（达鲁，SECTION_2）目标。 / Segment 2 (SECTION_2) target. */
	private static final int DARU_NPC_ID = 241664;
	/** 第 3 段（波波库，SECTION_3）目标。 / Segment 3 (SECTION_3) target. */
	private static final int POPOKU_NPC_ID = 241676;
	private static final int STAGE_CEILING = 4;

	@Test
	void fourKillsSaturateTheFirstSegmentAndAnnounceProgress() throws Exception {
		RetailHuntLadderShape.assertLadder(ProductionQuestDefinitions.definition(QUEST_ID),
			List.of(List.of(STAGE_CEILING, STAGE_CEILING, STAGE_CEILING, STAGE_CEILING)));
		Fixture fixture = new Fixture();
		for (int kill = 1; kill <= STAGE_CEILING; kill++) {
			final int progress = kill;
			fixture.afterCommit.clear();
			assertHandled(fixture.dispatchKill(ELEMENTAL_LIGHT_NPC_ID));
			assertEquals(progress, fixture.counter("var1"), "SECTION_1 progress after kill " + kill);
			assertEquals(0, fixture.counter("var0"), "the row index stays 0 while the row runs");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				fixture.afterCommit, "kill " + kill + " must announce the counter progress");
		}
	}

	@Test
	void extraKillsOfASaturatedSegmentDoNotAnnounceAQuestUpdate() throws Exception {
		Fixture fixture = new Fixture();
		for (int kill = 1; kill <= STAGE_CEILING; kill++) {
			assertHandled(fixture.dispatchKill(ELEMENTAL_LIGHT_NPC_ID));
		}
		// 其余段仍未计数：任务既不能完成，也不该因为第 1 段的超额击杀下发任何状态更新。
		// The other segments are still empty: the row cannot close, and the surplus kill must not
		// announce any state update.
		assertEquals(0, fixture.counter("var2"));
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
	void parallelSegmentsCountIndependentlyUntilTheRowCloses() throws Exception {
		Fixture fixture = new Fixture();
		// 同行的四段互不阻塞：第 3 段目标在第 1 段刚起步时照样计数（真端 block 内并列，客户端同侧）。
		// The four segments of one row never block each other: a segment-3 target counts right away
		// (the retail block lists them side by side and the client mirrors that).
		assertHandled(fixture.dispatchKill(POPOKU_NPC_ID));
		assertEquals(1, fixture.counter("var3"), "SECTION_3 counts while segment 1 is open");
		assertEquals(0, fixture.counter("var1"), "a segment-3 kill must not touch segment 1");

		for (int kill = 1; kill <= STAGE_CEILING; kill++) {
			assertHandled(fixture.dispatchKill(ELEMENTAL_LIGHT_NPC_ID));
		}
		fixture.afterCommit.clear();
		assertHandled(fixture.dispatchKill(DARU_NPC_ID));

		assertEquals(1, fixture.counter("var2"), "SECTION_2 must keep counting");
		assertEquals(QuestStatus.START, fixture.status.get(),
			"the row stays open until every segment saturates");
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

	/**
	 * 击杀路线只允许携带计数增量与行号推进；其余动作由状态端口应用，这里不应出现物品/货币类动作。
	 * Kill routes carry only counter increments and the row-index advance; item or currency actions
	 * stay out of the kill lane.
	 */
	private static QuestActionPort counterIncrementPort() {
		return new QuestActionPort() {
			@Override
			public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) {
			}

			@Override
			public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
					List<QuestAction> actions) {
				actions.forEach(action -> assertTrue(
					action instanceof QuestAction.IncrementVariable || action instanceof QuestAction.SetVariable,
					() -> "kill routes must only carry counter increments and the row advance: " + action));
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
