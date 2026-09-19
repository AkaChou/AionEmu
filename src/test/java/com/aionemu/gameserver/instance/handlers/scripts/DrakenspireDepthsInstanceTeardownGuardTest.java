package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 龙脊深渊副本销毁后的延迟任务闸门。
 * Gate for delayed scene tasks that outlive a destroyed Drakenspire Depths instance.
 *
 * <p>背景：两个副本处理器（301390000 剧情副本与 301520000 任务副本）的场景延迟任务最长排到 87 秒后，
 * 副本销毁后仍会执行；此时世界实例已经拆除，底层生成必然 NPE（{@code mapRegion is null} / 纯
 * {@code NullPointerException}），日志里表现为「生成 NPC 209679/237219/237232/237217 时出错」。
 * Background: scene tasks of both handlers (story 301390000 and quest 301520000) run up to 87 seconds later and still
 * fire after teardown, when the world instance is gone and the underlying spawn NPEs ({@code mapRegion is null} or a
 * bare {@code NullPointerException}), logged as "生成 NPC 209679/237219/237232/237217 时出错".</p>
 */
class DrakenspireDepthsInstanceTeardownGuardTest {
	private static final Path QUEST_HANDLER_SOURCE = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DrakenspireDepthsQInstance.java");
	private static final Path STORY_HANDLER_SOURCE = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DrakenspireDepthsInstance.java");

	@Test
	void questInstanceSuppressesSpawnsAndNullNpcListsAfterTeardown() {
		DrakenspireDepthsQInstance handler = new DrakenspireDepthsQInstance();
		handler.isInstanceDestroyed = true;

		assertNull(handler.spawn(237219, 632.9971f, 788.14307f, 1596.5493f, (byte) 28),
			"the quest instance must not reach the world after teardown");
		assertDoesNotThrow(() -> handler.killNpc(null),
			"the null list returned after teardown must not be iterated");
	}

	@Test
	void storyInstanceSuppressesSpawnsAndNullNpcListsAfterTeardown() {
		DrakenspireDepthsInstance handler = new DrakenspireDepthsInstance();
		handler.isInstanceDestroyed = true;

		assertNull(handler.spawn(209679, 353.3932f, 185.61818f, 1684.2164f, (byte) 1),
			"the story instance must not reach the world after teardown");
		assertDoesNotThrow(() -> handler.killNpc(null),
			"the null list returned after teardown must not be iterated");
	}

	@Test
	void delayedDoorTasksUseTheTeardownSafeHelper() throws IOException {
		for (Path path : List.of(QUEST_HANDLER_SOURCE, STORY_HANDLER_SOURCE)) {
			String source = Files.readString(path);

			assertEquals(1, occurrences(source, "doors.get("),
				path + " must resolve doors only inside the guarded helper");
			assertEquals(13, occurrences(source, "openDoor("),
				path + " must route every delayed door opening through openDoor");
			assertTrue(source.contains("private void openDoor(int doorId) {"));
			assertTrue(source.contains("if (isInstanceDestroyed || doors == null) {"));
			assertTrue(source.contains("if (door != null) {"));
			assertFalse(source.contains("doors.get(267).setOpen(true)"));
			assertFalse(source.contains("doors.get(271).setOpen(true)"));
			assertTrue(source.contains("if (doors != null) {"),
				path + " must guard teardown cleanup when the door map was never installed");
		}
	}

	private static int occurrences(String source, String text) {
		int count = 0;
		int index = 0;
		while ((index = source.indexOf(text, index)) >= 0) {
			count++;
			index += text.length();
		}
		return count;
	}
}
