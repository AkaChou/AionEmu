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

/**
 * 召唤物属性数据容器，按 NPC ID 与等级哈希索引召唤属性模板。
 * Summon stats data holder, indexing summon stat templates by npc-id and level hash.
 */
@XmlRootElement(name = "summon_stats_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class SummonStatsData {
	@XmlElement(name = "summon_stats", required = true)
	private List<SummonStatsType> summonTemplatesList = new ArrayList<SummonStatsType>();

	private final IntObjectHashMap<SummonStatsTemplate> summonTemplates = new IntObjectHashMap<SummonStatsTemplate>();

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
	 *
	 * @param npcId NPC ID / npc id
	 * @param level 等级 / level
	 * @return 召唤属性模板，可能为 null / summon stats template, may be null
	 */
	public SummonStatsTemplate getSummonTemplate(int npcId, int level) {
		SummonStatsTemplate template = summonTemplates.get(makeHash(npcId, level));
		if (template == null) {
			// 水精灵 4.8 / Water Spirit 4.8
			template = summonTemplates.get(makeHash(833305, 19));
			template = summonTemplates.get(makeHash(833306, 19));
			template = summonTemplates.get(makeHash(833307, 24));
			template = summonTemplates.get(makeHash(833308, 24));
			template = summonTemplates.get(makeHash(833309, 29));
			template = summonTemplates.get(makeHash(833310, 29));
			template = summonTemplates.get(makeHash(833311, 34));
			template = summonTemplates.get(makeHash(833312, 34));
			template = summonTemplates.get(makeHash(833313, 39));
			template = summonTemplates.get(makeHash(833314, 39));
			template = summonTemplates.get(makeHash(833315, 44));
			template = summonTemplates.get(makeHash(833316, 44));
			template = summonTemplates.get(makeHash(833317, 49));
			template = summonTemplates.get(makeHash(833318, 49));
			template = summonTemplates.get(makeHash(833319, 54));
			template = summonTemplates.get(makeHash(833320, 54));
			template = summonTemplates.get(makeHash(833321, 59));
			template = summonTemplates.get(makeHash(833322, 59));
			template = summonTemplates.get(makeHash(833255, 64));
			template = summonTemplates.get(makeHash(833256, 64));
			// 火焰精灵 4.8 / Fire Spirit 4.8
			template = summonTemplates.get(makeHash(833343, 10));
			template = summonTemplates.get(makeHash(833344, 10));
			template = summonTemplates.get(makeHash(833345, 15));
			template = summonTemplates.get(makeHash(833346, 15));
			template = summonTemplates.get(makeHash(833347, 20));
			template = summonTemplates.get(makeHash(833348, 20));
			template = summonTemplates.get(makeHash(833349, 25));
			template = summonTemplates.get(makeHash(833350, 25));
			template = summonTemplates.get(makeHash(833351, 30));
			template = summonTemplates.get(makeHash(833352, 30));
			template = summonTemplates.get(makeHash(833353, 35));
			template = summonTemplates.get(makeHash(833354, 35));
			template = summonTemplates.get(makeHash(833355, 40));
			template = summonTemplates.get(makeHash(833356, 40));
			template = summonTemplates.get(makeHash(833357, 45));
			template = summonTemplates.get(makeHash(833358, 45));
			template = summonTemplates.get(makeHash(833359, 50));
			template = summonTemplates.get(makeHash(833360, 50));
			template = summonTemplates.get(makeHash(833361, 55));
			template = summonTemplates.get(makeHash(833362, 55));
			template = summonTemplates.get(makeHash(833363, 60));
			template = summonTemplates.get(makeHash(833364, 60));
			template = summonTemplates.get(makeHash(833259, 65));
			template = summonTemplates.get(makeHash(833260, 65));
			// 大地精灵 4.8 / Earth Spirit 4.8
			template = summonTemplates.get(makeHash(833287, 16));
			template = summonTemplates.get(makeHash(833288, 16));
			template = summonTemplates.get(makeHash(833289, 21));
			template = summonTemplates.get(makeHash(833290, 21));
			template = summonTemplates.get(makeHash(833291, 26));
			template = summonTemplates.get(makeHash(833292, 26));
			template = summonTemplates.get(makeHash(833293, 31));
			template = summonTemplates.get(makeHash(833294, 31));
			template = summonTemplates.get(makeHash(833295, 36));
			template = summonTemplates.get(makeHash(833296, 36));
			template = summonTemplates.get(makeHash(833297, 41));
			template = summonTemplates.get(makeHash(833298, 41));
			template = summonTemplates.get(makeHash(833299, 46));
			template = summonTemplates.get(makeHash(833300, 46));
			template = summonTemplates.get(makeHash(833301, 51));
			template = summonTemplates.get(makeHash(833302, 51));
			template = summonTemplates.get(makeHash(833303, 56));
			template = summonTemplates.get(makeHash(833304, 56));
			template = summonTemplates.get(makeHash(833253, 61));
			template = summonTemplates.get(makeHash(833254, 61));
			// 风精灵 4.8 / Wind Spirit 4.8
			template = summonTemplates.get(makeHash(833323, 13));
			template = summonTemplates.get(makeHash(833324, 13));
			template = summonTemplates.get(makeHash(833325, 18));
			template = summonTemplates.get(makeHash(833326, 18));
			template = summonTemplates.get(makeHash(833327, 23));
			template = summonTemplates.get(makeHash(833328, 23));
			template = summonTemplates.get(makeHash(833329, 28));
			template = summonTemplates.get(makeHash(833330, 28));
			template = summonTemplates.get(makeHash(833331, 33));
			template = summonTemplates.get(makeHash(833332, 33));
			template = summonTemplates.get(makeHash(833333, 38));
			template = summonTemplates.get(makeHash(833334, 38));
			template = summonTemplates.get(makeHash(833335, 43));
			template = summonTemplates.get(makeHash(833336, 43));
			template = summonTemplates.get(makeHash(833337, 48));
			template = summonTemplates.get(makeHash(833338, 48));
			template = summonTemplates.get(makeHash(833339, 53));
			template = summonTemplates.get(makeHash(833340, 53));
			template = summonTemplates.get(makeHash(833341, 58));
			template = summonTemplates.get(makeHash(833342, 58));
			template = summonTemplates.get(makeHash(833257, 63));
			template = summonTemplates.get(makeHash(833258, 63));
			// 岩浆精灵 4.8 / Magma Spirit 4.8
			template = summonTemplates.get(makeHash(833366, 50));
			template = summonTemplates.get(makeHash(833368, 55));
			template = summonTemplates.get(makeHash(833370, 60));
			template = summonTemplates.get(makeHash(833262, 65));
			// 风暴精灵 4.8 / Tempest Spirit 4.8
			template = summonTemplates.get(makeHash(833365, 50));
			template = summonTemplates.get(makeHash(833367, 55));
			template = summonTemplates.get(makeHash(833369, 60));
			template = summonTemplates.get(makeHash(833261, 65));
			// 攻城武器 / Siege Weapon
			template = summonTemplates.get(makeHash(201054, 40));
			template = summonTemplates.get(makeHash(201055, 40));
			// 优质攻城武器 / Quality Siege Weapon
			template = summonTemplates.get(makeHash(201056, 56));
			template = summonTemplates.get(makeHash(201057, 56));
			template = summonTemplates.get(makeHash(201058, 60));
			template = summonTemplates.get(makeHash(201059, 60));
		}
		return template;
	}

	/**
	 * 返回已加载的召唤属性模板数量。
	 * Returns the number of loaded summon stats templates.
	 *
	 * @return 已加载的召唤物属性模板数量 / Returns the number of loaded summon stats templates.
	 */
	public int size() {
		return summonTemplates.size();
	}

	/**
	 * 单条召唤属性条目，绑定暗/光 NPC ID、需求等级与属性模板。
	 * Single summon-stats entry binding dark/light npc ids, required level, and the stats template.
	 */
	@XmlRootElement(name = "summonStatsTemplateType")
	private static class SummonStatsType {
		@XmlAttribute(name = "npc_id_dark", required = true)
		private int npcIdDark;

		@XmlAttribute(name = "npc_id_light", required = true)
		private int npcIdLight;

		@XmlAttribute(name = "level", required = true)
		private int requiredLevel;

		@XmlElement(name = "stats_template")
		private SummonStatsTemplate template;

		/**
		 * 返回暗属性 NPC ID。
		 * Returns the dark-side npc id.
		 *
		 * @return 暗属性 NPC ID / dark-side npc id
		 */
		public int getNpcIdDark() {
			return npcIdDark;
		}

		/**
		 * 返回光属性 NPC ID。
		 * Returns the light-side npc id.
		 *
		 * @return 光属性 NPC ID / light-side npc id
		 */
		public int getNpcIdLight() {
			return npcIdLight;
		}

		/**
		 * 返回需求等级。
		 * Returns the required level.
		 *
		 * @return 所需等级 / Returns the required level.
		 */
		public int getRequiredLevel() {
			return requiredLevel;
		}

		/**
		 * 返回召唤属性模板。
		 * Returns the summon stats template.
		 *
		 * @return 召唤物属性模板 / Returns the summon stats template.
		 */
		public SummonStatsTemplate getTemplate() {
			return template;
		}
	}

	private static int makeHash(int npcId, int level) {
		return npcId << 10 | level;
	}
}
