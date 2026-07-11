package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAttribute;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATUPDATE_EXP;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 触发即时安息能量（VP/Reposte）治疗：在上限内增加玩家安息能量。
 * Proc instant repose-energy (VP) heal: adds repose energy up to a configured cap.
 */
public class ProcVPHealInstantEffect extends EffectTemplate {
	@XmlAttribute(required = true)
	protected int value2;

	@XmlAttribute
	protected boolean percent;

	/**
	 * 在安息能量未达 value2% 上限时增加能量并同步经验包。
	 * Adds repose energy while under the value2% cap and syncs the exp packet.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void applyEffect(Effect effect) {
		if ((effect.getEffected() instanceof Player)) {
			Player player = (Player) effect.getEffected();
			PlayerCommonData pcd = player.getCommonData();
			long cap = pcd.getMaxReposteEnergy() * value2 / 100;
			if (pcd.getCurrentReposteEnergy() < cap) {
				int valueWithDelta = value + delta * effect.getSkillLevel();
				long addEnergy = 0;
				if (percent) {
					addEnergy = (int) (pcd.getMaxReposteEnergy() * valueWithDelta * 0.001);
				} else {
					addEnergy = valueWithDelta;
				}
				pcd.addReposteEnergy(addEnergy);
				PacketSendUtility.sendPacket(player, new SM_STATUPDATE_EXP(pcd.getExpShown(), pcd.getExpRecoverable(),
						pcd.getExpNeed(), pcd.getCurrentReposteEnergy(), pcd.getMaxReposteEnergy()));
			}
		}
	}
}
