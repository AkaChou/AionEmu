package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 旧存档自愈边登记（{@code quest_legacy_heal_rows.tsv}，只读视图）：单步行族 build() 装载时按
 * 登记发射 EnterWorld 自愈边（[REWARD, var0=staleRow] → var0:=rewardRow）。
 * <p>
 * 口径（判例 80290/80294）：客户端任务书单行任务真端投影在行 0（0 基末行），XML 时代存档停在
 * 行 1（1 基行号），不修复则领奖书页落在不存在的行上。链路径的 weapon 方向
 * （[REWARD, var0=0] → rewardRow）由 {@code buildChain} 的 journalRowRepair 从投影直接派生、
 * 无需登记；本表只收链通道覆盖不到的 armour 方向（staleRow 由逐任务证据登记）。与测试侧
 * {@code retail-legacy-save-normalization.tsv}（采纳时移除的 XML 边登记，门禁要求登记行零
 * EnterWorld 路由）互斥——已编译的自愈边是真端形状的一部分，不入该表。
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

	private static final String REGISTRY = "/aion/data/static_data/quest/retail/quest_legacy_heal_rows.tsv";
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
	 * 解析登记表（quest_id / stale_row / reward_row / 依据；{@code #} 注释与空行跳过）。
	 * Parses the registry TSV (quest_id / stale_row / reward_row / evidence).
	 */
	static Map<Integer, HealEdge> load(InputStream input) throws IOException {
		Map<Integer, HealEdge> parsed = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 3) {
					throw new IOException("malformed heal registry row: " + line);
				}
				parsed.put(Integer.parseInt(parts[0].trim()),
					new HealEdge(Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())));
			}
		} catch (RuntimeException e) {
			throw new IOException("failed to parse legacy-save heal registry", e);
		}
		return Map.copyOf(parsed);
	}
}
