package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 狂风旋涡观察者：玩家从 12 外进入 12 内时触发。
 * Gale cyclone observer: fires when the player moves from beyond 12 into range 12.
 *
 * @author xTz
 */
public abstract class GaleCycloneObserver extends ActionObserver {

	/** 被观察玩家 / Observed player */
	private Player player;
	/** 旋涡生物 / Cyclone creature */
	private Creature creature;
	/** 上一次距离 / Previous distance */
	private double oldRange;

	/**
	 * @param player 被观察玩家 / observed player
	 * @param creature 旋涡生物 / cyclone creature
	 */
	public GaleCycloneObserver(Player player, Creature creature) {
		super(ObserverType.MOVE);
		this.player = player;
		this.creature = creature;
		oldRange = MathUtil.getDistance(player, creature);
	}

	@Override
	public void moved() {
		double newRange = MathUtil.getDistance(player, creature);
		if (creature == null || creature.getLifeStats().isAlreadyDead()) {
			if (player != null) {
				player.getObserveController().removeObserver(this);
			}
			return;
		}
		if (oldRange > 12 && newRange <= 12) {
			onMove();
		}
		oldRange = newRange;
	}

	/**
	 * 玩家进入旋涡有效范围时调用。
	 * Called when the player enters the cyclone effective range.
	 */
	public abstract void onMove();
}
