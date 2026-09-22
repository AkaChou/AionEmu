package com.aionemu.gameserver.model.rvr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.rvr.RvrTemplate;
import com.aionemu.gameserver.services.rvrservice.Rvrlf3df3;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 阵营战位置模型。
 * Rvr Location model.
 * @author Rinzler (Encom)
 */

@Getter
@NoArgsConstructor
public class RvrLocation {
	/** 返回 ID / Returns the id */
	protected int id;
	/** 是否激活。 / Whether Active. */
	protected boolean isActive;
	protected RvrTemplate template;
	/** 返回 active rvr / Returns the active rvr */
	protected Rvrlf3df3<RvrLocation> activeRvr;
	/** 返回玩家集合 / Returns the players */
	protected Map<Integer, Player> players = new HashMap<>();
	/** 返回是否已刷新 / Returns the spawned */
	private final List<VisibleObject> spawned = new ArrayList<>();

	public RvrLocation(RvrTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置 active rvr / Sets the active rvr */
	public void setActiveRvr(Rvrlf3df3<RvrLocation> rvr) {
		isActive = rvr != null;
		this.activeRvr = rvr;
	}

	/** 获取模板。 / Returns the template. */
	public final RvrTemplate getTemplate() {
		return template;
	}
}
