package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Spring 迁移护栏：冻结"双源静态兜底"清单，并保证每个兜底点都有 provider 注入点。
 * Spring-migration guard: freezes the dual-source fallback inventory and requires every fallback site to be wired.
 *
 * <p>背景：一次未彻底完成的 Spring 改造留下了
 * {@code provider.getIfAvailable(() -> SingletonHolder.instance)}
 * 这种"Spring 优先、静态兜底"的双源写法。兜底一旦被走到，就会在容器之外静默创建第二套实例。
 * 本测试把清单冻结：
 * 数量只能通过显式修改常量而减少，且新增必须先在
 * `.agents/summary/architecture-performance-refactor/2026-09-17-spring-migration-leftovers.md` 登记。
 * The inventory may only shrink by explicitly editing the constant below; new fallbacks must be registered in the
 * migration-leftovers document first.</p>
 */
class LegacySingletonFallbackAuditTest {

	/**
	 * 已退役双源兜底的类：不得再出现任何回落或静态兜底定义。
	 * Classes whose dual-source fallback is retired.
	 */
	private static final Set<String> RETIRED = Set.of(
		"InGameShopEn",
		"AbyssLandingSpecialService",
		"AnnouncementService",
		"BGService",
		"CuringZoneService",
		"DebugService",
		"FindGroupService",
		"FlyRingService",
		"GameTimeService",
		"LandingUpdateService",
		"MailService",
		"PeriodicSaveService",
		"SpringZoneService",
		"TaskManagerFromDB",
		"ThievesGuildService",
		"VeteranRewardsService",
		"WebshopService",
		"AionPacketHandlerFactory",
		"ChatServer",
		"DataManager",
		"EventScheduler",
		"IDFactory",
		"LoginServer",
		"LsPacketHandlerFactory",
		"PacketFloodFilter",
		"World");

	/** 冻结的遗留双源兜底点数量（131 个点中 26 个类已退役）。 */
	private static final int FROZEN_FALLBACK_SITES = 105;

	@Test
	void legacySingletonFallbacksStayFrozenAndWired() throws IOException {
		List<String> fallbackFiles = new ArrayList<>();
		List<String> unwired = new ArrayList<>();
		Set<String> retiredSeen = new HashSet<>();
		int sites = 0;
		try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				String source = Files.readString(file);
				String name = file.getFileName().toString();
				name = name.substring(0, name.length() - ".java".length());
				if (RETIRED.contains(name)) {
					retiredSeen.add(name);
					assertFalse(source.contains("getIfAvailable(() ->"),
						"已退役双源兜底的类不得再出现 provider 回落：" + file);
					assertFalse(source.contains("class SingletonHolder"),
						"已退役双源兜底的类不得再出现静态兜底持有者：" + file);
					continue;
				}
				int fileSites = 0;
				for (String line : source.lines().toList()) {
					if (line.contains("getIfAvailable(() ->") && line.contains("SingletonHolder")) {
						fileSites++;
					}
				}
				if (fileSites == 0) {
					continue;
				}
				sites += fileSites;
				fallbackFiles.add(file.toString());
				if (!source.contains("setInstanceProvider") && !source.contains("@Setter")) {
					unwired.add(file.toString());
				}
			}
		}

		assertEquals(RETIRED, retiredSeen, "RETIRED 中的每个类都必须能在 src/main/java 中找到对应文件");
		assertTrue(unwired.isEmpty(),
			"这些类没有 provider 注入点（手写 setInstanceProvider 或 Lombok @Setter），"
				+ "会永远走静态兜底（属于未接线孤儿）：" + unwired);
		assertEquals(FROZEN_FALLBACK_SITES, sites,
			"遗留双源兜底点数量变化：新增必须先在迁移清单文档登记，退役则显式下调本常量；"
				+ "涉及文件：" + fallbackFiles);
	}
}
