package com.aionemu.chatserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.qos.logback.classic.Level;
import com.aionemu.testutil.LogCapture;
import org.junit.jupiter.api.Test;

/**
 * 验证已注册但未建立聊天通道的正常退出不产生认证失败告警。
 * Verifies normal logout after registration without a chat channel does not emit authentication-failure warnings.
 */
class ChatLogoutLoggingTest {

	@Test
	void registeredPlayerWithoutChannelLogsDebugOnly() throws Exception {
		ChatService service = new ChatService(new BroadcastService());
		service.registerPlayer(42, "account", "player");
		try (LogCapture logs = new LogCapture(ChatService.class)) {
			service.playerLogout(42);
			service.playerLogout(42);

			assertEquals(1, logs.events().size());
			assertEquals(Level.DEBUG, logs.events().getFirst().getLevel());
		}
	}
}
