/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
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
