package com.aionemu.gameserver.services.towerofeternityservice;

import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityLocation;
import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityStateType;

/**
 * 永恒之塔默认实现：切入 OPEN / 回到 CLOSED。
 * back to CLOSED. / back to CLOSED.
 *
 * @author Wnkrz
 */
public class Tower extends TowerOfEternity<TowerOfEternityLocation> {

	/**
	 * 绑定永恒之塔地点。
	 * Binds the tower location.
	 *
	 * location
	 */
	public Tower(TowerOfEternityLocation towerOfEternity) {
		super(towerOfEternity);
	}

	/**
	 * 激活活动并刷新 OPEN 刷怪。
	 * Activates the event and spawns OPEN entities.
	 */
	@Override
	protected void startTowerOfEternity() {
		getTowerOfEternityLocation().setActiveTowerOfEternity(this);
		despawn();
		spawn(TowerOfEternityStateType.OPEN);
	}

	/**
	 * 结束活动并恢复 CLOSED 刷怪。
	 * Ends the event and restores CLOSED spawns.
	 */
	@Override
	protected void stopTowerOfEternity() {
		getTowerOfEternityLocation().setActiveTowerOfEternity(null);
		despawn();
		spawn(TowerOfEternityStateType.CLOSED);
	}
}
