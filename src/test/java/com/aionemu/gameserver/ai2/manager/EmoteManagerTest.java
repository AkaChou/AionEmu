package com.aionemu.gameserver.ai2.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.knownlist.KnownList;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

/**
 * 脱战“放弃追踪”提示的可见性闸门测试。
 * Visibility-gate tests for the disengage "gives up pursuit" notice.
 */
class EmoteManagerTest {

	/** 被测源码路径 / Source under test. */
	private static final Path EMOTE_MANAGER =
			Path.of("src/main/java/com/aionemu/gameserver/ai2/manager/EmoteManager.java");

	@Test
	void disengagingNpcStaysNotifiedWhileThePlayerStillKnowsIt() {
		Player player = player(21);
		Npc npc = npc(22);
		TestKnownList knownList = new TestKnownList(player);
		player.setKnownlist(knownList);
		knownList.addKnown(npc);

		assertTrue(EmoteManager.canSeeDisengagingNpc(npc, player));
	}

	/**
	 * 报障回归：克罗坦要塞防空眼 276225 脱战时玩家多半已飞出数百米，提示必须被拦下。
	 * Reported regression: the Krotan sentinel eye 276225 usually disengages long after the player flew away, so
	 * the notice must be suppressed.
	 */
	@Test
	void disengagingNpcIsNotNotifiedAfterLeavingThePlayerKnownList() {
		Player player = player(21);
		Npc npc = npc(22);
		TestKnownList knownList = new TestKnownList(player);
		player.setKnownlist(knownList);
		knownList.addKnown(npc);
		knownList.getKnownObjects().remove(npc.getObjectId());

		assertFalse(EmoteManager.canSeeDisengagingNpc(npc, player),
				"NPC 已离开玩家已知列表时不得下发“放弃追踪”。"
						+ " / The notice must not be sent once the NPC left the player's known list.");
	}

	@Test
	void disengagingNpcToleratesPlayerWithoutKnownList() {
		Player player = player(21);
		Npc npc = npc(22);

		assertFalse(EmoteManager.canSeeDisengagingNpc(npc, player));
	}

	/**
	 * 源码闸门：消息下发必须留在可见性判定之内，且提示本身不能整体删除。
	 * Source gate: the notice send must stay behind the visibility gate and must not be dropped entirely.
	 * @throws IOException 读取源码失败 / when the source cannot be read
	 */
	@Test
	void stopAttackingSendsNoticeOnlyBehindTheVisibilityGate() throws IOException {
		String body = methodBody(Files.readString(EMOTE_MANAGER),
				"public static final void emoteStopAttacking(Npc owner)");

		assertTrue(body.contains("canSeeDisengagingNpc(owner, playerTarget)"),
				"脱战提示必须先判定目标玩家是否还看得见该 NPC。"
						+ " / The disengage notice must be gated on the player still seeing the NPC.");
		assertTrue(body.contains("STR_UI_COMBAT_NPC_RETURN"),
				"闸门内仍要下发真端 1300039 提示，不能把提示整体删掉。"
						+ " / The gate must keep the retail 1300039 notice for players that still see the NPC.");
	}

	private static Player player(int objectId) {
		return object(Player.class, objectId);
	}

	private static Npc npc(int objectId) {
		return object(Npc.class, objectId);
	}

	private static <T extends AionObject> T object(Class<T> type, int objectId) {
		try {
			T object = new ObjenesisStd().newInstance(type);
			Field field = AionObject.class.getDeclaredField("objectId");
			field.setAccessible(true);
			field.set(object, objectId);
			return object;
		} catch (ReflectiveOperationException ex) {
			throw new AssertionError(ex);
		}
	}

	/**
	 * 按大括号配对提取指定方法体的源码片段。
	 * Extracts a method body by brace matching.
	 * @param source 源码 / source
	 * @param signature 方法签名 / method signature
	 * @return 方法体源码；找不到时返回空串（使闸门失败） / the method body, or an empty string when missing
	 */
	private static String methodBody(String source, String signature) {
		int start = source.indexOf(signature);
		if (start < 0) {
			return "";
		}
		int open = source.indexOf('{', start);
		if (open < 0) {
			return "";
		}
		int depth = 0;
		for (int i = open; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '{') {
				depth++;
			} else if (c == '}' && --depth == 0) {
				return source.substring(open, i + 1);
			}
		}
		return "";
	}

	/**
	 * 测试用已知列表：只写入已知对象映射，不触发控制器可见性通知。
	 * Test known list: writes the known-object map only, skipping the controller visibility notification.
	 */
	private static final class TestKnownList extends KnownList {

		private TestKnownList(VisibleObject owner) {
			super(owner);
		}

		private void addKnown(VisibleObject object) {
			getKnownObjects().put(object.getObjectId(), object);
		}
	}
}
