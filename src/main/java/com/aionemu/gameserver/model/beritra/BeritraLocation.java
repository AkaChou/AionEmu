package com.aionemu.gameserver.model.beritra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.beritra.BeritraTemplate;
import com.aionemu.gameserver.services.beritraservice.BeritraInvasion;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 布里特拉位置模型。
 * Beritra Location model.
 * @author Rinzler (Encom)
 */

@Getter
@NoArgsConstructor
public class BeritraLocation {
	/** 返回 ID / Returns the id */
	protected int id;
	/** 是否激活。 / Whether Active. */
	protected boolean isActive;
	protected BeritraTemplate template;
	/** 返回 active beritra / Returns the active beritra */
	protected BeritraInvasion<BeritraLocation> activeBeritra;
	/** 返回玩家集合 / Returns the players */
	protected Map<Integer, Player> players = new HashMap<>();
	/** 返回是否已刷新 / Returns the spawned */
	private final List<VisibleObject> spawned = new ArrayList<>();

	public BeritraLocation(BeritraTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置 active beritra / Sets the active beritra */
	public void setActiveBeritra(BeritraInvasion<BeritraLocation> beritra) {
		isActive = beritra != null;
		this.activeBeritra = beritra;
	}

	/** 获取模板。 / Returns the template. */
	public final BeritraTemplate getTemplate() {
		return template;
	}
}
