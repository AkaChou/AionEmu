package com.aionemu.gameserver.services.player;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.services.AutoGroupService;
import com.aionemu.gameserver.services.BrokerService;
import com.aionemu.gameserver.services.DuelService;
import com.aionemu.gameserver.services.ExchangeService;
import com.aionemu.gameserver.services.FindGroupService;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.services.drop.DropService;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.services.toypet.PetService;
import com.aionemu.gameserver.services.KiskService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.RepurchaseService;








import java.sql.Timestamp;

import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.summons.SummonMode;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.ChatService;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.services.StigmaLinkedService;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.summons.SummonsService;
import com.aionemu.gameserver.services.toypet.PetSpawnService;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.audit.GMService;

/**
 * 玩家离线服务，处理延迟下线与资源清理。
 * Player leave-world service handling delayed logout and resource cleanup.
 */
@Slf4j

public class PlayerLeaveWorldService {

	/**
	 * 延迟离线。
	 * Schedules delayed leave-world.
	 *
	 * @param player 玩家 / player
	 * @param delay  延迟毫秒 / delay
	 */
	public static final void startLeaveWorldDelay(final Player player, int delay) {
		player.getController().stopMoving();
		ThreadPoolManager.getInstance().schedule(() -> startLeaveWorld(player), delay);
	}

	/**
	 * 开始离线流程。
	 * Starts leave-world flow.
	 *
	 * @param player 玩家 / player
	 */
	public static final void startLeaveWorld(Player player) {
		log.info(I18n.get("log.6e504269f536", player.getName(), (player.getClientConnection() != null ? player.getClientConnection().getAccount().getName()
			: "Disconnected")));
		FindGroupService.getInstance().removeFindGroup(player.getRace(), 0x00, player.getObjectId());
		FindGroupService.getInstance().removeFindGroup(player.getRace(), 0x04, player.getObjectId());
		player.onLoggedOut();
		PetService.getInstance().onPlayerLogout(player);
		BrokerService.getInstance().removePlayerCache(player);
		ExchangeService.getInstance().cancelExchange(player);
		RepurchaseService.getInstance().removeRepurchaseItems(player);
		if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
			AutoGroupService.getInstance().onPlayerLogOut(player);
		}
		ProtectorConquerorService.getInstance().onLogout(player);
		InstanceService.onLogOut(player);
		GMService.getInstance().onPlayerLogedOut(player);
		KiskService.getInstance().onLogout(player);
		player.getMoveController().abortMove();
		if (player.isLooting()) {
			DropService.getInstance().closeDropList(player, player.getLootingNpcOid());
		}
		if (player.isInPrison()) {
			long prisonTimer = System.currentTimeMillis() - player.getStartPrison();
			prisonTimer = player.getPrisonTimer() - prisonTimer;
			player.setPrisonTimer(prisonTimer);
			log.debug("Update prison timer to {} seconds !", prisonTimer / 1000);
		}
		PlayerService.daos().playerEffectsDAO().storePlayerEffects(player);
		PlayerService.daos().playerCooldownsDAO().storePlayerCooldowns(player);
		PlayerService.daos().itemCooldownsDAO().storeItemCooldowns(player);
		PlayerService.daos().houseObjectCooldownsDAO().storeHouseObjectCooldowns(player);
		PlayerService.daos().playerLifeStatsDAO().updatePlayerLifeStat(player);
		PlayerService.daos().eventItemsDAO().storeItems(player);
		// 术古扫荡 / SHUGO SWEEP
		ShugoSweepService.getInstance().onLogout(player);
		PlayerGroupService.onPlayerLogout(player);
		PlayerAllianceService.onPlayerLogout(player);
		LegionService.getInstance().LegionWhUpdate(player);
		player.getEffectController().removeAllEffects(true);
		player.getLifeStats().cancelAllTasks();
		if (player.getLifeStats().isAlreadyDead()) {
			if (player.isInInstance()) {
				PlayerReviveService.instanceRevive(player);
			} else {
				PlayerReviveService.bindRevive(player);
			}
		} else if (DuelService.getInstance().isDueling(player.getObjectId())) {
			DuelService.getInstance().loseDuel(player);
		}

		if (player.getSummon() != null) {
			SummonsService.doMode(SummonMode.RELEASE, player.getSummon(), UnsummonType.LOGOUT);
		}

		if (player.getPet() != null) {
			PetSpawnService.dismissPet(player, true);
		}

		if (player.getMinion() != null) {
			MinionService.getInstance().despawnMinion(player, player.getMinion().getObjectId());
		}

		if (player.getPostman() != null) {
			player.getPostman().getController().onDelete();
		}
		player.setPostman(null);
		PunishmentService.stopPrisonTask(player, true);
		PunishmentService.stopGatherableTask(player, true);
		if (player.isLegionMember()) {
			LegionService.getInstance().onLogout(player);
		}
		QuestEngine.getInstance().onLogOut(new QuestEnv(null, player, 0, 0));
		player.getController().delete();
		// 重置楼层“试炼尖塔 5.6” / Reset Floor "Crucible Spire 5.6"
		player.getCommonData().setFloor(0);
		player.getCommonData().setOnline(false);
		player.getCommonData().setLastOnline(new Timestamp(System.currentTimeMillis()));
		player.setClientConnection(null);
		PlayerService.daos().playerDAO().onlinePlayer(player, false);
		if (GSConfig.ENABLE_CHAT_SERVER) {
			ChatService.onPlayerLogout(player);
		}
		PlayerService.storePlayer(player);
		ExpireTimerTask.getInstance().removePlayer(player);
		if (player.getCraftingTask() != null) {
			player.getCraftingTask().stop(true);
		}
		player.getEquipment().setOwner(null);
		player.getInventory().setOwner(null);
		player.getWarehouse().setOwner(null);
		player.getStorage(StorageType.ACCOUNT_WAREHOUSE.getId()).setOwner(null);
		PacketSendUtility.broadcastPacket(player, new SM_DELETE(player, 2), 50);
		PlayerAccountData pad = player.getPlayerAccount().getPlayerAccountData(player.getObjectId());
		pad.setEquipment(player.getEquipment().getEquippedItems());
		StigmaLinkedService.onLogOut(player);
	}

	/**
	 * 尝试离线。
	 * Attempts to leave the world.
	 *
	 * @param player 玩家 / player
	 */
	public static void tryLeaveWorld(Player player) {
		player.getMoveController().abortMove();
		if (player.getController().isInShutdownProgress()) {
			PlayerLeaveWorldService.startLeaveWorld(player);
		} else {
			int delay = 15;
			PlayerLeaveWorldService.startLeaveWorldDelay(player, (delay * 1000));
		}
	}
}
