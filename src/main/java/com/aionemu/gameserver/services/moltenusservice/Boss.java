package com.aionemu.gameserver.services.moltenusservice;

import com.aionemu.gameserver.model.moltenus.MoltenusLocation;
import com.aionemu.gameserver.model.moltenus.MoltenusStateType;

/**
 * 熔岩领主默认实现：切入 FIGHT / 回到 PEACE。
 * back to PEACE.
 *
 * @author Rinzler (Encom)
 */
public class Boss extends MoltenusFight<MoltenusLocation> {

	/**
	 * 绑定熔岩领主地点。
	 * Binds the Moltenus location.
	 *
	 * @param moltenus 熔岩领主地点 / Moltenus location
	 */
	public Boss(MoltenusLocation moltenus) {
		super(moltenus);
	}

	/**
	 * 激活活动并刷新 FIGHT 刷怪。
	 * Activates the event and spawns FIGHT entities.
	 */
	@Override
	public void startMoltenus() {
		getMoltenusLocation().setActiveMoltenus(this);
		despawn();
		spawn(MoltenusStateType.FIGHT);
	}

	/**
	 * 结束活动并恢复 PEACE 刷怪。
	 * Ends the event and restores PEACE spawns.
	 */
	@Override
	public void stopMoltenus() {
		getMoltenusLocation().setActiveMoltenus(null);
		despawn();
		spawn(MoltenusStateType.PEACE);
	}
}
