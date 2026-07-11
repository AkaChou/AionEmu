package com.aionemu.gameserver.model.bonus_service;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatRateFunction;
import com.aionemu.gameserver.model.templates.bonus_service.BonusPenaltyAttr;
import com.aionemu.gameserver.model.templates.bonus_service.BonusServiceAttr;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ICON_INFO;
import com.aionemu.gameserver.skillengine.change.Func;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 服务 Buff，用于加成服务相关逻辑。
 * Service Buff for bonus service logic.
 *
 * @author Ranastic (Encom)
 */

public class ServiceBuff implements StatOwner {
	private List<IStatFunction> functions = new ArrayList<IStatFunction>();
	private BonusServiceAttr serviceBonusAttr;

	public ServiceBuff(int buffId) {
		serviceBonusAttr = DataManager.SERVICE_BUFF_DATA.getInstanceBonusattr(buffId);
	}

	/** 应用效果。 / Apply effect. */
	public void applyEffect(Player player, int buffId) {
		if (serviceBonusAttr == null) {
			return;
		}
		for (BonusPenaltyAttr bonusPenaltyAttr : serviceBonusAttr.getPenaltyAttr()) {
			if (bonusPenaltyAttr.getFunc().equals(Func.PERCENT)) {
				functions.add(new StatRateFunction(bonusPenaltyAttr.getStat(), bonusPenaltyAttr.getValue(), true));
			} else {
				functions.add(new StatAddFunction(bonusPenaltyAttr.getStat(), bonusPenaltyAttr.getValue(), true));
			}
		}
		player.setBonus(true);
		player.getGameStats().addEffect(this, functions);
		PacketSendUtility.sendPacket(player, new SM_ICON_INFO(buffId, true));
	}

	/** 结束效果 / End Effect */
	public void endEffect(Player player, int buffId) {
		functions.clear();
		player.setBonus(false);
		player.getGameStats().endEffect(this);
		PacketSendUtility.sendPacket(player, new SM_ICON_INFO(buffId, false));
	}
}
