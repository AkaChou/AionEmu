package com.aionemu.gameserver.questEngine.retail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

/**
 * 旧存档自愈边登记（{@code quest_legacy_heal_rows.xml}，只读视图）：单步行族 build() 装载时按
 * 登记发射 EnterWorld 自愈边（[REWARD, var0=staleRow] → var0:=rewardRow）。
 * <p>
 * 口径（判例 80290/80294）：客户端任务书单行任务原版投影在行 0（0 基末行），XML 时代存档停在
 * 行 1（1 基行号），不修复则领奖书页落在不存在的行上。链路径的 weapon 方向
 * （[REWARD, var0=0] → rewardRow）由 {@code buildChain} 的 journalRowRepair 从投影直接派生、
 * 无需登记；本表只收链通道覆盖不到的 armour 方向（staleRow 由逐任务证据登记）。与测试侧
 * {@code retail-legacy-save-normalization.tsv}（采纳时移除的 XML 边登记，门禁要求登记行零
 * EnterWorld 路由）互斥——已编译的自愈边是原版形状的一部分，不入该表。
 * <p>
 * Read-only registry of compiled legacy-save heal edges for the single-step build path. The chain
 * path derives its weapon-direction edge from the projection; this table carries the armour-direction
 * rows whose stale value needs per-quest evidence. Disjoint from the test-side normalization
 * registry, which documents removed XML edges and requires zero enter-world routes.
 */
public final class RetailLegacySaveHealRows {

	/** 一条自愈边：REWARD 态 EnterWorld 触发，var0 从 staleRow 修到 rewardRow。 */
	public record HealEdge(int staleRow, int rewardRow) {
	}

	private static final String REGISTRY = "/aion/data/static_data/quest/retail/quest_legacy_heal_rows.xml";
	private static volatile Map<Integer, HealEdge> rows;

	private RetailLegacySaveHealRows() {
	}

	/** 任务的登记自愈边；未登记返回 {@code null}。 / The registered heal edge, or null. */
	public static HealEdge forQuest(int questId) {
		return rows().get(questId);
	}

	private static Map<Integer, HealEdge> rows() {
		Map<Integer, HealEdge> local = rows;
		if (local == null) {
			synchronized (RetailLegacySaveHealRows.class) {
				local = rows;
				if (local == null) {
					try (InputStream input = RetailLegacySaveHealRows.class.getResourceAsStream(REGISTRY)) {
						if (input == null) {
							throw new IOException("missing registry " + REGISTRY);
						}
						local = load(input);
					} catch (IOException e) {
						throw new IllegalStateException("retail legacy-save heal registry unreadable", e);
					}
					rows = local;
				}
			}
		}
		return local;
	}

	/**
	 * 解析登记表（{@code quest_legacy_heal_rows.xml}：quest_id / stale_row / reward_row / evidence 列；
	 * 旧 TSV 的「少于三列即 malformed」语义保留 = 三个前置列缺席即失败，evidence 列不读）。
	 * Parses the registry XML (columns quest_id / stale_row / reward_row / evidence; the old
	 * "fewer than three columns is malformed" semantics are preserved — the three leading columns
	 * are mandatory, the evidence column is not read).
	 */
	static Map<Integer, HealEdge> load(InputStream input) throws IOException {
		Map<Integer, HealEdge> parsed = new HashMap<>();
		try {
			for (Element row : RetailLedgerXml.rows(RetailLedgerXml.parse(input), "heal_row")) {
				String questId = RetailLedgerXml.text(row, "quest_id");
				String staleRow = RetailLedgerXml.text(row, "stale_row");
				String rewardRow = RetailLedgerXml.text(row, "reward_row");
				if (questId == null || staleRow == null || rewardRow == null) {
					throw new IOException("malformed heal registry row: quest_id=" + questId);
				}
				parsed.put(Integer.parseInt(questId),
					new HealEdge(Integer.parseInt(staleRow), Integer.parseInt(rewardRow)));
			}
		} catch (RuntimeException e) {
			throw new IOException("failed to parse legacy-save heal registry", e);
		}
		return Map.copyOf(parsed);
	}
}
