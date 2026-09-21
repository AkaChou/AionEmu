package com.aionemu.gameserver.utils.chathandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;

import org.junit.jupiter.api.Test;

/**
 * 多别名（含中文别名）解析与参数切分守卫。
 * Gate for multi-alias (including Chinese aliases) resolution and argument splitting.
 */
class ChatCommandAliasTest {

	/**
	 * 测试用命令：主别名 {@code dropinfo} + 中文别名 {@code 掉落}。
	 * Test command: primary alias {@code dropinfo} plus the Chinese alias {@code 掉落}.
	 */
	private static final class FakeCommand extends AdminCommand {

		FakeCommand() {
			super("dropinfo", "掉落");
		}

		@Override
		public void execute(Player player, String... params) {
		}
	}

	private final FakeCommand command = new FakeCommand();

	/**
	 * 中文别名必须能被识别，且主别名保持不变。
	 * The Chinese alias must resolve while the primary alias stays unchanged.
	 */
	@Test
	void resolvesAliases() {
		assertEquals("dropinfo", command.getAlias());
		assertEquals(List.of("dropinfo", "掉落"), command.getAliases());
		assertEquals("掉落", command.resolveAlias("掉落"));
		assertEquals("掉落", command.resolveAlias("掉落 123"));
		assertEquals("dropinfo", command.resolveAlias("dropinfo 123"));
		assertEquals("dropinfo", command.resolveAlias("unknown alias"));
	}

	/**
	 * 参数必须在匹配到的别名之后切分。
	 * Arguments must be sliced after the matched alias.
	 */
	@Test
	void splitsArgumentsAfterMatchedAlias() {
		assertEquals("", command.argumentsOf("掉落"));
		assertEquals("123", command.argumentsOf("掉落 123"));
		assertEquals("123", command.argumentsOf("掉落   123"));
		assertEquals("npc 123", command.argumentsOf("dropinfo npc 123"));
	}

	/**
	 * 访问等级按别名独立绑定。
	 * Access levels are bound per alias.
	 */
	@Test
	void keepsAccessLevelPerAlias() {
		assertNull(command.getLevel("掉落"));
		command.setAccessLevel("dropinfo", (byte) 3);
		command.setAccessLevel("掉落", (byte) 0);
		assertEquals(3, command.getLevel().intValue());
		assertEquals(0, command.getLevel("掉落").intValue());
	}
}
