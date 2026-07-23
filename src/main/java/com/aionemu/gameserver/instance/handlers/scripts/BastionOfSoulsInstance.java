package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.flyring.FlyRing;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.flyring.FlyRingTemplate;
import com.aionemu.gameserver.model.utils3d.Point3D;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(302340000)
public class BastionOfSoulsInstance extends GeneralInstanceHandler {

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		new FlyRing(new FlyRingTemplate("BASTION_OF_SOULS", mapId,
			new Point3D(1169.6204, 1153.8145, 491.13086),
			new Point3D(1164.8580, 1152.0957, 497.11038),
			new Point3D(1159.7225, 1153.7646, 491.1022), 90), instanceId).spawn();
	}

	@Override
	public boolean onPassFlyingRing(Player player, String flyingRing) {
		if (!"BASTION_OF_SOULS".equals(flyingRing)) {
			return false;
		}
		RetailConditionSpawnEngine.setVariable(instance, "statdown", 1, 0);
		TeleportService2.teleportTo(player, mapId, instanceId, 1183.3602f, 734.0874f, 433.22742f, (byte) 90);
		PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, 957));
		return false;
	}

	@Override
	public void onPlayerLogOut(Player player) {
		cleanup(player);
	}

	@Override
	public void onLeaveInstance(Player player) {
		cleanup(player);
	}

	private void cleanup(Player player) {
		player.getEffectController().removeEffect(17649);
		player.getEffectController().removeEffect(17672);
	}
}
