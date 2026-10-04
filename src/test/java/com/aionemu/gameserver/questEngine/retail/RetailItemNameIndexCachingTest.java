package com.aionemu.gameserver.questEngine.retail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物品名索引的进程内缓存回归锚（2026-10-04 启动耗时归因）。
 * <p>
 * 背景：{@code RetailQuestDriver.verifyProductionCoverage} 逐家族构造 handler，每个 handler 首次
 * 使用都要这份索引；若每次调用都重扫 11 个分片（约 87MB 文本 + 正则），启动关键路径会付 10 次以上
 * 全量扫描（2026-10-04 JFR 实测：静态数据窗口 18.4s 中约 8.7s 在此）。本测试钉住"装载即单例"契约。
 * <p>
 * Regression anchor for the process-wide item-name index cache (2026-10-04 startup-cost
 * attribution). The overlay build constructs family handlers one by one and each first use
 * needs this index; re-scanning all 11 shards (~87MB) per call cost 10+ full scans on the
 * startup critical path. This nails down the "loads once per process" contract.
 */
class RetailItemNameIndexCachingTest {

	@Test
	void loadItemTemplatesReturnsTheSameCachedInstance() throws Exception {
		RetailItemNameIndex first = RetailItemNameIndex.loadItemTemplates();
		assertSame(first, RetailItemNameIndex.loadItemTemplates());
		// 下界防真空通过：空索引同样会被缓存，必须确认扫描真的装入了物品名。
		// Guard against a vacuous pass: an empty index would also be cached, so assert real content.
		assertTrue(first.size() > 100_000, "索引必须真的装入物品名，实测约 12.8 万条");
	}
}
