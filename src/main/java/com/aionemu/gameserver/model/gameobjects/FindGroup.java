package com.aionemu.gameserver.model.gameobjects;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TemporaryPlayerTeam;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;

/**
 * 寻找队伍游戏对象。
 * Find Group game object.
 *
 * @author MrPoke
 */
public class FindGroup {

	private AionObject object;
	private String message;
	private int groupType, minMembers, instanceId;
	private int lastUpdate = (int) (System.currentTimeMillis() / 1000);

	public FindGroup(AionObject object, String message, int groupType) {
		this.object = object;
		this.message = message;
		this.groupType = groupType;
	}

	/** 获取消息。 / Returns the message. */
	public String getMessage() {
		return message;
	}

	/** 获取队伍类型。 / Returns the group type. */
	public int getGroupType() {
		return groupType;
	}

	/** 返回对象 ID / Returns the object id */
	public int getObjectId() {
		return object.getObjectId();
	}

	/** 返回副本 ID / Returns the instance id */
	public int getInstanceId() {
		return instanceId;
	}

	/** 返回最少成员数 / Returns the min members */
	public int getMinMembers() {
		return minMembers;
	}

	/** 返回职业 ID / Returns the class id */
	public int getClassId() {
		if (object instanceof Player) {
			return ((Player) (object)).getPlayerClass().getClassId();
		} else if (object instanceof PlayerAlliance) {
			((PlayerAlliance) (object)).getLeaderObject().getCommonData().getPlayerClass();
		} else if (object instanceof PlayerGroup) {
			((PlayerGroup) object).getLeaderObject().getPlayerClass();
		}
		return 0;
	}

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel() {
		if (object instanceof Player) {
			return ((Player) (object)).getLevel();
		} else if (object instanceof PlayerAlliance) {
			int minLvl = 99;
			for (Player member : ((PlayerAlliance) (object)).getMembers()) {
				int memberLvl = member.getCommonData().getLevel();
				if (memberLvl < minLvl) {
					minLvl = memberLvl;
				}
			}
			return minLvl;
		} else if (object instanceof PlayerGroup) {
			return ((PlayerGroup) object).getMinExpPlayerLevel();
		} else if (object instanceof TemporaryPlayerTeam) {
			return ((TemporaryPlayerTeam<?>) object).getMinExpPlayerLevel();
		}
		return 1;
	}

	/** 获取最大等级。 / Returns the max level. */
	public int getMaxLevel() {
		if (object instanceof Player) {
			return ((Player) (object)).getLevel();
		} else if (object instanceof PlayerAlliance) {
			int maxLvl = 1;
			for (Player member : ((PlayerAlliance) (object)).getMembers()) {
				int memberLvl = member.getCommonData().getLevel();
				if (memberLvl > maxLvl) {
					maxLvl = memberLvl;
				}
			}
			return maxLvl;
		} else if (object instanceof PlayerGroup) {
			return ((PlayerGroup) object).getMaxExpPlayerLevel();
		} else if (object instanceof TemporaryPlayerTeam) {
			return ((TemporaryPlayerTeam<?>) object).getMaxExpPlayerLevel();
		}
		return 1;
	}

	/** 返回未知字段 / Returns the unk */
	public int getUnk() {
		if (object instanceof Player) {
			return 65557;
		} else {
			return 0;
		}
	}

	/**
	 * 返回最后更新时间。
	 * Returns the last update time.
	 *
	 * @return 最后更新时间 / the lastUpdate
	 */
	public int getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * 返回队伍名称。
	 * Returns the team name.
	 *
	 * @return 名称 / the name
	 */
	public String getName() {
		if (object instanceof Player) {
			return ((Player) object).getName();
		} else if (object instanceof PlayerAlliance) {
			return ((PlayerAlliance) object).getLeaderObject().getCommonData().getName();
		} else if (object instanceof PlayerGroup) {
			return ((PlayerGroup) object).getLeaderObject().getName();
		}
		return "";
	}

	/** 返回大小 / Returns the size. */
	public int getSize() {
		if (object instanceof Player) {
			return 1;
		} else if (object instanceof PlayerAlliance) {
			return ((PlayerAlliance) object).size();
		} else if (object instanceof PlayerGroup) {
			return ((PlayerGroup) object).size();
		}
		return 1;
	}

	/** 设置消息。 / Sets the message. */
	public void setMessage(String message) {
		lastUpdate = (int) (System.currentTimeMillis() / 1000);
		this.message = message;
	}
}
