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
 * 创造力属性：敏捷，按等级变更敏捷加成。
 * Creativity stat: Agility; applies agility bonus by rank.
 */
public class Agility implements StatOwner {

	private static volatile ObjectProvider<Agility> instanceProvider;

	private List<IStatFunction> agility = new ArrayList<IStatFunction>();

	/**
	 * 属性变更时重算。
	 * Recalculates when the stat changes.
	 *
	 * @param player 玩家 / player
	 * @param point 点数 / point
	 */
	public void onChange(Player player, int point) {
		if (point >= 1) {
			agility.clear();
			player.getGameStats().endEffect(this);
			agility.add(new StatAddFunction(StatEnum.HDEX, point, true));
			player.getGameStats().addEffect(this, agility);
		} else if (point == 0) {
			agility.clear();
			agility.add(new StatAddFunction(StatEnum.HDEX, point, false));
			player.getGameStats().endEffect(this);
		}
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 *
	 * @return 服务单例 / service singleton
	 */
	public static Agility getInstance() {
		ObjectProvider<Agility> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> NewSingletonHolder.INSTANCE);
		}
		return NewSingletonHolder.INSTANCE;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * @param provider 提供器 / provider
	 */
	public static void setInstanceProvider(ObjectProvider<Agility> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {

		private static final Agility INSTANCE = new Agility();
	}
}
