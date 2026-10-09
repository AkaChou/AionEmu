package com.aionemu.gameserver.world.knownlist;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Minion;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 守护灵已知列表契约：主仆对在两个方向上都恒为在范围内，且刷怪装配点绑定本实现。
 * Minion known-list contract: the master-minion pair is always in range in both directions,
 * and the spawner wires this implementation.
 * <p>背景（CL-001）：风之路/高移速下主人 KnownList 的 95m 可见距离曾把 minion 反复移除/加回，
 * 客户端「取消召唤/召唤了」成对刷屏；主人侧 forget/find 走 minion 的反向判定、minion 侧走
 * 自身距离判定，两个方向都必须对主人豁免。</p>
 * <p>Background (CL-001): on windstreams or at very high movement speed the master's 95m
 * visibility kept dropping and re-adding the minion, spamming paired "unsummon/summon"
 * messages; the master side consults the minion's reverse check while the minion side uses its
 * own range check, so both directions must exempt the master.</p>
 */
class MinionKnownListTest {

	@Test
	void masterIsAlwaysInRangeInBothDirections() throws Exception {
		Player master = new ObjenesisStd().newInstance(Player.class);
		Minion minion = new ObjenesisStd().newInstance(Minion.class);
		Field masterField = Minion.class.getDeclaredField("master");
		masterField.setAccessible(true);
		masterField.set(minion, master);

		MinionKnownList knownList = new MinionKnownList(minion);

		assertTrue(knownList.checkObjectInRange(master),
				"minion 侧对主人恒在范围内 / the minion side always keeps its master in range");
		assertTrue(knownList.checkReversedObjectInRange(master),
				"主人侧反向判定恒在范围内 / the master's reverse check always keeps its minion");
	}

	@Test
	void spawnerWiresMinionKnownListAndLeavesPetUntouched() throws Exception {
		String spawner = Files.readString(
				Path.of("src/main/java/com/aionemu/gameserver/spawnengine/VisibleObjectSpawner.java"));

		assertTrue(spawner.contains("minion.setKnownlist(new MinionKnownList(minion))"),
				"守护灵必须装配 MinionKnownList / the minion must be wired with MinionKnownList");
		assertTrue(spawner.contains("pet.setKnownlist(new PlayerAwareKnownList(pet))"),
				"宠物保持原感知规则 / the pet keeps the default awareness rules");
	}

	@Test
	void minionMovementStaysClientAuthored() throws Exception {
		String controller = Files.readString(
				Path.of("src/main/java/com/aionemu/gameserver/controllers/MinionController.java"));
		String service = Files.readString(
				Path.of("src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java"));

		// 真端架构：minion 移动由客户端本地模拟，服务端禁止跟随 tick / SM_MOVE 移动包 / 距离瞬拉。
		// 断言匹配代码形态（import/调用），避免命中本文档注释中的术语字样。
		// Retail architecture: minion movement is client-simulated; the server must not run follow
		// ticks, emit SM_MOVE segments or blink-teleport by distance. Assertions match code shapes
		// (imports/calls) so the prose terms in this controller's own doc comment never trip them.
		assertFalse(controller.contains("scheduleAtFixedRate"), "禁止重新引入服务端跟随任务 / no server-side follow tasks");
		assertFalse(controller.contains("serverpackets.SM_MOVE"), "禁止服务端下发 minion 移动包 / no server-driven SM_MOVE");
		assertFalse(controller.contains("teleportToPlayer(Player"), "禁止距离瞬拉 / no distance teleport");
		assertFalse(service.contains("startFollowing"), "召唤路径不得重启跟随调度 / spawning must not restart follow scheduling");
	}
}
