package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.stats.CalculatedPlayerStatsTemplate;
import com.aionemu.gameserver.model.templates.stats.PlayerStatsTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 玩家属性模板数据容器，按职业与等级哈希索引基础属性。
 * Player stats template data holder, indexing base stats by class and level hash.
 */
@XmlRootElement(name = "player_stats_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class PlayerStatsData {
	@XmlElement(name = "player_stats", required = true)
	private List<PlayerStatsType> templatesList = new ArrayList<PlayerStatsType>();

	private final IntObjectHashMap<PlayerStatsTemplate> playerTemplates = new IntObjectHashMap<PlayerStatsTemplate>();

	/**
	 * JAXB 反序列化完成后，归一化属性、写入等级模板并为各职业注册 0 级计算模板。
	 * After JAXB unmarshalling, normalizes stats, indexes level templates, and registers level-0 calculated templates per class.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (PlayerStatsType pt : templatesList) {
			int code = makeHash(pt.getRequiredPlayerClass(), pt.getRequiredLevel());
			PlayerStatsTemplate template = pt.getTemplate();
			template.setMaxMp(Math.round(template.getMaxMp() * 100f / template.getWill()));
			template.setMaxHp(Math.round(template.getMaxHp() * 100f / template.getHealth()));
			int agility = template.getAgility();
			agility = (agility - 100);
			template.setEvasion(Math.round(template.getEvasion() - template.getEvasion() * agility * 0.003f));
			template.setBlock(Math.round(template.getBlock() - template.getBlock() * agility * 0.0025f));
			template.setParry(Math.round(template.getParry() - template.getParry() * agility * 0.0025f));
			template.setStrikeResist(template.getStrikeResist());
			template.setSpellResist(template.getSpellResist());
			playerTemplates.put(code, pt.getTemplate());
		}
		playerTemplates.put(makeHash(PlayerClass.WARRIOR, 0), new CalculatedPlayerStatsTemplate(PlayerClass.WARRIOR));
		playerTemplates.put(makeHash(PlayerClass.GLADIATOR, 0),
				new CalculatedPlayerStatsTemplate(PlayerClass.GLADIATOR));
		playerTemplates.put(makeHash(PlayerClass.TEMPLAR, 0), new CalculatedPlayerStatsTemplate(PlayerClass.TEMPLAR));
		playerTemplates.put(makeHash(PlayerClass.SCOUT, 0), new CalculatedPlayerStatsTemplate(PlayerClass.SCOUT));
		playerTemplates.put(makeHash(PlayerClass.ASSASSIN, 0), new CalculatedPlayerStatsTemplate(PlayerClass.ASSASSIN));
		playerTemplates.put(makeHash(PlayerClass.RANGER, 0), new CalculatedPlayerStatsTemplate(PlayerClass.RANGER));
		playerTemplates.put(makeHash(PlayerClass.PRIEST, 0), new CalculatedPlayerStatsTemplate(PlayerClass.PRIEST));
		playerTemplates.put(makeHash(PlayerClass.CHANTER, 0), new CalculatedPlayerStatsTemplate(PlayerClass.CHANTER));
		playerTemplates.put(makeHash(PlayerClass.CLERIC, 0), new CalculatedPlayerStatsTemplate(PlayerClass.CLERIC));
		playerTemplates.put(makeHash(PlayerClass.MAGE, 0), new CalculatedPlayerStatsTemplate(PlayerClass.MAGE));
		playerTemplates.put(makeHash(PlayerClass.SORCERER, 0), new CalculatedPlayerStatsTemplate(PlayerClass.SORCERER));
		playerTemplates.put(makeHash(PlayerClass.SPIRIT_MASTER, 0),
				new CalculatedPlayerStatsTemplate(PlayerClass.SPIRIT_MASTER));

		// 资讯类 4.3 / News Class 4.3
		playerTemplates.put(makeHash(PlayerClass.MUSE, 0), new CalculatedPlayerStatsTemplate(PlayerClass.MUSE));
		playerTemplates.put(makeHash(PlayerClass.SONGWEAVER, 0),
				new CalculatedPlayerStatsTemplate(PlayerClass.SONGWEAVER));
		playerTemplates.put(makeHash(PlayerClass.TECHNIST, 0), new CalculatedPlayerStatsTemplate(PlayerClass.TECHNIST));
		playerTemplates.put(makeHash(PlayerClass.GUNSLINGER, 0),
				new CalculatedPlayerStatsTemplate(PlayerClass.GUNSLINGER));
		// 资讯类 4.5 / News Class 4.5
		playerTemplates.put(makeHash(PlayerClass.AETHERTECH, 0),
				new CalculatedPlayerStatsTemplate(PlayerClass.AETHERTECH));
		templatesList.clear();
		templatesList = null;
	}

	/**
	 * 按玩家当前职业与等级获取属性模板；缺失时回退到 0 级模板。
	 * Returns the stats template for the player's class and level; falls back to the level-0 template if missing.
	 *
	 * 玩家 / player
	 * stats template
	 */
	public PlayerStatsTemplate getTemplate(Player player) {
		PlayerStatsTemplate template = getTemplate(player.getCommonData().getPlayerClass(), player.getLevel());
		if (template == null) {
			template = getTemplate(player.getCommonData().getPlayerClass(), 0);
		}
		return template;
	}

	/**
	 * 按职业与等级获取属性模板；缺失时回退到 0 级模板。
	 * Returns the stats template for the given class and level; falls back to the level-0 template if missing.
	 *
	 * player class
	 * level
	 * stats template
	 */
	public PlayerStatsTemplate getTemplate(PlayerClass playerClass, int level) {
		PlayerStatsTemplate template = playerTemplates.get(makeHash(playerClass, level));
		if (template == null) {
			template = getTemplate(playerClass, 0);
		}
		return template;
	}

	/**
	 * 返回已加载的属性模板数量。
	 * Returns the number of loaded stats templates.
	 *
	 * template count
	 */
	public int size() {
		return playerTemplates.size();
	}

	@XmlRootElement(name = "playerStatsTemplateType")
	private static class PlayerStatsType {
		@XmlAttribute(name = "class", required = true)
		private PlayerClass requiredPlayerClass;

		@XmlAttribute(name = "level", required = true)
		private int requiredLevel;

		@XmlElement(name = "stats_template")
		private PlayerStatsTemplate template;

		public PlayerClass getRequiredPlayerClass() {
			return requiredPlayerClass;
		}

		public int getRequiredLevel() {
			return requiredLevel;
		}

		public PlayerStatsTemplate getTemplate() {
			return template;
		}
	}

	// In 4.5 (11)
	private static int makeHash(PlayerClass playerClass, int level) {
		return level << 11 | playerClass.ordinal();
	}
}
