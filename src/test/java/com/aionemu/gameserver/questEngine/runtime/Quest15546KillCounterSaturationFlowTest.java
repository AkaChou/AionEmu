package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
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
 * 15546《[每日]雷欧娜的委托》计数器饱和后的额外击杀合同。
 * Oversaturation contract for the four-counter daily 15546: while other families are still incomplete, a kill of an
 * already saturated family must not change quest state and must not announce a quest update to the client.
 *
 * <p>回归背景：四个 SECTION 各自计数上限为 4（客户端 {@code Progress(SECTION_n<4)}）。原先第 4 次击杀由
 * "收口"路线（{@code variable-at-least 3 -> set 4}）写入；该路线在计数器到达 4 之后仍然命中，
 * 于是超额击杀会提交一笔状态未变化的空事务，并因 {@code sync-quest-state PACKET_ONLY} 下发一次
 * {@code SM_QUEST_ACTION}，客户端因此显示"任务更新"而进度并未变化。</p>
 */
class Quest15546KillCounterSaturationFlowTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 15546;
	/** 星光精灵 T_ 变体：Iluma 生产刷怪数据里真实刷新的第 1 族目标。 */
	private static final int ELEMENTAL_LIGHT_NPC_ID = 241656;
	/** 第 2 族（达鲁）目标，用于验证其他族未满时第 1 族的超额击杀。 */
	private static final int DARU_NPC_ID = 241664;
	private static final int COUNTER_CEILING = 4;

	@Test
	void fourKillsSaturateTheCounterAndAnnounceProgress() throws Exception {
		Fixture fixture = new Fixture();
		for (int kill = 1; kill <= COUNTER_CEILING; kill++) {
			fixture.afterCommit.clear();
			assertHandled(fixture.dispatchKill(ELEMENTAL_LIGHT_NPC_ID));
			assertEquals(kill, fixture.counter("var1"), "SECTION_1 progress after kill " + kill);
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				fixture.afterCommit, "kill " + kill + " must announce the counter progress");
		}
	}

	@Test
	void extraKillsOfASaturatedFamilyDoNotAnnounceAQuestUpdate() throws Exception {
		Fixture fixture = new Fixture();
		for (int kill = 1; kill <= COUNTER_CEILING; kill++) {
			assertHandled(fixture.dispatchKill(ELEMENTAL_LIGHT_NPC_ID));
		}
		// 第 2 族仍未计数：任务既不能完成，也不该因为第 1 族的超额击杀下发任何状态更新。
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
	void incompleteFamiliesStillCountWhileTheFirstOneIsSaturated() throws Exception {
		Fixture fixture = new Fixture();
		for (int kill = 1; kill <= COUNTER_CEILING; kill++) {
			assertHandled(fixture.dispatchKill(ELEMENTAL_LIGHT_NPC_ID));
		}
		fixture.afterCommit.clear();
		assertHandled(fixture.dispatchKill(DARU_NPC_ID));

		assertEquals(1, fixture.counter("var2"), "SECTION_2 must keep counting");
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
			this.definition = definition();
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

	private static CompiledQuestDefinition definition() throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/15546.xml";
		try (InputStream input = Quest15546KillCounterSaturationFlowTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + resource);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
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
