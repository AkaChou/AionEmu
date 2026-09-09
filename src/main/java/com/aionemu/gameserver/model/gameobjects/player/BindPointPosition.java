package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import lombok.Getter;

/**
 * 绑定点坐标游戏对象。
 * Bind Point Position game object.
 *
 * @author evilset
 */
public class BindPointPosition {

	/**
	 * @return 地图 ID / the mapId
	 */
	@Getter
	private final int mapId;
	/**
	 * @return X 坐标 / the x
	 */
	@Getter
	private final float x;
	/**
	 * @return Y 坐标 / the y
	 */
	@Getter
	private final float y;
	/**
	 * @return Z 坐标 / the z
	 */
	@Getter
	private final float z;
	/**
	 * @return 朝向 / the heading
	 */
	@Getter
	private final byte heading;
	/**
	 * @return the persistentState
	 */
	@Getter
	private PersistentState persistentState;

	/**
	 * @param mapId
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 */
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
