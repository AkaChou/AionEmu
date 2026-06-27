/*
 * This file is part of Encom.
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.IronWallWarfrontReward;
import com.aionemu.gameserver.model.instance.playerreward.InstancePlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.IronWallWarfrontPlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.AutoGroupService;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/****/
/** Author (Encom)
/****/
@InstanceID(301220000)
public class IronWallWarfrontInstance extends GeneralInstanceHandler {

    private int ironWallBase;
    private long instanceTime;
    private Map<Integer, StaticDoor> doors;
    private Race RaceKilledCommander = null;
    protected IronWallWarfrontReward ironWallWarfrontReward;
    private float loosingGroupMultiplier = 1;
    private boolean isInstanceDestroyed = false;
    protected AtomicBoolean isInstanceStarted = new AtomicBoolean(false);
    private final List<Future<?>> ironWallTask = new ArrayList<Future<?>>();
    private static Race RaceKilledCommanderStatic = null;
    private static int ironWallBaseStatic = 0;
    private static IronWallWarfrontInstance instanceStatic = null;

    private static class SpawnData {
        final int npcId;
        final float x, y, z;
        final byte h;
        SpawnData(int npcId, float x, float y, float z, byte h) {
            this.npcId = npcId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.h = h;
        }
    }

    private static class NpcHandler {
        final int points;
        final boolean despawn;
        final java.util.function.BiConsumer<Npc, Player> onDeath;
        final Map<Race, List<SpawnData>> spawns;
        NpcHandler(int points) { this(points, true, null); }
        NpcHandler(int points, boolean despawn) { this(points, despawn, null); }
        NpcHandler(int points, java.util.function.BiConsumer<Npc, Player> onDeath) { this(points, true, onDeath); }
        NpcHandler(int points, boolean despawn, java.util.function.BiConsumer<Npc, Player> onDeath) {
            this.points = points;
            this.despawn = despawn;
            this.onDeath = onDeath;
            this.spawns = new HashMap<>();
        }
        NpcHandler addSpawn(Race race, SpawnData... data) {
            spawns.put(race, java.util.Arrays.asList(data));
            return this;
        }
    }

    private static final Map<Integer, NpcHandler> HANDLERS = new HashMap<>();

    static {
        HANDLERS.put(233497, new NpcHandler(0));
        HANDLERS.put(233508, new NpcHandler(0));
        HANDLERS.put(233517, new NpcHandler(0));
        HANDLERS.put(233528, new NpcHandler(0));
        HANDLERS.put(233543, new NpcHandler(0));
        HANDLERS.put(233549, new NpcHandler(0));
        HANDLERS.put(233561, new NpcHandler(0));
        
        HANDLERS.put(233547, new NpcHandler(100));
        HANDLERS.put(233548, new NpcHandler(100));
        HANDLERS.put(233537, new NpcHandler(250));
        
        int[][] cannons = {
            {233741, 726565, 328077, 254216, 48, 701596, 801960, 701610, 801961},
            {233742, 761632, 381781, 240922, 84, 701596, 801960, 701610, 801961},
            {233743, 710055, 410757, 241013, 32, 701596, 801960, 701610, 801961},
            {233744, 625838, 339615, 235741, 54, 701596, 801960, 701610, 801961},
            {233745, 644943, 302792, 235742, 114, 701596, 801960, 701610, 801961},
            {233746, 618395, 361984, 224943, 42, 701596, 801960, 701610, 801961},
            {233747, 685211, 427900, 229821, 33, 701596, 801960, 701610, 801961},
            {233748, 670069, 560677, 229349, 113, 701596, 801960, 701610, 801961},
            {233749, 518424, 230842, 231920, 0, 701596, 801960, 701610, 801961}
        };
        
        for (int[] c : cannons) {
            HANDLERS.put(c[0], new NpcHandler(250).addSpawn(Race.ELYOS, new SpawnData(c[5], c[1]/1000f, c[2]/1000f, c[3]/1000f, (byte)c[4]), new SpawnData(c[6], c[1]/1000f, c[2]/1000f, c[3]/1000f, (byte)c[4])).addSpawn(Race.ASMODIANS, new SpawnData(c[7], c[1]/1000f, c[2]/1000f, c[3]/1000f, (byte)c[4]), new SpawnData(c[8], c[1]/1000f, c[2]/1000f, c[3]/1000f, (byte)c[4])));
        }

        int[] walls = {233564, 233565, 233566, 233567, 233568, 233569};
        for (int wall : walls) {
            HANDLERS.put(wall, new NpcHandler(2000, (n, p) -> {
                sendMsgByRaceStatic(1402207, Race.PC_ALL);
            }));
        }

        int[] supplies = {233510, 233511, 233512, 233530, 233531, 233532};
        for (int s : supplies) {
            HANDLERS.put(s, new NpcHandler(400));
        }

        HANDLERS.put(233544, new NpcHandler(200000, (n, p) -> {
            RaceKilledCommanderStatic = p.getRace();
        }));

        int[][] baseOfficers = {
            {233518, 1, 831875, 233517, 233528, 831885, 233498, 233497, 233508},
            {233519, 2, 831876, 233517, 233528, 831886, 233499, 233497, 233508},
            {233520, 3, 831877, 233517, 233528, 831887, 233500, 233497, 233508},
            {233521, 4, 831878, 233517, 233528, 831888, 233501, 233497, 233508},
            {233522, 5, 831879, 233517, 233528, 831889, 233502, 233497, 233508},
            {233523, 6, 831880, 233517, 233528, 831890, 233503, 233497, 233508},
            {233524, 7, 831881, 233517, 233528, 831891, 233504, 233497, 233508},
            {233525, 8, 831882, 233517, 233528, 831892, 233505, 233497, 233508},
            {233526, 9, 831883, 233517, 233528, 831893, 233506, 233497, 233508},
            {233527, 10, 831884, 233517, 233528, 831894, 233507, 233497, 233508},
            {233498, 1, 831885, 233497, 233508, 831875, 233518, 233517, 233528},
            {233499, 2, 831886, 233497, 233508, 831876, 233519, 233517, 233528},
            {233500, 3, 831887, 233497, 233508, 831877, 233520, 233517, 233528},
            {233501, 4, 831888, 233497, 233508, 831878, 233521, 233517, 233528},
            {233502, 5, 831889, 233497, 233508, 831879, 233522, 233517, 233528},
            {233503, 6, 831890, 233497, 233508, 831880, 233523, 233517, 233528},
            {233504, 7, 831891, 233497, 233508, 831881, 233524, 233517, 233528},
            {233505, 8, 831892, 233497, 233508, 831882, 233525, 233517, 233528},
            {233506, 9, 831893, 233497, 233508, 831883, 233526, 233517, 233528},
            {233507, 10, 831894, 233497, 233508, 831884, 233527, 233517, 233528},
            {233550, 1, 831895, 233543, 233547, 831875, 233518, 233517, 233528},
            {233551, 2, 831896, 233543, 233547, 831876, 233519, 233517, 233528},
            {233552, 3, 831897, 233543, 233547, 831877, 233520, 233517, 233528},
            {233553, 4, 831898, 233543, 233547, 831878, 233521, 233517, 233528},
            {233554, 5, 831899, 233543, 233547, 831879, 233522, 233517, 233528},
            {233555, 6, 831900, 233543, 233547, 831880, 233523, 233517, 233528},
            {233556, 7, 831901, 233543, 233547, 831881, 233524, 233517, 233528},
            {233557, 8, 831902, 233543, 233547, 831882, 233525, 233517, 233528},
            {233558, 9, 831903, 233543, 233547, 831883, 233526, 233517, 233528},
            {233559, 10, 831904, 233543, 233547, 831884, 233527, 233517, 233528}
        };

        for (int[] o : baseOfficers) {
            HANDLERS.put(o[0], new NpcHandler(400, (n, p) -> {
                if (ironWallBaseStatic == o[1]) {
                    if (p.getRace() == Race.ELYOS) {
                        deleteNpcStatic(o[2]);
                        deleteNpcStatic(o[3]);
                        deleteNpcStatic(o[4]);
                    } else {
                        deleteNpcStatic(o[5]);
                        deleteNpcStatic(o[6]);
                        deleteNpcStatic(o[7]);
                        deleteNpcStatic(o[8]);
                    }
                }
            }));
        }
    }

    private static void sendMsgByRaceStatic(int msg, Race race) {
        if (instanceStatic != null) {
            instanceStatic.sendMsgByRace(msg, race, 0);
        }
    }

    private static void deleteNpcStatic(int npcId) {
        if (instanceStatic != null) {
            instanceStatic.deleteNpc(npcId);
        }
    }

    protected IronWallWarfrontPlayerReward getPlayerReward(Player player) {
        ironWallWarfrontReward.regPlayerReward(player);
        return (IronWallWarfrontPlayerReward) ironWallWarfrontReward.getPlayerReward(player.getObjectId());
    }
    
    private boolean containPlayer(Integer object) {
        return ironWallWarfrontReward.containPlayer(object);
    }
    
    @Override
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
        int npcId = npc.getNpcId();
        switch (npcId) {
            case 233510: case 233511: case 233512: case 233530: case 233531: case 233532:
                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000287, 5));
                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000288, 5));
                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000286, 5));
                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 164000285, 5));
                break;
            case 831328: case 831329:
                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 182006996, 10));
                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 182006997, 10));
                break;
            case 831330:
                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000137, 1));
                break;
        }
    }
    
    protected void startInstanceTask() {
        instanceTime = System.currentTimeMillis();
        ironWallWarfrontReward.setInstanceStartTime();
        ironWallTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                if (!ironWallWarfrontReward.isRewarded()) {
                    openFirstDoors();
                    sendMsgByRace(1401181, Race.PC_ALL, 5000);
                    ironWallWarfrontReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
                    startInstancePacket();
                    ironWallWarfrontReward.sendPacket(4, null);
                }
            }
        }, 90000));
        ironWallTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                sendPacket(false);
                ironWallWarfrontReward.sendPacket(4, null);
                sendMsgByRace(1402210, Race.PC_ALL, 0);
                sendMsgByRace(1402228, Race.PC_ALL, 10000);
                sendMsgByRace(1402229, Race.PC_ALL, 20000);
                sp(701624, 422.98706f, 641.44116f, 214.52452f, (byte) 92, 0);
                sp(702589, 426.4476f, 617.95264f, 214.52452f, (byte) 32, 0);
            }
        }, 600000));
        ironWallTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                sendPacket(false);
                ironWallWarfrontReward.sendPacket(4, null);
                sendMsgByRace(1402206, Race.PC_ALL, 0);
                sp(233510, 298.95648f, 399.21204f, 227.56165f, (byte) 17, 0);
                sp(233511, 304.02267f, 396.9381f, 227.68314f, (byte) 26, 0);
                sp(233512, 309.49344f, 395.97568f, 227.2273f, (byte) 28, 0);
                sp(831979, 298.95648f, 399.21204f, 227.56165f, (byte) 17, 0);
                sp(831979, 304.02267f, 396.9381f, 227.68314f, (byte) 26, 0);
                sp(831979, 309.49344f, 395.97568f, 227.2273f, (byte) 28, 0);
                sp(233530, 707.57275f, 648.9977f, 203.91081f, (byte) 10, 0);
                sp(233531, 706.97595f, 644.1978f, 203.07692f, (byte) 113, 0);
                sp(233532, 701.6543f, 643.7906f, 202.58696f, (byte) 84, 0);
                sp(831978, 707.57275f, 648.9977f, 203.91081f, (byte) 10, 0);
                sp(831978, 706.97595f, 644.1978f, 203.07692f, (byte) 113, 0);
                sp(831978, 701.6543f, 643.7906f, 202.58696f, (byte) 84, 0);
            }
        }, 900000));
        ironWallTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                sendPacket(false);
                ironWallWarfrontReward.sendPacket(4, null);
                sendMsgByRace(1401819, Race.PC_ALL, 0);
                spawn(233544, 744.06085f, 293.31564f, 233.70102f, (byte) 104);
                spawn(801956, 744.06085f, 293.31564f, 233.70102f, (byte) 104);
            }
        }, 1800000));
        ironWallTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                if (!ironWallWarfrontReward.isRewarded()) {
                    Race winnerRace = ironWallWarfrontReward.getWinnerRaceByScore();
                    stopInstance(winnerRace);
                }
            }
        }, 2400000));
    }
    
    protected void stopInstance(Race race) {
        stopInstanceTask();
        ironWallWarfrontReward.setWinnerRace(race);
        ironWallWarfrontReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
        reward();
        ironWallWarfrontReward.sendPacket(5, null);
    }
    
    @Override
    public void onEnterInstance(final Player player) {
        if (!containPlayer(player.getObjectId())) {
            ironWallWarfrontReward.regPlayerReward(player);
        }
        sendEnterPacket(player);
    }
    
    private void sendEnterPacket(final Player player) {
        instance.doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player opponent) {
                if (player.getRace() != opponent.getRace()) {
                    PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(11, getTime(), getInstanceReward(), player.getObjectId()));
                    PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(11, getTime(), getInstanceReward(), opponent.getObjectId()));
                    PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(3, getTime(), getInstanceReward(), player.getObjectId()));
                } else {
                    PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(11, getTime(), getInstanceReward(), opponent.getObjectId()));
                    if (player.getObjectId() != opponent.getObjectId()) {
                        PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(3, getTime(), getInstanceReward(), player.getObjectId(), 20, 0));
                    }
                }
            }
        });
        sendPacket(true);
        sendPacket(false);
        PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(4, getTime(), getInstanceReward(), player.getObjectId(), 20, 0));
    }
    
    private void startInstancePacket() {
        instance.doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(7, getTime(), ironWallWarfrontReward, instance.getPlayersInside(), true));
                PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(3, getTime(), ironWallWarfrontReward, player.getObjectId(), 0, 0));
                PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(7, getTime(), ironWallWarfrontReward, instance.getPlayersInside(), true));
                PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(11, getTime(), getInstanceReward(), player.getObjectId()));
            }
        });
    }
    
    private void sendPacket(boolean isObjects) {
        if (isObjects) {
            instance.doOnAllPlayers(new Visitor<Player>() {
                @Override
                public void visit(Player player) {
                    PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(6, getTime(), ironWallWarfrontReward, instance.getPlayersInside(), true));
                }
            });
        } else {
            instance.doOnAllPlayers(new Visitor<Player>() {
                @Override
                public void visit(Player player) {
                    PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(7, getTime(), ironWallWarfrontReward, instance.getPlayersInside(), true));
                }
            });
        }
    }
    
    @Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
        ironWallWarfrontReward = new IronWallWarfrontReward(mapId, instanceId, instance);
        ironWallWarfrontReward.setInstanceScoreType(InstanceScoreType.PREPARING);
        doors = instance.getDoors();
        instanceStatic = this;
        startInstanceTask();
    }
    
    protected void reward() {
        int ElyosPvPKills = getPvpKillsByRace(Race.ELYOS).intValue();
        int ElyosPoints = getPointsByRace(Race.ELYOS).intValue();
        int AsmoPvPKills = getPvpKillsByRace(Race.ASMODIANS).intValue();
        int AsmoPoints = getPointsByRace(Race.ASMODIANS).intValue();
        for (Player player : instance.getPlayersInside()) {
            if (PlayerActions.isAlreadyDead(player)) {
                PlayerReviveService.duelRevive(player);
            }
            IronWallWarfrontPlayerReward playerReward = ironWallWarfrontReward.getPlayerReward(player.getObjectId());
            int abyssPoint = 3163;
            int gloryPoint = 300;
            int expPoint = 10000;
            playerReward.setRewardAp(abyssPoint);
            playerReward.setRewardGp(gloryPoint);
            playerReward.setRewardExp(expPoint);
            if (player.getRace().equals(ironWallWarfrontReward.getWinnerRace())) {
                abyssPoint += ironWallWarfrontReward.AbyssReward(true, isCommanderKilled(player.getRace()));
                gloryPoint += ironWallWarfrontReward.GloryReward(true, isCommanderKilled(player.getRace()));
                expPoint += ironWallWarfrontReward.ExpReward(true, isCommanderKilled(player.getRace()));
                playerReward.setBonusAp(ironWallWarfrontReward.AbyssReward(true, isCommanderKilled(player.getRace())));
                playerReward.setBonusGp(ironWallWarfrontReward.GloryReward(true, isCommanderKilled(player.getRace())));
                playerReward.setBonusExp(ironWallWarfrontReward.ExpReward(true, isCommanderKilled(player.getRace())));
                playerReward.setBrokenSpinel(188100391);
                playerReward.setBonusReward(186000243);
            } else {
                abyssPoint += ironWallWarfrontReward.AbyssReward(false, isCommanderKilled(player.getRace()));
                gloryPoint += ironWallWarfrontReward.GloryReward(false, isCommanderKilled(player.getRace()));
                expPoint += ironWallWarfrontReward.ExpReward(false, isCommanderKilled(player.getRace()));
                playerReward.setRewardAp(ironWallWarfrontReward.AbyssReward(false, isCommanderKilled(player.getRace())));
                playerReward.setRewardGp(ironWallWarfrontReward.GloryReward(false, isCommanderKilled(player.getRace())));
                playerReward.setRewardExp(ironWallWarfrontReward.ExpReward(false, isCommanderKilled(player.getRace())));
                playerReward.setBrokenSpinel(188100391);
                playerReward.setBonusReward(186000243);
            }
            if (RaceKilledCommander == player.getRace()) {
                playerReward.setMedalBundle(188052938);
                ItemService.addItem(player, 188052938, 1);
            }
            ItemService.addItem(player, 188100391, 750);
            ItemService.addItem(player, 186000243, 1);
            AbyssPointsService.addAp(player, abyssPoint);
            AbyssPointsService.addGp(player, gloryPoint);
            player.getCommonData().addExp(expPoint, RewardType.HUNTING);
        }
        for (Npc npc : instance.getNpcs()) {
            npc.getController().onDelete();
        }
        GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    for (Player player : instance.getPlayersInside()) {
                        onExitInstance(player);
                    }
                    AutoGroupService.getInstance().unRegisterInstance(instanceId);
                }
            }
        }, 60000);
    }
    
    private int getTime() {
        long result = System.currentTimeMillis() - instanceTime;
        if (result < 90000) {
            return (int) (90000 - result);
        } else if (result < 2400000) {
            return (int) (2400000 - (result - 90000));
        }
        return 0;
    }
    
    @Override
    public boolean onReviveEvent(Player player) {
        PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
        PlayerReviveService.revive(player, 100, 100, false, 0);
        player.getGameStats().updateStatsAndSpeedVisually();
        ironWallWarfrontReward.portToPosition(player);
        return true;
    }
    
    @Override
    public boolean onDie(Player player, Creature lastAttacker) {
        IronWallWarfrontPlayerReward ownerReward = ironWallWarfrontReward.getPlayerReward(player.getObjectId());
        ownerReward.endBoostMoraleEffect(player);
        ownerReward.applyBoostMoraleEffect(player);
        int points = 60;
        if (lastAttacker instanceof Player) {
            if (lastAttacker.getRace() != player.getRace()) {
                InstancePlayerReward playerReward = ironWallWarfrontReward.getPlayerReward(player.getObjectId());
                if (getPointsByRace(lastAttacker.getRace()).compareTo(getPointsByRace(player.getRace())) < 0) {
                    points *= loosingGroupMultiplier;
                } else if (loosingGroupMultiplier == 10 || playerReward.getPoints() == 0) {
                    points = 0;
                }
                updateScore((Player) lastAttacker, player, points, true);
            }
        }
        updateScore(player, player, -points, false);
        return true;
    }
    
    private boolean isCommanderKilled(Race PlayerRace) {
        return PlayerRace == RaceKilledCommander;
    }
    
    private MutableInt getPvpKillsByRace(Race race) {
        return ironWallWarfrontReward.getPvpKillsByRace(race);
    }
    
    private MutableInt getPointsByRace(Race race) {
        return ironWallWarfrontReward.getPointsByRace(race);
    }
    
    private void addPointsByRace(Race race, int points) {
        ironWallWarfrontReward.addPointsByRace(race, points);
    }
    
    private void addPvpKillsByRace(Race race, int points) {
        ironWallWarfrontReward.addPvpKillsByRace(race, points);
    }
    
    private void addPointToPlayer(Player player, int points) {
        ironWallWarfrontReward.getPlayerReward(player.getObjectId()).addPoints(points);
    }
    
    private void addPvPKillToPlayer(Player player) {
        ironWallWarfrontReward.getPlayerReward(player.getObjectId()).addPvPKillToPlayer();
    }
    
    protected void updateScore(Player player, Creature target, int points, boolean pvpKill) {
        if (points == 0) {
            return;
        }
        addPointsByRace(player.getRace(), points);
        List<Player> playersToGainScore = new ArrayList<Player>();
        if (target != null && player.isInGroup2()) {
            for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
                if (member.getLifeStats().isAlreadyDead()) {
                    continue;
                }
                if (MathUtil.isIn3dRange(member, target, GroupConfig.GROUP_MAX_DISTANCE)) {
                    playersToGainScore.add(member);
                }
            }
        } else {
            playersToGainScore.add(player);
        }
        for (Player playerToGainScore : playersToGainScore) {
            addPointToPlayer(playerToGainScore, points / playersToGainScore.size());
            if (target instanceof Npc) {
                PacketSendUtility.sendPacket(playerToGainScore, new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(((Npc) target).getObjectTemplate().getNameId() * 2 + 1), points));
            } else if (target instanceof Player) {
                PacketSendUtility.sendPacket(playerToGainScore, new SM_SYSTEM_MESSAGE(1400237, target.getName(), points));
            }
        }
        int pointDifference = getPointsByRace(Race.ASMODIANS).intValue() - (getPointsByRace(Race.ELYOS)).intValue();
        if (pointDifference < 0) {
            pointDifference *= -1;
        }
        if (pointDifference >= 3000) {
            loosingGroupMultiplier = 10;
        } else if (pointDifference >= 1000) {
            loosingGroupMultiplier = 1.5f;
        } else {
            loosingGroupMultiplier = 1;
        }
        if (pvpKill && points > 0) {
            addPvpKillsByRace(player.getRace(), 1);
            addPvPKillToPlayer(player);
        }
        ironWallWarfrontReward.sendPacket(11, player.getObjectId());
        if (ironWallWarfrontReward.hasCapPoints()) {
            stopInstance(ironWallWarfrontReward.getWinnerRaceByScore());
        }
    }
    
    @Override
    public void onEnterZone(Player player, ZoneInstance zone) {
        if (zone.getAreaTemplate().getZoneName() == ZoneName.get("PERIPHERAL_SUPPLY_BASE_301220000")) {
            ironWallBase = 1;
        } else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("MILITARY_SUPPLY_BASE_301220000")) {
            ironWallBase = 2;
        } else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("CENTRAL_SUPPLY_BASE_301220000")) {
            ironWallBase = 3;
        } else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("ARTILLERY_COMMAND_CENTER_301220000")) {
            ironWallBase = 4;
        } else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("AXIAL_SENTRY_POST_301220000")) {
            ironWallBase = 5;
        } else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("ANCILLARY_SENTRY_POST_301220000")) {
            ironWallBase = 6;
        } else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("HOLY_GROUND_OF_RESURRECTION_301220000")) {
            ironWallBase = 7;
        } else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("ASSAULT_COMMAND_CENTER_301220000")) {
            ironWallBase = 8;
        } else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("HEADQUARTERS_301220000")) {
            ironWallBase = 9;
        } else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("HEADQUARTERS_ANNEX_301220000")) {
            ironWallBase = 10;
        }
        ironWallBaseStatic = ironWallBase;
    }
    
    @Override
    public void onDie(Npc npc) {
        Player mostPlayerDamage = npc.getAggroList().getMostPlayerDamage();
        if (mostPlayerDamage == null) {
            return;
        }
        
        RaceKilledCommanderStatic = RaceKilledCommander;
        NpcHandler handler = HANDLERS.get(npc.getNpcId());
        
        if (handler != null) {
            if (handler.onDeath != null) {
                handler.onDeath.accept(npc, mostPlayerDamage);
            }
            
            Race killerRace = mostPlayerDamage.getRace();
            List<SpawnData> spawns = handler.spawns.get(killerRace);
            if (spawns != null) {
                for (SpawnData s : spawns) {
                    spawn(s.npcId, s.x, s.y, s.z, s.h, 0);
                }
            }
            
            if (handler.despawn) {
                despawnNpc(npc);
            }
            
            updateScore(mostPlayerDamage, npc, handler.points, false);
        }
        
        RaceKilledCommander = RaceKilledCommanderStatic;
    }
    
    @Override
    public void handleUseItemFinish(Player player, Npc npc) {
        switch (npc.getNpcId()) {
            case 831909: case 831910: case 831914: case 831915:
                despawnNpc(npc);
                sendMsgByRace(1402109, Race.PC_ALL, 0);
                sp(855240, 440.48346f, 648.9064f, 213.875f, (byte) 82, 5000);
                sp(855240, 435.1033f, 637.4064f, 214.52452f, (byte) 81, 5500);
                sp(855240, 426.12326f, 628.37146f, 214.52452f, (byte) 62, 6000);
                sp(855240, 410.35355f, 626.63544f, 214.52452f, (byte) 62, 6500);
                sp(855240, 394.76648f, 640.9771f, 214.52452f, (byte) 44, 7000);
                sp(855240, 398.99435f, 611.30725f, 214.52452f, (byte) 83, 7500);
                break;
            case 831911: case 831912: case 831913: case 831916: case 831917: case 831918:
                despawnNpc(npc);
                sendMsgByRace(1402110, Race.PC_ALL, 0);
                sp(855240, 612.48193f, 246.57852f, 227.24548f, (byte) 33, 5000);
                sp(855240, 607.7852f, 273.68695f, 226.78299f, (byte) 33, 5500);
                sp(855240, 609.47485f, 295.29547f, 226.25f, (byte) 28, 6000);
                sp(855240, 611.2592f, 316.33997f, 226.25f, (byte) 22, 6500);
                sp(855240, 619.06146f, 338.56107f, 225.94135f, (byte) 22, 7000);
                sp(855240, 623.8632f, 352.7094f, 225.85753f, (byte) 14, 7500);
                sp(855240, 637.295f, 366.08438f, 228.58621f, (byte) 15, 8000);
                sp(855240, 649.25397f, 381.53574f, 228.625f, (byte) 14, 8500);
                sp(855240, 638.361f, 393.87704f, 226.625f, (byte) 44, 9000);
                sp(855240, 624.7944f, 409.59f, 226.625f, (byte) 44, 9500);
                sp(855240, 619.216f, 426.68207f, 226.61574f, (byte) 30, 10000);
                sp(855240, 640.5313f, 435.39978f, 226.62898f, (byte) 6, 10500);
                sp(855240, 653.0702f, 420.92535f, 226.99039f, (byte) 103, 11000);
                sp(855240, 667.27106f, 404.47855f, 228.23833f, (byte) 103, 11500);
                sp(855240, 681.4431f, 404.91577f, 229.27058f, (byte) 7, 12000);
                sp(855240, 699.13165f, 412.75574f, 230.8985f, (byte) 6, 12500);
                sp(855240, 713.01105f, 416.59003f, 231.0f, (byte) 3, 13000);
                sp(855240, 730.56836f, 415.3179f, 230.96448f, (byte) 118, 13500);
                break;
        }
    }
    
    private void removeItems(Player player) {
        Storage storage = player.getInventory();
        storage.decreaseByItemId(185000137, storage.getItemCountByItemId(185000137));
        storage.decreaseByItemId(182006996, storage.getItemCountByItemId(182006996));
        storage.decreaseByItemId(182006997, storage.getItemCountByItemId(182006997));
    }
    
    private void despawnNpc(Npc npc) {
        if (npc != null) {
            npc.getController().onDelete();
        }
    }
    
    private void deleteNpc(int npcId) {
        Npc npc = getNpc(npcId);
        if (npc != null) {
            npc.getController().onDelete();
        }
    }
    
    @Override
    public void onInstanceDestroy() {
        ironWallWarfrontReward.clear();
        isInstanceDestroyed = true;
        stopInstanceTask();
        doors.clear();
        instanceStatic = null;
    }
    
    protected void openFirstDoors() {
        openDoor(2);
        openDoor(17);
        openDoor(26);
        openDoor(35);
    }
    
    protected void openDoor(int doorId) {
        StaticDoor door = doors.get(doorId);
        if (door != null) {
            door.setOpen(true);
        }
    }
    
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time) {
        sp(npcId, x, y, z, h, 0, time, 0, null);
    }
    
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final int msg, final Race race) {
        sp(npcId, x, y, z, h, 0, time, msg, race);
    }
    
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int entityId, final int time, final int msg, final Race race) {
        ironWallTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    spawn(npcId, x, y, z, h, entityId);
                    if (msg > 0) {
                        sendMsgByRace(msg, race, 0);
                    }
                }
            }
        }, time));
    }
    
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        ironWallTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    Npc npc = (Npc) spawn(npcId, x, y, z, h);
                    npc.getSpawn().setWalkerId(walkerId);
                    WalkManager.startWalking((NpcAI2) npc.getAi2());
                }
            }
        }, time));
    }
    
    protected void sendMsgByRace(final int msg, final Race race, int time) {
        ironWallTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                instance.doOnAllPlayers(new Visitor<Player>() {
                    @Override
                    public void visit(Player player) {
                        if (player.getRace().equals(race) || race.equals(Race.PC_ALL)) {
                            PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msg));
                        }
                    }
                });
            }
        }, time));
    }
    
    private void stopInstanceTask() {
        for (Future<?> task : ironWallTask) {
			if (task != null) {
				task.cancel(true);
			}
        }
    }
    
    @Override
    public InstanceReward<?> getInstanceReward() {
        return ironWallWarfrontReward;
    }
    
    @Override
    public void onExitInstance(Player player) {
        TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
    }
    
    @Override
    public void onLeaveInstance(Player player) {
        PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
        IronWallWarfrontPlayerReward playerReward = ironWallWarfrontReward.getPlayerReward(player.getObjectId());
        playerReward.endBoostMoraleEffect(player);
        removeItems(player);
    }
    
    @Override
    public void onPlayerLogin(Player player) {
        ironWallWarfrontReward.sendPacket(10, player.getObjectId());
    }
}
