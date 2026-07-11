package com.aionemu.gameserver.skillengine.condition;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.templates.item.WeaponType;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.Skill.SkillMethod;

/**
 * 武器条件：校验施法者主手武器类型是否在允许列表中（仅 CAST 路径强制）。
 * Weapon condition: validates the effector main-hand weapon type is in the allowed list (enforced on CAST only).
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WeaponCondition")
public class WeaponCondition extends Condition {

	@XmlAttribute(name = "weapon")
	private List<WeaponType> weaponType;

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param env 技能环境 / skill environment
	 * whether valid
	 */
	@Override
	public boolean validate(Skill env) {
		if (env.getSkillMethod() != SkillMethod.CAST) {
			return true;
		}
		return isValidWeapon(env.getEffector());
	}

	/**
	 * 校验属性计算环境是否满足本条件。
	 * Validates whether the stat calculation environment satisfies this condition.
	 *
	 * @param stat 属性对象 / stat object
	 * stat function
	 * whether valid
	 */
	@Override
	public boolean validate(Stat2 stat, IStatFunction statFunction) {
		return isValidWeapon(stat.getOwner());
	}

	/**
	 * 判断生物主手武器是否在允许类型列表中（NPC 不校验）。
	 * Checks whether the creature's main-hand weapon is in the allowed type list (NPCs skip validation).
	 *
	 * creature
	 * whether valid
	 */
	private boolean isValidWeapon(Creature creature) {
		if (creature instanceof Player) {
			Player player = (Player) creature;
			return weaponType.contains(player.getEquipment().getMainHandWeaponType());
		}
		// 对 NPC 不校验武器，尽管模板中存在。 / for npcs we don't validate weapon, though in templates they are present
		return true;
	}
}
