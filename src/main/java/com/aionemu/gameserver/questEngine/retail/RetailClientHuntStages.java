package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端串行阶段契约登记（原 {@code quest_client_hunt_stages.tsv} 退役后转为动态测试资源流与规范视图）。
 * <p>
 * 客户端 {@code quest_monster.csv} 的 {@code Progress(SECTION_n<count; SECTION_(n-1)==count')} 行给出
 * SimpleSerialHunt 的逐段链式门控（乱序不计数）、段内计数与完整刷怪名单（含真端表未列的变体刷怪），
 * 是串行阶梯合成口径的权威。
 * <p>
 * Read-only view of the client serial-stage contract for the SimpleSerialHunt family.
 */
public final class RetailClientHuntStages {

	private static final RetailClientHuntStages EMPTY = new RetailClientHuntStages(Map.of());
	private static volatile RetailClientHuntStages defaultInstance;

	/** 一个串行阶段。 / One serial stage. */
	public record Stage(int stage, int count, List<Integer> npcIds, List<String> names) {
	}

	private final Map<Integer, List<Stage>> entries;

	private RetailClientHuntStages(Map<Integer, List<Stage>> entries) {
		this.entries = entries;
	}

	/** 缺省规范串行阶段契约登记（退役后生产通道）。 / Default canonical hunt stages registry. */
	public static RetailClientHuntStages defaultHuntStages() {
		RetailClientHuntStages instance = defaultInstance;
		if (instance == null) {
			synchronized (RetailClientHuntStages.class) {
				instance = defaultInstance;
				if (instance == null) {
					instance = decodeDefaultInstance();
					defaultInstance = instance;
				}
			}
		}
		return instance;
	}

	/** 空登记（测试合成器用）。 / Empty registry for tests. */
	public static RetailClientHuntStages empty() {
		return EMPTY;
	}

	private static RetailClientHuntStages decodeDefaultInstance() {
		InputStream in = RetailClientHuntStages.class.getResourceAsStream("/quest/quest_client_hunt_stages.tsv");
		if (in == null) {
			in = RetailClientHuntStages.class.getResourceAsStream("/aion/data/static_data/quest/retail/quest_client_hunt_stages.tsv");
		}
		if (in == null) {
			return EMPTY;
		}
		try (InputStream input = in) {
			return load(input);
		} catch (IOException e) {
			return EMPTY;
		}
	}

	/** 解析登记表（UTF-8 TSV；{@code #} 注释行跳过）。 / Parses the registry TSV. */
	public static RetailClientHuntStages load(InputStream input) throws IOException {
		Map<Integer, List<Stage>> entries = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 4) {
					continue;
				}
				int questId = Integer.parseInt(parts[0]);
				List<Integer> npcIds = new ArrayList<>();
				for (String id : parts[3].split(",")) {
					npcIds.add(Integer.parseInt(id.trim()));
				}
				List<String> names = new ArrayList<>();
				if (parts.length >= 5 && !parts[4].isBlank()) {
					for (String name : parts[4].split(",")) {
						names.add(name.trim());
					}
				}
				Stage stage = new Stage(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
					List.copyOf(npcIds), List.copyOf(names));
				entries.computeIfAbsent(questId, ignored -> new ArrayList<>()).add(stage);
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("failed to parse client hunt stage registry", e);
		}
		Map<Integer, List<Stage>> copy = new HashMap<>();
		entries.forEach((questId, stages) -> copy.put(questId, List.copyOf(stages)));
		return new RetailClientHuntStages(Map.copyOf(copy));
	}

	/** 该任务的串行阶段清单。 / The serial stages of one quest. */
	public List<Stage> stages(int questId) {
		return entries.getOrDefault(questId, List.of());
	}
}
