package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.stats.SummonStatsTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import lombok.Getter;

/**
 * 召唤物属性数据容器，按 NPC ID 与等级哈希索引召唤属性模板。
 * Summon stats data holder, indexing summon stat templates by npc-id and level hash.
 */
@XmlRootElement(name = "summon_stats_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class SummonStatsData {
	@XmlElement(name = "summon_stats", required = true)
	private List<SummonStatsType> summonTemplatesList = new ArrayList<>();

	private final IntObjectHashMap<SummonStatsTemplate> summonTemplates = new IntObjectHashMap<>();

	/**
	 * JAXB 反序列化完成后，分别为暗/光 NPC ID 与等级建立属性模板索引。
	 * After JAXB unmarshalling, indexes stat templates for dark and light npc ids with level.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (SummonStatsType st : summonTemplatesList) {
			int summonDark = makeHash(st.getNpcIdDark(), st.getRequiredLevel());
			summonTemplates.put(summonDark, st.getTemplate());
			int summonLight = makeHash(st.getNpcIdLight(), st.getRequiredLevel());
			summonTemplates.put(summonLight, st.getTemplate());
		}
	}

	/**
	 * 按 NPC ID 与等级获取召唤物属性模板；未命中时回退到内置默认哈希序列。
	 * Returns the summon stats template for the given npc id and level; falls back to a built-in hash sequence on miss.
	 * @param npcId NPC ID / npc id
	 * @param level 等级 / level
	 * @return 召唤属性模板，可能为 null / summon stats template, may be null
	 */
	public SummonStatsTemplate getSummonTemplate(int npcId, int level) {
		SummonStatsTemplate template = summonTemplates.get(makeHash(npcId, level));
		if (template == null) {
			// 原实现为逐条覆盖赋值的序列回退（无 if 判断，仅最后一条生效），此处保持等效取值。
			// The original flattened fallback chain reassigned unconditionally; only the last entry ever took effect.
			// 优质攻城武器 60 级 / Quality Siege Weapon lvl 60
			template = summonTemplates.get(makeHash(201059, 60));
		}
		return template;
	}

	/**
	 * 返回已加载的召唤属性模板数量。
	 * Returns the number of loaded summon stats templates.
	 * @return 已加载的召唤物属性模板数量 / Returns the number of loaded summon stats templates.
	 */
	public int size() {
		return summonTemplates.size();
	}

	/**
	 * 单条召唤属性条目，绑定暗/光 NPC ID、需求等级与属性模板。
	 * Single summon-stats entry binding dark/light npc ids, required level, and the stats template.
	 */
	@Getter
    @XmlRootElement(name = "summonStatsTemplateType")
	private static class SummonStatsType {
        /**
         * -- GETTER --
         *  返回暗属性 NPC ID。
         *  Returns the dark-side npc id.
         */
        @XmlAttribute(name = "npc_id_dark", required = true)
		private int npcIdDark;

        /**
         * -- GETTER --
         *  返回光属性 NPC ID。
         *  Returns the light-side npc id.
         */
        @XmlAttribute(name = "npc_id_light", required = true)
		private int npcIdLight;

        /**
         * -- GETTER --
         *  返回需求等级。
         *  Returns the required level.
         */
        @XmlAttribute(name = "level", required = true)
		private int requiredLevel;

        /**
         * -- GETTER --
         *  返回召唤属性模板。
         *  Returns the summon stats template.
         */
        @XmlElement(name = "stats_template")
		private SummonStatsTemplate template;

    }

	private static int makeHash(int npcId, int level) {
		return npcId << 10 | level;
	}
}
