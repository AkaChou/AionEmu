package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.model.templates.rewards.BonusType;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 事件奖励加成（bonus-apply）运行时触发点。
 * <p>
 * 活动任务在 REWARD 状态收到物品 bonus（80016/80018 的 MOVIE、80034-80037 的 LUNAR）
 * 时追加奖励并播放影片（见 docs/quest/claims/80016.md、80034.md）。DSL 早已支持
 * {@code bonus-apply} 事件（QuestDefinitionXmlCompiler），但 QuestEngine 没有触发点：
 * 安装白名单会拒绝该事件类型，{@link QuestEngine#onBonusApplyEvent} 只分发 legacy handler。
 * 本测试验证补全后的触发点：白名单接受、按 bonus-type 路由、typed owner 主张后 legacy 被跳过。
 */
class QuestBonusApplyDispatchTest {
	private static final int TYPED_QUEST = 800016;
	private static final int LEGACY_QUEST = 800116;
	private static final int PLAYER_ID = 7;

	private final QuestEngine engine = new QuestEngine();

	@AfterEach
	void cleanup() {
		engine.clear();
	}

	@Test
	void installWhitelistAcceptsBonusApplyTransitions() throws Exception {
		// 安装校验白名单必须接受 bonus-apply，否则迁移后的活动任务无法启动。
		installProductionDefinitions(engine, new ImmutableQuestCatalog(List.of(typedDefinition())));

		assertTrue(engine.isProductionOwner(TYPED_QUEST));
	}

	@Test
	void bonusApplyRoutesAreIndexedByBonusType() {
		QuestProductionDispatcher dispatcher = dispatcher(List.of(typedDefinition()), new ArrayList<>(),
			new ArrayList<>());

		assertTrue(dispatcher.hasRoutes(new QuestEvent.BonusApply("MOVIE")));
		assertTrue(dispatcher.hasRoutes(new QuestEvent.BonusApply("MOVIE"), TYPED_QUEST));
		assertFalse(dispatcher.hasRoutes(new QuestEvent.BonusApply("LUNAR"), TYPED_QUEST));
	}

	@Test
	void bonusApplyDispatchCommitsOwnerAndRunsMovieAfterCommit() {
		List<String> calls = new ArrayList<>();
		List<AfterCommitAction> afterCommit = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(List.of(typedDefinition()), calls, afterCommit);

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(
			new QuestEvent.BonusApply("MOVIE"), PLAYER_ID, 0, QuestDispatchContract.BROADCAST);

		assertTrue(result.consumed());
		assertTrue(result.claimed());
		assertEquals(List.of(TYPED_QUEST), result.claimedOwners().stream().toList());
		// 自环过渡不改变状态/变量，requiresStatePersistence=false，故不执行 state/publish
		// （与 QuestExecutionCoordinatorTest.protocolOnlySelfTransitionDoesNotRewriteStateOrRunTerminalCleanup 一致）。
		assertEquals(List.of("setAutoCommit:false", "preflight", "apply", "commit", "close"), calls);
		assertEquals(1, afterCommit.stream().filter(AfterCommitAction.PlayMovie.class::isInstance).count());
	}

	@Test
	void bonusApplyWrongTypeLeavesOwnerUnclaimed() {
		List<AfterCommitAction> afterCommit = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(List.of(typedDefinition()), new ArrayList<>(),
			afterCommit);

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(
			new QuestEvent.BonusApply("LUNAR"), PLAYER_ID, 0, QuestDispatchContract.BROADCAST);

		assertFalse(result.consumed());
		assertTrue(result.owners().isEmpty());
		assertTrue(afterCommit.isEmpty());
	}

	@Test
	void engineOnBonusApplyRunsLegacyWhenNoTypedOwnerMatches() throws Exception {
		setProductionDispatcher(dispatcher(List.of(typedDefinition()), new ArrayList<>(), new ArrayList<>()));
		AtomicBoolean legacyCalled = new AtomicBoolean();
		engine.addQuestHandler(new QuestHandler(LEGACY_QUEST) {
			@Override
			public void register() {
			}

			@Override
			public HandlerResult onBonusApplyEvent(QuestEnv env, BonusType bonusType, List<QuestItems> rewardItems) {
				legacyCalled.set(true);
				return HandlerResult.SUCCESS;
			}
		});
		engine.registerOnBonusApply(LEGACY_QUEST, BonusType.LUNAR);

		QuestEnv env = new QuestEnv(null, player(), LEGACY_QUEST, 0);
		HandlerResult result = engine.onBonusApplyEvent(env, BonusType.LUNAR, new ArrayList<>());

		assertTrue(legacyCalled.get(), "an unmatched typed owner must not suppress a legacy bonus-apply owner");
		assertEquals(HandlerResult.SUCCESS, result);
	}

	/**
	 * 80016 语义：REWARD 状态收到 MOVIE bonus 时追加 Hat Box 奖励并播放影片。
	 * 确定性影片在此表达；50% 随机选择需要 DSL 随机语义（A11 范围）。
	 */
	private static CompiledQuestDefinition typedDefinition() {
		return QuestDsl.quest(TYPED_QUEST)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(new QuestEvent.BonusApply("MOVIE")).from("reward")
			.when(QuestDsl.statusIs(QuestStatus.REWARD))
			.then(QuestDsl.grantReward("item", 188051106, 1))
			.goTo("reward")
			.afterCommit(QuestDsl.playMovie(103))
			.compile();
	}

	private static void installProductionDefinitions(QuestEngine engine, QuestCatalog catalog)
			throws ReflectiveOperationException {
		Method method = QuestEngine.class.getDeclaredMethod("installProductionDefinitions", QuestCatalog.class);
		method.setAccessible(true);
		method.invoke(engine, catalog);
	}

	private void setProductionDispatcher(QuestProductionDispatcher dispatcher) throws ReflectiveOperationException {
		Field field = QuestEngine.class.getDeclaredField("productionDispatcher");
		field.setAccessible(true);
		field.set(engine, dispatcher);
	}

	private static QuestProductionDispatcher dispatcher(List<CompiledQuestDefinition> definitions,
			List<String> calls, List<AfterCommitAction> afterCommit) {
		return new QuestProductionDispatcher(new ImmutableQuestCatalog(definitions),
			new QuestExecutionCoordinator(new PlayerSerialExecutor()),
			(connection, playerId, questId, event) ->
				new QuestSnapshot(playerId, questId, QuestStatus.REWARD, 1, Map.of()),
			recordingActions(calls), recordingState(calls),
			(action, snapshot, plan) -> afterCommit.add(action), () -> connection(calls),
			ignored -> { }, new QuestRuntimeMetricsCollector());
	}

	private static QuestActionPort recordingActions(List<String> calls) {
		return new QuestActionPort() {
			@Override
			public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) {
				calls.add("preflight");
			}

			@Override
			public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
					List<QuestAction> actions) {
				calls.add("apply");
				return QuestTransactionParticipant.none();
			}
		};
	}

	private static QuestStatePort recordingState(List<String> calls) {
		return new QuestStatePort() {
			@Override
			public void apply(Connection connection, int playerId, QuestMutationPlan plan) {
				calls.add("state");
			}

			@Override
			public void publish(int playerId, QuestMutationPlan plan) {
				calls.add("publish");
			}
		};
	}

	private static Connection connection(List<String> calls) {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
			new Class<?>[] { Connection.class }, (proxy, method, args) -> switch (method.getName()) {
				case "getAutoCommit" -> true;
				case "setAutoCommit" -> {
					calls.add("setAutoCommit:" + args[0]);
					yield null;
				}
				case "commit" -> {
					calls.add("commit");
					yield null;
				}
				case "rollback" -> {
					calls.add("rollback");
					yield null;
				}
				case "close" -> {
					calls.add("close");
					yield null;
				}
				default -> null;
			});
	}

	private static Player player() throws ReflectiveOperationException {
		Player player = new ObjenesisStd().newInstance(Player.class);
		Field objectId = AionObject.class.getDeclaredField("objectId");
		objectId.setAccessible(true);
		objectId.set(player, PLAYER_ID);
		return player;
	}
}
