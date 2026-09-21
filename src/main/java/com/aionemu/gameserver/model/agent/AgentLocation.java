package com.aionemu.gameserver.model.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.agent.AgentTemplate;
import com.aionemu.gameserver.services.agentservice.AgentFight;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 代理人位置模型。
 * Agent Location model.
 *
 * @author Rinzler (Encom)
 */

@Getter
@NoArgsConstructor
public class AgentLocation {
	/** 返回 ID / Returns the id */
	protected int id;
	/** 是否激活。 / Whether Active. */
	protected boolean isActive;
	protected AgentTemplate template;
	/** 返回当前战斗 / Returns the active fight */
	protected AgentFight<AgentLocation> activeAgent;
	/** 返回玩家集合 / Returns the players */
	protected Map<Integer, Player> players = new HashMap<>();
	/** 返回已刷新的对象列表 / Returns the spawned objects */
	private final List<VisibleObject> spawned = new ArrayList<>();

	public AgentLocation(AgentTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置当前战斗 / Sets the active fight */
	public void setActiveAgent(AgentFight<AgentLocation> agent) {
		isActive = agent != null;
		this.activeAgent = agent;
	}

	/** 获取模板。 / Returns the template. */
	public final AgentTemplate getTemplate() {
		return template;
	}
}
