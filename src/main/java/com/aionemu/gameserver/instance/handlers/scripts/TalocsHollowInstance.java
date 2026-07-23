package com.aionemu.gameserver.instance.handlers.scripts;

import java.util.HashSet;
import java.util.Set;

import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.summons.UnsummonType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.services.summons.SummonsService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

@InstanceID(300190000)
public class TalocsHollowInstance extends GeneralInstanceHandler {

	private final Set<Integer> movies = new HashSet<>();

	@Override
	public void onEnterInstance(Player player) {
		switch (player.getRace()) {
			case ELYOS -> sendMovie(player, 434);
			case ASMODIANS -> sendMovie(player, 438);
		}
	}

	@Override
	public void onEnterZone(Player player, ZoneInstance zone) {
		if (zone.getAreaTemplate().getZoneName() == ZoneName.get("KINQUIDS_DEN_300190000")) {
			sendMovie(player, 463);
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("MOSQUAS_NEST_300190000")) {
			sendMovie(player, 464);
		}
	}

	@Override
	public void onPlayerLogOut(Player player) {
		cleanupEffects(player);
	}

	@Override
	public void onLeaveInstance(Player player) {
		cleanupPlayer(player);
	}

	private void sendMovie(Player player, int movieId) {
		if (movies.add(movieId)) {
			PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movieId));
		}
	}

	private void cleanupPlayer(Player player) {
		Storage storage = player.getInventory();
		for (int itemId : new int[] { 164000137, 164000138, 164000139 }) {
			storage.decreaseByItemId(itemId, storage.getItemCountByItemId(itemId));
		}
		cleanupEffects(player);
	}

	private void cleanupEffects(Player player) {
		PlayerEffectController effects = player.getEffectController();
		effects.removeEffect(10251);
		effects.removeEffect(10252);
		if (player.getSummon() != null) {
			SummonsService.release(player.getSummon(), UnsummonType.UNSPECIFIED, false);
		}
	}
}
