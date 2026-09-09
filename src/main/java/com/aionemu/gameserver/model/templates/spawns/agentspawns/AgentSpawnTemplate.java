package com.aionemu.gameserver.model.templates.spawns.agentspawns;

import com.aionemu.gameserver.model.agent.AgentStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import lombok.Getter;
import lombok.Setter;

/**
 * 代理人刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

public class AgentSpawnTemplate extends SpawnTemplate {
	/** 返回 ID / Returns the id */
	@Getter
	@Setter
	private int id;
	private AgentStateType agentType;

	public AgentSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public AgentSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回代理状态类型 / Returns the agent state type */
	public AgentStateType getAStateType() {
		return agentType;
	}

	/** 设置代理状态类型 / Sets the agent state type */
	public void setAStateType(AgentStateType agentType) {
		this.agentType = agentType;
	}

	/**
	 * 是否处于代理战斗状态。
	 * Whether agent fight.
	 *
	 * @return 战斗状态则为 true / true if fighting
	 */
	public final boolean isAgentFight() {
		return agentType.equals(AgentStateType.FIGHT);
	}

	/**
	 * 是否处于代理和平状态。
	 * Whether agent peace.
	 *
	 * @return 和平状态则为 true / true if at peace
	 */
	public final boolean isAgentPeace() {
		return agentType.equals(AgentStateType.PEACE);
	}
}
