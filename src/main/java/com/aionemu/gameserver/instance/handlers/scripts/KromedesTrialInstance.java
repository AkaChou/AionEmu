package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameStaticDataServices;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

import java.util.ArrayList;
import java.util.List;

@InstanceID(300230000)
public class KromedesTrialInstance extends GeneralInstanceHandler
{
	/** 已播放动画集合 / played-movie set */
	private List<Integer> movies = new ArrayList<Integer>();
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(Player player) {
		final int transformation = transformationFor(player.getRace());
		GameEngineServices.skillEngine().applyEffectDirectly(transformation, player, player, 3600000 * 1);
		sendMovie(player, 453);
		HTMLService.showHTML(player, GameStaticDataServices.htmlCache().getHTML("instances/kromedeTrial.xhtml"));
	}

	static int transformationFor(Race race) {
		return race == Race.ASMODIANS ? 19270 : 19220;
	}
	
	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * 玩家 / player
	 * npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 282093: //Mana Relic.
				GameEngineServices.skillEngine().getSkill(npc, 19248, 1, player).useNoAnimationSkill(); //Mana Relic Effect.
			break;
			case 282095: //Strength Relic.
			    GameEngineServices.skillEngine().getSkill(npc, 19247, 1, player).useNoAnimationSkill(); //Strength Relic Effect.
			break;
		}
	}
	
	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
		removeEffects(player);
	}
	
	/**
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
		removeEffects(player);
	}
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
    public void onDie(Npc npc) {
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 700835: //Sealed Stone Door.
			    despawnNpc(npc);
			break;
        }
    }

	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		// 克罗梅德变身。 / Kromede Transformation.
		effectController.removeEffect(19220);
		effectController.removeEffect(19270);
	}
	/**
	 * 移除相关物品。
	 * Remove related items.
	 *
	 * @param player 玩家 / player
	 */
	
	public void removeItems(Player player) {
        Storage storage = player.getInventory();
        storage.decreaseByItemId(185000101, storage.getItemCountByItemId(185000101)); //Secret Safe Key.
		storage.decreaseByItemId(185000102, storage.getItemCountByItemId(185000102)); //Kaliga's Key.
        storage.decreaseByItemId(185000109, storage.getItemCountByItemId(185000109)); //Relic Key.
		storage.decreaseByItemId(164000140, storage.getItemCountByItemId(164000140)); //Explosive Bead.
		storage.decreaseByItemId(164000141, storage.getItemCountByItemId(164000141)); //Silver Blade Rotan.
        storage.decreaseByItemId(164000142, storage.getItemCountByItemId(164000142)); //Sapping Pollen.
		storage.decreaseByItemId(164000143, storage.getItemCountByItemId(164000143)); //Maga's Potion.
    }
	/**
	 * 玩家进入区域时处理。
	 * Handle a player entering a zone.
	 *
	 * 玩家 / player
	 * zone
	 */
	@Override
	    public void onEnterZone(Player player, ZoneInstance zone) {
	        if (zone.getAreaTemplate().getZoneName() == ZoneName.get("MANOR_ENTRANCE_300230000")) {
	            sendMovie(player, 462);
				// 附近有强大物品。 / There is an object of great power nearby.
				sendMsg(1400653);
	        }
	    }
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
    public void onInstanceDestroy() {
		movies.clear();
    }
	
	private void sendMovie(Player player, int movie) {
		if (!movies.contains(movie)) {
			movies.add(movie);
			PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
		}
	}
	
}
