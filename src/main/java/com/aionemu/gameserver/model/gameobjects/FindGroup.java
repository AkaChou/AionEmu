package com.aionemu.gameserver.model.gameobjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.gameserver.model.Race;
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
	private int groupType, minMembers, instanceId, teamId;
	private int lastUpdate = (int) (System.currentTimeMillis() / 1000);
	private final Set<Integer> bannedPlayers = ConcurrentHashMap.newKeySet();

	public FindGroup(AionObject object, String message, int groupType) {
		this.object = object;
		this.message = message;
		this.groupType = groupType;
	}

	public FindGroup(Player recruiter, int instanceId, int minMembers, String message) {
		this(recruiter, message, 0);
		this.instanceId = instanceId;
		this.minMembers = minMembers;
		this.teamId = recruiter.getCurrentTeamId();
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

	/** 返回 min members / Returns the min members */
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
		if (instanceId != 0) {
			return getMembers().stream().mapToInt(Player::getLevel).min().orElse(1);
		}
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
		if (instanceId != 0) {
			return getMembers().stream().mapToInt(Player::getLevel).max().orElse(1);
		}
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

	/** 返回 unk / Returns the unk */
	public int getUnk() {
		if (object instanceof Player) {
			return 65557;
		} else {
			return 0;
		}
	}

	/**
	 * @return the lastUpdate
	 */
	public int getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * @return the name
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

	/** 返回大小 / Returns the size*/
	public int getSize() {
		if (instanceId != 0) {
			return getMembers().size();
		}
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

	public void setGroupType(int groupType) {
		this.groupType = groupType;
	}

	public Player getRecruiter() {
		return object instanceof Player player ? player : null;
	}

	public int getTeamId() {
		return teamId;
	}

	public void setRecruiter(Player recruiter) {
		object = recruiter;
		teamId = recruiter.getCurrentTeamId();
		lastUpdate = (int) (System.currentTimeMillis() / 1000);
	}

	public Race getRace() {
		Player recruiter = getRecruiter();
		return recruiter == null ? Race.PC_ALL : recruiter.getRace();
	}

	public List<Player> getMembers() {
		Player recruiter = getRecruiter();
		if (recruiter == null) {
			return List.of();
		}
		TemporaryPlayerTeam<?> team = recruiter.getCurrentTeam();
		return team == null ? List.of(recruiter) : new ArrayList<>(team.getOnlineMembers());
	}

	public boolean isLeader(Player player) {
		Player recruiter = getRecruiter();
		return recruiter == player;
	}

	public void ban(int playerId) {
		bannedPlayers.add(playerId);
	}

	public boolean isBanned(int playerId) {
		return bannedPlayers.contains(playerId);
	}
}
