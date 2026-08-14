package com.aionemu.gameserver.ai.worlds.inggison;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.List;

/**
 * Inggison 区域 NPC AI：Boss Windstream（@AIName "boss_windstream"），继承 AggressiveNpcAI2。
 * Inggison zone NPC AI: Boss Windstream (@AIName "boss_windstream"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("boss_windstream")
public class Boss_WindstreamAI2 extends AggressiveNpcAI2
{
	private Map<Integer, VisibleObject> objects = new LinkedHashMap<Integer, VisibleObject>();
	
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
		/**
		 * 英吉斯温风道 / WINDSTREAM INGGISON
		 */
			case 215584: // 泰坦 Starturtle / Titan Starturtle.
				announceWindBox();
				SpawnTemplate CastShadowPLSM = SpawnEngine.addNewSingleTimeSpawn(getOwner().getWorldId(), 281817, 338.26440f, 573.72168f, 458.27939f, (byte) 0);
				CastShadowPLSM.setEntityId(755);
				objects.put(281817, SpawnEngine.spawnObject(CastShadowPLSM, 1));
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    @Override
					public void run() {
					    despawnNpc(281817);
					}
				}, 300000); //5 分钟。 / 5 Minutes.
			break;
			case 216849: // 守望者 Garma / Watcher Garma.
				announceWindBox();
				SpawnTemplate EnvWeatherShow = SpawnEngine.addNewSingleTimeSpawn(getOwner().getWorldId(), 281817, 2602.6992f, 1526.0367f, 258.13651f, (byte) 0);
				EnvWeatherShow.setEntityId(754);
				objects.put(281817, SpawnEngine.spawnObject(EnvWeatherShow, 1));
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    @Override
					public void run() {
					    despawnNpc(281817);
					}
				}, 300000); //5 分钟。 / 5 Minutes.
			break;
			case 216848: //Illanthe Hundredyears / Illanthe Hundredyears.
				announceWindBox();
				SpawnTemplate SkipOnLowSpec = SpawnEngine.addNewSingleTimeSpawn(getOwner().getWorldId(), 281817, 1745.8660f, 1716.3790f, 226.37808f, (byte) 0);
				SkipOnLowSpec.setEntityId(1039);
				objects.put(281817, SpawnEngine.spawnObject(SkipOnLowSpec, 1));
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    @Override
					public void run() {
					    despawnNpc(281817);
					}
				}, 300000); //5 分钟。 / 5 Minutes.
			break;
			case 217071: // Esalki 第四 / Esalki The Fourth.
				announceWindBox();
				SpawnTemplate EnvWeatherHide = SpawnEngine.addNewSingleTimeSpawn(getOwner().getWorldId(), 281817, 2288.1091f, 1067.0475f, 285.73407f, (byte) 0);
				EnvWeatherHide.setEntityId(2311);
				objects.put(281817, SpawnEngine.spawnObject(EnvWeatherHide, 1));
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    @Override
					public void run() {
					    despawnNpc(281817);
					}
				}, 300000); //5 分钟。 / 5 Minutes.
			break;
			case 217072: // 巨型瀑布 Starturtle / Huge Waterfall Starturtle.
				announceWindBox();
				SpawnTemplate DisplayFilled = SpawnEngine.addNewSingleTimeSpawn(getOwner().getWorldId(), 281817, 1660.0439f, 928.57129f, 404.99213f, (byte) 0);
				DisplayFilled.setEntityId(2292);
				objects.put(281817, SpawnEngine.spawnObject(DisplayFilled, 1));
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    @Override
					public void run() {
					    despawnNpc(281817);
					}
				}, 300000); //5 分钟。 / 5 Minutes.
			break;
		/**
		 * 格尔克马洛斯风道 / WINDSTREAM GELKMAROS
		 */
			case 216846: //Agrima / Agrima.
				announceWindBox();
				SpawnTemplate FileLadderCGF = SpawnEngine.addNewSingleTimeSpawn(getOwner().getWorldId(), 281817, 1719.2194f, 2301.7344f, 318.70938f, (byte) 0);
				FileLadderCGF.setEntityId(1821);
				objects.put(281817, SpawnEngine.spawnObject(FileLadderCGF, 1));
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				    @Override
					public void run() {
					    despawnNpc(281817);
					}
				}, 300000); //5 分钟。 / 5 Minutes.
			break;
		}
		super.handleDied();
		AI2Actions.scheduleRespawn(this);
	}
	
	private void announceWindBox() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_JUMP_TRIGGER_ON_INFO);
			}
		});
	}
	
	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
}
