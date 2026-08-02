package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.RetailAreaEngine;
import com.aionemu.gameserver.ai.RetailDynamicAreaEngine;
import com.aionemu.gameserver.ai.RetailWindstreamEngine;
import com.aionemu.gameserver.model.gameobjects.Minion;
import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CHAR_BM_PACK_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DYNAMIC_LIMIT_AREA_INFO_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_OBJECTS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_COUNT_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_NOTIFY_VIP_ICON;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.AStationService;
import com.aionemu.gameserver.services.OutpostService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.TownService;
import com.aionemu.gameserver.services.WeatherService;
import com.aionemu.gameserver.services.rift.RiftInformer;
import com.aionemu.gameserver.services.summons.SummonsService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapType;

/**
 * 地图加载完成就绪通知的客户端包。
 * Client packet notifying that the level/map has finished loading.
 */
public class CM_LEVEL_READY extends AionClientPacket {
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_LEVEL_READY(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	/**
	 * 本包无载荷。
	 * This packet has no payload.
	 */
	@Override
	protected void readImpl() {
	}
	/**
	 * 地图就绪后同步玩家信息、房屋对象与保护状态。
	 * After map ready, syncs player info, house objects, and protection state.
	 */
	@Override
	protected void runImpl() {
		Player activePlayer = getConnection().getActivePlayer();
		if (activePlayer.getHouseRegistry() != null) {
			sendPacket(new SM_HOUSE_OBJECTS(activePlayer));
		}
		if (activePlayer.isInInstance()) {
			sendPacket(new SM_INSTANCE_COUNT_INFO(activePlayer.getWorldId(), activePlayer.getInstanceId()));
		}
		sendPacket(SM_CHAR_BM_PACK_LIST.vipForCharSelect(
			activePlayer.getPlayerAccount().getVipLevel(), activePlayer.getPlayerAccount().getVipExp()));
		sendPacket(new SM_PLAYER_INFO(activePlayer, false));
		sendPacket(new SM_NOTIFY_VIP_ICON(activePlayer));
		activePlayer.getController().startProtectionActiveTask();
		sendPacket(new SM_MOTION(activePlayer.getObjectId(), activePlayer.getMotions().getActiveMotions()));
		RetailWindstreamEngine.sendStates(activePlayer);
		RetailDynamicAreaEngine.sendStates(activePlayer);
		sendPacket(new SM_DYNAMIC_LIMIT_AREA_INFO_LIST(RetailAreaEngine.getNoRecallStates(activePlayer.getPosition().getWorldMapInstance())));
		if (activePlayer.isSpawned()) {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(activePlayer);
		}
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().spawn(activePlayer);
		activePlayer.getController().refreshZoneImpl();
		if (activePlayer.isInSiegeWorld()) {
			GameFeatureServices.siegeService().onEnterSiegeWorld(activePlayer);
		}
		activePlayer.getController().updateZone();
		activePlayer.getController().updateNearbyQuests();
		GameRuntimeServices.weatherService().loadWeather(activePlayer);
		if (activePlayer.isOnAStation()) {
			if (activePlayer.A_STATION_TYPE == 1) {
				activePlayer.A_STATION_TYPE = 2;
			} else if (activePlayer.A_STATION_TYPE == 2) {
				GameFeatureServices.aStationService().handleMoveBack(activePlayer);
			}
		}
		GameEngineServices.questEngine().onEnterWorld(new QuestEnv(null, activePlayer, 0, 0));
		activePlayer.getController().onEnterWorld();
		if (!WorldMapType.getWorld(activePlayer.getWorldId()).isPersonal()) {
			sendPacket(new SM_SYSTEM_MESSAGE(1390122, activePlayer.getPosition().getInstanceId()));
		}
		// 裂隙 / Rift
		RiftInformer.sendRiftsInfo(activePlayer);
		// 领地 / Territory
		GameRuntimeServices.territoryService().onEnterWorld(activePlayer);
		// 城镇 3.9 / Town 3.9
		GameHousingServices.townService().onEnterWorld(activePlayer);
		// 守护者/征服者 / Protector Conqueror
		GameFeatureServices.protectorConquerorService().onEnterMap(activePlayer);
		// 基地 4.3 / Base 4.3
		GameFeatureServices.baseService().onEnterBaseWorld(activePlayer);
		// 术古皇陵 4.3 / Shugo Imperial Tomb 4.3
		ShugoImperialTombSpawnManager.sendImperialStatus(activePlayer);
		// 欧比斯登陆 4.9.1 / Abyss Landing 4.9.1
		GameLocationBootstrapServices.abyssLandingService().onEnterWorld(activePlayer);
		// 永恒之塔 5.0 / Tower Of Eternity 5.0
		GameLocationBootstrapServices.towerOfEternityService().onEnterTowerWorld(activePlayer);
		// 前哨 5.8 / Outpost 5.8
		GameLocationBootstrapServices.outpostService().onEnterOutpostWorld(activePlayer);
		activePlayer.getEffectController().updatePlayerEffectIcons();
		sendPacket(SM_CUBE_UPDATE.cubeSize(StorageType.CUBE, activePlayer));
		TeleportService2.archdaevaTransformation(activePlayer);
		TeleportService2.playerTransformation(activePlayer);
		TeleportService2.instanceTransformation(activePlayer);
		// 战场联合 5.3 / BattleField Union 5.3
		// GameCoreGameplayServices.battlefieldUnionService().onEnterWorld(activePlayer);
		// 宠物 / Pet
		Pet pet = activePlayer.getPet();
		if (pet != null && !pet.isSpawned()) {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().spawn(pet);
		}
		// 召唤 / Summon
		SummonsService.restoreAfterTeleport(activePlayer);
		// 随从 / Minion
		Minion minion = activePlayer.getMinion();
		if (minion != null && !minion.isSpawned()) {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().spawn(minion);
		}

		activePlayer.setPortAnimation(0x02);
		PacketSendUtility.sendPacket(activePlayer, new SM_CHAR_BM_PACK_LIST(1));
		PacketSendUtility.sendPacket(activePlayer, SM_CHAR_BM_PACK_LIST.vip(
			activePlayer.getPlayerAccount().getVipLevel(),
			activePlayer.getPlayerAccount().getVipRemainingSeconds()));
	}
}
