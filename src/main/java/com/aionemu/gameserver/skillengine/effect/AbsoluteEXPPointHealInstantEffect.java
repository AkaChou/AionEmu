package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 绝对经验点数即时治疗：按固定点数增加目标玩家的伯丁之星（Berdin Star）。
 * Absolute EXP point instant heal: adds a fixed amount of Berdin Star points to the target player.
 */
public class AbsoluteEXPPointHealInstantEffect extends EffectTemplate {

	@XmlAttribute(required = true)
	protected int points;

	/**
	 * 为目标玩家增加指定伯丁之星点数并同步属性包。
	 * Adds the configured Berdin Star points to the target player and syncs stats.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void applyEffect(Effect effect) {
		if ((effect.getEffected() instanceof Player)) {
			Player player = (Player) effect.getEffected();
			player.getCommonData().addBerdinStar(points);
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
		}
	}
}
