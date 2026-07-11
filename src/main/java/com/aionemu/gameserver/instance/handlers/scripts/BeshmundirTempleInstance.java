package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.*;
import java.util.concurrent.Future;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.commons.network.util.ThreadPoolManager;

import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 贝斯蒙迪尔神殿副本事件处理器。
 * Instance event handler for Beshmundir Temple.
 *
 * @author Encom
 */

@InstanceID(300170000)
public class BeshmundirTempleInstance extends GeneralInstanceHandler
{
	/** macunbello soul / macunbello soul */
		private int macunbelloSoul;
	/** warrior monument / warrior monument */
		private int warriorMonument;
	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;
	/** 已播放动画集合 / played-movie set */
	private List<Integer> movies = new ArrayList<Integer>();
	/** beshmundir 任务 / beshmundir task */
		private final List<Future<?>> beshmundirTask = new ArrayList<Future<?>>();
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 216161: //Vehala The Cursed.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 122001162, 1)); //Vehalla's Ring.
		    break;
			case 216163: //The Plaguebearer.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 120001039, 1)); //Plaguebearer's Earrings.
		    break;
			case 216168: //Flarestorm.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054283, 1)); //Blood Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188051387, 1)); //Flarestorm's Fabled Headgear Chest.
					} switch (Rnd.get(1, 2)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 123001032, 1)); //Flarestorm's Leather Belt.
				        break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 123001033, 1)); //Flarestorm's Sash.
				        break;
					} switch (Rnd.get(1, 4)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 100001081, 1)); //Flarestorm's Sword.
				        break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 100200954, 1)); //Flarestorm's Dagger.
				        break;
					    case 3:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 101800869, 1)); //Flarestorm's Pistol.
				        break;
						case 4:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 102100758, 1)); //Flarestorm's Cipher-Blade.
				        break;
					}
				}
			break;
			case 216248: //Taros Lifebane.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000092, 1)); //Temple Of Eternity Key.
		    break;
			case 216170: //Gatekeeper Darfall.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000093, 1)); //Meditation Chamber Key.
		    break;
			case 216171: //Gatekeeper Kutarrun.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000094, 1)); //Contemplation Chamber Key.
		    break;
			case 216172: //Gatekeeper Samarrn.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000095, 1)); //Supplication Chamber Key.
		    break;
			case 216173: //Gatekeeper Rhapsharr.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000096, 1)); //Petition Chamber Key.
		    break;
			case 216238: //Captain Lakhara.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054283, 1)); //Blood Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 122001163, 1)); //Lakhara's Ring.
					}
				}
			break;
			case 216239: //Ahbana The Wicked.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054283, 1)); //Blood Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188051390, 1)); //Ahbana's Eternal Shoes Chest.
					}
				}
			break;
			case 216241: //The Plaguebearer.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 120001040, 1)); //Manadar's Earrings.
		    break;
			case 216245: //Macunbello.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054283, 1)); //Blood Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188051391, 1)); //Macunbello's Eternal Gloves Chest.
					}
				}
			break;
			case 216246: //The Great Virhana.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054283, 1)); //Blood Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					} switch (Rnd.get(1, 4)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 100001006, 1)); //Virhana's Sword.
				        break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 100200900, 1)); //Virhana's Dagger.
				        break;
					    case 3:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 101800868, 1)); //Virhana's Pistol.
				        break;
						case 4:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 102100757, 1)); //Virhana's Cipher-Blade.
				        break;
					}
				}
			break;
			case 216250: //Dorakiki The Bold.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054283, 1)); //Blood Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188051392, 1)); //Dorakiki The Bold's Eternal Shoulder Armor Chest.
					}
				}
			break;
			case 216263: //Isbariya The Resolute.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054283, 1)); //Blood Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					} switch (Rnd.get(1, 2)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051388, 1)); //Isbariya's Fabled Jacket Chest.
				        break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051393, 1)); //Isbariya's Eternal Pants Chest.
				        break;
					}
				}
			break;
			case 216264: //Stormwing.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188054283, 1)); //Blood Mark Box.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 186000099, 1)); //Vorpal Essence.
					} switch (Rnd.get(1, 3)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051389, 1)); //Stormwing's Fabled Weapon Chest.
				        break;
						case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051394, 1)); //Stormwing's Eternal Jacket Chest.
				        break;
					    case 3:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188051395, 1)); //Stormwing's Eternal Weapon Chest.
				        break;
					} switch (Rnd.get(1, 2)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 170030043, 1)); //[Souvenir] Stormwing Wall-Mount Trophy.
				        break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 170490002, 1)); //[Souvenir] Stormwing Statue.
				        break;
					} switch (Rnd.get(1, 3)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190000079, 1)); //Golden Stormwing Egg.
				        break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190020133, 1)); //[Event] Stormwing Egg.
				        break;
					    case 3:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 190020138, 1)); //Stormwing Egg.
				        break;
					} switch (Rnd.get(1, 4)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188100013, 1)); //[Souvenir] Stormwing's Scroll Piece.
				        break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188100034, 1)); //[Souvenir] Stormwing's Head.
				        break;
					    case 3:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188100035, 1)); //[Souvenir] Stormwing's Skeleton.
				        break;
						case 4:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188100036, 1)); //[Souvenir] Stormwing's Wing.
				        break;
					}
				}
			break;
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
        doors = instance.getDoors();
		Npc npc = instance.getNpc(216245); //Macunbello.
		if (npc != null) {
			npc.getEffectController().unsetAbnormal(AbnormalState.SLEEP.getId());
			GameEngineServices.skillEngine().getSkill(npc, 19046, 60, npc).useNoAnimationSkill(); //Soul Starved I.
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
			case 730274: // Altar Of Soul Invocation.
				if (instance.getNpc(799506) == null
						&& (canSummonRespondent(player, 30208, 182209610)
								|| canSummonRespondent(player, 30308, 182209710))) {
					spawn(799506, 1360, 390, 250, (byte) 183);
				}
				break;
			case 730290: //Entrance Of Blue Flame Incinerator.
				if (player.getInventory().decreaseByItemId(185000091, 1)) { //Incinerator Key.
					despawnNpc(npc);
			    } else {
					// 需要钥匙。 / Key required.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403686));
				}
            break;
        }
    }

	private static boolean canSummonRespondent(Player player, int questId, int itemId) {
		return canSummonRespondent(player.getQuestStateList().getQuestState(questId),
				player.getInventory().getItemCountByItemId(itemId));
	}

	static boolean canSummonRespondent(QuestState questState, long itemCount) {
		return questState != null && questState.getStatus() == QuestStatus.START && itemCount > 0;
	}
	
	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(185000091, storage.getItemCountByItemId(185000091)); //Incinerator Key.
		storage.decreaseByItemId(185000092, storage.getItemCountByItemId(185000092)); //Temple Of Eternity Key.
		storage.decreaseByItemId(185000093, storage.getItemCountByItemId(185000093)); //Meditation Chamber Key.
		storage.decreaseByItemId(185000094, storage.getItemCountByItemId(185000094)); //Contemplation Chamber Key.
		storage.decreaseByItemId(185000095, storage.getItemCountByItemId(185000095)); //Supplication Chamber Key.
		storage.decreaseByItemId(185000096, storage.getItemCountByItemId(185000096)); //Petition Chamber Key.
	}
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
    public void onDie(Npc npc) {
        Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
		   /**
	 * Path To Watcher's Nexus
	 */
			case 216238: //Captain Lakhara.
			    doors.get(470).setOpen(true);
				//某处沉重的门已打开。 / A heavy door has opened somewhere.
				sendMsgByRace(1401839, Race.PC_ALL, 0);
            break;
			case 216739: //Warrior Monument.
                warriorMonument++;
				if (warriorMonument == 15) {
                	// 邪恶的阿巴纳已出现在看守者之枢纽。 / Ahbana the Wicked has appeared in the Watcher's Nexus.
					sendMsgByRace(1400470, Race.PC_ALL, 5000);
					sp(216239, 1356.9945f, 149.51117f, 246.27036f, (byte) 29, 5000, 0, null); //Ahbana The Wicked.
                }
				despawnNpc(npc);
				// 战士纪念碑已被摧毁。邪恶的阿巴纳进入警戒。 / The Warrior Monument has been destroyed. Ahbana the Wicked is on alert.
				sendMsgByRace(1400465, Race.PC_ALL, 0);
            break;
			case 216239: //Ahbana The Wicked.
			    doors.get(471).setOpen(true);
				//某处沉重的门已打开。 / A heavy door has opened somewhere.
				sendMsgByRace(1401839, Race.PC_ALL, 0);
            break;
			
		   /**
	 * Path To Macunbello's Refuge
	 */
			case 216583: //Brutal Soulwatcher (1st Island)
				sp(799518, 933.982971f, 444.269104f, 222.00f, (byte) 21, 3000, 0, null); //Plegeton Boatman II.
			break;
			case 216584: //Brutal Soulwatcher (2nd Island)
				sp(799519, 788.744690f, 442.353271f, 222.00f, (byte) 0, 3000, 0, null); //Plegeton Boatman III.
			break;
			case 216585: //Brutal Soulwatcher (3th Island)
				sp(799520, 818.578740f, 277.745270f, 220.19f, (byte) 53, 3000, 0, null); //Plegeton Boatman IV.
			break;
			case 216206: //Elyos Spiritblade.
			case 216207: //Elyos Spiritmage.
			case 216208: //Elyos Spiritbow.
			case 216209: //Elyos Spiritsalve.
			case 216210: //Asmodian Soulsword.
			case 216211: //Asmodian Soulspell.
			case 216212: //Asmodian Soulranger.
			case 216213: //Asmodian Soulmedic.
			    Npc macunbello = instance.getNpc(216245); //Macunbello.
			    macunbelloSoul++;
				if (macunbello != null) {
				    if (macunbelloSoul == 7) {
					    // 马昆贝洛的力量正在减弱。 / Macunbello's power is weakening.
					    sendMsgByRace(1400466, Race.PC_ALL, 2000);
						macunbello.getEffectController().removeEffect(19046); //Soul Starved I.
						GameEngineServices.skillEngine().applyEffectDirectly(19047, macunbello, macunbello, 0); //Soul Starved II.
				    } else if (macunbelloSoul == 14) {
					    // 马昆贝洛的力量已减弱。 / Macunbello's power has weakened.
					    sendMsgByRace(1400467, Race.PC_ALL, 2000);
						macunbello.getEffectController().removeEffect(19047); //Soul Starved II.
						GameEngineServices.skillEngine().applyEffectDirectly(19048, macunbello, macunbello, 0); //Soul Starved III.
				    } else if (macunbelloSoul == 21) {
					    // 马昆贝洛已被削弱。 / Macunbello has been crippled.
					    sendMsgByRace(1400468, Race.PC_ALL, 2000);
						macunbello.getEffectController().removeEffect(19048); //Soul Starved III.
				    }
				}
			break;
			case 216586: //Temadaro.
			    sendMovie(player, 445);
				doors.get(467).setOpen(true);
				//某处沉重的门已打开。 / A heavy door has opened somewhere.
				sendMsgByRace(1401839, Race.PC_ALL, 0);
			    PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 0));
			break;
			
		   /**
	 * Path To Garden Of The Entombed
	 */
			case 216246: //The Great Virhana.
				doors.get(473).setOpen(true);
				//某处沉重的门已打开。 / A heavy door has opened somewhere.
				sendMsgByRace(1401839, Race.PC_ALL, 0);
			break;
			case 216250: //Dorakiki The Bold.
				deleteNpc(281647); //Fixit.
				deleteNpc(281648); //Sorcerer Haskin.
				deleteNpc(281649); //Chopper.
			break;
			
		   /**
	 * Path To The Prison Of Ice
	 */
			case 216263: //Isbariya The Resolute.
				sendMovie(player, 439);
				// 封印守护者已倒下。裂隙宝珠发光，封印削弱。 / The Seal Protector has fallen. The Rift Orb shines while the seal weakens.
				sendMsgByRace(1400480, Race.PC_ALL, 0);
				deleteNpc(281645); //Sacrificial Soul.
				sp(216264, 556.59375f, 1367.2274f, 224.79459f, (byte) 75, 3000, 0, null); //Stormwing.
				sp(730275, 1611.1663f, 1604.7267f, 311.04984f, (byte) 0, 426, 3000, 0, null); //Rift Orb.
			break;
			case 216264: //Stormwing.
				deleteNpc(281794);
				deleteNpc(281795);
				deleteNpc(281796);
				deleteNpc(281797);
				deleteNpc(281798);
				sp(730287, 565.25275f, 1376.6252f, 224.79459f, (byte) 74, 3000, 0, null); //Rift Orb.
			break;
		}
    }
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	
	private void sendMovie(Player player, int movie) {
        if (!movies.contains(movie)) {
             movies.add(movie);
             PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movie));
        }
    }
	
	private void stopInstanceTask() {
        for (Future<?> task : beshmundirTask) {
			if (task != null) {
				task.cancel(true);
			}
        }
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * time
	 */
	
	protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time) {
        sp(npcId, x, y, z, h, 0, time, 0, null);
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * time
	 * message
	 * 阵营 / race
	 */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final int msg, final Race race) {
        sp(npcId, x, y, z, h, 0, time, msg, race);
    }
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * entity id
	 * time
	 * message
	 * 阵营 / race
	 */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int entityId, final int time, final int msg, final Race race) {
        beshmundirTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
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
	/**
	 * 处理 sp。
	 * Handle sp.
	 *
	 * NPC
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / h
	 * time
	 * walkerId
	 */
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        beshmundirTask.add(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            /**
             * 处理 run。
             * Handle run.
             */
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
        isInstanceDestroyed = true;
		movies.clear();
		doors.clear();
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
	}
}
