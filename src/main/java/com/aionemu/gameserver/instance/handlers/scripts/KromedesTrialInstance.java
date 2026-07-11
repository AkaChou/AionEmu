package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameStaticDataServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 克罗米德斯试炼副本事件处理器。
 * Instance event handler for Kromedes Trial.
 *
 * @author Encom
 */

@InstanceID(300230000)
public class KromedesTrialInstance extends GeneralInstanceHandler
{
	/** 技能种族 / skill race */
		private Race skillRace;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
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
		final int transformation = skillRace == Race.ASMODIANS ? 19270 : 19220;
		GameEngineServices.skillEngine().applyEffectDirectly(transformation, player, player, 3600000 * 1);
		sendMovie(player, 453);
		HTMLService.showHTML(player, GameStaticDataServices.htmlCache().getHTML("instances/kromedeTrial.xhtml"));
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
		doors = instance.getDoors();
        switch (Rnd.get(1, 2)) {
		    case 1:
				spawn(217005, 670.7984f, 774.2175f, 216.88036f, (byte) 59); //Shadow Judge Kaliga.
			break;
			case 2:
				spawn(217006, 670.7984f, 774.2175f, 216.88036f, (byte) 59); //Kaliga The Unjust.
			break;
        }
    }
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		switch (npcId) {
			case 216967: //Petrahulk Gatekeeper.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000098, 1)); //Temple Vault Door Key.
			break;	
			case 216968: //Divine Hisen.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000109, 1)); //Relic Key.
			break;
			case 216980: //Warden Baal.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000099, 1)); //Dungeon Grate Key.
			break;
			case 216981: //Manor Guard Captain.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000100, 1)); //Dungeon Door Key.
			break;
			case 216999: //Jesse.
			    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000101, 1)); //Secret Safe Key.
			break;
			case 217005: //Shadow Judge Kaliga.
			case 217006: //Kaliga The Unjust.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188052826, 1)); //Judge's Fabled Weapon Chest.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000102, 1)); //Kaliga's Key.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053787, 1)); //烙印之石支援包。 / Stigma Support Bundle.
				switch (Rnd.get(1, 5)) {
					case 1:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080005, 2)); //低级随从契约。 / Lesser Minion Contract.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080006, 2)); //高级随从契约。 / Greater Minion Contract.
					break;
					case 3:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080007, 2)); //大型随从契约。 / Major Minion Contract.
					break;
					case 4:
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190080008, 2)); //可爱随从契约。 / Cute Minion Contract.
					break;
					case 5:
					    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190200000, 50)); //Minium.
					break;
				}
			break;
			case 211861: //Ancient Treasure Box.
				switch (Rnd.get(1, 13)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 110601097, 1)); //Corrupt Judge's Breastplate.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 111601074, 1)); //Corrupt Judge's Gauntlets.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 113601058, 1)); //Corrupt Judge's Greaves.
					break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 125002432, 1)); //Corrupt Judge's Helm.
					break;
					case 5:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 114601054, 1)); //Corrupt Judge's Sabatons.
					break;
					case 6:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 112601049, 1)); //Corrupt Judge's Shoulderplates.
					break;
					case 7:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 123001099, 1)); //Corrupt Judge's Belt.
					break;
					case 8:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 120001391, 1)); //Corrupt Judge's Topaz Earrings.
					break;
					case 9:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 120001392, 1)); //Corrupt Judge's Aquamarine Earrings.
					break;
					case 10:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 121001274, 1)); //Corrupt Judge's Topaz Necklace.
					break;
					case 11:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 121001275, 1)); //Corrupt Judge's Aquamarine Necklace.
					break;
					case 12:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 122001543, 1)); //Corrupt Judge's Topaz Ring.
					break;
					case 13:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 122001544, 1)); //Corrupt Judge's Aquamarine Ring.
					break;
				}
			break;
			case 212333: //Ancient Treasure Box.
				switch (Rnd.get(1, 13)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 125002429, 1)); //Corrupt Judge's Bandana.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 111101099, 1)); //Corrupt Judge's Gloves.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 113101113, 1)); //Corrupt Judge's Leggings.
					break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 112101059, 1)); //Corrupt Judge's Pauldrons.
					break;
					case 5:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 114101140, 1)); //Corrupt Judge's Shoes.
					break;
					case 6:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 110101209, 1)); //Corrupt Judge's Tunic.
					break;
					case 7:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 123001099, 1)); //Corrupt Judge's Belt.
					break;
					case 8:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 120001391, 1)); //Corrupt Judge's Topaz Earrings.
					break;
					case 9:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 120001392, 1)); //Corrupt Judge's Aquamarine Earrings.
					break;
					case 10:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 121001274, 1)); //Corrupt Judge's Topaz Necklace.
					break;
					case 11:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 121001275, 1)); //Corrupt Judge's Aquamarine Necklace.
					break;
					case 12:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 122001543, 1)); //Corrupt Judge's Topaz Ring.
					break;
					case 13:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 122001544, 1)); //Corrupt Judge's Aquamarine Ring.
					break;
				}
			break;
			case 212335: //Ancient Treasure Box.
				switch (Rnd.get(1, 20)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 114501100, 1)); //Corrupt Judge's Brogans.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 114501598, 1)); //Corrupt Judge's Magic Brogans.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 125002431, 1)); //Corrupt Judge's Chain Hood.
					break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 125003841, 1)); //Corrupt Judge's Magic Helm.
					break;
					case 5:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 113501093, 1)); //Corrupt Judge's Chausses.
					break;
					case 6:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 113501589, 1)); //Corrupt Judge's Magic Chausses.
					break;
					case 7:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 111501084, 1)); //Corrupt Judge's Handguards.
					break;
					case 8:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 111501571, 1)); //Corrupt Judge's Magic Handguards.
					break;
					case 9:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 110501115, 1)); //Corrupt Judge's Hauberk.
					break;
					case 10:
					    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 110551013, 1)); //Corrupt Judge's Magic Hauberk.
					break;
					case 11:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 112501035, 1)); //Corrupt Judge's Spaulders.
					break;
					case 12:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 112501513, 1)); //Corrupt Judge's Magic Spaulders.
					break;
					case 13:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 123001099, 1)); //Corrupt Judge's Belt.
					break;
					case 14:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 123001334, 1)); //Corrupt Judge's Belt.
					break;
					case 15:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 120001391, 1)); //Corrupt Judge's Topaz Earrings.
					break;
					case 16:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 120001392, 1)); //Corrupt Judge's Aquamarine Earrings.
					break;
					case 17:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 121001274, 1)); //Corrupt Judge's Topaz Necklace.
					break;
					case 18:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 121001275, 1)); //Corrupt Judge's Aquamarine Necklace.
					break;
					case 19:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 122001543, 1)); //Corrupt Judge's Topaz Ring.
					break;
					case 20:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 122001544, 1)); //Corrupt Judge's Aquamarine Ring.
					break;
				}
			break;
			case 212338: //Ancient Treasure Box.
				switch (Rnd.get(1, 20)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 114301150, 1)); //Corrupt Judge's Boots.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 114301595, 1)); //Corrupt Judge's Magic Boots.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 113301116, 1)); //Corrupt Judge's Breeches.
					break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 113301557, 1)); //Corrupt Judge's Magic Breeches.
					break;
					case 5:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 125002430, 1)); //Corrupt Judge's Hat.
					break;
					case 6:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 125003573, 1)); //Corrupt Judge's Hat.
					break;
					case 7:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 110301144, 1)); //Corrupt Judge's Jerkin.
					break;
					case 8:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 110301591, 1)); //Corrupt Judge's Magic Jerkin.
					break;
					case 9:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 112301045, 1)); //Corrupt Judge's Shoulderguards.
					break;
					case 10:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 112301470, 1)); //Corrupt Judge's Magic Shoulderguards.
					break;
					case 11:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 111301098, 1)); //Corrupt Judge's Vambrace.
					break;
					case 12:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 111301529, 1)); //Corrupt Judge's Magic Vambrace.
					break;
					case 13:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 123001099, 1)); //Corrupt Judge's Belt.
					break;
					case 14:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 123001098, 1)); //Corrupt Judge's Leather Belt.
					break;
					case 15:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 120001391, 1)); //Corrupt Judge's Topaz Earrings.
					break;
					case 16:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 120001392, 1)); //Corrupt Judge's Aquamarine Earrings.
					break;
					case 17:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 121001274, 1)); //Corrupt Judge's Topaz Necklace.
					break;
					case 18:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 121001275, 1)); //Corrupt Judge's Aquamarine Necklace.
					break;
					case 19:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 122001543, 1)); //Corrupt Judge's Topaz Ring.
					break;
					case 20:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 122001544, 1)); //Corrupt Judge's Aquamarine Ring.
					break;
				}
			break;
		}
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
		Player player = getDeathRewardPlayer(npc);
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 700835: //Sealed Stone Door.
			    despawnNpc(npc);
			break;
			case 216981: //Manor Guard Captain.
				spawnClassTreasure(player, 740.83966f, 535.38837f, 199.12067f, (byte) 89);
            break;
			case 216982: //Hamam The Torturer.
				//sendMsg("<Wounded Hamam> appear");
				spawn(217004, 651.186f, 767.856f, 215.584f, (byte) 59); //Wounded Hamam.
				spawnClassTreasure(player, 757.48157f, 617.7071f, 197.17694f, (byte) 108);
            break;
			case 216999: //Jesse.
				announceKaligaTreasury();
				if (player != null) {
					removeSilverBladeRotan(player);
				}
				spawnClassTreasure(player, 581.11005f, 775.1529f, 215.53482f, (byte) 112);
            break;
			case 217000: //Lady Angerr.
			    //sendMsg("<Distraught Lady Angerr> appear");
				spawn(217001, 650.679f, 774.197f, 215.584f, (byte) 60); //Distraught Lady Angerr.
				spawnClassTreasure(player, 512.89886f, 570.039f, 216.89487f, (byte) 31);
            break;
			case 217002: //Justicetaker Wyr.
			    //sendMsg("<Injured Justicetaker Wyr> appear");
				spawn(217003, 651.341f, 780.757f, 215.584f, (byte) 59); //Injured Justicetaker Wyr.
            break;
			case 217005: //Shadow Judge Kaliga.
			case 217006: //Kaliga The Unjust.
				if (player != null) {
					sendMovie(player, 455);
					// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Kromede's Trial>");
					ItemService.addItem(player, 188900010, 1); //Secret Remedy Of Growth IV.
				}
            break;
        }
    }

	private Player getDeathRewardPlayer(Npc npc) {
		Player player = npc.getAggroList().getMostPlayerDamage();
		if (player != null || instance == null) {
			return player;
		}
		List<Player> players = instance.getPlayersInside();
		return players.size() == 1 ? players.get(0) : null;
	}

	private void spawnClassTreasure(Player player, float x, float y, float z, byte heading) {
		if (player == null) {
			return;
		}
		switch (player.getPlayerClass()) {
			case TEMPLAR:
			case GLADIATOR:
				spawn(211861, x, y, z, heading); //Treasure Box Kromede Trial (Gladiator-Templar).
			break;
			case RANGER:
			case ASSASSIN:
			case GUNSLINGER:
				spawn(212338, x, y, z, heading); //Treasure Box Kromede Trial (Assassin-Ranger-Gunslinger).
			break;
			case SORCERER:
			case SONGWEAVER:
			case SPIRIT_MASTER:
				spawn(212333, x, y, z, heading); //Treasure Box Kromede Trial (Sorcerer-SpiritMaster-Songweaver).
			break;
			case CLERIC:
			case CHANTER:
			case AETHERTECH:
				spawn(212335, x, y, z, heading); //Treasure Box Kromede Trial (Cleric-Chanter-Aethertech).
			break;
		}
	}
	
	private void announceKaligaTreasury() {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 通往卡利加宝库的门应该就在附近…… / The door to the Kaliga Treasury should be around here somewhere....
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(false, 1111370, player.getObjectId(), 2));
				}
			}
		});
	}
	
	private void removeEffects(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		// 克罗梅德变身。 / Kromede Transformation.
		effectController.removeEffect(19220);
		effectController.removeEffect(19270);
		effectController.removeEffect(19288); //Rage Of Kromede.
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
	 * 处理 removeSilverBladeRotan。
	 * Handle removeSilverBladeRotan.
	 *
	 * @param player 玩家 / player
	 */
	
	public void removeSilverBladeRotan(Player player) {
        Storage storage = player.getInventory();
		storage.decreaseByItemId(164000141, storage.getItemCountByItemId(164000141)); //Silver Blade Rotan.
    }
	
	private void sendMsg(final String str) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendWhiteMessageOnCenter(player, str);
			}
		});
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
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
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
				});
			}
		}, time);
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
			sendMsgByRace(1400653, Race.PC_ALL, 0);
		} else if (zone.getAreaTemplate().getZoneName() == ZoneName.get("KALIGA_DUNGEONS_300230000")) {
			sendMovie(player, 454);
			rageOfKromede();
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
        doors.clear();
		movies.clear();
    }
	
	private void sendMovie(Player player, int movie) {
		if (!movies.contains(movie)) {
			movies.add(movie);
			PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
		}
	}
	
	private void rageOfKromede() {
		for (Player p: instance.getPlayersInside()) {
			SkillTemplate st =  DataManager.SKILL_DATA.getSkillTemplate(19288); //Rage Of Kromede.
			Effect e = new Effect(p, p, st, 1, st.getEffectsDuration(9));
			e.initialize();
			e.applyEffect();
		}
	}
}
