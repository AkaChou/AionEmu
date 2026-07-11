package com.aionemu.gameserver.services.player.CreativityPanel.stats;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 创造力属性：命中，按等级变更命中加成。
 * Creativity stat: Accuracy; applies accuracy bonus by rank.
 *
 * @author Ranastic (Encom)
 */


public class Accuracy implements StatOwner {
	private static volatile ObjectProvider<Accuracy> instanceProvider;

	private List<IStatFunction> accuracy = new ArrayList<IStatFunction>();

	/**
	 * 属性变更时重算。
	 * Recalculates when the stat changes.
	 *
	 * 玩家 / player
	 * point
	 */
	public void onChange(Player player, int point) {
		if (point >= 1) {
			accuracy.clear();
			player.getGameStats().endEffect(this);
			accuracy.add(new StatAddFunction(StatEnum.HAGI, point, true));
			player.getGameStats().addEffect(this, accuracy);
		} else if (point == 0) {
			accuracy.clear();
			accuracy.add(new StatAddFunction(StatEnum.HAGI, point, false));
			player.getGameStats().endEffect(this);
		}
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * result
	 */
	public static Accuracy getInstance() {
		ObjectProvider<Accuracy> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> NewSingletonHolder.INSTANCE);
		}
		return NewSingletonHolder.INSTANCE;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<Accuracy> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {
		private static final Accuracy INSTANCE = new Accuracy();
	}
}
