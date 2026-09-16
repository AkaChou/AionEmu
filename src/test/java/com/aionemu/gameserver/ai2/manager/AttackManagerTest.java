package com.aionemu.gameserver.ai2.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * {@code AttackManager#targetTooFar} 对不可移动 NPC 的契约闸门。
 * Contract gate for {@code AttackManager#targetTooFar} and immobile NPCs.
 *
 * <p>背景：卵、固定炮台这类 0 移速但有伤害的 NPC 一旦在这里放弃目标，就会清空仇恨；下一次受击或视野事件会立刻把它
 * 拉回战斗，客户端因此反复播放脱战表现（Taloc's Hollow 的 mosqua egg 就是该症状）。真端数据里没有这个驱动
 * （0 移速不会产生寻路失败、{@code max_chase_time=0} 不设追击超时、pattern 在进入战斗时 {@code do_nothing}），
 * 因此该分支必须保持不存在。
 * Background: an immobile but damaging NPC that gives up here clears hate only to be pulled back into combat by the next
 * hit or sight event, which makes the client replay the disengage animation (the mosqua egg symptom). Retail data defines
 * no such driver, so this branch must stay absent.</p>
 */
class AttackManagerTest {

	/** 被测源码路径 / Source under test. */
	private static final Path ATTACK_MANAGER = Path
			.of("src/main/java/com/aionemu/gameserver/ai2/manager/AttackManager.java");

	/**
	 * 不可移动的 NPC 不得因目标够不着而放弃目标。
	 * Immobile NPCs must not give up a target they cannot reach.
	 *
	 * @throws IOException 读取源码失败 / when the source cannot be read
	 */
	@Test
	void immobileNpcsKeepUnreachableTargets() throws IOException {
		String body = methodBody(Files.readString(ATTACK_MANAGER), "public static void targetTooFar(NpcAI2 npcAI)");
		assertTrue(body.contains("isMoveSupported()"),
				"targetTooFar 必须保留“能移动则继续追击”的判断。 / targetTooFar must keep the move-supported chase branch.");
		assertFalse(body.contains("TARGET_GIVEUP"),
				"不可移动的 NPC 不得在 targetTooFar 中放弃目标（会与受击重新仇恨形成脱战抖动）。"
						+ " / Immobile NPCs must not give up in targetTooFar; it oscillates with damage-driven re-aggro.");
	}

	/**
	 * 按大括号配对提取指定方法体的源码片段。
	 * Extracts a method body by brace matching.
	 *
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
}
