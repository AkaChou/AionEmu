package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.AgentService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 管理员天族/魔族代理战命令：启动或停止指定代理战。
 * Admin agent-fight command: starts or stops a specified agent fight.
 */
public class Agent extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";
	
	/**
	 * 注册 {@code //agent} 命令。
	 * Registers the {@code //agent} command.
	 */
	public Agent() {
		super("agent");
	}
	
	/**
	 * 执行代理战控制：解析 start/stop 与地点 ID。
	 * Executes agent-fight control: parses start/stop and location id.
	 *
	 * admin
	 * start|stop, location id。 / start|stop, location id
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			showHelp(player);
			return;
		} if (COMMAND_STOP.equalsIgnoreCase(params[0]) || COMMAND_START.equalsIgnoreCase(params[0])) {
			handleStartStopFight(player, params);
		}
	}
	
	/**
	 * 处理代理战的开始/停止逻辑。
	 * Handles start/stop logic for an agent fight.
	 *
	 * admin
	 * @param params 参数：动作与地点 ID / action and location id
	 */
	protected void handleStartStopFight(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int agentId = NumberUtils.toInt(params[1]);
		if (!isValidAgentLocationId(player, agentId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.agentService().isFightInProgress(agentId)) {
				PacketSendUtility.sendMessage(player, "<Agent Fight> " + agentId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<Agent Fight> " + agentId + " started!");
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF4_Advance_GodElite);
					}
				});
				GameLocationBootstrapServices.agentService().startAgentFight(agentId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.agentService().isFightInProgress(agentId)) {
				PacketSendUtility.sendMessage(player, "<Agent Fight> " + agentId + " is not start!");
			} else {
				PacketSendUtility.sendMessage(player, "<Agent Fight> " + agentId + " stopped!");
				GameLocationBootstrapServices.agentService().stopAgentFight(agentId);
			}
		}
	}
	
	/**
	 * 校验代理战地点 ID 是否有效。
	 * Validates whether the agent location id exists.
	 *
	 * admin
	 * location id
	 *
	 * @return {@code true} if valid。 / {@code true} if valid
	 */
	protected boolean isValidAgentLocationId(Player player, int agentId) {
		if (!GameLocationBootstrapServices.agentService().getAgentLocations().keySet().contains(agentId)) {
			PacketSendUtility.sendMessage(player, "Id " + agentId + " is invalid");
			return false;
		}
		return true;
	}
	
	/**
	 * 向管理员输出 {@code //agent} 用法。
	 * Sends {@code //agent} usage help to the admin.
	 *
	 * admin
	 */
	protected void showHelp(Player player) {
		PacketSendUtility.sendMessage(player, "AdminCommand //agent start|stop <Id>");
	}
}