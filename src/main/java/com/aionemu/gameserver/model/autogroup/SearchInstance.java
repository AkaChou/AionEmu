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

	public long getRegistrationTime() {
		return registrationTime;
	}

	/** 获取条目请求类型。 / Returns the entry request type. */
	public EntryRequestType getEntryRequestType() {
		return ert;
	}

}
