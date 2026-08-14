package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.Guides.GuideTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 新手引导 HTML 配置数据容器，按职业、种族与等级组合索引引导模板。
 * Guide HTML configuration data holder, indexed by player class, race, and level.
 *
 * @author xTz
 */
@XmlRootElement(name = "guides")
@XmlAccessorType(XmlAccessType.FIELD)
public class GuideHtmlData {

	@XmlElement(name = "guide", type = GuideTemplate.class)
	private List<GuideTemplate> guideTemplates;
	private final IntObjectHashMap<ArrayList<GuideTemplate>> templates = new IntObjectHashMap<ArrayList<GuideTemplate>>();
	private final int CLASS_ALL = 255;

	/**
	 * JAXB 反序列化完成后，将引导模板按哈希键写入索引。
	 * After JAXB unmarshalling, indexes guide templates by composite hash keys.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (GuideTemplate template : guideTemplates) {
			addTemplate(template);
		}
		guideTemplates = null;
	}

	/**
	 * 将单个引导模板加入按职业/种族/等级哈希的索引。
	 * Adds a single guide template into the class/race/level hash index.
	 *
	 * @param template 待加入的引导模板 / guide template to add
	 */
	private void addTemplate(GuideTemplate template) {
		Race race = template.getRace();
		if (race == null) {
			race = Race.PC_ALL;
		}
		int classId = template.getPlayerClass() == null ? CLASS_ALL : template.getPlayerClass().ordinal();

		int hash = makeHash(classId, race.ordinal(), template.getLevel());
		ArrayList<GuideTemplate> value = templates.get(hash);
		if (value == null) {
			value = new ArrayList<GuideTemplate>();
			templates.put(hash, value);
		}
		value.add(template);
	}

	/**
	 * 返回引导哈希键数量。
	 * Returns the number of guide hash keys.
	 *
	 * @return 哈希键数量 / hash key count
	 */
	public int size() {
		return templates.size();
	}

	/**
	 * 返回全部引导模板索引。
	 * Returns the full guide template index.
	 *
	 * @return 哈希键到引导模板列表的映射 / map of hash key to guide template list
	 */
	public IntObjectHashMap<ArrayList<GuideTemplate>> getTemplates() {
		return templates;
	}

	/**
	 * 按标题查找引导模板。
	 * Finds a guide template by title.
	 *
	 * @param title 引导标题 / guide title
	 * @return 匹配的引导模板，不存在则为 null / matching guide template, or null if absent
	 */
	public GuideTemplate getTemplateByTitle(String title) {
		for (int templateHash : templates.keys()) {
			for (GuideTemplate template : templates.get(templateHash)) {
				if (template.getTitle().equals(title)) {
					return template;
				}
			}
		}
		return null;
	}

	/**
	 * 按职业、种族与等级聚合匹配的引导模板（含通用回退）。
	 * Aggregates matching guide templates for the given class, race, and level (including general fallbacks).
	 *
	 * @param playerClass 玩家职业 / player class
	 * @param race 玩家种族 / player race
	 * @param level 玩家等级 / player level
	 * @return 匹配的引导模板数组 / array of matching guide templates
	 */
	public GuideTemplate[] getTemplatesFor(PlayerClass playerClass, Race race, int level) {
		List<GuideTemplate> guideTemplate = new ArrayList<GuideTemplate>();
		List<GuideTemplate> classRaceSpecificTemplates = templates
				.get(makeHash(playerClass.ordinal(), race.ordinal(), level));
		List<GuideTemplate> classSpecificTemplates = templates
				.get(makeHash(playerClass.ordinal(), Race.PC_ALL.ordinal(), level));
		List<GuideTemplate> raceSpecificTemplates = templates.get(makeHash(CLASS_ALL, race.ordinal(), level));
		List<GuideTemplate> generalTemplates = templates.get(makeHash(CLASS_ALL, Race.PC_ALL.ordinal(), level));

		if (classRaceSpecificTemplates != null) {
			guideTemplate.addAll(classRaceSpecificTemplates);
		}
		if (classSpecificTemplates != null) {
			guideTemplate.addAll(classSpecificTemplates);
		}
		if (raceSpecificTemplates != null) {
			guideTemplate.addAll(raceSpecificTemplates);
		}
		if (generalTemplates != null) {
			guideTemplate.addAll(generalTemplates);
		}
		return guideTemplate.toArray(new GuideTemplate[guideTemplate.size()]);
	}

	/**
	 * 根据职业、种族与等级生成组合哈希键。
	 * Builds a composite hash key from class type, race, and level.
	 *
	 * @param classType 职业类型序号 / class type ordinal
	 * @param race 种族序号 / race ordinal
	 * @param level 等级 / level
	 * @return 组合哈希键 / composite hash key
	 */
	private static int makeHash(int classType, int race, int level) {
		int result = classType << 10;
		result = (result | race) << 10;
		return result | level;
	}
}
