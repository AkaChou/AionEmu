package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 客户端「接取 NPC 集合」投影（{@code quest_client_accept_npc_sets.tsv}）。
 * <p>
 * 真端模板的 {@code acquired_npc_name} 是**逻辑 NPC 名**，客户端把它展开成可接取的 NPC 集合
 * （多区域/多实例变体，例如 {@code HousingManager_Li} ↔ {@code HousingManager_L_S..L_D}）。
 * 当真端名解析出多于一个 id 时，合成器只认本投影：解析集必须与该任务客户端声明的集合
 * **逐元素相等**才放行，否则维持 fail-closed（`RETAIL_ACQUIRE_NPC_AMBIGUOUS`）——客户端是唯一仲裁，
 * 不允许按名字猜测或按登记补页。
 * <p>
 * Client-declared accept-NPC sets. The sole arbiter when a retail acquire name resolves to more than
 * one id: the resolved set must equal the client declaration element-wise, otherwise the row stays
 * fail-closed.
 */
public final class RetailClientAcceptNpcSets {

	private static final RetailClientAcceptNpcSets EMPTY = new RetailClientAcceptNpcSets(Map.of());
	private static volatile RetailClientAcceptNpcSets defaultInstance;

	private final Map<Integer, Set<Integer>> entries;

	private RetailClientAcceptNpcSets(Map<Integer, Set<Integer>> entries) {
		this.entries = entries;
	}

	/** 缺省投影（退役后生产通道）。 / The default projection used by production. */
	public static RetailClientAcceptNpcSets defaultSets() {
		RetailClientAcceptNpcSets instance = defaultInstance;
		if (instance == null) {
			synchronized (RetailClientAcceptNpcSets.class) {
				instance = defaultInstance;
				if (instance == null) {
					instance = decodeDefaultInstance();
					defaultInstance = instance;
				}
			}
		}
		return instance;
	}

	/** 空投影（测试合成器用）。 / Empty projection for test synthesizers. */
	public static RetailClientAcceptNpcSets empty() {
		return EMPTY;
	}

	private static RetailClientAcceptNpcSets decodeDefaultInstance() {
		InputStream in = RetailClientAcceptNpcSets.class.getResourceAsStream(
			"/quest/quest_client_accept_npc_sets.tsv");
		if (in == null) {
			in = RetailClientAcceptNpcSets.class.getResourceAsStream(
				"/aion/data/static_data/quest/retail/quest_client_accept_npc_sets.tsv");
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

	/** 解析投影表（UTF-8 TSV；{@code #} 注释行跳过）。 / Parses the projection TSV. */
	public static RetailClientAcceptNpcSets load(InputStream input) throws IOException {
		Map<Integer, Set<Integer>> entries = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 2) {
					continue;
				}
				Set<Integer> npcIds = new LinkedHashSet<>();
				for (String id : parts[1].split(",")) {
					if (!id.isBlank()) {
						npcIds.add(Integer.parseInt(id.trim()));
					}
				}
				if (npcIds.size() < 2) {
					throw new IOException("accept npc set must declare at least two ids: " + line);
				}
				entries.put(Integer.parseInt(parts[0].trim()), Set.copyOf(npcIds));
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("failed to parse client accept npc sets", e);
		}
		return new RetailClientAcceptNpcSets(Map.copyOf(entries));
	}

	/** 该任务的客户端接取 NPC 集（未声明为空集）。 / The client accept-NPC set for one quest. */
	public Set<Integer> npcIds(int questId) {
		return entries.getOrDefault(questId, Set.of());
	}
}
