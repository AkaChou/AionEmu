package com.aionemu.gameserver.services.toypet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 飞行传送守护灵收起/恢复契约门禁：所有进入点必须收起守护灵，落地路径必须恢复。
 * Gate for the fly-teleport minion suspend/restore contract: every FLIGHT_TELEPORT entry
 * must hide the minion, and the landing path must bring it back.
 * <p>背景：飞行传送期间客户端持续上报位置（CM_MOVE_IN_AIR），主人 KnownList 曾因 95m 可见
 * 距离反复移除/加回 minion，客户端「取消召唤/召唤了」消息刷屏；现在统一在传送入口收起、
 * 落地恢复，本门禁防止将来新增入口遗漏收起调用。</p>
 * <p>Background: during a fly teleport the client streams positions (CM_MOVE_IN_AIR) and the
 * master's known list used to drop/re-add the minion at the 95m visibility edge, spamming the
 * "unsummon/summon" chat messages; entries now hide the minion and landing restores it, and this
 * gate stops future entry points from missing the hide call.</p>
 */
class FlyTeleportMinionSuspendGateTest {

	private static final Path MAIN_SOURCES = Path.of("src/main/java");
	// 以「.」前缀精确匹配进入状态，避免匹配到 unsetState(...)。 / Leading dot matches the entry only, never unsetState(...).
	private static final String ENTRY_STATE = ".setState(CreatureState.FLIGHT_TELEPORT)";
	private static final String SUSPEND_CALL = "suspendForFlyTeleport(player)";
	private static final String RESTORE_CALL = "restoreAfterFlyTeleport(player)";

	@Test
	void everyFlyTeleportEntrySuspendsTheMinion() throws IOException {
		List<String> offenders = new ArrayList<>();
		try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				String source = Files.readString(file);
				if (source.contains(ENTRY_STATE) && !source.contains(SUSPEND_CALL)) {
					offenders.add(file.toString());
				}
			}
		}
		assertTrue(offenders.isEmpty(), "FLIGHT_TELEPORT 入口缺少守护灵收起调用 / entries missing the minion suspend: " + offenders);
	}

	@Test
	void landingRestoresTheMinionAfterTheSummon() throws IOException {
		String controller = Files.readString(
				Path.of("src/main/java/com/aionemu/gameserver/controllers/PlayerController.java"));
		int summonRestore = controller.indexOf("SummonsService.restoreAfterTeleport(player)");
		int minionRestore = controller.indexOf(RESTORE_CALL);

		assertTrue(summonRestore >= 0);

		assertTrue(minionRestore > summonRestore, "守护灵恢复应紧邻召唤兽恢复之后 / the minion restore must follow the summon restore");
	}

	@Test
	void suspendHidesTheMinionWithoutReleasingIt() throws IOException {
		String service = Files.readString(
				Path.of("src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java"));
		String suspend = methodBody(service, "public void suspendForFlyTeleport");
		String restore = methodBody(service, "public void restoreAfterFlyTeleport");

		assertTrue(suspend.contains("minion.isSpawned()"));
		assertTrue(suspend.contains("world().despawn(minion)"));
		assertFalse(suspend.contains("setMinion(null)"), "收起不是收回：不得清空主人引用 / hiding is not releasing: the master reference must survive");

		assertTrue(restore.contains("!minion.isSpawned()"));
		assertTrue(restore.contains("world().setPosition(minion, player.getWorldId(), player.getInstanceId(),"));
		assertTrue(restore.contains("world().spawn(minion)"));
	}

	private static String methodBody(String source, String signature) {
		int start = source.indexOf(signature);
		assertTrue(start >= 0, "缺少方法 / missing method: " + signature);
		int end = source.indexOf("\n\t}", start);
		return source.substring(start, end < 0 ? source.length() : end);
	}
}
