package com.aionemu.gameserver.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 审计 NPC 对玩家施放技能时的属性校验边界，
 * 防止交互型增益再次被 NPC 未建模属性阻断。
 * Audits NPC-to-player skill property checks so unmodeled NPC attributes cannot block interaction buffs.
 */
class NpcPlayerBuffPropertyAuditTest {
	private static final Path GAMESERVER_ROOT = Path.of("src/main/java/com/aionemu/gameserver");
	private static final List<Path> AI_ROOTS = List.of(
		GAMESERVER_ROOT.resolve("ai"),
		GAMESERVER_ROOT.resolve("ai2"));
	private static final Pattern PROPERTY_CHECKED_NPC_TO_PLAYER_CAST = Pattern.compile(
		"getSkill\\(\\s*getOwner\\(\\)\\s*,[^;]{1,400}?,\\s*(?:player|\\(Player\\)\\s+creature)\\s*\\)\\s*"
			+ "\\.(?:useSkill|useNoAnimationSkill)\\(\\)",
		Pattern.DOTALL);
	private static final Set<String> EXPECTED_HOSTILE_MECHANICS = Set.of(
		"ai/housing/GaleCycloneAI2.java",
		"ai/instance/elementisForest/TremoringGroundAI2.java");

	@Test
	void onlyHostileMechanicsUsePropertyCheckedNpcToPlayerCasts() throws IOException {
		Set<String> actual = new TreeSet<>();
		for (Path aiRoot : AI_ROOTS) {
			try (var files = Files.walk(aiRoot)) {
				for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
					if (PROPERTY_CHECKED_NPC_TO_PLAYER_CAST.matcher(Files.readString(source)).find()) {
						actual.add(GAMESERVER_ROOT.relativize(source).toString().replace('\\', '/'));
					}
				}
			}
		}

		assertEquals(EXPECTED_HOSTILE_MECHANICS, actual,
			"Friendly interaction buffs must bypass NPC property checks; review any new exception explicitly");
	}
}
