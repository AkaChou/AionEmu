package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 击杀事件的路由归一化：把 {@link QuestEvent.KillNpcSet} 展开成逐个 npc id 的
 * {@link QuestEvent.KillNpc}，与运行期 {@code QuestEventIndex.routeKeys} 的口径一致。
 * <p>
 * quest-definition XML 允许把一组击杀目标写成单个 {@code KillNpcSet} 转换，
 * 而真端合成器为每个 npc id 生成一条转换；两者在运行期路由到同一批击杀事件，
 * 因此对拍与冻结指纹都必须按本归一化口径比较，否则会报出"表达差异"这种伪不等价。
 * <p>
 * Route-level normalization for kill events, matching {@code QuestEventIndex.routeKeys}.
 */
final class RetailKillRoutes {

	private RetailKillRoutes() {
	}

	/** 单个转换在路由层的规范化事件文本（集合按 id 升序展开）。 / Normalized route-key event texts. */
	static List<String> eventTexts(QuestEvent event) {
		if (event instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)) {
			List<String> texts = new ArrayList<>(npcIds.size());
			npcIds.stream().sorted().forEach(npcId -> texts.add(new QuestEvent.KillNpc(npcId).toString()));
			return texts;
		}
		return List.of(event.toString());
	}
}
