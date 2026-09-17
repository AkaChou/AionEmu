package com.aionemu.gameserver.questEngine.runtime;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * 任务交互物测试使用的 NPC AI 索引。
 * NPC AI index used by quest interaction-object tests.
 */
public final class QuestInteractionObjectTestData {
	private static final List<String> NPC_SHARDS = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml",
		"npc_template_235749_247606.xml", "npc_template_247607_270057.xml",
		"npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");

	private static volatile NpcAiIndex cachedIndex;

	private QuestInteractionObjectTestData() {
	}

	/** 返回所有 quest_use_item NPC ID。 / Returns all quest_use_item NPC IDs. */
	public static Set<Integer> questUseItemNpcIds(ClassLoader loader) throws Exception {
		return index(loader).questUseItemNpcIds();
	}

	/**
	 * 返回与启动校验器相同的 NPC AI 查询函数。
	 * Returns the same NPC AI lookup used by the startup validator.
	 */
	public static IntFunction<String> npcAiResolver(ClassLoader loader) throws Exception {
		NpcAiIndex index = index(loader);
		return npcId -> !index.knownNpcIds().contains(npcId)
			? null
			: index.questUseItemNpcIds().contains(npcId) ? "quest_use_item" : "other";
	}

	private static NpcAiIndex index(ClassLoader loader) throws Exception {
		NpcAiIndex current = cachedIndex;
		if (current != null) {
			return current;
		}
		synchronized (QuestInteractionObjectTestData.class) {
			if (cachedIndex == null) {
				cachedIndex = loadIndex(loader);
			}
			return cachedIndex;
		}
	}

	private static NpcAiIndex loadIndex(ClassLoader loader) throws Exception {
		Set<Integer> knownNpcIds = new HashSet<>();
		Set<Integer> questUseItemNpcIds = new HashSet<>();
		XMLInputFactory factory = XMLInputFactory.newFactory();
		for (String shard : NPC_SHARDS) {
			try (InputStream input = resource(loader, "aion/data/static_data/npcs/" + shard)) {
				var reader = factory.createXMLStreamReader(input);
				while (reader.hasNext()) {
					if (reader.next() != XMLStreamConstants.START_ELEMENT
							|| !"npc_template".equals(reader.getLocalName())) {
						continue;
					}
					String rawNpcId = reader.getAttributeValue(null, "npc_id");
					if (rawNpcId == null) {
						continue;
					}
					int npcId = Integer.parseInt(rawNpcId);
					knownNpcIds.add(npcId);
					if ("quest_use_item".equals(reader.getAttributeValue(null, "ai"))) {
						questUseItemNpcIds.add(npcId);
					}
				}
			}
		}
		return new NpcAiIndex(Set.copyOf(knownNpcIds), Set.copyOf(questUseItemNpcIds));
	}

	private static InputStream resource(ClassLoader loader, String path) {
		InputStream input = loader.getResourceAsStream(path);
		if (input == null) {
			throw new IllegalStateException("missing resource " + path);
		}
		return input;
	}

	private record NpcAiIndex(Set<Integer> knownNpcIds, Set<Integer> questUseItemNpcIds) {
	}
}
