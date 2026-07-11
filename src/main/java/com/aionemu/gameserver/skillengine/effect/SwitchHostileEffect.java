package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.controllers.attack.AggroList;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 仇恨切换效果：交换玩家与其召唤物在目标仇恨列表中的仇恨值。
 * Switch-hostile effect: swaps hate between the player and their summon on the target aggro list.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SwitchHostileEffect")
public class SwitchHostileEffect extends EffectTemplate {

	/**
	 * 读取玩家与召唤物仇恨并互换。
	 * Reads player and summon hate, then swaps them on the aggro list.
	 */
	@Override
	public void applyEffect(Effect effect) {

		Creature effected = effect.getEffected();
		Creature effector = effect.getEffector();
		AggroList aggroList = effected.getAggroList();

		if (((Player) effector).getSummon() != null) {
			Creature summon = ((Player) effector).getSummon();
			int playerHate = aggroList.getAggroInfo(effector).getHate();
			int summonHate = aggroList.getAggroInfo(((Player) effector).getSummon()).getHate();

			aggroList.stopHating(summon);
			aggroList.stopHating(effector);
			aggroList.addHate(effector, summonHate);
			aggroList.addHate(summon, playerHate);
		}
	}
}
