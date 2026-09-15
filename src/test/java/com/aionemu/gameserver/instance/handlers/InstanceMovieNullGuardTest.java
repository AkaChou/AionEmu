package com.aionemu.gameserver.instance.handlers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 实例脚本 {@code sendMovie(Player, int)} 的空值守卫闸门。
 * Gate ensuring every instance script's {@code sendMovie(Player, int)} guards against a null player.
 *
 * <p>背景：脚本普遍以 {@code npc.getAggroList().getMostPlayerDamage()} 作为入参，该调用在无玩家伤害时返回
 * {@code null}；历史上暗黑波伊塔因此在 NPC 死亡处理中途抛 NPE，中断后续点位与刷怪逻辑。
 * Background: scripts commonly pass {@code npc.getAggroList().getMostPlayerDamage()}, which is null when no
 * player dealt damage; Dark Poeta used to throw an NPE mid-death-handling and abort the remaining logic.</p>
 */
class InstanceMovieNullGuardTest {

	private static final Path ROOT = Path.of("src/main/java/com/aionemu/gameserver/instance/handlers");
	/**
	 * 匹配任意访问修饰符与 void/boolean 返回值的 sendMovie 声明。
	 * Matches any sendMovie declaration regardless of access modifier or void/boolean return type.
	 */
	private static final Pattern DECLARATION = Pattern.compile(
			"(?:private|protected|public)\\s+(?:void|boolean)\\s+sendMovie\\(Player\\s+\\w+,");
	/**
	 * 至少应检查到的实例脚本数量，防止包路径变动导致闸门静默失效。
	 * Minimum number of inspected scripts so a package move cannot silently disable the gate.
	 */
	private static final int MIN_INSPECTED_SCRIPTS = 20;

	@Test
	void everyInstanceSendMovieGuardsAgainstNullPlayer() throws IOException {
		List<String> offenders = new ArrayList<>();
		int inspected = 0;
		try (var files = Files.walk(ROOT)) {
			for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
				if (!containsSendMovieDeclaration(path)) {
					continue;
				}
				inspected++;
				if (sendMovieWithoutNullGuard(path)) {
					offenders.add(path.toString());
				}
			}
		}
		offenders.sort(String::compareTo);
		assertTrue(inspected >= MIN_INSPECTED_SCRIPTS,
				"Expected at least " + MIN_INSPECTED_SCRIPTS + " instance scripts declaring sendMovie(Player, int), "
						+ "found " + inspected + " — the gate may have stopped matching after a package change");
		assertTrue(offenders.isEmpty(),
				"Instance sendMovie must ignore a null player (AggroList#getMostPlayerDamage may be null):\n"
						+ String.join("\n", offenders));
	}

	/**
	 * 判断文件是否存在未判空的 {@code sendMovie} 助手。
	 * Checks whether a file has a {@code sendMovie} helper without a null guard.
	 *
	 * @param path 待检查文件 / file to inspect
	 * @return 缺守卫返回 true / true when the guard is missing
	 */
	private static boolean sendMovieWithoutNullGuard(Path path) {
		try {
			String source = Files.readString(path);
			Matcher matcher = DECLARATION.matcher(source);
			while (matcher.find()) {
				int windowEnd = Math.min(source.length(), matcher.end() + 240);
				if (!source.substring(matcher.end(), windowEnd).contains("== null")) {
					return true;
				}
			}
			return false;
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * 判断文件是否声明了 sendMovie 助手。
	 * Checks whether the file declares a sendMovie helper.
	 *
	 * @param path 待检查文件 / file to inspect
	 * @return 存在声明返回 true / true when a declaration exists
	 */
	private static boolean containsSendMovieDeclaration(Path path) {
		try {
			return DECLARATION.matcher(Files.readString(path)).find();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
}
