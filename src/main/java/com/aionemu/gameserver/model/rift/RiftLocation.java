package com.aionemu.gameserver.model.rift;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.rift.RiftTemplate;

/**
 * 裂隙位置模型。
 * Rift Location model.
 *
 * @author Source
 */
public class RiftLocation {

	private boolean opened;
	protected RiftTemplate template;
	private List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public RiftLocation() {
	}

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

	/**
	 * @return Whether opened
	 */
	public boolean isOpened() {
		return opened;
	}

	/** 设置 opened / Sets the opened */
	public void setOpened(boolean state) {
		opened = state;
	}

	/** 返回是否已刷新 / Returns the spawned */
	public List<VisibleObject> getSpawned() {
		return spawned;
	}
}
