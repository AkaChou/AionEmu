package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestActionType;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.zone.ZoneName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestEngineLegacyDispatchBoundaryTest {
	private static final int TYPED_QUEST = 991001;
	private static final int LEGACY_QUEST = 991002;
	private static final int PLAYER_ID = 7;

	private final QuestEngine engine = new QuestEngine();

	@AfterEach
	void cleanup() {
		engine.clear();
	}

	@Test
	void typedZoneOwnerDoesNotSuppressUnrelatedLegacyOwner() throws Exception {
		CompiledQuestDefinition typedDefinition = QuestDsl.quest(TYPED_QUEST)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("advanced", project(QuestStatus.START, vars("var0", 1)))
			.on(new QuestEvent.EnterZone("AKARIOS_VILLAGE_210010000"))
			.from("started").goTo("advanced")
			.compile();
		setProductionDispatcher(dispatcher(List.of(typedDefinition)));

		AtomicBoolean legacyCalled = new AtomicBoolean();
		engine.addQuestHandler(new QuestHandler(LEGACY_QUEST) {
			@Override
			public void register() {
			}

			@Override
			public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
				legacyCalled.set(true);
				return true;
			}
		});
		ZoneName zone = ZoneName.get("AKARIOS_VILLAGE_210010000");
		engine.registerOnEnterZone(zone, LEGACY_QUEST);

		assertTrue(engine.onEnterZone(new QuestEnv(null, player(), 0, 0), zone));
		assertTrue(legacyCalled.get(), "a handled typed owner must not swallow another quest's legacy event");
	}

	@Test
	void typedEntrypointsFailClosedForMissingEnvironmentOrRequiredActors() {
		assertFalse(engine.onDialog(null));
		assertFalse(engine.onKill(null));
		assertFalse(engine.onAttack(null));
		assertFalse(engine.onKillRanked(null, null));
		assertFalse(engine.onKillInWorld(null, 210010000));
		assertFalse(engine.onEnterZone(null, ZoneName.get("AKARIOS_VILLAGE_210010000")));
		assertFalse(engine.onLeaveZone(null, ZoneName.get("AKARIOS_VILLAGE_210010000")));
		assertFalse(engine.onMovieEnd(null, 1));
		assertFalse(engine.onUseSkill(null, 1));
		assertFalse(engine.onHouseItemUseEvent(null, 1));
		assertEquals(HandlerResult.FAILED, engine.onItemUseEvent(null, null));
		assertFalse(engine.onCanAct(null, 1, QuestActionType.ITEM_USE));
		assertEquals(HandlerResult.FAILED, engine.onBonusApplyEvent(null, null, List.of()));

		engine.onLvlUp(null);
		engine.onEnterZoneMissionEnd(null);
		engine.onDie(null);
		engine.onLogOut(null);
		engine.onNpcReachTarget(null);
		engine.onNpcLostTarget(null);
		engine.onPassFlyingRing(null, "RING");
		engine.onEnterWorld(null);
		engine.onItemGet(null, 1);
		engine.onQuestTimerEnd(null);
		engine.onInvisibleTimerEnd(null);
		engine.onFailCraft(null, 1);
		engine.onEquipItem(null, 1);
		engine.onDredgionReward(null);
		engine.onEnterWindStream(null, 1);
		assertFalse(engine.onAtDistance(null));
	}

	private void setProductionDispatcher(QuestProductionDispatcher dispatcher) throws ReflectiveOperationException {
		Field field = QuestEngine.class.getDeclaredField("productionDispatcher");
		field.setAccessible(true);
		field.set(engine, dispatcher);
	}

	private static QuestProductionDispatcher dispatcher(List<CompiledQuestDefinition> definitions) {
		QuestEventPort events = (connection, playerId, questId, event) ->
			new QuestSnapshot(playerId, questId, QuestStatus.START, 0, Map.of(), Map.of());
		QuestActionPort actions = new QuestActionPort() {
			@Override
			public void preflight(Connection connection, QuestSnapshot snapshot,
				List<com.aionemu.gameserver.questEngine.definition.QuestAction> requiredActions) {
			}

			@Override
			public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
				List<com.aionemu.gameserver.questEngine.definition.QuestAction> requiredActions) {
				return QuestTransactionParticipant.none();
			}
		};
		QuestStatePort state = new QuestStatePort() {
			@Override
			public void apply(Connection connection, int playerId, QuestMutationPlan plan) {
			}

			@Override
			public void publish(int playerId, QuestMutationPlan plan) {
			}
		};
		return new QuestProductionDispatcher(new ImmutableQuestCatalog(definitions),
			new QuestExecutionCoordinator(new PlayerSerialExecutor()), events, actions, state,
			(action, snapshot, plan) -> { }, QuestEngineLegacyDispatchBoundaryTest::connection,
			ignored -> { }, new QuestRuntimeMetricsCollector());
	}

	private static Player player() throws ReflectiveOperationException {
		Player player = new ObjenesisStd().newInstance(Player.class);
		Field objectId = AionObject.class.getDeclaredField("objectId");
		objectId.setAccessible(true);
		objectId.set(player, PLAYER_ID);
		return player;
	}

	private static Connection connection() {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
			new Class<?>[] { Connection.class }, (proxy, method, args) -> switch (method.getName()) {
				case "getAutoCommit" -> true;
				case "setAutoCommit", "commit", "rollback", "close" -> null;
				default -> method.getReturnType() == boolean.class ? false
					: method.getReturnType() == int.class ? 0
					: method.getReturnType() == long.class ? 0L : null;
			});
	}
}
