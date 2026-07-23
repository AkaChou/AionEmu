package com.aionemu.gameserver.instance.handlers.scripts.crucible;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.ranking.SeasonRankingService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.*;

@InstanceID(302400000)
public class CrucibleSpireInstance extends GeneralInstanceHandler {
	private static final String FLOOR_CONTROLLER_DEADLINE = "infinity.floor_controller_deadline";
    /** 层数 / floor */
    private byte floor;
    /** 刷怪种族 / spawn race */
    private Race spawnRace;
    /** 副本是否已销毁 / whether the instance is destroyed */
    protected boolean isInstanceDestroyed = false;
    /** last teleport time / last teleport time */
        private final Map<Integer, Long> lastTeleportTime = new HashMap<>();
    /** floor npcs / floor npcs */
    
    private static final int[][] FLOOR_NPCS = {
        {247247, 247248},
        {247249, 247250},
        {247251, 247252},
        {247236},
        {247253, 247254},
        {247255, 247256},
        {247257, 247258},
        {247237},
        {247259, 247260},
        {247261, 247262},
        {247263, 247264},
        {247400},
        {247265, 247266},
        {247267, 247268},
        {247269, 247270},
        {247239},
        {247271, 247272},
        {247273, 247274, 247355},
        {247275, 247276, 247356},
        {247240},
        {247277, 247278},
        {247279, 247280},
        {247281, 247282},
        {247241},
        {247283, 247284},
        {247285, 247286},
        {247287},
        {247242},
        {247289, 247290},
        {247291, 247292},
        {247293, 247294},
        {247243},
        {247295, 247296},
        {247297, 247298},
        {247299, 247300},
        {247244},
        {247301, 247302},
        {247303, 247304},
        {247305, 247306},
        {247245}
    };
    
    /**
     * NPC 掉落表注册时处理。
     * Handle NPC drop-table registration.
     *
     * npc
     */
    @Override
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
        int npcId = npc.getNpcId();
        switch (npcId) {
            case 247546: //IDInfinity Heal 02.
                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000530, 1));
                break;
        }
    }
    
    private void removeItems(Player player) {
        Storage storage = player.getInventory();
        storage.decreaseByItemId(164000530, storage.getItemCountByItemId(164000530));
    }
    
    /**
     * 玩家进入副本时处理。
     * Handle a player entering the instance.
     *
     * @param player 玩家 / player
     */
    @Override
    public void onEnterInstance(final Player player) {
        super.onEnterInstance(player); 
        if (spawnRace == null) {
            spawnRace = player.getRace();
            RetailConditionSpawnEngine.setVariable(instance, "race", spawnRace == Race.ELYOS ? 1 : 2, 0);
            int pfloor = player.getFloor();
            sendPacket(player, "Condition_Infinity_PRE_SEASON_Floor", pfloor);
            sendPacket(player, "Condition_Infinity_THIS_SEASON_Floor", pfloor + 1);
            sendPacket(player, "Condition_Infinity_THIS_SEASON_Floor_Reward", pfloor);
        }
    }
    
    /**
     * 副本创建时初始化逻辑。
     * Initialize logic when the instance is created.
     *
     * @param instance 世界地图实例 / world-map instance
     */
    @Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
        floor = (byte) runtimeState().getInt("infinity.floor", 1);
        spawnFloorRings();
        spawn(247546, 254.38080f, 245.29360f, 241.08308f, (byte) 55);
        spawn(701773, 263.67166f, 249.42833f, 240.82626f, (byte) 0, 284);
        spawn(247310, 279.90976f, 243.26570f, 243.45923f, (byte) 0, 57);
        spawn(247310, 279.61618f, 1255.5001f, 243.42058f, (byte) 0, 58);
        spawn(247310, 279.62357f, 1243.2299f, 243.50325f, (byte) 0, 59);
        spawn(247310, 279.90237f, 255.53593f, 243.45923f, (byte) 0, 60);
        spawn(701772, 280.85883f, 249.46001f, 241.08347f, (byte) 0, 115);
		long controllerDeadline = runtimeState().getLong(FLOOR_CONTROLLER_DEADLINE, 0);
		if (controllerDeadline > 0) {
			scheduleDeadline("delete_floor_controller", controllerDeadline, this::deleteFloorController);
		}
    }
    
    private void sendPacket(Player player, final String variable, final int floor) {
        RetailConditionSpawnEngine.setVariable(instance, variable, floor, 0);
        PacketSendUtility.sendPacket(player, new SM_CONDITION_VARIABLE(player, variable, floor));
    }

    private void restoreFailureController() {
        int npcId = spawnRace == Race.ASMODIANS ? 247386 : 247376;
        spawn(npcId, 255.26721f, 249.49001f, 242.03000f, (byte) 60);
    }
    
    private void teleportCrucibleFloor(Player player) {
        int pfloor = player.getFloor();
        long deadline = System.currentTimeMillis() + 2500;
		runtimeState().put(FLOOR_CONTROLLER_DEADLINE, deadline);
        scheduleDeadline("delete_floor_controller", deadline, this::deleteFloorController);
        if (pfloor >= 1 && pfloor <= 38) {
            spawn(701000, 263.55551f, 1249.5244f, 240.73053f, (byte) 0, 56);
            teleportFloor(player, 219.33264f, 1249.4528f, 240.85301f, (byte) 0);
        } else if (pfloor == 39) {
            teleportFloor(player, 210.42656f, 249.58434f, 971.3951f, (byte) 0);
        }
        sendPacket(player, "Condition_Infinity_THIS_SEASON_Floor", pfloor + 1);
    }
    
    private void spawnFloorRings() {
        FlyRing f1 = new FlyRing(new FlyRingTemplate("FLOOR", mapId,
        new Point3D(317.41605, 1254.6891, 258.0014),
        new Point3D(317.88123, 1249.1969, 264.8329),
        new Point3D(317.51993, 1244.0759, 258.0506), 30), instanceId);
        f1.spawn();
    }
    
    private boolean isFloorCleared(int floorNum, Npc npc) {
        if (floorNum < 1 || floorNum > FLOOR_NPCS.length) return false;
        
        int[] npcs = FLOOR_NPCS[floorNum - 1];
        for (int npcId : npcs) {
            if (!getNpcs(npcId).isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 处理死亡事件。
     * Handle a death event.
     *
     * npc
     */
    @Override
    public void onDie(final Npc npc) {
        Player player = npc.getAggroList().getMostPlayerDamage();
        if (player == null) return;
        
        int npcId = npc.getNpcId();
        
        if (npcId == 247361 || npcId == 247362 || npcId == 247363) {
            handleFloor12Transformation(npc);
            return;
        }
        
        for (int i = 0; i < FLOOR_NPCS.length; i++) {
            for (int id : FLOOR_NPCS[i]) {
                if (id == npcId) {
                    despawnNpc(npc);
                    int nextFloor = i + 2;
                    
                    if (isFloorCleared(i + 1, npc)) {
                        floor = (byte) nextFloor;
                        runtimeState().put("infinity.floor", floor);
                        deleteNpc(701000);
                        
                        if (i + 1 == 5) despawnNpcs(getNpcs(247351));
                        else if (i + 1 == 6) despawnNpcs(getNpcs(247352));
                        else if (i + 1 == 7) despawnNpcs(getNpcs(247354));
                        else if (i + 1 == 8) {
                            despawnNpcs(getNpcs(247353));
                            despawnNpcs(getNpcs(247401));
                        }
                        else if (i + 1 == 29 || i + 1 == 30 || i + 1 == 31 || i + 1 == 32) {
                            despawnNpcs(getNpcs(701692));
                            despawnNpcs(getNpcs(247359));
                            despawnNpcs(getNpcs(247360));
                        }
                        else if (i + 1 == 36) {
                            despawnNpcs(getNpcs(247373));
                        }
                        
                        spawn(701773, 280.65912f, 1249.3933f, 240.99275f, (byte) 0, 114);
                        
                        if (player != null) {
                            sendPacket(player, "Condition_Infinity_THIS_SEASON_Floor_Reward", floor - 1);
                            player.setFloor(floor - 1);
                            rewardForFloorId(player);
                        }
                        
                        if (i + 1 == 40) {
                            sendPacket(player, "Condition_Infinity_THIS_SEASON_Floor_Reward", 100);
                        }
                    }
                    return;
                }
            }
        }
    }
    
    private void handleFloor12Transformation(Npc npc) {
        despawnNpc(npc);
        if (npc.getNpcId() == 247361 && getNpcs(247361).isEmpty()) {
            spawn(247362, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading());
        } else if (npc.getNpcId() == 247362 && getNpcs(247362).isEmpty()) {
            spawn(247363, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading());
        } else if (npc.getNpcId() == 247363 && getNpcs(247363).isEmpty()) {
            spawn(247400, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading());
        }
    }
    /**
     * 处理 rewardForFloorId。
     * Handle rewardForFloorId.
     *
     * @param player 玩家 / player
     */
    
    public void rewardForFloorId(Player player) {
        int floor = player.getFloor();
        InstanceSettlementService.settleInfinity(instance, player, floor);
    }
    
    /**
     * 玩家通过飞行环时处理。
     * Handle a player passing a flying ring.
     *
     * 玩家 / player
     * @param flyingRing 飞行环标识 / flying-ring id
     * result
     */
    @Override
    public boolean onPassFlyingRing(Player player, String flyingRing) {
        if (flyingRing.equals("FLOOR")) {
           int objId = player.getObjectId();
           long now = System.currentTimeMillis();
           Long last = lastTeleportTime.get(objId);
        
           if (last == null || now - last > 5000) {
               lastTeleportTime.put(objId, now);
               teleportCrucibleFloor(player);
           }
        }
        return false;
    }
    /**
     * 处理 sendMsgByRace。
     * Handle sendMsgByRace.
     *
     * message
     * 阵营 / race
     * time
     */
    
    protected void sendMsgByRace(final int msg, final Race race, int time) {
        long deadline = System.currentTimeMillis() + time;
        scheduleDeadline("message_" + msg + '_' + deadline, deadline, () ->
                instance.doOnAllPlayers(new Visitor<Player>() {
                    /**
                     * 处理 visit。
                     * Handle visit.
                     *
                     * @param player 玩家 / player
                     */
                    @Override
                    public void visit(Player player) {
                        if (player.getRace().equals(race) || race.equals(Race.PC_ALL)) {
                            PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msg));
                        }
                    }
                }));
    }
    
    private void deleteNpc(int npcId) {
        if (getNpc(npcId) != null) {
            getNpc(npcId).getController().onDelete();
        }
    }

	private void deleteFloorController() {
		deleteNpc(701773);
		runtimeState().remove(FLOOR_CONTROLLER_DEADLINE);
	}
    
    private void despawnNpc(Npc npc) {
        if (npc != null) {
            npc.getController().onDelete();
        }
    }
    /**
     * 处理 despawnNpcs。
     * Handle despawnNpcs.
     *
     * npcs
     */
    
    protected void despawnNpcs(List<Npc> npcs) {
        if (npcs == null) return;
        for (Npc npc: npcs) {
            if (npc != null) {
                npc.getController().onDelete();
            }
        }
    }
    /**
 * 返回 npcs。
     * Return the npcs.
     *
     * NPC
     * result
     */
    
    protected List<Npc> getNpcs(int npcId) {
        if (!isInstanceDestroyed && instance != null) {
            return instance.getNpcs(npcId);
        }
        return new ArrayList<>();
    }
    /**
     * 处理 onFailCrucible。
     * Handle onFailCrucible.
     *
     * @param player 玩家 / player
     */
    
    public void onFailCrucible(Player player) {
        TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
    }
    /**
     * 玩家请求退出副本时处理。
     * Handle a player exit request.
     *
     * @param player 玩家 / player
     */
    
    public void onExitInstance(Player player) {
        removeItems(player);
        TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
    }
    /**
     * 副本销毁时清理资源。
     * Clean up resources when the instance is destroyed.
     */
    @Override
    public void onInstanceDestroy() {
        isInstanceDestroyed = true;
    }
    
    private void teleportFloor(float x, float y, float z, byte h) {
        for (Player playerInside: instance.getPlayersInside()) {
            if (playerInside.isOnline()) {
                teleportCrucibleFloor(playerInside);
            }
        }
    }
    /**
     * 处理 teleportFloor。
     * Handle teleportFloor.
     *
     * @param player 玩家 / player
     * @param x X 坐标 / X
     * @param y Y 坐标 / Y
     * @param z Z 坐标 / Z
     * @param h 朝向 / h
     */
    
    protected void teleportFloor(Player player, float x, float y, float z, byte h) {
        TeleportService2.teleportTo(player, mapId, instanceId, x, y, z, h);
    }
    
    /**
     * 处理玩家复活事件。
     * Handle a player revive event.
     *
     * 玩家 / player
     * result
     */
    @Override
    public boolean onReviveEvent(Player player) {
        for (Npc npc: instance.getNpcs()) {
            npc.getController().onDelete();
        }
        restoreFailureController();
        player.getGameStats().updateStatsAndSpeedVisually();
        PlayerReviveService.revive(player, 100, 100, false, 0);
        PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
        PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_INFINITY_INDUN_RESURRECT, 0, 0));
        onFailCrucible(player);
        return true;
    }
}
