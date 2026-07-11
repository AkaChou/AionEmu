package com.aionemu.gameserver.model.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.agent.AgentTemplate;
import com.aionemu.gameserver.services.agentservice.AgentFight;

/**
 * 代理人位置模型。
 * Agent Location model.
 *
 * @author Rinzler (Encom)
 */

public class AgentLocation {
	protected int id;
	protected boolean isActive;
	protected AgentTemplate template;
	protected AgentFight<AgentLocation> activeAgent;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public AgentLocation() {
	}

	public AgentLocation(AgentTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 是否激活。 / Whether Active. */
	public boolean isActive() {
		return isActive;
	}

	/** 设置 active agent / Sets the active agent */
	public void setActiveAgent(AgentFight<AgentLocation> agent) {
		isActive = agent != null;
		this.activeAgent = agent;
	}

	/** 返回 active agent / Returns the active agent */
	public AgentFight<AgentLocation> getActiveAgent() {
		return activeAgent;
	}

	/** 获取模板。 / Returns the template. */
	public final AgentTemplate getTemplate() {
		return template;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回是否已刷新 / Returns the spawned */
	public List<VisibleObject> getSpawned() {
		return spawned;
	}

	/** 返回玩家集合 / Returns the players */
	public Map<Integer, Player> getPlayers() {
		return players;
	}
}
