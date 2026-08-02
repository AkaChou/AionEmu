package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** 候选任务 shadow runner 的生产诊断配置；默认关闭且不接管旧 owner。 */
public final class QuestShadowConfig {
	@Property(key = "gameserver.quest.shadow.enable", defaultValue = "false")
	public static boolean ENABLED;

	@Property(key = "gameserver.quest.shadow.report.path",
		defaultValue = "./log/quest-shadow/unified-shadow-report.json")
	public static String REPORT_PATH;

	@Property(key = "gameserver.quest.shadow.persist.interval.seconds", defaultValue = "300")
	public static int PERSIST_INTERVAL_SECONDS;

	private QuestShadowConfig() {
	}

	public static Path reportPath() {
		if (REPORT_PATH == null || REPORT_PATH.isBlank()) {
			throw new IllegalStateException("gameserver.quest.shadow.report.path must not be blank");
		}
		return Path.of(REPORT_PATH).normalize();
	}

	public static long persistIntervalMillis() {
		if (PERSIST_INTERVAL_SECONDS <= 0) {
			throw new IllegalStateException("gameserver.quest.shadow.persist.interval.seconds must be positive");
		}
		return TimeUnit.SECONDS.toMillis(PERSIST_INTERVAL_SECONDS);
	}
}
