package com.aionemu.gameserver.model.instancerift;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.instancerift.InstanceRiftTemplate;
import com.aionemu.gameserver.services.instanceriftservice.RiftInstance;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 副本裂隙位置模型。
 * Instance Rift Location model.
 *
 * @author Rinzler (Encom)
 */

@Getter
@NoArgsConstructor
public class InstanceRiftLocation {
	/** 返回 ID / Returns the id */
	protected int id;
	/** 是否激活。 / Whether Active. */
	protected boolean isActive;
	protected InstanceRiftTemplate template;
	/** 返回 active instance rift / Returns the active instance rift */
	protected RiftInstance<InstanceRiftLocation> activeInstanceRift;
	/** 返回玩家集合 / Returns the players */
	protected Map<Integer, Player> players = new HashMap<>();
	/** 返回已生成的对象列表 / Returns the spawned objects */
	private final List<VisibleObject> spawned = new ArrayList<>();

	public InstanceRiftLocation(InstanceRiftTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	/** 设置 active instance rift / Sets the active instance rift */
	public void setActiveInstanceRift(RiftInstance<InstanceRiftLocation> instanceRift) {
		isActive = instanceRift != null;
		this.activeInstanceRift = instanceRift;
	}

	/** 获取模板。 / Returns the template. */
	public final InstanceRiftTemplate getTemplate() {
		return template;
	}
}
