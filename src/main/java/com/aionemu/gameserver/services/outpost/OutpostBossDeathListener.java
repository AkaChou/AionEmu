package com.aionemu.gameserver.services.outpost;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.eventcallback.OnDieEventCallback;
import com.aionemu.gameserver.dao.OutpostDAO;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.services.OutpostService;

/**
 * 前哨 BOSS 死亡监听器，按最大伤害方切换前哨归属。
 * Outpost boss death listener that switches outpost ownership by top damager.
 *
 * @author Wnkrz
 */
public class OutpostBossDeathListener extends OnDieEventCallback {
	private final Outpost<?> outpost;

	/**
	 * 绑定目标前哨实例。
	 * Binds the target outpost instance.
	 *
	 * @param outpost 目标前哨 / target outpost
	 */
	public OutpostBossDeathListener(Outpost outpost) {
		this.outpost = outpost;
	}

	/**
	 * BOSS 死亡前根据仇恨列表结算归属并触发占领。
	 * Before boss death, resolves ownership from aggro list and triggers capture.
	 *
	 * @param obj 死亡的 AI / dying AI
	 */
	@Override
	public void onBeforeDie(AbstractAI obj) {
		AionObject winner = outpost.getBoss().getAggroList().getMostDamage();
		if (winner instanceof Creature) {
			final Creature kill = (Creature) winner;
			if (kill.getRace().isPlayerRace()) {
				outpost.setRace(kill.getRace());
			}
		} else if (winner instanceof TemporaryPlayerTeam) {
			final TemporaryPlayerTeam team = (TemporaryPlayerTeam) winner;
			if (team.getRace().isPlayerRace()) {
				outpost.setRace(team.getRace());
			}
		} else {
			outpost.setRace(Race.NPC);
		}
		GameLocationBootstrapServices.outpostService().capture(outpost.getId(), outpost.getRace());
	}

	/**
	 * BOSS 死亡后回调（当前无额外逻辑）。
	 * After-death callback (no-op currently).
	 *
	 * @param obj 死亡的 AI / dying AI
	 */
	@Override
	public void onAfterDie(AbstractAI obj) {
	}

	/**
	 * 获取前哨 DAO。
	 * Returns the outpost DAO.
	 *
	 * @return 前哨 DAO / outpost DAO
	 */
	private OutpostDAO getDAO() {
		return DAOManager.getDAO(OutpostDAO.class);
	}
}
