package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.shield.Shield;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 要塞护盾观察者：非友方/非 GM 穿越激活护盾时死亡。
 * Fortress shield observer: non-friendly non-GM creatures die when passing an active shield.
 */
public class ShieldObserver extends ActionObserver {
	/** 被观察生物 / Observed creature */
	private Creature creature;
	/** 护盾对象 / Shield object */
	private Shield shield;
	/** 上一位置 / Previous position */
	private Point3D oldPosition;

	/**
	 * 空构造（字段为 null）。
	 * Empty constructor (fields null).
	 */
	public ShieldObserver() {
		super(ObserverType.MOVE);
		this.creature = null;
		this.shield = null;
		this.oldPosition = null;
	}

	/**
	 * @param shield 护盾对象 / shield object
	 * @param creature 被观察生物 / observed creature
	 */
	public ShieldObserver(Shield shield, Creature creature) {
		super(ObserverType.MOVE);
		this.creature = creature;
		this.shield = shield;
		this.oldPosition = new Point3D(creature.getX(), creature.getY(), creature.getZ());
	}

	@Override
	public void moved() {
		boolean isGM = false;
		boolean passedThrough = false;
		boolean isFriendlyShield = false;
		if (GameFeatureServices.siegeService().getFortress(shield.getId()).isUnderShield()) {
			if (!(creature.getZ() < shield.getZ() && oldPosition.getZ() < shield.getZ())) {
				if (MathUtil.isInSphere(shield, (float) oldPosition.getX(), (float) oldPosition.getY(),
						(float) oldPosition.getZ(), shield.getTemplate().getRadius()) != MathUtil.isIn3dRange(shield,
								creature, shield.getTemplate().getRadius())) {
					passedThrough = true;
				}
			}
		}
		if (passedThrough) {
			if (creature instanceof Player) {
				PacketSendUtility.sendMessage(((Player) creature), "You passed through shield.");
				isGM = ((Player) creature).isGM();
				if (!GameFeatureServices.siegeService().getFortresses().get(shield.getId()).isEnemy(creature)) {
					isFriendlyShield = true;
				}
			}
			if (!isGM && !isFriendlyShield) {
				if (!(creature.getLifeStats().isAlreadyDead())) {
					creature.getController().die();
				}
				if (creature instanceof Player) {
					((Player) creature).getFlyController().endFly(true);
				}
				creature.getObserveController().removeObserver(this);
			}
		}
		oldPosition.x = creature.getX();
		oldPosition.y = creature.getY();
		oldPosition.z = creature.getZ();
	}
}
