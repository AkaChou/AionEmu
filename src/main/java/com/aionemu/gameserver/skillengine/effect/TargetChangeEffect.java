package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_SELECTED;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_UPDATE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 目标变更效果：对玩家强制切换或清空当前目标（部分技能有概率切向施法者）。
 * Target-change effect: forces a player to retarget or clear target (some skills may retarget the effector).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TargetChangeEffect")
public class TargetChangeEffect extends EffectTemplate {

	/**
	 * 本效果无即时结算逻辑。
	 * No instant apply logic for this effect.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void applyEffect(Effect effect) {
	}

	/**
	 * 启动时变更玩家目标：特定技能概率切向施法者，否则清空目标。
	 * On start, changes player target: listed skills may retarget effector, otherwise clear target.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	@Override
	public void startEffect(Effect effect) {
		Creature effected = effect.getEffected();
		if (effected instanceof Player) {
			if (effect.getSkillId() == 2992 || effect.getSkillId() == 3018 || effect.getSkillId() == 2511
					|| effect.getSkillId() == 2952 || effect.getSkillId() == 22099 || effect.getSkillId() == 22099
					|| effect.getSkillId() == 22100 || effect.getSkillId() == 22101 || effect.getSkillId() == 22102
					|| effect.getSkillId() == 22103 || effect.getSkillId() == 22104 || effect.getSkillId() == 22557
					|| effect.getSkillId() == 22558 || effect.getSkillId() == 22559 || effect.getSkillId() == 22560
					|| effect.getSkillId() == 22561 || effect.getSkillId() == 22562 || effect.getSkillId() == 22563
					|| effect.getSkillId() == 22564 || effect.getSkillId() == 22565 || effect.getSkillId() == 22566
					|| effect.getSkillId() == 22567 || effect.getSkillId() == 22568 || effect.getSkillId() == 22569
					|| effect.getSkillId() == 22570 || effect.getSkillId() == 22571 || effect.getSkillId() == 22572
					|| effect.getSkillId() == 22573 || effect.getSkillId() == 22574 || effect.getSkillId() == 22575
					|| effect.getSkillId() == 22576 || effect.getSkillId() == 22577 || effect.getSkillId() == 22578
					|| effect.getSkillId() == 22579 || effect.getSkillId() == 22580 || effect.getSkillId() == 22581
					|| effect.getSkillId() == 22582 || effect.getSkillId() == 22583 || effect.getSkillId() == 22584
					|| effect.getSkillId() == 22585 || effect.getSkillId() == 22586 || effect.getSkillId() == 22587
					|| effect.getSkillId() == 22588 || effect.getSkillId() == 22589 || effect.getSkillId() == 22590
					|| effect.getSkillId() == 22591 || effect.getSkillId() == 22592 || effect.getSkillId() == 22593
					|| effect.getSkillId() == 22594 || effect.getSkillId() == 22595 || effect.getSkillId() == 22596
					|| effect.getSkillId() == 22597 || effect.getSkillId() == 22598 || effect.getSkillId() == 22599
					|| effect.getSkillId() == 22600 || effect.getSkillId() == 22601 || effect.getSkillId() == 22666
					|| effect.getSkillId() == 22665) {
				int random = Rnd.get(0, 1000);
				if (random < 250) {
					if (effect.getEffector() != null) {
						effected.setTarget(effect.getEffector());
						PacketSendUtility.sendPacket((Player) effected, new SM_TARGET_SELECTED((Player) effected));
						PacketSendUtility.broadcastPacket(effected, new SM_TARGET_UPDATE((Player) effected));
					}
				}
			} else {
				effected.setTarget(null);
				PacketSendUtility.sendPacket((Player) effected, new SM_TARGET_SELECTED(null));
				PacketSendUtility.broadcastPacket(effected, new SM_TARGET_UPDATE((Player) effected));
			}
		}
	}
}
