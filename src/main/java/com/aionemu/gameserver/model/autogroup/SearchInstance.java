package com.aionemu.gameserver.model.autogroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * Search 副本，用于 autogroup 相关逻辑。
 * Search Instance for autogroup logic.
 */

public class SearchInstance {
	private long registrationTime = System.currentTimeMillis();
	private int instanceMaskId;
	private EntryRequestType ert;
	private List<Integer> members;

	public SearchInstance(int instanceMaskId, EntryRequestType ert, Collection<Player> members) {
		this.instanceMaskId = instanceMaskId;
		this.ert = ert;
		if (members != null) {
			this.members = new ArrayList<Integer>();
			for (Player member : members) {
				this.members.add(member.getObjectId());
			}
		}
	}

	/** 返回成员数 / Returns the members */
	public List<Integer> getMembers() {
		return members;
	}

	/** 返回副本掩码 ID / Returns the instance mask id */
	public int getInstanceMaskId() {
		return instanceMaskId;
	}

	/** 返回剩余时间 / Returns the remaining time */
	public int getRemainingTime() {
		return (int) (System.currentTimeMillis() - registrationTime) / 1000 * 256;
	}

	/** 获取条目请求类型。 / Returns the entry request type. */
	public EntryRequestType getEntryRequestType() {
		return ert;
	}

	/** 是否为无畏舰。 / Whether dredgion. */
	public boolean isDredgion() {
		return instanceMaskId == 1 || instanceMaskId == 2 || instanceMaskId == 3;
	}

	/**
	 * @return 是否为卡玛尔战场 / Whether kamar battlefield
	 */
	public boolean isKamar() {
		return instanceMaskId == 107;
	}

	/**
	 * @return 是否为吞噬奥菲丹桥 / Whether ophidan
	 */
	public boolean isOphidan() {
		return instanceMaskId == 108;
	}

	/**
	 * @return 是否为铁壁战线 / Whether bastion
	 */
	public boolean isBastion() {
		return instanceMaskId == 109;
	}

	/**
	 * @return 是否为 Idgel Dome / Whether idgel dome
	 */
	public boolean isIdgelDome() {
		return instanceMaskId == 111;
	}

	/**
	 * @return 是否为阿休纳塔尔无畏舰 / Whether asyunatar
	 */
	public boolean isAsyunatar() {
		return instanceMaskId == 121;
	}

	/**
	 * @return 是否为可疑奥菲丹桥 / Whether suspicious ophidan
	 */
	public boolean isSuspiciousOphidan() {
		return instanceMaskId == 122;
	}

	/**
	 * @return 是否为 Idgel Dome 地标 / Whether idgel dome landmark
	 */
	public boolean isIdgelDomeLandmark() {
		return instanceMaskId == 123;
	}

	/**
	 * @return 是否为黄金神庙训练所 / Whether hall of tenacity
	 */
	public boolean isHallOfTenacity() {
		return instanceMaskId == 125;
	}

	/**
	 * @return 是否为大竞技场训练营 / Whether grand arena training camp
	 */
	public boolean isGrandArenaTrainingCamp() {
		return instanceMaskId == 127;
	}

	/**
	 * @return 是否为 IDRun 类型 / Whether id run
	 */
	public boolean isIDRun() {
		return instanceMaskId == 131;
	}
}
