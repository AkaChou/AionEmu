package com.aionemu.gameserver.model.team2.common.events;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

/**
 * 团队 Command 枚举。
 * Team Command enumeration.
 *
 * @author ATracer
 */
public enum TeamCommand {

	/** 小队封禁成员、设置队长、移除成员、设置招募 / Group ban member, set leader, remove member, set LFG */
	GROUP_BAN_MEMBER(2), GROUP_SET_LEADER(3), GROUP_REMOVE_MEMBER(6), GROUP_SET_LFG(9),
	/** 小队开始指导、结束指导；联盟离开、封禁成员 / Group start mentoring, end mentoring; alliance leave, ban member */
	GROUP_START_MENTORING(10), GROUP_END_MENTORING(11), ALLIANCE_LEAVE(14), ALLIANCE_BAN_MEMBER(16),
	/** 联盟设置队长、取消确认、开始确认 / Alliance set captain, checkready cancel, checkready start */
	ALLIANCE_SET_CAPTAIN(17), ALLIANCE_CHECKREADY_CANCEL(20), ALLIANCE_CHECKREADY_START(21),
	/** 联盟自动取消确认、确认就绪、确认未就绪 / Alliance checkready autocancel, ready, notready */
	ALLIANCE_CHECKREADY_AUTOCANCEL(22), ALLIANCE_CHECKREADY_READY(23), ALLIANCE_CHECKREADY_NOTREADY(24),
	/** 联盟设置副队长、取消副队长、调整分组；联盟军团离开 / Alliance set/unset vicecaptain, change group; league leave */
	ALLIANCE_SET_VICECAPTAIN(25), ALLIANCE_UNSET_VICECAPTAIN(26), ALLIANCE_CHANGE_GROUP(27), LEAGUE_LEAVE(29),
	/** 联盟军团驱逐 / League expel */
	LEAGUE_EXPEL(30);

	private static Map<Integer, TeamCommand> teamCommands;

	static {
		teamCommands = new HashMap<Integer, TeamCommand>();
		for (TeamCommand eventCode : values()) {
			teamCommands.put(eventCode.getCodeId(), eventCode);
		}
	}

	private final int commandCode;

	private TeamCommand(int commandCode) {
		this.commandCode = commandCode;
	}

	/** 返回 code id / Returns the code id */
	public int getCodeId() {
		return commandCode;
	}

	/** 返回 command / Returns the command */
	public static final TeamCommand getCommand(int commandCode) {
		TeamCommand command = teamCommands.get(commandCode);
		Preconditions.checkNotNull(command, "Invalid team command code " + commandCode);
		return command;
	}
}
