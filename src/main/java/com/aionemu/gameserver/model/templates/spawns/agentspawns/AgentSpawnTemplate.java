package com.aionemu.gameserver.model.templates.spawns.agentspawns;

import com.aionemu.gameserver.model.agent.AgentStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

/**
 * 代理人刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler (Encom)
 */

public class AgentSpawnTemplate extends SpawnTemplate {
	private int id;
	private AgentStateType agentType;

	public AgentSpawnTemplate(SpawnGroup2 spawnGroup, SpawnSpotTemplate spot) {
		super(spawnGroup, spot);
	}

	public AgentSpawnTemplate(SpawnGroup2 spawnGroup, float x, float y, float z, byte heading, int randWalk,
			String walkerId, int entityId, int fly) {
		super(spawnGroup, x, y, z, heading, randWalk, walkerId, entityId, fly);
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回状态类型 / Returns the a state type */
	public AgentStateType getAStateType() {
		return agentType;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 设置 a state type / Sets the a state type */
	public void setAStateType(AgentStateType agentType) {
		this.agentType = agentType;
	}

	/**
	 * @return Whether agent fight / Whether agent fight
	 */
	public final boolean isAgentFight() {
		return agentType.equals(AgentStateType.FIGHT);
	}

	/**
	 * @return Whether agent peace / Whether agent peace
	 */
	public final boolean isAgentPeace() {
		return agentType.equals(AgentStateType.PEACE);
	}
}
