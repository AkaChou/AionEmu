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
package com.aionemu.gameserver.instance.handlers.scripts.crucible;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.templates.tower_reward.TowerStageRewardTemplate;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.ranking.SeasonRankingService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.*;
import java.util.concurrent.Future;

/****/
/** Author (Encom)
/** Source: https://www.youtube.com/watch?v=KURJ3_EcrB4&feature=youtu.be
/****/

@InstanceID(302400000)
public class CrucibleSpireInstance extends GeneralInstanceHandler {

    private byte floor;
    private Race spawnRace;
    private Map<Integer, StaticDoor> doors;
    protected boolean isInstanceDestroyed = false;
    private final List<Future<?>> crucibleTask = new ArrayList<>();
    private final Map<Integer, Long> lastTeleportTime = new HashMap<>();
    private boolean isSpawning = false;

    private long bossTimerStart;
    private long bossTimerEnd;
    
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
    
    @Override
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = DropRegistrationService.getInstance().getCurrentDropMap().get(npc.getObjectId());
        int npcId = npc.getNpcId();
        switch (npcId) {
            case 247546: //IDInfinity Heal 02.
                dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 164000530, 1));
                break;
        }
    }
    
    private void removeItems(Player player) {
        Storage storage = player.getInventory();
        storage.decreaseByItemId(164000530, storage.getItemCountByItemId(164000530));
    }
    
    @Override
    public void onEnterInstance(final Player player) {
        super.onEnterInstance(player); 
        if (spawnRace == null) {
            spawnRace = player.getRace();
            spawnInggrilInggness1();
            int pfloor = player.getFloor();
            sendPacket(player, "Condition_Infinity_PRE_SEASON_Floor", pfloor);
            sendPacket(player, "Condition_Infinity_THIS_SEASON_Floor", pfloor + 1);
            sendPacket(player, "Condition_Infinity_THIS_SEASON_Floor_Reward", pfloor);
        }
    }
    
    @Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
        doors = instance.getDoors();
        floor = 1;
        spawnFloorRings();
        spawn(247546, 254.38080f, 245.29360f, 241.08308f, (byte) 55);
        spawn(247311, 255.26721f, 249.49001f, 242.03000f, (byte) 0, 71);
        spawn(701773, 263.67166f, 249.42833f, 240.82626f, (byte) 0, 284);
        spawn(247310, 279.90976f, 243.26570f, 243.45923f, (byte) 0, 57);
        spawn(247310, 279.61618f, 1255.5001f, 243.42058f, (byte) 0, 58);
        spawn(247310, 279.62357f, 1243.2299f, 243.50325f, (byte) 0, 59);
        spawn(247310, 279.90237f, 255.53593f, 243.45923f, (byte) 0, 60);
        spawn(701772, 280.85883f, 249.46001f, 241.08347f, (byte) 0, 115);
    }
    
    private void sendPacket(Player player, final String variable, final int floor) {
        PacketSendUtility.sendPacket(player, new SM_CONDITION_VARIABLE(player, variable, floor));
    }
    
    private void spawnInggrilInggness1() {
        final int Inggril_Inggness1 = spawnRace == Race.ASMODIANS ? 247386 : 247376;
        spawn(Inggril_Inggness1, 255.26721f, 249.49001f, 242.03000f, (byte) 60);
    }
    
    private void spawnInggrilInggness2() {
        final int Inggril_Inggness2 = spawnRace == Race.ASMODIANS ? 247386 : 247376;
        spawn(Inggril_Inggness2, 255.26721f, 249.49001f, 242.03000f, (byte) 60);
    }
    
    private void teleportCrucibleFloor(Player player) {
        isSpawning = true;
        int pfloor = player.getFloor();
        spawnNextFloor(pfloor + 1);
        GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                deleteNpc(701773);
            }
        }, 2500);
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
    
	private void spawnNextFloor(int next) {
		switch (next) {
			case 1:
			break;
			case 2:
				spawn(247249, 241.18533f, 1256.9836f, 240.63419f, (byte) 61);
				spawn(247249, 241.29503f, 1242.1162f, 240.63419f, (byte) 60);
				spawn(247250, 244.41614f, 1246.4686f, 240.63419f, (byte) 61);
                spawn(247250, 244.21953f, 1252.8258f, 240.63419f, (byte) 61);
			break;
			case 3:
				spawn(247251, 248.74625f, 1249.5614f, 240.63419f, (byte) 60);
                spawn(247251, 241.17021f, 1257.0828f, 240.63419f, (byte) 61);
                spawn(247251, 241.17314f, 1242.0288f, 240.63419f, (byte) 61);
				spawn(247252, 259.44302f, 1249.6051f, 240.71162f, (byte) 60);
                spawn(247252, 253.85323f, 1262.3983f, 240.71162f, (byte) 76);
                spawn(247252, 254.24211f, 1236.7045f, 240.71162f, (byte) 45);
			break;
			case 4:
				spawn(247236, 241.45598f, 1249.5740f, 240.63419f, (byte) 60);
			break;
			case 5:
				spawn(247351, 254.19934f, 1262.5269f, 240.71162f, (byte) 36);
				spawn(247351, 253.87862f, 1236.6519f, 240.71162f, (byte) 9);
				spawn(247253, 252.67638f, 1264.4946f, 240.71162f, (byte) 101);
				spawn(247253, 252.09209f, 1233.2440f, 240.71162f, (byte) 14);
                spawn(247253, 250.93297f, 1235.5901f, 240.71162f, (byte) 7);
                spawn(247253, 251.68967f, 1262.4159f, 240.71162f, (byte) 113);
				spawn(247254, 255.53033f, 1260.8771f, 240.71162f, (byte) 42);
                spawn(247254, 256.08560f, 1237.6760f, 240.71162f, (byte) 77);
                spawn(247254, 253.70976f, 1238.7314f, 240.71162f, (byte) 84);
			break;
			case 6:
				spawn(247352, 253.81844f, 1262.5720f, 240.71162f, (byte) 61);
                spawn(247352, 245.95355f, 1231.9944f, 240.71162f, (byte) 25);
				spawn(247255, 252.66000f, 1265.1761f, 240.71162f, (byte) 102);
                spawn(247255, 251.31412f, 1262.7773f, 240.71162f, (byte) 114);
				spawn(247255, 248.28351f, 1231.3329f, 240.71162f, (byte) 58);
				spawn(247256, 256.23257f, 1261.0988f, 240.71162f, (byte) 46);
                spawn(247256, 254.00407f, 1259.5002f, 240.71162f, (byte) 36);
                spawn(247256, 242.98157f, 1229.6989f, 240.71162f, (byte) 6);
                spawn(247256, 242.80790f, 1232.9885f, 240.71162f, (byte) 116);
                spawn(247256, 247.98917f, 1234.1317f, 240.71162f, (byte) 73);
			break;
			case 7:
				spawn(247354, 254.03172f, 1262.3046f, 240.71162f, (byte) 113);
				spawn(247354, 253.98647f, 1236.5012f, 240.71162f, (byte) 23);
				spawn(247257, 252.40446f, 1265.0981f, 241.09122f, (byte) 103);
                spawn(247257, 251.31787f, 1263.1189f, 240.71162f, (byte) 113);
                spawn(247257, 252.93219f, 1233.4966f, 240.71162f, (byte) 17);
                spawn(247257, 251.10315f, 1235.8412f, 240.71162f, (byte) 7);
				spawn(247258, 256.58646f, 1261.8520f, 240.71162f, (byte) 49);
                spawn(247258, 254.23650f, 1260.2627f, 240.71162f, (byte) 36);
                spawn(247258, 254.60794f, 1239.1844f, 240.71162f, (byte) 83);
                spawn(247258, 256.51114f, 1237.3129f, 240.71162f, (byte) 71);
			break;
			case 8:
				spawn(247237, 241.19528f, 1249.6161f, 240.63419f, (byte) 60);
				spawn(247353, 236.23781f, 1266.9647f, 240.71162f, (byte) 95);
                spawn(247353, 254.19570f, 1236.9214f, 240.71162f, (byte) 44);
                spawn(247353, 253.82140f, 1262.2661f, 240.71162f, (byte) 74);
                spawn(247353, 236.80017f, 1231.8394f, 240.71162f, (byte) 25);
				spawn(247401, 241.33926f, 1241.8188f, 240.63419f, (byte) 57);
				spawn(247401, 241.39090f, 1257.0540f, 240.63419f, (byte) 60);
                spawn(247401, 249.24400f, 1249.5896f, 240.63419f, (byte) 60);
                spawn(247401, 245.36331f, 1253.8690f, 240.63419f, (byte) 63);
                spawn(247401, 245.44702f, 1245.5221f, 240.63419f, (byte) 61);
			break;
			case 9:
				spawn(247259, 241.29816f, 1246.0281f, 240.63419f, (byte) 60);
                spawn(247259, 241.24530f, 1253.0829f, 240.63419f, (byte) 60);
				spawn(247260, 241.01903f, 1268.0564f, 240.71162f, (byte) 60);
                spawn(247260, 241.35820f, 1231.3574f, 240.71162f, (byte) 60);
			break;
			case 10:
				spawn(247261, 260.63998f, 1252.6572f, 240.71162f, (byte) 63);
                spawn(247261, 260.53223f, 1246.9858f, 240.71162f, (byte) 59);
				spawn(247262, 241.46776f, 1249.6780f, 240.63419f, (byte) 61);
                spawn(247262, 240.68369f, 1264.4435f, 240.63419f, (byte) 68);
                spawn(247262, 241.24583f, 1234.4695f, 240.63419f, (byte) 53);
			break;
			case 11:
				spawn(247263, 240.94872f, 1264.3417f, 240.63419f, (byte) 68);
                spawn(247263, 241.18830f, 1234.7659f, 240.63419f, (byte) 51);
				spawn(247264, 238.01044f, 1249.5957f, 240.63419f, (byte) 60);
                spawn(247264, 245.00499f, 1249.6553f, 240.63419f, (byte) 60);
                spawn(247264, 241.31288f, 1253.0376f, 240.63419f, (byte) 59);
                spawn(247264, 241.27722f, 1246.1240f, 240.63419f, (byte) 59);
			break;
			case 12:
				spawn(247238, 241.50197f, 1249.5283f, 240.63419f, (byte) 60);
			break;
			case 13:
				spawn(247265, 241.18263f, 1249.5398f, 240.63419f, (byte) 60);
                spawn(247265, 258.61935f, 1253.3063f, 240.71162f, (byte) 62);
                spawn(247265, 258.54715f, 1246.3235f, 240.71162f, (byte) 58);
				spawn(247266, 241.18523f, 1260.1180f, 240.63419f, (byte) 59);
                spawn(247266, 241.04256f, 1239.2163f, 240.63419f, (byte) 60);
			break;
			case 14:
			    spawn(247267, 241.15300f, 1260.6820f, 240.63419f, (byte) 60);
                spawn(247267, 241.20154f, 1238.8978f, 240.63419f, (byte) 60);
				spawn(247268, 260.02588f, 1244.4471f, 240.71162f, (byte) 56);
                spawn(247268, 260.64893f, 1249.4431f, 240.71162f, (byte) 58);
                spawn(247268, 259.95993f, 1254.9393f, 240.71162f, (byte) 63);
			break;
			case 15:
			    spawn(247269, 241.31754f, 1249.5798f, 240.63419f, (byte) 60);
				spawn(247270, 260.19244f, 1247.5491f, 240.71162f, (byte) 58);
                spawn(247270, 259.95400f, 1251.8992f, 240.71162f, (byte) 61);
                spawn(247270, 241.28294f, 1242.0948f, 240.63419f, (byte) 60);
                spawn(247270, 241.15080f, 1257.0294f, 240.63419f, (byte) 62);
			break;
			case 16:
				spawn(247239, 241.06525f, 1249.5922f, 240.63419f, (byte) 59);
                spawn(247239, 246.20310f, 1244.5913f, 240.63419f, (byte) 61);
                spawn(247239, 246.25809f, 1254.7360f, 240.63419f, (byte) 60);
			break;
			case 17:
			    spawn(247271, 241.25314f, 1246.1554f, 240.63419f, (byte) 60);
                spawn(247271, 241.21352f, 1253.0051f, 240.63419f, (byte) 61);
				spawn(247272, 248.43343f, 1262.3562f, 240.63419f, (byte) 51);
                spawn(247272, 248.46645f, 1236.8959f, 240.63419f, (byte) 69);
			break;
			case 18:
				spawn(247273, 259.59085f, 1254.5885f, 240.71162f, (byte) 64);
                spawn(247273, 259.67526f, 1244.8475f, 240.71162f, (byte) 57);
				spawn(247274, 241.08554f, 1264.1904f, 240.63419f, (byte) 61);
				spawn(247355, 259.31012f, 1249.4916f, 240.71162f, (byte) 59);
			break;
			case 19:
			    spawn(247275, 255.78647f, 1238.9512f, 240.71162f, (byte) 56);
                spawn(247275, 250.05235f, 1242.1001f, 240.63419f, (byte) 0);
				spawn(247276, 257.48360f, 1246.4463f, 240.71162f, (byte) 76);
                spawn(247276, 247.59322f, 1232.3746f, 240.71162f, (byte) 18);
				spawn(247356, 248.72170f, 1237.0615f, 240.63419f, (byte) 59);
                spawn(247356, 254.23114f, 1242.8639f, 240.63419f, (byte) 75);
			break;
			case 20:
			    spawn(247240, 241.38167f, 1249.6044f, 240.63419f, (byte) 60);
			break;
			case 21:
			    spawn(247277, 241.23924f, 1249.5948f, 240.63419f, (byte) 60);
				spawn(247278, 246.15216f, 1254.5807f, 240.63419f, (byte) 60);
                spawn(247278, 246.55399f, 1244.3586f, 240.63419f, (byte) 60);
			break;
			case 22:
			    spawn(247279, 241.14186f, 1264.6486f, 240.63419f, (byte) 90);
                spawn(247279, 241.03874f, 1234.2322f, 240.63419f, (byte) 29);
				spawn(247280, 259.70460f, 1254.7360f, 240.71162f, (byte) 62);
                spawn(247280, 259.69165f, 1244.3368f, 240.71162f, (byte) 58);
			break;
			case 23:
			    spawn(247281, 241.41281f, 1249.5133f, 240.63419f, (byte) 60);
				spawn(247282, 246.13629f, 1244.5453f, 240.63419f, (byte) 60);
                spawn(247282, 245.91959f, 1254.5669f, 240.63419f, (byte) 62);
                spawn(247282, 261.21832f, 1249.5925f, 240.71162f, (byte) 60);
			break;
			case 24:
			    spawn(247241, 241.41281f, 1249.5133f, 240.63419f, (byte) 60);
			break;
			case 25:
			    spawn(247283, 261.01346f, 1249.5574f, 240.71162f, (byte) 60);
				spawn(247284, 254.40323f, 1242.5317f, 240.63419f, (byte) 51);
                spawn(247284, 254.34718f, 1256.6465f, 240.63419f, (byte) 69);
			break;
			case 26:
			    spawn(247285, 248.59853f, 1249.5581f, 240.63419f, (byte) 60);
                spawn(247285, 241.13426f, 1234.3336f, 240.63419f, (byte) 30);
                spawn(247285, 241.01393f, 1264.6925f, 240.63419f, (byte) 90);
				spawn(247286, 261.09265f, 1249.5715f, 240.71162f, (byte) 60);
			break;
			case 27:
			    spawn(247287, 241.04361f, 1249.5405f, 240.63419f, (byte) 60);
                spawn(247287, 241.02223f, 1254.7899f, 240.63419f, (byte) 60);
                spawn(247287, 241.13141f, 1244.1161f, 240.63419f, (byte) 60);
                spawn(247287, 244.44319f, 1246.9247f, 240.63419f, (byte) 60);
                spawn(247287, 244.40683f, 1252.1543f, 240.63419f, (byte) 60);
			break;
			case 28:
			    spawn(247242, 241.41281f, 1249.5133f, 240.63419f, (byte) 60);
			break;
			case 29:
			    switch (Rnd.get(1, 2)) {
				    case 1:
				        spawn(701692, 240.99178f, 1236.1763f, 238.74855f, (byte) 0, 186);
						spawn(701692, 240.99176f, 1262.8556f, 238.74855f, (byte) 0, 196);
						spawn(247360, 240.99178f, 1236.1763f, 242.58624f, (byte) 0, 195);
						spawn(247360, 240.99176f, 1262.8556f, 242.58624f, (byte) 0, 199);
						spawn(247289, 241.41281f, 1249.5133f, 240.63419f, (byte) 60);
					break;
					case 2:
				        spawn(701692, 240.99178f, 1236.1763f, 238.74855f, (byte) 0, 186);
						spawn(701692, 240.99176f, 1262.8556f, 238.74855f, (byte) 0, 196);
						spawn(247359, 240.99178f, 1236.1763f, 242.58624f, (byte) 0, 197);
						spawn(247359, 240.99176f, 1262.8556f, 242.58624f, (byte) 0, 198);
						spawn(247290, 241.41281f, 1249.5133f, 240.63419f, (byte) 60);
					break;
				}
			break;
			case 30:
			    switch (Rnd.get(1, 2)) {
				    case 1:
				        spawn(701692, 240.99178f, 1236.1763f, 238.74855f, (byte) 0, 186);
						spawn(701692, 240.99176f, 1262.8556f, 238.74855f, (byte) 0, 196);
						spawn(247360, 240.99178f, 1236.1763f, 242.58624f, (byte) 0, 195);
						spawn(247360, 240.99176f, 1262.8556f, 242.58624f, (byte) 0, 199);
						spawn(247291, 241.41281f, 1249.5133f, 240.63419f, (byte) 60);
					break;
					case 2:
				        spawn(701692, 240.99178f, 1236.1763f, 238.74855f, (byte) 0, 186);
						spawn(701692, 240.99176f, 1262.8556f, 238.74855f, (byte) 0, 196);
						spawn(247359, 240.99178f, 1236.1763f, 242.58624f, (byte) 0, 197);
						spawn(247359, 240.99176f, 1262.8556f, 242.58624f, (byte) 0, 198);
						spawn(247292, 241.41281f, 1249.5133f, 240.63419f, (byte) 60);
					break;
				}
			break;
			case 31:
			    spawn(701692, 240.99178f, 1236.1763f, 238.74855f, (byte) 0, 186);
				spawn(247359, 240.99178f, 1236.1763f, 242.58624f, (byte) 0, 197);
			    spawn(701692, 240.99176f, 1262.8556f, 238.74855f, (byte) 0, 196);
				spawn(247360, 240.99176f, 1262.8556f, 242.58624f, (byte) 0, 199);
				spawn(247293, 259.91907f, 1254.8942f, 240.71162f, (byte) 62);
				spawn(247294, 260.09055f, 1244.4081f, 240.71162f, (byte) 61);
			break;
			case 32:
			    spawn(701692, 240.99178f, 1236.1763f, 238.74855f, (byte) 0, 186);
				spawn(247359, 240.99178f, 1236.1763f, 242.58624f, (byte) 0, 197);
			    spawn(701692, 240.99176f, 1262.8556f, 238.74855f, (byte) 0, 196);
				spawn(247360, 240.99176f, 1262.8556f, 242.58624f, (byte) 0, 199);
				spawn(247243, 241.41281f, 1249.5133f, 240.63419f, (byte) 60);
			break;
			case 33:
			    switch (Rnd.get(1, 2)) {
				    case 1:
				        spawn(247295, 257.12485f, 1258.1670f, 240.71162f, (byte) 70);
						spawn(247295, 257.45935f, 1240.7285f, 240.71162f, (byte) 49);
						spawn(247295, 245.13070f, 1245.4384f, 240.63419f, (byte) 60);
                        spawn(247295, 245.39058f, 1253.8103f, 240.63419f, (byte) 60);
					break;
					case 2:
				        spawn(247296, 257.12485f, 1258.1670f, 240.71162f, (byte) 70);
						spawn(247296, 257.45935f, 1240.7285f, 240.71162f, (byte) 49);
						spawn(247296, 245.13070f, 1245.4384f, 240.63419f, (byte) 60);
                        spawn(247296, 245.39058f, 1253.8103f, 240.63419f, (byte) 60);
					break;
				}
			break;
			case 34:
			    spawn(247297, 236.40814f, 1244.7013f, 240.63419f, (byte) 60);
                spawn(247297, 236.40082f, 1254.6990f, 240.63419f, (byte) 60);
				spawn(247298, 241.12361f, 1249.5305f, 240.63419f, (byte) 60);
                spawn(247298, 248.58983f, 1257.3235f, 240.63419f, (byte) 60);
                spawn(247298, 248.41464f, 1242.6595f, 240.63419f, (byte) 59);
			break;
			case 35:
			    spawn(247299, 241.43329f, 1249.5488f, 240.63419f, (byte) 60);
                spawn(247299, 241.20960f, 1257.1047f, 240.63419f, (byte) 66);
                spawn(247299, 241.27136f, 1241.9293f, 240.63419f, (byte) 55);
				spawn(247300, 253.67844f, 1262.4369f, 240.71162f, (byte) 75);
				spawn(247300, 259.53360f, 1249.4660f, 240.71162f, (byte) 59);
                spawn(247300, 254.33441f, 1236.7448f, 240.71162f, (byte) 44);
			break;
			case 36:
			    spawn(247244, 241.41281f, 1249.5133f, 240.63419f, (byte) 60);
				spawn(247373, 246.26080f, 1254.5687f, 240.63419f, (byte) 59);
                spawn(247373, 246.05994f, 1244.3612f, 240.63419f, (byte) 57);
			break;
			case 37:
			    spawn(247301, 246.11182f, 1254.5605f, 240.63419f, (byte) 59);
                spawn(247301, 246.02896f, 1244.5856f, 240.63419f, (byte) 59);
				spawn(247302, 241.12560f, 1247.9340f, 240.63419f, (byte) 59);
                spawn(247302, 241.15268f, 1251.1821f, 240.63419f, (byte) 59);
			break;
			case 38:
			    spawn(247303, 241.44724f, 1249.6206f, 240.63419f, (byte) 60);
                spawn(247303, 254.86494f, 1242.2816f, 240.63419f, (byte) 50);
                spawn(247303, 254.50504f, 1256.7817f, 240.63419f, (byte) 70);
				spawn(247304, 241.28506f, 1260.7113f, 240.63419f, (byte) 61);
                spawn(247304, 241.32637f, 1238.9640f, 240.63419f, (byte) 61);
			break;
			case 39:
			    spawn(247305, 246.26220f, 1244.2563f, 240.63419f, (byte) 60);
                spawn(247305, 246.33160f, 1254.7949f, 240.63419f, (byte) 62);
				spawn(247306, 241.41690f, 1249.6360f, 240.63419f, (byte) 59);
			break;
			case 40:
				spawn(247245, 241.05147f, 249.50418f, 971.14140f, (byte) 60);
			break;
		}
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
                        isSpawning = false;
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
                            bossTimerEnd = System.currentTimeMillis() - bossTimerStart;
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
    
    public void rewardForFloorId(Player player) {
        final TowerStageRewardTemplate reward = DataManager.TOWER_REWARD_DATA.getTowerReward(player.getFloor());
        int floor = player.getFloor();
        
        int itemId1 = reward.getItemId();
        int itemCount1 = reward.getItemCount();
        if (itemId1 != 0 && itemCount1 != 0) {
            ItemService.addItem(player, itemId1, itemCount1);
        }
        
        int itemId2 = reward.getItemId2();
        int itemCount2 = reward.getItemCount2();
        if (itemId2 != 0 && itemCount2 != 0) {
            ItemService.addItem(player, itemId2, itemCount2);
        }
        
        int kinahCount = reward.getKinahCount();
        if (kinahCount != 0) {
            ItemService.addItem(player, 182400001, kinahCount);
        }
        
        int expCount = reward.getExpCount();
        if (expCount != 0) {
            player.getCommonData().addExp(expCount, RewardType.QUEST);
        }
        
        int apCount = reward.getApCount();
        if (apCount != 0) {
            AbyssPointsService.addAp(player, apCount);
        }
        
        int gpCount = reward.getGpCount();
        if (gpCount != 0) {
            AbyssPointsService.addGp(player, gpCount);
        }
        
        if (floor % 5 == 0 && floor <= 40) {
            switch (floor) {
                case 5:
                    ItemService.addItem(player, 162002009, 1);
                    ItemService.addItem(player, 186000389, 5);
                    break;
                case 10:
                    ItemService.addItem(player, 162002010, 1);
                    ItemService.addItem(player, 186000389, 10);
                    break;
                case 15:
                    ItemService.addItem(player, 162002011, 1);
                    ItemService.addItem(player, 186000389, 15);
                    break;
                case 20:
                    ItemService.addItem(player, 162002012, 1);
                    ItemService.addItem(player, 186000389, 20);
                    break;
                case 25:
                    ItemService.addItem(player, 162002013, 1);
                    ItemService.addItem(player, 186000389, 25);
                    break;
                case 30:
                    ItemService.addItem(player, 162002014, 1);
                    ItemService.addItem(player, 186000389, 30);
                    break;
                case 35:
                    ItemService.addItem(player, 162002015, 1);
                    ItemService.addItem(player, 186000389, 35);
                    break;
                case 40:
                    ItemService.addItem(player, 162002016, 1);
                    ItemService.addItem(player, 186000389, 100);
                    ItemService.addItem(player, 188052207, 1);
                    break;
            }
        }
        
        PacketSendUtility.sendMessage(player, "You received a reward for completing floor " + floor + "!");
    }
    
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
    
    protected void sendMsgByRace(final int msg, final Race race, int time) {
        GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
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
        }, time);
    }
    
    private void deleteNpc(int npcId) {
        if (getNpc(npcId) != null) {
            getNpc(npcId).getController().onDelete();
        }
    }
    
    private void despawnNpc(Npc npc) {
        if (npc != null) {
            npc.getController().onDelete();
        }
    }
    
    protected void despawnNpcs(List<Npc> npcs) {
        if (npcs == null) return;
        for (Npc npc: npcs) {
            if (npc != null) {
                npc.getController().onDelete();
            }
        }
    }
    
    protected List<Npc> getNpcs(int npcId) {
        if (!isInstanceDestroyed && instance != null) {
            return instance.getNpcs(npcId);
        }
        return new ArrayList<>();
    }
    
    public void onFailCrucible(Player player) {
        TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
    }
    
    public void onExitInstance(Player player) {
        removeItems(player);
        TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
    }
    
    public void onPlayerLogOut(Player player) {
        removeItems(player);
        TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
    }
    
    @Override
    public void onInstanceDestroy() {
        isInstanceDestroyed = true;
        for (Future<?> task : crucibleTask) {
            if (task != null && !task.isDone()) {
                task.cancel(true);
            }
        }
        crucibleTask.clear();
        if (doors != null) {
            doors.clear();
        }
    }
    
    private void teleportFloor(float x, float y, float z, byte h) {
        for (Player playerInside: instance.getPlayersInside()) {
            if (playerInside.isOnline()) {
                teleportCrucibleFloor(playerInside);
            }
        }
    }
    
    protected void teleportFloor(Player player, float x, float y, float z, byte h) {
        TeleportService2.teleportTo(player, mapId, instanceId, x, y, z, h);
    }
    
    @Override
    public boolean onReviveEvent(Player player) {
        for (Npc npc: instance.getNpcs()) {
            npc.getController().onDelete();
        }
        spawnInggrilInggness2();
        player.getGameStats().updateStatsAndSpeedVisually();
        PlayerReviveService.revive(player, 100, 100, false, 0);
        PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
        PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_INFINITY_INDUN_RESURRECT, 0, 0));
        onFailCrucible(player);
        return true;
    }
}
