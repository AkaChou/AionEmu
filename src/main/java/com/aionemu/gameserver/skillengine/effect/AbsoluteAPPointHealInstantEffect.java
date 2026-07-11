package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 绝对欧比斯点数即时治疗：按固定点数增加目标玩家的欧比斯恩惠（Abyss Favor）。
 * Absolute AP point instant heal: adds a fixed amount of abyss favor to the target player.
 */
public class AbsoluteAPPointHealInstantEffect extends EffectTemplate {

	@XmlAttribute(required = true)
	protected int points;

	/**
	 * 为目标玩家增加指定欧比斯恩惠点数并同步属性包。
	 * Adds the configured abyss-favor points to the target player and syncs stats.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void applyEffect(Effect effect) {
		if ((effect.getEffected() instanceof Player)) {
			Player player = (Player) effect.getEffected();
			player.getCommonData().addAbyssFavor(points);
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		}
	}
}
