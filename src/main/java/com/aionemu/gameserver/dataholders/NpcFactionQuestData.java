package com.aionemu.gameserver.dataholders;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * NPC 势力每日任务的星期位数据容器。
 * <p>
 * 每个条目描述某势力每日任务（按 quest id）在星期几可发放，数据源自真端
 * npcfactions_quest.xml（由 docs/quest/tools/retail-alignment/generate_npc_faction_quests.py 生成）。
 * 星期位全 0 表示该任务已禁用；未收录的任务按历史行为视为每天可发放（向后兼容）。
 *
 * @author AionEmu
 */
@XmlRootElement(name = "npc_faction_quests")
@XmlAccessorType(XmlAccessType.FIELD)
public class NpcFactionQuestData {

	@XmlElement(name = "npc_faction_quest", required = true)
	protected List<NpcFactionQuestEntry> npcFactionQuests;
	private Map<Integer, NpcFactionQuestEntry> questsByQuestId = new HashMap<>();

	/**
	 * JAXB 反序列化完成后，按 quest id 建立索引。
	 * After JAXB unmarshalling, indexes entries by quest id.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		questsByQuestId.clear();
		for (NpcFactionQuestEntry entry : npcFactionQuests) {
			questsByQuestId.put(entry.questId, entry);
		}
	}

	/**
	 * 判断给定任务在指定星期是否激活。
	 * Returns whether the quest is active on the given calendar day of week.
	 *
	 * @param questId            任务 id / quest id
	 * @param calendarDayOfWeek  {@link Calendar#DAY_OF_WEEK} 值（1=SUNDAY..7=SATURDAY）
	 * @return 无该任务条目时返回 true（向后兼容，所有势力任务照旧每天可发放）
	 */
	public boolean isActiveOn(int questId, int calendarDayOfWeek) {
		NpcFactionQuestEntry entry = questsByQuestId.get(questId);
		if (entry == null) {
			return true;
		}
		return switch (calendarDayOfWeek) {
			case Calendar.SUNDAY -> entry.sun;
			case Calendar.MONDAY -> entry.mon;
			case Calendar.TUESDAY -> entry.tue;
			case Calendar.WEDNESDAY -> entry.wed;
			case Calendar.THURSDAY -> entry.thu;
			case Calendar.FRIDAY -> entry.fri;
			case Calendar.SATURDAY -> entry.sat;
			default -> true;
		};
	}

	/**
	 * 返回全部条目。
	 * Returns all entries.
	 */
	public Collection<NpcFactionQuestEntry> getEntries() {
		return npcFactionQuests;
	}

	/**
	 * 返回条目数量。
	 * Returns the number of loaded entries.
	 */
	public int size() {
		return npcFactionQuests.size();
	}

	/**
	 * 单条星期位数据。
	 * A single quest weekday entry.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class NpcFactionQuestEntry {
		@XmlAttribute(name = "quest_id")
		private int questId;
		@XmlAttribute(name = "faction_id")
		private int factionId;
		@XmlAttribute
		private boolean mon;
		@XmlAttribute
		private boolean tue;
		@XmlAttribute
		private boolean wed;
		@XmlAttribute
		private boolean thu;
		@XmlAttribute
		private boolean fri;
		@XmlAttribute
		private boolean sat;
		@XmlAttribute
		private boolean sun;

		public int getQuestId() {
			return questId;
		}

		public int getFactionId() {
			return factionId;
		}

		public boolean isMon() {
			return mon;
		}

		public boolean isTue() {
			return tue;
		}

		public boolean isWed() {
			return wed;
		}

		public boolean isThu() {
			return thu;
		}

		public boolean isFri() {
			return fri;
		}

		public boolean isSat() {
			return sat;
		}

		public boolean isSun() {
			return sun;
		}
	}
}
