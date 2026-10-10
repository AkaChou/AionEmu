package com.aionemu.gameserver.model.gameobjects.player.motion;


import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.RequiredArgsConstructor;

/**
 * Motion 列表。
 * Motion List game object.
 */
@Slf4j

@RequiredArgsConstructor
public class MotionList {
	private final Player owner;
	private Map<Integer, Motion> activeMotions;
	private Map<Integer, Motion> motions;

	/**
	 * 返回 active motions / Returns the active motions
	 */
	public Map<Integer, Motion> getActiveMotions() {
		if (activeMotions == null) {
			return Collections.emptyMap();
		}
		return activeMotions;
	}

	/**
	 * 返回 motions / Returns the motions
	 */
	public Map<Integer, Motion> getMotions() {
		if (motions == null) {
			return Collections.emptyMap();
		}
		return motions;
	}

	/**
	 * 添加。 / Add.
	 */
	public void add(Motion motion, boolean persist) {
		if (motions == null) {
			motions = new HashMap<>();
		}
		if (motions.containsKey(motion.getId()) && motion.getExpireTime() == 0) {
			remove(motion.getId());
		}
		motions.put(motion.getId(), motion);
		if (motion.isActive()) {
			if (activeMotions == null) {
				activeMotions = new HashMap<>();
			}
			Motion old = activeMotions.put(Motion.motionType.get(motion.getId()), motion);
			if (old != null) {
				old.setActive(false);
				PlayerService.daos().motionDAO().updateMotion(owner.getObjectId(), old);
			}
		}
		if (persist) {
			if (motion.getExpireTime() != 0) {
				GameTaskManagerServices.expireTimerTask().addTask(motion, owner);
			}
			PlayerService.daos().motionDAO().storeMotion(owner.getObjectId(), motion);
		}
	}

	/**
	 * 移除。 / Remove.
	 */
	public boolean remove(int motionId) {
		Motion motion = motions.remove(motionId);
		if (motion != null) {
			PacketSendUtility.sendPacket(owner, new SM_MOTION((short) motionId));
			PlayerService.daos().motionDAO().deleteMotion(owner.getObjectId(), motionId);
			if (motion.isActive()) {
				activeMotions.remove(Motion.motionType.get(motionId));
				return true;
			}
		}
		return false;
	}

	/**
	 * 设置 active / Sets the active
	 */
	public void setActive(int motionId, int motionType) {
		if (motionId != 0) {
			Motion motion = motions.get(motionId);
			if (motion == null || motion.isActive()) {
				return;
			}
			if (activeMotions == null) {
				activeMotions = new HashMap<>();
			}
			Motion old = activeMotions.put(motionType, motion);
			if (old != null) {
				old.setActive(false);
				PlayerService.daos().motionDAO().updateMotion(owner.getObjectId(), old);
			}
			motion.setActive(true);
			PlayerService.daos().motionDAO().updateMotion(owner.getObjectId(), motion);
		} else if (activeMotions != null) {
			Motion old = activeMotions.remove(motionType);
			if (old == null) {
				return;
			}
			old.setActive(false);
			PlayerService.daos().motionDAO().updateMotion(owner.getObjectId(), old);
		}
		PacketSendUtility.sendPacket(owner, new SM_MOTION((short) motionId, (byte) motionType));
		PacketSendUtility.broadcastPacket(owner, new SM_MOTION(owner.getObjectId(), activeMotions), true);
	}

	/**
	 * 检查玩家是否拥有指定 ID 的动画。 / Checks if the player has a motion with the given ID.
	 */
	public boolean hasMotion(Integer motionId) {
		return motions != null && motions.containsKey(motionId);
	}
}
