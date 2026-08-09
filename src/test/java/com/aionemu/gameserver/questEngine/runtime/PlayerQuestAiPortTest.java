package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestNpcEmotion;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real {@link QuestAiPort}: AI 命令通过 slot 只寻址本任务 spawn 的权威 handle,
 * 绝不凭 templateId;slot 无 handle 或玩家离线时 best-effort 跳过。
 */
class PlayerQuestAiPortTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;
	private static final int WORLD = 310040000;

	@Test
	void aiCommandsTargetOnlyTheAuthoritativeSlotHandle() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Npc guardian = npc();
		registry.register(snapshot(), "guardian", guardian);
		List<PlayerQuestAiPort.Command> issued = new ArrayList<>();
		Player player = player();
		VisibleObject target = npc();
		PlayerQuestAiPort port = new PlayerQuestAiPort(playerId -> player, registry,
			(npc, p, resolvedTarget, command, argument) -> {
				issued.add(command);
				return true;
			}, objectId -> target,
			(p, npc, questId, zone) -> CompletableFuture.completedFuture(null));

		QuestSnapshot snapshot = snapshot().withTargetObjectId(target.getObjectId());
		assertTrue(port.startFollow(snapshot, plan(), "guardian"));
		assertTrue(port.stopFollow(snapshot, plan(), "guardian"));
		assertTrue(port.attackTarget(snapshot, plan(), "guardian"));
		assertTrue(port.startWalking(snapshot, plan(), "guardian"));
		assertEquals(List.of(
			PlayerQuestAiPort.Command.START_FOLLOW,
			PlayerQuestAiPort.Command.STOP_FOLLOW,
			PlayerQuestAiPort.Command.ATTACK_TARGET,
			PlayerQuestAiPort.Command.START_WALKING), issued);
	}

	@Test
	void attackUsesFrozenSnapshotTargetWhenLivePlayerTargetDrifts() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		registry.register(snapshot(), "guardian", npc(77));
		Player player = player();
		VisibleObject frozenTarget = npc(88);
		player.setTarget(npc(99));
		VisibleObject[] received = {null};
		PlayerQuestAiPort port = new PlayerQuestAiPort(playerId -> player, registry,
			(npc, p, target, command, argument) -> {
				received[0] = target;
				return true;
			}, objectId -> objectId == 88 ? frozenTarget : null,
			(p, npc, questId, zone) -> CompletableFuture.completedFuture(null));

		assertTrue(port.attackTarget(snapshot().withTargetObjectId(88), plan(), "guardian"));
		assertSame(frozenTarget, received[0]);
	}

	@Test
	void attackNpcTemplateResolvesTheCurrentWorldNpcAndIssuesDedicatedCommand() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Npc guardian = npc(77);
		Npc kimeia = npc(204044);
		registry.register(snapshot(), "defense-mob", guardian);
		Player player = player();
		VisibleObject[] received = {null};
		PlayerQuestAiPort.Command[] command = {null};
		PlayerQuestAiPort port = new PlayerQuestAiPort(playerId -> player, registry,
			(npc, p, target, issued, argument) -> {
				received[0] = target;
				command[0] = issued;
				return true;
			}, objectId -> null,
			(p, npc, questId, zone) -> CompletableFuture.completedFuture(null),
			(p, npc, questId, x, y, z) -> CompletableFuture.completedFuture(null),
			(p, task) -> { }, (p, npc) -> { },
			(p, templateId) -> templateId == 204044 ? kimeia : null);

		assertTrue(port.attackNpcTemplate(snapshot(), plan(), "defense-mob", 204044));
		assertSame(kimeia, received[0]);
		assertEquals(PlayerQuestAiPort.Command.ATTACK_NPC_TEMPLATE, command[0]);
	}

	@Test
	void emotionAndFollowZoneRouteToOwnedNpcAndCleanupCancelsWatcher() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Npc guardian = npc();
		registry.register(snapshot(), "guardian", guardian);
		List<String> calls = new ArrayList<>();
		CompletableFuture<Void> watcher = new CompletableFuture<>();
		PlayerQuestAiPort port = new PlayerQuestAiPort(playerId -> player(), registry,
			(npc, player, target, command, argument) -> {
				calls.add(command.name());
				return true;
			}, objectId -> null,
			(player, npc, questId, zone) -> {
				calls.add("WATCH:" + questId + ":" + zone);
				return watcher;
			});

		assertTrue(port.broadcastEmotion(snapshot(), plan(), "guardian", QuestNpcEmotion.START_EMOTE2));
		assertTrue(port.watchFollowZone(snapshot(), plan(), "guardian", "DF2_ITEMUSEAREA_Q2333"));
		assertEquals(List.of("BROADCAST_START_EMOTE2", "WATCH:1001:DF2_ITEMUSEAREA_Q2333"), calls);

		registry.cleanup(PLAYER_ID, QUEST_ID);
		assertTrue(watcher.isCancelled());
	}

	@Test
	void residentInteractionNpcBroadcastsClosedSetEmotionTowardThePlayer() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Npc resident = npc(799768);
		Player player = player();
		List<String> calls = new ArrayList<>();
		PlayerQuestAiPort port = new PlayerQuestAiPort(playerId -> player, registry,
			(npc, p, target, command, argument) -> {
				calls.add(npc.getObjectId() + ":" + target.getObjectId() + ":" + command + ":" + argument);
				return true;
			}, objectId -> objectId == resident.getObjectId() ? resident : null,
			(p, npc, questId, zone) -> CompletableFuture.completedFuture(null));

		assertTrue(port.broadcastInteractionEmotion(
			snapshot().withInteractionObjectId(resident.getObjectId()), plan(), QuestNpcEmotion.NO));
		assertEquals(List.of(resident.getObjectId() + ":" + player.getObjectId()
			+ ":BROADCAST_INTERACTION_EMOTION:NO"), calls);
	}

	@Test
	void residentInteractionNpcCanFollowPlayerToCoordinateTarget() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Npc escort = npc(799036);
		Player player = player();
		List<String> calls = new ArrayList<>();
		CompletableFuture<Void> watcher = new CompletableFuture<>();
		PlayerQuestAiPort port = new PlayerQuestAiPort(playerId -> player, registry,
			(npc, p, target, command, argument) -> {
				calls.add(command.name());
				return true;
			}, objectId -> objectId == escort.getObjectId() ? escort : null,
			(p, npc, questId, zone) -> CompletableFuture.completedFuture(null),
			(p, npc, questId, x, y, z) -> {
				calls.add("POINT:" + x + ":" + y + ":" + z);
				return watcher;
			}, (p, task) -> calls.add("REGISTER"), (p, npc) -> calls.add("NPC_INFO"));

		assertTrue(port.startFollowCurrentTargetToPoint(snapshot().withInteractionObjectId(escort.getObjectId()),
			plan(), 292.63895f, 489.47452f, 574.2429f));
		assertEquals(List.of("NPC_INFO", "START_FOLLOW", "POINT:292.63895:489.47452:574.2429", "REGISTER"), calls);
		assertFalse(watcher.isCancelled());
	}

	@Test
	void taskOwnedNpcCanWatchFollowCoordinateAndCleanupCancelsWatcher() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Npc survivor = npc(204830);
		registry.register(snapshot(), "survivor", survivor);
		List<String> calls = new ArrayList<>();
		CompletableFuture<Void> watcher = new CompletableFuture<>();
		PlayerQuestAiPort port = new PlayerQuestAiPort(playerId -> player(), registry,
			(npc, player, target, command, argument) -> true, objectId -> null,
			(player, npc, questId, zone) -> CompletableFuture.completedFuture(null),
			(player, npc, questId, x, y, z) -> {
				calls.add("POINT:" + x + ":" + y + ":" + z);
				return watcher;
			}, (player, task) -> calls.add("REGISTER"), (player, npc) -> { });

		assertTrue(port.watchFollowCoordinate(snapshot(), plan(), "survivor",
			213.177994f, 370.8797f, 503.3588f));
		assertEquals(List.of("POINT:213.178:370.8797:503.3588"), calls);

		registry.cleanup(PLAYER_ID, QUEST_ID);
		assertTrue(watcher.isCancelled());
	}

	@Test
	void aiCommandSkipsWhenSlotHasNoHandle() {
		PlayerQuestAiPort port = new PlayerQuestAiPort(playerId -> player(), new QuestSpawnRegistry(),
			(npc, p, target, command, argument) -> true);

		assertFalse(port.startFollow(snapshot(), plan(), "never-spawned"));
		assertFalse(port.attackTarget(snapshot(), plan(), "never-spawned"));
	}

	@Test
	void aiCommandSkipsWhenPlayerLoggedOut() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		registry.register(snapshot(), "guardian", npc());
		PlayerQuestAiPort port = new PlayerQuestAiPort(playerId -> null, registry,
			(npc, p, target, command, argument) -> true);

		assertFalse(port.startFollow(snapshot(), plan(), "guardian"));
	}

	@Test
	void aiPortFailsClosedOnBlankSlot() {
		PlayerQuestAiPort port = new PlayerQuestAiPort(playerId -> player(), new QuestSpawnRegistry(),
			(npc, p, target, command, argument) -> true);

		assertThrows(IllegalArgumentException.class, () -> port.startFollow(snapshot(), plan(), ""));
		assertThrows(IllegalArgumentException.class, () -> port.startWalking(snapshot(), plan(), " "));
	}

	private static QuestSnapshot snapshot() {
		return new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of(), Map.of());
	}

	private static QuestMutationPlan plan() {
		return new QuestMutationPlan(QUEST_ID, QuestStatus.COMPLETE, 0, List.of(), List.of());
	}

	private static Player player() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		player.setPosition(new WorldPosition(WORLD));
		return player;
	}

	private static Npc npc() {
		return npc(77);
	}

	private static Npc npc(int id) {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		try {
			java.lang.reflect.Field objectId = com.aionemu.gameserver.model.gameobjects.AionObject.class
				.getDeclaredField("objectId");
			objectId.setAccessible(true);
			objectId.set(npc, Integer.valueOf(id));
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
		npc.setPosition(new WorldPosition(WORLD));
		return npc;
	}
}
