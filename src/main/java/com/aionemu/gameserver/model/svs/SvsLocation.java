package com.aionemu.gameserver.model.svs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.svs.SvsTemplate;
import com.aionemu.gameserver.services.svsservice.Panesterra;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 势力战位置模型。
 * Svs Location model.
 *
 * @author Rinzler (Encom)
 */

@NoArgsConstructor
public class SvsLocation {
	/** 返回 ID / Returns the id */
	@Getter
	protected int id;
	/** 是否激活。 / Whether Active. */
	@Getter
	protected boolean isActive;
	protected SvsTemplate template;
	/** 返回当前势力战 / Returns the active svs */
	@Getter
	protected Panesterra<SvsLocation> activeSvs;
	/** 返回玩家集合 / Returns the players */
	@Getter
	protected Map<Integer, Player> players = new HashMap<>();
	/** 返回已刷新的对象列表 / Returns the spawned objects */
	@Getter
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public SvsLocation(SvsTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置当前势力战 / Sets the active svs */
	public void setActiveSvs(Panesterra<SvsLocation> svs) {
		isActive = svs != null;
		this.activeSvs = svs;
	}

	/** 获取模板。 / Returns the template. */
	public final SvsTemplate getTemplate() {
		return template;
	}
}
