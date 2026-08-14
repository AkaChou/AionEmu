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
 * 创造力属性：知识，按等级变更知识加成。
 * Creativity stat: Knowledge; applies knowledge bonus by rank.
 */
public class Knowledge implements StatOwner {

	private static volatile ObjectProvider<Knowledge> instanceProvider;

	private List<IStatFunction> knowledge = new ArrayList<IStatFunction>();

	/**
	 * 属性变更时重算。
	 * Recalculates when the stat changes.
	 *
	 * @param player 玩家 / player
	 * @param point 点数 / point
	 */
	public void onChange(Player player, int point) {
		if (point >= 1) {
			knowledge.clear();
			player.getGameStats().endEffect(this);
			knowledge.add(new StatAddFunction(StatEnum.HKNO, point, true));
			player.getGameStats().addEffect(this, knowledge);
		} else if (point == 0) {
			knowledge.clear();
			knowledge.add(new StatAddFunction(StatEnum.HKNO, point, false));
			player.getGameStats().endEffect(this);
		}
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 *
	 * @return 服务单例 / service singleton
	 */
	public static Knowledge getInstance() {
		ObjectProvider<Knowledge> provider = instanceProvider;
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
	public static void setInstanceProvider(ObjectProvider<Knowledge> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {

		private static final Knowledge INSTANCE = new Knowledge();
	}
}
