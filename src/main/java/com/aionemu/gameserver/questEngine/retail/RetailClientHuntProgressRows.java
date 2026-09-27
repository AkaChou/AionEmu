package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端 hunt 进度行登记表（{@code quest_client_hunt_progress_rows.tsv}，只读视图）——混合链
 * 客户端 SECTION 校准的生产化（门→段归属判据的落表）。
 * <p>
 * 每行来自客户端任务书（{@code quest_monster.csv}）的 simpleQuest 进度行：行阶梯
 * （SECTION_0==k）、计数段（SECTION_m&lt;count，m≥1）与该行全部怪物名。同一 ladder_row 的行 =
 * 同一 hunt 块的并行目标（18990 形：僵尸 SECTION_1 / 公主 SECTION_2）；多段共享计数行
 * （15306 形：五段全 SECTION_1）由行阶梯区分段。生成器
 * {@code build_quest_hunt_progress_rows_tsv.py}；SECTION_5==0 门控的并行网格族不属于本表。
 * <p>
 * Read-only view of the client hunt progress-row registry (the production form of the gate-to-
 * SECTION calibration). Each row comes from the client journal's simpleQuest rows: the row ladder
 * (SECTION_0==k), the counter section (SECTION_m&lt;count) and the row's monster names. Rows sharing
 * a ladder row are one hunt block's parallel objectives (18990: zombie SECTION_1 / princess
 * SECTION_2); shared-counter stages (15306: all SECTION_1) are separated by the ladder. Rows gated
 * by SECTION_5==0 belong to the parallel-grid lane and are not in this table.
 */
public final class RetailClientHuntProgressRows {

	/** 一条客户端进度行。 / One client progress row. */
	public record Row(int ladderRow, int section, int count, List<String> monsters) {
	}

	private static final RetailClientHuntProgressRows EMPTY = new RetailClientHuntProgressRows(Map.of());

	private final Map<Integer, List<Row>> rows;

	private RetailClientHuntProgressRows(Map<Integer, List<Row>> rows) {
		this.rows = rows;
	}

	/** 空登记（测试合成器用）。 / Empty registry for tests. */
	public static RetailClientHuntProgressRows empty() {
		return EMPTY;
	}

	/** 解析登记表（UTF-8 TSV；{@code #} 注释行跳过；怪物列 {@code ;} 分隔）。 / Parses the registry TSV. */
	public static RetailClientHuntProgressRows load(InputStream input) throws IOException {
		Map<Integer, List<Row>> parsed = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// 跳过注释行与列头行（quest_id<TAB>ladder_row...）。
				// Skip comment lines and the column-header line.
				if (line.startsWith("#") || line.isBlank() || !Character.isDigit(line.charAt(0))) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 5) {
					continue;
				}
				List<String> monsters = new ArrayList<>();
				for (String name : parts[4].split(";")) {
					if (!name.isBlank()) {
						monsters.add(name.trim());
					}
				}
				Row row = new Row(Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()),
					Integer.parseInt(parts[3].trim()), List.copyOf(monsters));
				parsed.computeIfAbsent(Integer.parseInt(parts[0].trim()), key -> new ArrayList<>()).add(row);
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("malformed client hunt progress-row registry", e);
		}
		Map<Integer, List<Row>> frozen = new HashMap<>();
		for (Map.Entry<Integer, List<Row>> entry : parsed.entrySet()) {
			entry.getValue().sort(Comparator.comparingInt(Row::ladderRow)
				.thenComparingInt(Row::section));
			frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
		}
		return new RetailClientHuntProgressRows(Map.copyOf(frozen));
	}

	/**
	 * 该任务按行阶梯分组的进度行组（升序；未登记为空表）。行阶梯相同 = 同一 hunt 块。
	 * The quest's progress rows grouped by ladder row (ascending; empty when unregistered). Rows
	 * sharing a ladder row belong to one hunt block.
	 */
	public List<List<Row>> groups(int questId) {
		List<Row> questRows = rows.getOrDefault(questId, List.of());
		List<List<Row>> grouped = new ArrayList<>();
		int currentRow = Integer.MIN_VALUE;
		for (Row row : questRows) {
			if (row.ladderRow() != currentRow) {
				grouped.add(new ArrayList<>());
				currentRow = row.ladderRow();
			}
			grouped.getLast().add(row);
		}
		return List.copyOf(grouped);
	}

	/**
	 * 该任务的全部进度行（扁平；行阶梯/段序升序，未登记为空表）。供单段网格行的计数对齐使用
	 * （组边界仍由 {@link #groups(int)} 保留）。
	 * The quest's progress rows flattened (ascending by ladder row and section; empty when
	 * unregistered), used by the single-stage grid count alignment.
	 */
	public List<Row> rows(int questId) {
		return rows.getOrDefault(questId, List.of());
	}
}
