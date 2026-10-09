package com.aionemu.gameserver.world.knownlist;

import com.aionemu.gameserver.model.gameobjects.Minion;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;

/**
 * 守护灵已知列表：主仆之间不受可见距离约束，跟随物的生命周期由显式事件决定。
 * Minion known list: the master-minion pair ignores visibility distance; the companion's
 * lifecycle is driven by explicit events, not by the master's known-list range.
 * <p>背景：飞行传送、风之路（windstream）与高移速下，主人 KnownList 的 95m 可见距离会把
 * minion 反复移除/加回，客户端「取消召唤/召唤了」成对刷屏（CL-001）。主人与守护灵互相视为
 * 永久在范围内；其余对象仍按 {@link PlayerAwareKnownList} 的默认规则感知（95m + Z 上限）。</p>
 * <p>Background: during fly teleports, windstreams or very fast movement the master's 95m
 * visibility distance kept dropping and re-adding the minion, spamming the client with paired
 * "unsummon/summon" messages (CL-001). The master-minion pair is always in range for each
 * other; every other object still follows the default {@link PlayerAwareKnownList} rules.</p>
 */
public class MinionKnownList extends PlayerAwareKnownList {

	/**
	 * 创建守护灵已知列表。
	 * Creates a minion known list.
	 * @param owner 列表所有者（守护灵） / list owner (the minion)
	 */
	public MinionKnownList(VisibleObject owner) {
		super(owner);
	}

	/**
	 * 判断目标是否为本守护灵的主人。
	 * Checks whether the object is this minion's master.
	 * @param object 待检查对象 / object to check
	 * @return 是主人返回 {@code true} / {@code true} when the object is the master
	 */
	private boolean isMaster(VisibleObject object) {
		return owner instanceof Minion minion && minion.getMaster() == object;
	}

	/**
	 * {@inheritDoc}
	 * 主人永远在感知范围内，其余对象沿用默认距离判定。
	 * The master is always in range; other objects keep the default range check.
	 */
	@Override
	protected boolean checkObjectInRange(VisibleObject newObject) {
		return isMaster(newObject) || super.checkObjectInRange(newObject);
	}

	/**
	 * {@inheritDoc}
	 * 对主人的反向距离判定恒为在范围内（主人侧 forget/find 以此保留本守护灵）。
	 * The reverse range check against the master always succeeds (the master's forget/find
	 * path keeps this minion because of it).
	 */
	@Override
	protected boolean checkReversedObjectInRange(VisibleObject newObject) {
		return isMaster(newObject) || super.checkReversedObjectInRange(newObject);
	}
}
