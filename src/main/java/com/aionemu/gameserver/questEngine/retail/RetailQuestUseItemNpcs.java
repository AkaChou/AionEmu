package com.aionemu.gameserver.questEngine.retail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * 交互物 NPC 登记（{@code quest_use_item_npcs.tsv}，只读视图）：NPC 模板里 {@code ai="quest_use_item"}
 * 的 id 集合。
 * <p>
 * 启动期交互对象合同（{@code QuestInteractionObjectValidator}）要求"掉落物箱 NPC 必须有 START 态
 * {@code ACTION_ITEM_USE} 路由"；合成器只想给物箱发这种路由，不能给普通怪物掉落也发（否则 IR 被
 * 上千条惰性路由污染）。集合来源 = NPC 模板的 ai 属性（生成器
 * {@code build_quest_use_item_npcs.py}），与校验器同源。
 * <p>
 * Read-only registry of interaction-object NPCs, sourced from the NPC templates' {@code ai} attribute
 * so synthesized drop routes cover exactly the boxes the startup contract checks.
 */
public final class RetailQuestUseItemNpcs {

	private static final RetailQuestUseItemNpcs EMPTY = new RetailQuestUseItemNpcs(Set.of());

	private final Set<Integer> npcIds;

	private RetailQuestUseItemNpcs(Set<Integer> npcIds) {
		this.npcIds = npcIds;
	}

	/** 空登记（测试合成器用）。 / Empty registry for tests. */
	public static RetailQuestUseItemNpcs empty() {
		return EMPTY;
	}

	/** 解析登记表（UTF-8 TSV，每行一个 npc id；{@code #} 注释行跳过）。 / Parses the registry TSV. */
	public static RetailQuestUseItemNpcs load(InputStream input) throws IOException {
		Set<Integer> ids = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				ids.add(Integer.parseInt(line.trim()));
			}
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("failed to parse interaction-object npc registry", e);
		}
		return new RetailQuestUseItemNpcs(Set.copyOf(ids));
	}

	/** 该 NPC 是否需要任务交互对象路由。 / Whether the npc needs interaction-object routes. */
	public boolean isInteractionObject(int npcId) {
		return npcIds.contains(npcId);
	}

	public int size() {
		return npcIds.size();
	}
}
