package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import lombok.Getter;

/**
 * 绑定点坐标游戏对象。
 * Bind Point Position game object.
 * @author evilset
 */
@Getter
public class BindPointPosition {

	private final int mapId;
	private final float x;
	private final float y;
	private final float z;
	private final byte heading;
	private PersistentState persistentState;

	public BindPointPosition(int mapId, float x, float y, float z, byte heading) {
		this.mapId = mapId;
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = heading;
		this.persistentState = PersistentState.NEW;
	}

	/**
	 * @param persistentState the persistentState to set
	 */
	public void setPersistentState(PersistentState persistentState) {
		switch (persistentState) {
		case UPDATE_REQUIRED:
			if (this.persistentState == PersistentState.NEW) {
				break;
			}
		default:
			this.persistentState = persistentState;
		}
	}
}
