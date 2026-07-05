package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ArenaOfTenacityInstanceTest {

	@Test
	void clearsBoostMoraleWhenPlayerLeavesInstance() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/ArenaOfTenacityInstance.java"));
		String body = methodBody(source, "public void onLeaveInstance(Player player)");

		assertTrue(body.contains("playerReward.endBoostMoraleEffect(player);"));
		assertTrue(body.contains("instanceReward.removePlayerReward(playerReward);"));
	}

	private static String methodBody(String source, String signature) {
		int signatureStart = source.indexOf(signature);
		assertTrue(signatureStart >= 0, "Arena of Tenacity must clean instance morale buffs on leave");
		int bodyStart = source.indexOf('{', signatureStart);
		assertTrue(bodyStart >= 0, "onLeaveInstance must have a method body");

		int depth = 0;
		for (int i = bodyStart; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				depth--;
				if (depth == 0) {
					return source.substring(bodyStart + 1, i);
				}
			}
		}
		throw new AssertionError("onLeaveInstance method body was not closed");
	}
}
