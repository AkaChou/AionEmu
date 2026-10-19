package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 守护伤害效果必须登记命中结果。
 * Guards that damage effects register their hit result.
 */
class DamageEffectHitCalculationTest {

	private static final Path EFFECT_DIR = Path.of("src/main/java/com/aionemu/gameserver/skillengine/effect");

	/**
	 * 凡是覆写 calculate 的伤害效果，都必须把命中判定交回 DamageEffect。
	 * Every damage effect overriding calculate must delegate hit resolution to DamageEffect.
	 * <p>
	 * 若遗漏该调用，成功效果集合会保持为空，Effect.initialize 将整次技能判定为闪避（DODGE/RESIST），
	 * 表现为技能永远被怪物闪避、既无法造成伤害也无法挂载后续效果。
	 * Without that call the success-effect set stays empty and Effect.initialize treats the whole skill as
	 * dodged, so the skill never deals damage and never applies its follow-up effect.
	 * @throws IOException 读取源码失败 / reading sources failed
	 */
	@Test
	void damageEffectsDelegateHitCalculation() throws IOException {
		List<String> offenders = new ArrayList<>();
		try (Stream<Path> files = Files.list(EFFECT_DIR)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				String source = Files.readString(file);
				if (!source.contains("extends DamageEffect") || !source.contains("public void calculate(Effect effect)")) {
					continue;
				}
				if (!calculateBody(source).contains("super.calculate(")) {
					offenders.add(file.getFileName().toString());
				}
			}
		}

		assertTrue(offenders.isEmpty(), "damage effects must delegate hit calculation to DamageEffect: " + offenders);
	}

	/**
	 * 印记雕刻与绕后位移必须按物理伤害结算命中。
	 * Signet carving and move-behind must resolve their hit as physical damage.
	 */
	@Test
	void signetCarvingAndMoveBehindSettlePhysicalHit() throws IOException {
		for (String effect : List.of("CarveSignetEffect", "MoveBehindEffect")) {
			String body = calculateBody(Files.readString(EFFECT_DIR.resolve(effect + ".java")));

			assertTrue(body.contains("super.calculate(effect, DamageType.PHYSICAL)"),
					effect + " must settle its hit as physical damage");
		}
	}

	private static String calculateBody(String source) {
		int start = source.indexOf("public void calculate(Effect effect)");
		if (start < 0) {
			return "";
		}
		int end = source.indexOf("\n\t}", start);
		return end < 0 ? source.substring(start) : source.substring(start, end);
	}
}
