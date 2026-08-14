package com.aionemu.gameserver.spawnengine;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;

import lombok.Getter;

/**
 * 集群巡逻 NPC 刷怪信息，用于组建巡逻队并生成 NPC。
 * Spawn data for a clustered walker NPC, used to form groups and spawn NPCs.
 *
 * @author vlog
 * @modified Rolandas
 */
public class ClusteredNpc {

	/**
	 * 关联 NPC。
	 * Associated NPC.
	 */
	@Getter
	private Npc npc;

	/**
	 * 实例 ID。
	 * Instance id.
	 */
	@Getter
	private int instance;

	/**
	 * 巡逻路径模板。
	 * Walker route template.
	 */
	@Getter
	private WalkerTemplate walkTemplate;

	/**
	 * 刷怪 X 坐标。
	 * Spawn X coordinate.
	 */
	@Getter
	private float x;

	/**
	 * 刷怪 Y 坐标。
	 * Spawn Y coordinate.
	 */
	@Getter
	private float y;

	/**
	 * 巡逻成员序号。
	 * Walker member index.
	 */
	private int walkerIdx;

	/**
	 * 以 NPC、实例与巡逻模板构造集群数据。
	 * Builds clustered data from an NPC, instance and walker template.
	 *
	 * @param npc 关联 NPC / the associated NPC
	 * @param instance 实例 ID / the instance id
	 * @param walkTemplate 巡逻路径模板 / the walker route template
	 */
	public ClusteredNpc(Npc npc, int instance, WalkerTemplate walkTemplate) {
		this.npc = npc;
		this.instance = instance;
		this.walkTemplate = walkTemplate;
		this.x = npc.getSpawn().getX();
		this.y = npc.getSpawn().getY();
		this.walkerIdx = npc.getSpawn().getWalkerIndex();
	}

	/**
	 * 将 NPC 以当前坐标刷入世界。
	 * Brings the NPC into the world at the current coordinates.
	 *
	 * @param z 高度 / height Z
	 */
	public void spawn(float z) {
		SpawnEngine.bringIntoWorld(npc, npc.getSpawn().getWorldId(), instance, x, y, z, npc.getSpawn().getHeading());
	}

	/**
	 * 重生时替换底层 NPC，并迁移巡逻偏移与坐标。
	 * Replaces the underlying NPC on respawn and migrates walker shift and coordinates.
	 *
	 * @param npc 新 NPC / the new NPC
	 */
	public void setNpc(Npc npc) {
		npc.setWalkerGroupShift(this.npc.getWalkerGroupShift());
		this.npc = npc;
		x = npc.getSpawn().getX();
		y = npc.getSpawn().getY();
	}

	/**
	 * 判断与另一集群 NPC 是否坐标相同。
	 * Whether this clustered NPC shares the same X/Y as another.
	 *
	 * @param other 另一集群 NPC / other clustered npc
	 * @return 坐标相同则为 true / true if same position
	 */
	public boolean hasSamePosition(ClusteredNpc other) {
		if (this == other)
			return true;
		if (other == null) {
			return false;
		}
		return this.x == other.x && this.y == other.y;
	}

	/**
	 * 基于 X/Y 的位置哈希，用于编队分组。
	 * Position hash from X/Y used when grouping for formation.
	 *
	 * @return 位置哈希 / the position hash
	 */
	public int getPositionHash() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(x);
		result = prime * result + Float.floatToIntBits(y);
		return result;
	}

	/**
	 * 相对路径第一步的 X 偏移。
	 * X delta relative to the first route step.
	 *
	 * @return X 偏移 / the X delta
	 */
	public float getXDelta() {
		return walkTemplate.getRouteStep(1).getX() - x;
	}

	/**
	 * 设置 X 坐标并同步到刷怪模板。
	 * Sets X and syncs it onto the spawn template.
	 *
	 * @param x X 坐标 / X coordinate
	 */
	public void setX(float x) {
		this.x = x;
		this.getNpc().getSpawn().setX(x);
	}

	/**
	 * 相对路径第一步的 Y 偏移。
	 * Y delta relative to the first route step.
	 *
	 * @return Y 偏移 / the Y delta
	 */
	public float getYDelta() {
		return walkTemplate.getRouteStep(1).getY() - y;
	}

	/**
	 * 设置 Y 坐标并同步到刷怪模板。
	 * Sets Y and syncs it onto the spawn template.
	 *
	 * @param y Y 坐标 / Y coordinate
	 */
	public void setY(float y) {
		this.y = y;
		this.getNpc().getSpawn().setY(y);
	}

	/**
	 * 巡逻成员序号。
	 * Walker member index.
	 *
	 * @return 巡逻成员序号 / the walker index
	 */
	public int getWalkerIndex() {
		return walkerIdx;
	}
}
