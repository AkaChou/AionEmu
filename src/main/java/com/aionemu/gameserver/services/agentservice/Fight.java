package com.aionemu.gameserver.services.agentservice;

import com.aionemu.gameserver.model.agent.AgentLocation;
import com.aionemu.gameserver.model.agent.AgentStateType;

/**
 * 代理战默认实现：切入 FIGHT / 回到 PEACE。
 * back to PEACE. / back to PEACE.
 *
 * @author Rinzler (Encom)
 */
public class Fight extends AgentFight<AgentLocation> {

	/**
	 * 绑定代理地点。
	 * Binds the agent location.
	 *
	 * location
	 */
	public Fight(AgentLocation agent) {
		super(agent);
	}

	/**
	 * 激活战斗并刷新 FIGHT 刷怪。
	 * Activates the fight and spawns FIGHT entities.
	 */
	@Override
	public void startAgentFight() {
		getAgentLocation().setActiveAgent(this);
		despawn();
		spawn(AgentStateType.FIGHT);
	}

	/**
	 * 结束战斗并恢复 PEACE 刷怪。
	 * Ends the fight and restores PEACE spawns.
	 */
	@Override
	public void stopAgentFight() {
		getAgentLocation().setActiveAgent(null);
		despawn();
		spawn(AgentStateType.PEACE);
	}
}
