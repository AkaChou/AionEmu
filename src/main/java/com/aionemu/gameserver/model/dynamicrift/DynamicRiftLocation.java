package com.aionemu.gameserver.model.dynamicrift;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.dynamicrift.DynamicRiftTemplate;
import com.aionemu.gameserver.services.dynamicriftservice.DynamicRift;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 动态裂隙位置模型。
 * Dynamic Rift Location model.
 *
 * @author Rinzler (Encom)
 */

@NoArgsConstructor
public class DynamicRiftLocation {
	/** 返回 ID / Returns the id */
	@Getter
	protected int id;
	/** 是否激活。 / Whether Active. */
	@Getter
	protected boolean isActive;
	protected DynamicRiftTemplate template;
	/** 返回激活的动态裂隙 / Returns the active dynamic rift */
	@Getter
	protected DynamicRift<DynamicRiftLocation> activeDynamicRift;
	/** 返回玩家集合 / Returns the players */
	@Getter
	protected Map<Integer, Player> players = new HashMap<>();
	/** 返回是否已刷新 / Returns the spawned */
	@Getter
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public DynamicRiftLocation(DynamicRiftTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置激活的动态裂隙 / Sets the active dynamic rift */
	public void setActiveDynamicRift(DynamicRift<DynamicRiftLocation> dynamicRift) {
		isActive = dynamicRift != null;
		this.activeDynamicRift = dynamicRift;
	}

	/** 获取模板。 / Returns the template. */
	public final DynamicRiftTemplate getTemplate() {
		return template;
	}
}
