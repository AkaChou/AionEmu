package com.aionemu.gameserver.instance.handlers.scripts.ophidanBridge;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Map;
import java.util.Set;

/**
 * 奥菲丹桥副本事件处理器。
 * Instance event handler for Ophidan Bridge.
 *
 * @author Encom
 */

@InstanceID(300590000)
public class OphidanBridgeInstance extends GeneralInstanceHandler
{
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		switch (Rnd.get(1, 3)) {
			case 1:
				spawn(235780, 751.4241f, 527.29016f, 576.37476f, (byte) 33); // 逃犯 Mazikin / Fugitive Mazikin.
			break;
			case 2:
				spawn(235781, 751.4241f, 527.29016f, 576.37476f, (byte) 33); // 逃亡者 Hirakiki / Runaway Hirakiki.
			break;
			case 3:
				spawn(235782, 751.4241f, 527.29016f, 576.37476f, (byte) 33); // 越狱者 Asachin / Escapee Asachin.
			break;
		} switch (Rnd.get(1, 4)) {
			case 1:
				spawn(235768, 318.23724f, 488.92276f, 607.64343f, (byte) 1); // 勇猛的 Velkur / Spirited Velkur.
			break;
			case 2:
				spawn(235769, 318.23724f, 488.92276f, 607.64343f, (byte) 1); // Velkur 奥德施法者 / Velkur Aethercaster.
			break;
			case 3:
				spawn(235770, 318.23724f, 488.92276f, 607.64343f, (byte) 1); // Velkur 奥德祭司 / Velkur Aetherpriest.
			break;
			case 4:
				spawn(235771, 318.23724f, 488.92276f, 607.64343f, (byte) 1); // Velkur 奥德刺客 / Velkur Aetherknife.
			break;
		} switch (Rnd.get(1, 3)) {
			case 1:
				spawn(235721, 673.0f, 472.0f, 599.3125f, (byte) 0); // 据点防御 Drakenclaw / Post Defense Drakenclaw.
			break;
			case 2:
				spawn(235726, 673.0f, 472.0f, 599.3125f, (byte) 0); // 防御部队 Spelltongue / Defense Spelltongue.
			break;
			case 3:
				spawn(235727, 673.0f, 472.0f, 599.3125f, (byte) 0); // 防御部队 Swiftrunner / Defense Swiftrunner.
			break;
		} switch (Rnd.get(1, 3)) {
			case 1:
				spawn(235728, 531.0988f, 437.3993f, 620.25f, (byte) 109); // 北线防御 Drakenclaw / North Defense Drakenclaw.
			break;
			case 2:
				spawn(235730, 531.0988f, 437.3993f, 620.25f, (byte) 109); // 北线防御 Ironscale / North Defense Ironscale.
			break;
			case 3:
				spawn(235731, 531.0988f, 437.3993f, 620.25f, (byte) 109); // 北线防御 Hidestitcher / North Defense Hidestitcher.
			break;
		} switch (Rnd.get(1, 5)) {
			case 1:
				spawn(235735, 608.1635f, 558.9905f, 590.57214f, (byte) 110); // 南线防御 Drakenclaw / South Defense Drakenclaw.
			break;
			case 2:
				spawn(235736, 608.1635f, 558.9905f, 590.57214f, (byte) 110); // 南线防御 Bard / South Defense Bard.
			break;
			case 3:
				spawn(235737, 608.1635f, 558.9905f, 590.57214f, (byte) 110); // 南线防御 Ironscale / South Defense Ironscale.
			break;
			case 4:
				spawn(235738, 608.1635f, 558.9905f, 590.57214f, (byte) 110); // 南线防御 Hidestitcher / South Defense Hidestitcher.
			break;
			case 5:
				spawn(235740, 608.1635f, 558.9905f, 590.57214f, (byte) 110); // 南线防御 Spelltongue / South Defense Spelltongue.
			break;
		} switch (Rnd.get(1, 6)) {
			case 1:
				spawn(235742, 480.99368f, 524.84326f, 597.43713f, (byte) 10); // 据点防御 Drakenclaw / Post Defense Drakenclaw.
			break;
			case 2:
				spawn(235743, 480.99368f, 524.84326f, 597.43713f, (byte) 10); // 据点防御 Bard / Post Defense Bard.
			break;
			case 3:
				spawn(235745, 480.99368f, 524.84326f, 597.43713f, (byte) 10); // 据点防御 Hidestitcher / Post Defense Hidestitcher.
			break;
			case 4:
				spawn(235746, 480.99368f, 524.84326f, 597.43713f, (byte) 10); // 据点防御 Gunner / Post Defense Gunner.
			break;
			case 5:
				spawn(235747, 480.99368f, 524.84326f, 597.43713f, (byte) 10); // 据点防御 Spelltongue / Post Defense Spelltongue.
			break;
			case 6:
				spawn(235748, 480.99368f, 524.84326f, 597.43713f, (byte) 10); // 据点防御 Swiftrunner / Post Defense Swiftrunner.
			break;
		} switch (Rnd.get(1, 4)) {
			case 1:
				spawn(235772, 672.9581f, 468.63168f, 599.4349f, (byte) 1); // 守卫 NPC：Hakara / Hakara.
			break;
			case 2:
				spawn(235773, 672.9581f, 468.63168f, 599.4349f, (byte) 1); // 守卫 NPC：Zubala / Zubala.
			break;
			case 3:
				spawn(235774, 672.9581f, 468.63168f, 599.4349f, (byte) 1); // 守卫 NPC：Visha / Visha.
			break;
			case 4:
				spawn(235775, 672.9581f, 468.63168f, 599.4349f, (byte) 1); // 守卫 NPC：Bahapa / Bahapa.
			break;
		} switch (Rnd.get(1, 4)) {
			case 1:
				spawn(235776, 552.2419f, 512.9514f, 610.10693f, (byte) 26); // 守卫 NPC：Hakara / Hakara.
			break;
			case 2:
				spawn(235777, 552.2419f, 512.9514f, 610.10693f, (byte) 26); // 守卫 NPC：Zubala / Zubala.
			break;
			case 3:
				spawn(235778, 552.2419f, 512.9514f, 610.10693f, (byte) 26); // 守卫 NPC：Visha / Visha.
			break;
			case 4:
				spawn(235779, 552.2419f, 512.9514f, 610.10693f, (byte) 26); // 守卫 NPC：Bahapa / Bahapa.
			break;
		}
	}
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * @param npc 目标 NPC / target NPC
	 */
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 235759: // 逃犯 Mazikin 首领 / Fugitive Mazikin Leader.
			case 235763: // 逃亡者 Hirakiki 首领 / Runaway Hirakiki Leader.
			case 235767: // 越狱者 Asachin 首领 / Escapee Asachin Leader.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
					    if (player.getCommonData().getRace() == Race.ELYOS) {
						    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 182215759, 1)); // 逃犯携带的碎片 / The Piece Carried By The Fugitive.
						} else if (player.getCommonData().getRace() == Race.ASMODIANS) {
							dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 182215760, 1)); // 逃犯携带的碎片 / The Piece Carried By The Fugitive.
					    } switch (Rnd.get(1, 3)) {
				            case 1:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053708, 1)); // 被盗避难所消耗品捆 / Stolen Shelter Consumables Bundle.
				            break;
					        case 2:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053709, 1)); // 被盗避难所古代金币捆 / Stolen Shelter Ancient Coin Bundle.
				            break;
					        case 3:
				                dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053710, 1)); // 缴获避难所遗物捆 / Captured Shelter Relics Bundle.
				            break;
						}
					}
				}
			break;
			case 235768: // 勇猛的 Velkur / Spirited Velkur.
			case 235769: // Velkur 奥德施法者 / Velkur Aethercaster.
			case 235770: // Velkur 奥德祭司 / Velkur Aetherpriest.
			case 235771: // Velkur 奥德刺客 / Velkur Aetherknife.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053789, 1)); //大型烙印之石支援包。 / Major Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052612, 1)); // Vera 的宝物箱 / Vera's Treasure Crate.
					}
				}
			break;
			case 702658: //修道院箱子。 / Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053579, 1)); //[活动] 修道院礼包。 / [Event] Abbey Bundle.
		    break;
			case 702659: //高级修道院箱子。 / Noble Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053580, 1)); //[活动] 高级修道院礼包。 / [Event] Noble Abbey Bundle.
		    break;
			case 802180: // 奥菲丹桥机会礼包 / Ophidan Bridge Opportunity Bundle.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000051, 30)); // 大型古代王冠 / Major Ancient Crown.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000052, 30)); // 高级古代王冠 / Greater Ancient Crown.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000236, 50)); // 血斗标记 / Blood Mark.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000237, 50)); // 古代金币 / Ancient Coin.
			break;
		}
	}
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * @param npc 目标 NPC / target NPC
	 */
	@Override
	public void onDie(Npc npc) {
		Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 235768: // 勇猛的 Velkur / Spirited Velkur.
			case 235769: // Velkur 奥德施法者 / Velkur Aethercaster.
			case 235770: // Velkur 奥德祭司 / Velkur Aetherpriest.
			case 235771: // Velkur 奥德刺客 / Velkur Aetherknife.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Ophidan Bridge>");
/* 				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 349.57327f, 495.25214f, 606.76013f, (byte) 91); //修道院箱子。 / Abbey Box.
					break;
					case 2:
					    spawn(702659, 349.57327f, 495.25214f, 606.76013f, (byte) 91); //高级修道院箱子。 / Noble Abbey Box.
					break;
				} */
				spawn(730868, 350.18478f, 490.73065f, 606.34015f, (byte) 1); // 奥菲丹桥出口 / Ophidan Bridge Exit.
				spawn(802180, 350.39514f, 486.26636f, 606.75397f, (byte) 32); // 奥菲丹桥机会礼包 / Ophidan Bridge Opportunity Bundle.
            break;
			case 235786: // 钢铁城墙 / Steel Wall.
				despawnNpc(npc);
			break;
		}
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
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
}