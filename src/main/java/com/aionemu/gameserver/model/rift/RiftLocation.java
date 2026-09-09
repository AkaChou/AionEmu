package com.aionemu.gameserver.model.rift;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.rift.RiftTemplate;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 裂隙位置模型。
 * Rift Location model.
 *
 * @author Source
 */
@NoArgsConstructor
public class RiftLocation {

	/**
	 * @return Whether opened
	 */
	@Getter
	@Setter
	private boolean opened;
	protected RiftTemplate template;
	/** 返回是否已刷新 / Returns the spawned */
	@Getter
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public RiftLocation(RiftTemplate template) {
		this.template = template;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return template.getId();
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return template.getWorldId();
	}
}
