package com.aionemu.gameserver.services.transfers;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.main.PlayerTransferConfig;
import com.aionemu.gameserver.dao.LegionMemberDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_PTRANSFER_CONTROL;
import com.aionemu.gameserver.services.AccountService;
import com.aionemu.gameserver.services.BrokerService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 玩家跨服转移服务，协调源服校验、角色数据下发、目标服克隆以及成功/失败回执。
 * Player cross-server transfer service coordinating source validation, character payload delivery,
 * target-side cloning and success/error callbacks.
 *
 * @author KID
 */
@Slf4j
public class PlayerTransferService {
	private static volatile ObjectProvider<PlayerTransferService> instanceProvider;

	/**
	 * 专用转移日志主题，用于记录跨服迁移过程明细。
	 * Dedicated transfer-log topic for cross-server migration details.
	 */
	@Slf4j(topic = "PLAYERTRANSFER")
	private static class TransferLog {
	}

	/**
	 * 获取服务单例（优先 Spring ObjectProvider，否则回退本地单例）。
	 * Get the service singleton (prefer Spring ObjectProvider, otherwise local holder).
	 *
	 * @return 服务单例 / service instance
	 */
	public static PlayerTransferService getInstance() {
		ObjectProvider<PlayerTransferService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / Instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<PlayerTransferService> instanceProvider) {
		PlayerTransferService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final PlayerTransferService instance = new PlayerTransferService();
	}

	private PlayerDAO dao;
	private Map<Integer, TransferablePlayer> transfers = new LinkedHashMap<>();
	private volatile List<Integer> rsList = List.of();

	/**
	 * 初始化服务并加载需移除的技能列表。
	 * Initialize the service and load skills that should be removed on transfer.
	 */
	public PlayerTransferService() {
		this.dao = DAOManager.getDAO(PlayerDAO.class);
		reload();
	}

	/**
	 * 重新加载转移配置中需要剔除的技能 ID 列表。
	 * Reload the skill ID list that must be stripped during transfer.
	 */
	public void reload() {
		List<Integer> skills = new ArrayList<>();
		if (!PlayerTransferConfig.REMOVE_SKILL_LIST.equals("*")) {
			for (String skillId : PlayerTransferConfig.REMOVE_SKILL_LIST.split(",")) {
				skills.add(Integer.parseInt(skillId));
			}
		}
		rsList = List.copyOf(skills);
		log.info(I18n.get("log.195bf15115b2", rsList.size()));
	}

	private String ptsnameitem = "ptsnameitem";

	/**
	 * 玩家进入世界时处理转移后缀名与改名道具发放。
	 * Handle transfer name-suffix messaging and rename-item grant when a player enters the world.
	 *
	 * @param player 进入世界的玩家 / Player entering the world
	 */
	public void onEnterWorld(Player player) {
		if (player.getName().endsWith(PlayerTransferConfig.NAME_PREFIX)) {
			PacketSendUtility.sendMessage(player, "You can add your oldnickname-friend to your friendlist!");
			if (!player.hasVar(ptsnameitem)) {
				long count = ItemService.addItem(player, 169670001, 1);
				if (count == 1) {
					PacketSendUtility.sendMessage(player,
							"Please empty your inventory and relogin again. After that you'll be able to receive item that allows you to change your name.");
				} else {
					player.setVar(ptsnameitem, 1, true);
				}
			}
		}
	}

	/**
	 * 在源服发起角色转移：校验账户归属、军团、在线状态、冷却与资产限制后打包角色数据。
	 * Start transfer on the source server: validate account ownership, legion, online state, cooldown and asset
	 * limits, then package character data.
	 *
	 * @param accountId 源账号 ID / Source account ID
	 * @param targetAccountId 目标账号 ID / Target account ID
	 * @param playerId 角色 ID / Character ID
	 * @param targetServerId 目标服务器 ID / Target server ID
	 * @param taskId 任务 ID / Task ID
	 */
	public void startTransfer(int accountId, int targetAccountId, int playerId, byte targetServerId, int taskId) {
		boolean exist = false;
		for (int id : DAOManager.getDAO(PlayerDAO.class).getPlayerOidsOnAccount(accountId))
			if (id == playerId) {
				exist = true;
				break;
			}

		if (!exist) {
			log.warn(I18n.get("log.526992334997", taskId, playerId, accountId));
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_PTRANSFER_CONTROL(SM_PTRANSFER_CONTROL.TASK_STOP, taskId,
					"player " + playerId + " is not present on account " + accountId));
			return;
		}

		if (DAOManager.getDAO(LegionMemberDAO.class).isIdUsed(playerId)) {
			log.warn(I18n.get("log.1cf8692ae407", taskId, playerId));
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_PTRANSFER_CONTROL(SM_PTRANSFER_CONTROL.TASK_STOP, taskId,
					"cannot transfer player with existing legion " + playerId));
			return;
		}

		PlayerCommonData common = dao.loadPlayerCommonData(playerId);
		if (common.isOnline()) {
			log.warn(I18n.get("log.9e938080f552", taskId, playerId));
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_PTRANSFER_CONTROL(SM_PTRANSFER_CONTROL.TASK_STOP, taskId,
					"cannot transfer online players " + playerId));
			return;
		}

		if (PlayerTransferConfig.REUSE_HOURS > 0 && common.getLastTransferTime()
				+ PlayerTransferConfig.REUSE_HOURS * 3600000 > System.currentTimeMillis()) {
			log.warn(I18n.get("log.9a7c79463061", taskId, playerId));
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_PTRANSFER_CONTROL(SM_PTRANSFER_CONTROL.TASK_STOP, taskId,
					"cannot transfer that player so often " + playerId));
			return;
		}

		Player player = PlayerService.getPlayer(playerId, AccountService.loadAccount(accountId));
		long kinah = player.getInventory().getKinah() + player.getWarehouse().getKinah();
		if (PlayerTransferConfig.MAX_KINAH > 0 && kinah >= PlayerTransferConfig.MAX_KINAH) {
			log.warn(I18n.get("log.e461e83eac61", taskId, kinah));
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_PTRANSFER_CONTROL(SM_PTRANSFER_CONTROL.TASK_STOP, taskId,
					"cannot transfer players with " + kinah + " kinah in inventory/wh."));
			return;
		}

		if (GameRuntimeServices.brokerService().hasRegisteredItems(player)) {
			log.warn(I18n.get("log.be9da2c1bc33", taskId));
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_PTRANSFER_CONTROL(SM_PTRANSFER_CONTROL.TASK_STOP, taskId,
					"cannot transfer player while he own some items in broker."));
			return;
		}

		TransferablePlayer tp = new TransferablePlayer(playerId, accountId, targetAccountId);
		tp.player = player;
		tp.targetServerId = targetServerId;
		tp.accountId = accountId;
		tp.targetAccountId = targetAccountId;
		tp.taskId = taskId;
		transfers.put(taskId, tp);

		TransferLog.log.info(I18n.get("log.0e5cc7ea4a0c", taskId));
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_PTRANSFER_CONTROL(SM_PTRANSFER_CONTROL.CHARACTER_INFORMATION, tp));
	}

	/**
	 * 在目标服根据源服下发的角色二进制数据克隆角色。
	 * Clone a character on the target server from the binary payload delivered by the source server.
	 *
	 * @param taskId 任务 ID / Task ID
	 * @param targetAccountId 目标账号 ID / Target account ID
	 * @param name 角色名 / Character name
	 * @param account 账号名 / Account name
	 * @param db 角色二进制数据 / Character binary payload
	 */
	public void cloneCharacter(int taskId, int targetAccountId, String name, String account, byte[] db) {
		if (!PlayerService.isFreeName(name)) {
			if (PlayerTransferConfig.BLOCK_SAMENAME) {
				com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(
						new SM_PTRANSFER_CONTROL(SM_PTRANSFER_CONTROL.ERROR, taskId, "Name is already in use"));
				return;
			}

			log.info(I18n.get("log.ae60dc78d89e", name));
			TransferLog.log.info(I18n.get("log.25ac00635c0c", taskId));
			String newName = name + PlayerTransferConfig.NAME_PREFIX;

			int i = 0;
			while (!PlayerService.isFreeName(newName)) {
				newName = name + PlayerTransferConfig.NAME_PREFIX + i;
			}
			name = newName;
		}
		if (AccountService.loadAccount(targetAccountId).size() >= GSConfig.CHARACTER_LIMIT_COUNT) {
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(
					new SM_PTRANSFER_CONTROL(SM_PTRANSFER_CONTROL.ERROR, taskId, "No free character slots"));
			return;
		}

		CMT_CHARACTER_INFORMATION acp = new CMT_CHARACTER_INFORMATION(0, State.CONNECTED);
		acp.setBuffer(ByteBuffer.wrap(db).order(ByteOrder.LITTLE_ENDIAN));
		Player cha = acp.readInfo(name, targetAccountId, account, rsList, TransferLog.log);

		if (cha == null) { // something went wrong!
			log.error(I18n.get("log.1be80ec82e92", taskId, name));
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_PTRANSFER_CONTROL(SM_PTRANSFER_CONTROL.ERROR, taskId,
					"unexpected sql error while creating a clone"));
		} else {
			DAOManager.getDAO(PlayerDAO.class).setPlayerLastTransferTime(cha.getObjectId(), System.currentTimeMillis());
			com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendPacket(new SM_PTRANSFER_CONTROL(SM_PTRANSFER_CONTROL.OK, taskId));
			log.info(I18n.get("log.0ced97702710", taskId, name));
			TransferLog.log.info(I18n.get("log.aa63f248efe4", taskId));
		}
	}

	/**
	 * 目标服确认成功后，在源服删除原角色。
	 * After target-server confirmation, delete the original character on the source server.
	 *
	 * @param taskId 任务 ID / Task ID
	 */
	public void onOk(int taskId) {
		TransferablePlayer tplayer = this.transfers.remove(taskId);
		TransferLog.log.info(I18n.get("log.d03b46dfce3e", taskId));
		PlayerService.deletePlayerFromDB(tplayer.playerId);
	}

	/**
	 * 目标服返回错误时清理源服转移任务。
	 * Clean up the source-side transfer task when the target server reports an error.
	 *
	 * @param taskId 任务 ID / Task ID
	 * @param reason 失败原因 / Failure reason
	 */
	public void onError(int taskId, String reason) {
		this.transfers.remove(taskId);
		TransferLog.log.info(I18n.get("log.bd3433f3717f", taskId, reason));
	}
}
