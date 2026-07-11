package com.aionemu.gameserver.skillengine.effect;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatDualWeaponMasteryFunction;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/**
 * 双持武器效果：设置双持效率/伤害参数，并应用双持专精属性修正。
 * Dual-wield weapon effect: sets dual efficiency/damage params and applies dual mastery modifiers.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WeaponDualEffect")
public class WeaponDualEffect extends BuffEffect {

	@XmlAttribute(name = "skill_efficiency")
	private int skillEfficiency;
	@XmlAttribute(name = "max_damage_chance")
	private int maxDamageChance;
	@XmlAttribute(name = "max_damage_delta")
	private int maxDamageDelta;

	@Override
	public void startEffect(Effect effect) {
		if (effect.getEffected() instanceof Player) {
			Player player = (Player) effect.getEffected();
			player.setDualEffectValue(value);
			player.getGameStats().setSkillEfficiency(skillEfficiency / 100f);
			player.getGameStats().setMaxDamageChance(maxDamageChance + effect.getSkillLevel() * maxDamageDelta);
			player.getGameStats().setMinDamageRatio((value + effect.getSkillLevel() * delta) / 100f);
		}
		if (change == null) {
			return;
		}

		List<IStatFunction> modifiers = getModifiers(effect);
		List<IStatFunction> masteryModifiers = new ArrayList<IStatFunction>(modifiers.size());
		for (IStatFunction modifier : modifiers) {
			masteryModifiers.add(new StatDualWeaponMasteryFunction(effect, modifier));
		}
		if (masteryModifiers.size() > 0) {
			effect.getEffected().getGameStats().addEffect(effect, masteryModifiers);
		}
	}

	@Override
	public void endEffect(Effect effect) {
		if (effect.getEffected() instanceof Player) {
			Player player = (Player) effect.getEffected();
			player.setDualEffectValue(0);
			player.getGameStats().setSkillEfficiency(0);
			player.getGameStats().setMaxDamageChance(0);
			player.getGameStats().setMinDamageRatio(0);
		}
		super.endEffect(effect);
	}

	/**
	 * 判断玩家是否拥有双持效果（未登场时扫描技能，否则看效率值）。
	 * Checks whether the player has dual-wield effect (scan skills if not spawned, else efficiency).
	 *
	 * @param player 玩家 / player
	 * @return 是否有双持效果 / true if dual-wield is active
	 */
	public static boolean hasDualWieldEffect(Player player) {
		if (!player.isSpawned()) {
			for (PlayerSkillEntry skillEntry : player.getSkillList().getAllSkills()) {
				SkillTemplate skillTemplate = DataManager.SKILL_DATA.getSkillTemplate(skillEntry.getSkillId());
				if (skillTemplate == null) {
					continue;
				}
				Effects effects = skillTemplate.getEffects();
				if (effects != null && effects.isEffectTypePresent(EffectType.WEAPONDUAL)) {
					return true;
				}
			}
		}
		return player.getGameStats().getSkillEfficiency() != 0;
	}
}
