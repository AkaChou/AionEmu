package com.aionemu.gameserver.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 阈值变身 AI 的幂等 + 死亡兜底闸门。
 * Gate for idempotent threshold transforms that must also survive a lethal blow.
 *
 * <p>背景：{@code handleAttack} 由 {@code AggroList#addDamage} 触发，早于 {@code LifeStats#reduceHp}，
 * 因此阈值检查读到的是本次伤害前的 HP；一击/爆发致死会跳过整个变身，任务或场景永远等不到替代形态。
 * 本闸门锁定同族 AI“阈值路径与死亡路径共用一次性生成”的合同，避免新增成员时只写阈值分支。</p>
 */
class ThresholdTransformDeathFallbackGateTest {

	private static final Path AI_ROOT = Path.of("src/main/java/com/aionemu/gameserver/ai");

	private record Entry(String path, int transformedNpcId) {}

	private static final List<Entry> FAMILY = List.of(
		new Entry("instance/azoturanFortress/Betrayer_IcaronixAI2.java", 214599),
		new Entry("instance/darkPoeta/Crazy_ScarAI2.java", 281116),
		new Entry("instance/drakenspireDepths/Fountless_Heatvent_ProtectorAI2.java", 236228),
		new Entry("instance/drakenspireDepths/Fountless_Lava_ProtectorAI2.java", 236227),
		new Entry("instance/drakenspireDepths/immortalOrissanAI2.java", 237231),
		new Entry("worlds/brusthonin/Unfaithful_NtuamuAI2.java", 214583),
		new Entry("worlds/tiamaranta_eye/Aide_IranatiAI2.java", 218555),
		new Entry("worlds/tiamaranta_eye/Master_At_Arms_RaniganAI2.java", 218558),
		new Entry("worlds/tiamaranta_eye/TDown_M_Drakan_Pagati_Named_60_AeAI2.java", 249099),
		new Entry("worlds/tiamaranta_eye/TDown_M_Drakan_Sikara_Named_60_AeAI2.java", 249102));

	@Test
	void everyThresholdTransformSpawnsOnceAndSurvivesALethalBlow() throws IOException {
		for (Entry entry : FAMILY) {
			String source = Files.readString(AI_ROOT.resolve(entry.path()));

			assertTrue(source.contains("handleDied()"), entry.path() + " must override handleDied");
			assertTrue(source.contains("compareAndSet"), entry.path() + " must guard the transform idempotently");
			assertTrue(source.contains("AI2Actions.deleteOwner(this)"), entry.path() + " must remove the entry form");
			assertTrue(source.contains("= " + entry.transformedNpcId() + ";"),
				entry.path() + " must keep transformed npc " + entry.transformedNpcId());
			assertTrue(methodBody(source, "handleDied()").contains("Once();"),
				entry.path() + " must spawn the transformed form when it dies before the threshold");
		}
	}

	@Test
	void thresholdAndDeathPathsShareTheSameSpawnOnceHelper() throws IOException {
		for (Entry entry : FAMILY) {
			String source = Files.readString(AI_ROOT.resolve(entry.path()));
			String death = methodBody(source, "handleDied()");
			Matcher matcher = Pattern.compile("(\\w+Once)\\(\\)").matcher(death);
			assertTrue(matcher.find(), entry.path() + " must call a spawn-once helper after death");

			String helper = matcher.group(1);
			assertTrue(source.contains("boolean " + helper + "()"),
				entry.path() + " must declare " + helper + " as its single spawn guard");
			assertTrue(methodBody(source, "private void checkPercentage(").contains(helper + "()"),
				entry.path() + " threshold path must share " + helper);
		}
	}

	private static String methodBody(String source, String signature) {
		int start = source.indexOf(signature);
		assertTrue(start >= 0, signature + " must exist");
		int brace = source.indexOf('{', start);
		assertTrue(brace >= 0, signature + " must have a body");
		int depth = 0;
		for (int i = brace; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				depth--;
				if (depth == 0) {
					return source.substring(brace + 1, i);
				}
			}
		}
		throw new AssertionError(signature + " was not closed");
	}
}
