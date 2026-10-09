package com.aionemu.gameserver.questEngine.retail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenQuestTable;
import com.aionemu.gameserver.questEngine.tablelane.NativeEnterAreaPort;

/**
 * 常设门：DD {@code enterarea} 别名的**目标区名必须已在 zones XML 登记**（fail-closed 死边守卫）。
 * <p>
 * P8 重锚：旧解析表 {@code quest_enterarea_zone_resolution.tsv} 与其读者
 * {@code RetailEnterAreaZoneResolution} 已随编译车道退役——别名解析现在只活在
 * {@link NativeEnterAreaPort#create}（表别名 ∈ 登记名才落面，缺席原版区定义的别名落
 * {@code RETAIL_ABSENT_ALIASES} 冻结集）。本门按生产同源输入重建端口，断言：
 * ①解析面非空且每个已解析目标都在 zones XML 登记名内（防空扫描/防静默死边）；
 * ②缺席冻结集恰等于 {@code RETAIL_ABSENT_ALIASES}（防原版区定义补齐后忘记解冻、
 * 或新增缺席别名被静默吞掉）。进区事件的运行时接取面由
 * {@code DataDrivenNativeRuntimeGateTest}（⑪-b）承担。
 * <p>
 * Standing gate: every resolved DD enterarea target must be registered by a zones XML template and
 * the retail-absent frozen set must stay exactly {@code RETAIL_ABSENT_ALIASES}. P8 re-anchor: the old
 * resolution TSV and its reader retired with the compile lane — the port rebuilt here from the same
 * production inputs is the only alias resolution left.
 */
class RetailEnterAreaZoneRegistrationGateTest {

	private static final String ZONES_DIR = "/aion/data/static_data/zones";
	private static final Pattern ZONE_NAME = Pattern.compile("<zone\\b[^>]*\\bname=\"([^\"]+)\"");
	/** zones 登记名总量的下界（防空表通过：目录缺失或解析失效时立刻红）。 / Sanity floor. */
	private static final int MIN_REGISTERED_ZONES = 4000;

	/** ①解析面：每个已解析目标区名必须在 zones XML 登记名内，且解析结果非空。 / Resolved face. */
	@Test
	void everyResolvedZoneNameIsRegistered() throws Exception {
		NativeEnterAreaPort port = productionPort(registeredZoneNames());
		Set<String> resolved = port.resolvedAliases();
		assertFalse(resolved.isEmpty(), "DD enterarea 别名解析面不得为空（表或登记名扫描失效）");
		Set<String> registered = registeredZoneNames();
		Set<String> unregistered = new TreeSet<>();
		for (int questId = 1; questId <= 99999; questId++) {
			for (String zoneName : port.zoneNames(questId)) {
				if (!registered.contains(zoneName)) {
					unregistered.add(zoneName);
				}
			}
		}
		assertTrue(unregistered.isEmpty(), () -> "解析目标区名未在 zones XML 登记（EA 步会成静默死边）: "
			+ unregistered);
	}

	/** ②缺席冻结集恒等于 RETAIL_ABSENT_ALIASES（无静默增长、无遗漏）。 / Frozen absent set. */
	@Test
	void frozenAbsentAliasesStayExactlyTheRetailAbsentSet() throws Exception {
		NativeEnterAreaPort port = productionPort(registeredZoneNames());
		assertEquals(NativeEnterAreaPort.RETAIL_ABSENT_ALIASES, port.frozenAbsentAliases(),
			"缺席别名冻结集漂移——原版补区或新增缺席别名都必须显式裁定");
	}

	/** 生产同源端口重建（表 + 切换集 + zones 登记名）。 / Port rebuilt from production inputs. */
	private static NativeEnterAreaPort productionPort(Set<String> registeredNames) throws Exception {
		DataDrivenQuestTable table;
		try (InputStream input = RetailEnterAreaZoneRegistrationGateTest.class
				.getResourceAsStream(DataDrivenNativeRuntime.TABLE_RESOURCE)) {
			assertNotNull(input, "缺少 DD 表资源");
			table = DataDrivenQuestTable.load(input);
		}
		return NativeEnterAreaPort.create(table, retentionSwitchSet(), registeredNames);
	}

	/** 切换集 = retention 台账 owner RETAIL_TABLE ∧ family DataDriven（与运行时同源解析；
	 * 2026-10-03 台账 XML 化批：文件系统直读改 classpath 主副本，去掉 CWD 依赖）。 /
	 * Switch set from the retention ledger (same source the runtime uses; the ledger-XML batch
	 * replaced the working-tree file read with a classpath read of the main copy). */
	private static Set<Integer> retentionSwitchSet() throws Exception {
		Set<Integer> ids = new TreeSet<>();
		for (Element row : RetailLedgerRows.rows(
				"/aion/data/static_data/quest/retail/retail-xml-retention.xml", "quest")) {
			if ("RETAIL_TABLE".equals(RetailLedgerRows.cell(row, "owner"))
					&& "DataDriven".equals(RetailLedgerRows.cell(row, "family"))) {
				ids.add(Integer.parseInt(RetailLedgerRows.cell(row, "quest_id")));
			}
		}
		assertFalse(ids.isEmpty(), "retention 切换集不得为空");
		return ids;
	}

	private static Set<String> registeredZoneNames() throws Exception {
		URL url = RetailEnterAreaZoneRegistrationGateTest.class.getResource(ZONES_DIR);
		assertNotNull(url, () -> "缺少 zones 数据目录: " + ZONES_DIR);
		Path dir = Path.of(url.toURI());
		Set<String> names = new TreeSet<>();
		try (Stream<Path> entries = Files.list(dir)) {
			for (Path entry : entries.filter(Files::isRegularFile).toList()) {
				String file = entry.getFileName().toString();
				if (!file.startsWith("zones_") || !file.endsWith(".xml")) {
					continue;
				}
				Matcher matcher = ZONE_NAME.matcher(Files.readString(entry, StandardCharsets.UTF_8));
				while (matcher.find()) {
					names.add(matcher.group(1));
				}
			}
		}
		assertTrue(names.size() >= MIN_REGISTERED_ZONES,
			() -> "zones XML 登记名解析结果异常偏少（解析失效或数据缺失）: " + names.size());
		return names;
	}
}
