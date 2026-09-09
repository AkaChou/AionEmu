package com.aionemu.gameserver.model.conquest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.conquest.ConquestTemplate;
import com.aionemu.gameserver.services.conquestservice.ConquestOffering;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 征服位置模型。
 * Conquest Location model.
 *
 * @author Rinzler (Encom)
 */

@NoArgsConstructor
public class ConquestLocation {
	/** 返回 ID / Returns the id */
	@Getter
	protected int id;
	/** 是否激活。 / Whether Active. */
	@Getter
	protected boolean isActive;
	protected ConquestTemplate template;
	/** 返回当前征服 / Returns the active conquest */
	@Getter
	protected ConquestOffering<ConquestLocation> activeConquest;
	/** 返回玩家集合 / Returns the players */
	@Getter
	protected Map<Integer, Player> players = new HashMap<>();
	/** 返回是否已刷新 / Returns the spawned */
	@Getter
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public ConquestLocation(ConquestTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置当前激活的征服 / Sets the active conquest */
	public void setActiveConquest(ConquestOffering<ConquestLocation> conquest) {
		isActive = conquest != null;
		this.activeConquest = conquest;
	}

	/** 获取模板。 / Returns the template. */
	public final ConquestTemplate getTemplate() {
		return template;
	}
}
