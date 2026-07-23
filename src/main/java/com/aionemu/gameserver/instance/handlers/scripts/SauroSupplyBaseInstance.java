package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.world.WorldMapInstance;
import java.util.Map;
import java.util.Set;

@InstanceID(301130000)
public class SauroSupplyBaseInstance extends GeneralInstanceHandler
{
	/** 门映射 / door map */
	private Map<Integer, StaticDoor> doors;

	@Override
	public void onDropRegistered(Npc npc) {
		int npcId = npc.getNpcId();
		if (npcId == 230847) {
			sendMsg(1401946, 0, false, 25, 0);
			return;
		}
		if (npcId != 802181) {
			return;
		}
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000051, 30)); //Major Ancient Crown.
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000052, 30)); //Greater Ancient Crown.
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000236, 50)); //Blood Mark.
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000237, 50)); //Ancient Coin.
	}
	
	/**
	 * 奖励怪：“索罗基地盗墓者”，可出现在 5 个房间，掉落古代硬币、古代魔石、外观。 / Bonus Monster: "Sauro Base Grave Robber" They can appear in "5 different rooms" and give: Ancient Coins. Ancient Manastones. Skins
	 */
	@Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
		doors = instance.getDoors();
		// 入侵警报响起。索罗精英护卫队正在集结。 / An intruder alarm has sounded. The Sauro Elite Protectorate are gathering.
		sendMsg(1401810, 0, false, 25, 10000);
		// 索罗精英护卫队已集结。 / The Sauro Elite Protectorate has assembled.
		sendMsg(1401811, 0, false, 25, 30000);
		// 索罗精英护卫队正在接近。 / The Sauro Elite Protectorate approaches.
		sendMsg(1401812, 0, false, 25, 50000);
		// 索罗精英护卫队还有一分钟到达。 / The Sauro Elite Protectorate is one minute out.
		sendMsg(1401813, 0, false, 25, 70000);
		// 索罗精英护卫队已到你面前。 / The Sauro Elite Protectorate is upon you.
		sendMsg(1401814, 0, false, 25, 130000);
		switch (Rnd.get(1, 5)) {
		    case 1:
				spawn(230846, 464.07788f, 401.3575f, 182.15321f, (byte) 10);
			break;
			case 2:
				spawn(230846, 496.30792f, 412.814f, 182.13792f, (byte) 73);
			break;
			case 3:
				spawn(230846, 497.15717f, 392.34656f, 182.14955f, (byte) 75);
			break;
			case 4:
				spawn(230846, 496.2902f, 358.0765f, 182.14955f, (byte) 48);
			break;
			case 5:
				spawn(230846, 464.15985f, 389.7157f, 182.15321f, (byte) 109);
			break;
		}
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
	 * 区域 1：警卫室与符文大厅。 / Area 1: Guardroom And Rune Hall
	 */
			case 230849: //Guard Captain Rohuka.
				doors.get(383).setOpen(true);
				// 通往被玷污的达努阿尔神殿的门已打开。 / The door to the Defiled Danuar Temple has opened.
				sendMsg(1401914, 0, false, 25, 0);
			break;
			case 230851: //Chief Gunner Kurmata.
				doors.get(59).setOpen(true);
				// 通往达努阿尔冥想花园的门已打开。 / The door to the Danuar Meditation Garden has opened.
				sendMsg(1401915, 0, false, 25, 0);
				switch (Rnd.get(1, 2)) {
				    case 1:
				        doors.get(382).setOpen(true);
						// 守门人倒下，左侧门已打开！ / With the gatekeeper down, the door on the left is open!
						sendMsg(1401229, 0, false, 25, 5000);
						spawn(230797, 610.7328f, 518.80884f, 191.2776f, (byte) 75); //Sheban Legion Elite Ambusher.
					break;
			        case 2:
						doors.get(387).setOpen(true);
						// 守门人倒下，右侧门已打开！ / With the gatekeeper down, the door on the right is open!
						sendMsg(1401230, 0, false, 25, 5000);
						spawn(230797, 611.1872f, 452.91882f, 191.2776f, (byte) 39); //Sheban Legion Elite Ambusher.
					break;
				}
			break;
			
		   /**
	 * 区域 2：符文回廊与后勤基地。 / Area 2: Rune Cloister And Logistic Base
	 */
			case 230818: //Sheban Legion Elite Gunner.
				doors.get(372).setOpen(true);
				// 通往首席研究员办公室的门已打开。 / The door to the Head Researcher's Office has opened.
				sendMsg(1401916, 0, false, 25, 0);
			break;
			case 230850: //Research Teselik.
				doors.get(375).setOpen(true);
				// 通往失落虔诚之树的门已打开。 / The door to the Lost Tree of Devotion has opened.
				sendMsg(1401917, 0, false, 25, 0);
			break;
			
		   /**
	 * 区域 3：符文桥与后勤基地军械库。 / Area 3: Rune Bridge And Logistic Base Arsenal
	 */
			case 233255: //Gatekeeper Stranir.
				doors.get(378).setOpen(true);
				// 通往索罗军械库的门已打开。 / The door to the Sauro Armory has opened.
				sendMsg(1401918, 0, false, 25, 0);
			break;
			case 230852: //Commander Ranodim.
				doors.get(388).setOpen(true);
				// 通往重型仓储区的门已打开。 / The door to the Heavy Storage Area has opened.
				sendMsg(1401919, 0, false, 25, 0);
			break;
			
			/**
	 * 区域 4。 / Area 4: Chiefs Chamber
	 */
			case 230791: //Sheban Legion Elite Assaulter.
				doors.get(376).setOpen(true);
				// 通往莫里亚塔住所的门已打开。 / The door to Moriata's Quarters has opened.
				sendMsg(1401920, 0, false, 25, 0);
			break;
			case 230853: //Chief Of Staff Moriata.
				// 通往达努阿尔万神殿的装置已激活。 / A device leading to the Danuar Omphanium has been activated.
				sendMsg(1401921, 0, false, 25, 0);
				// 通往达努阿尔万神殿的通道将开放 5 分钟。 / The passage to the Danuar Omphanium will be open for five minutes.
				sendMsg(1401922, 0, false, 25, 5000);
				spawn(730872, 127.77696f, 432.75684f, 151.69659f, (byte) 0, 3);
			break;
			
			/**
	 * 区域 5。 / Area 5: Final Boss
	 */
			case 230857: //Guard Captain Ahuradim.
/* 				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 703.3344f, 883.07666f, 411.5939f, (byte) 90); //修道院箱子。 / Abbey Box.
			        break;
			        case 2:
				        spawn(702659, 703.3344f, 883.07666f, 411.5939f, (byte) 90); //高级修道院箱子。 / Noble Abbey Box.
					break;
				} */
				spawn(801967, 708.9197f, 884.59625f, 411.57986f, (byte) 45); //Sauro Supply Base Exit.
				spawn(802181, 710.25726f, 889.6806f, 411.59103f, (byte) 0); //Sauro Supply Base Opportunity Bundle.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Sauro Supply Base>");
			break;
			case 230858: //Brigade General Sheba.
/* 				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 900.2497f, 896.3568f, 411.3568f, (byte) 30); //修道院箱子。 / Abbey Box.
			        break;
			        case 2:
				        spawn(702659, 900.2497f, 896.3568f, 411.3568f, (byte) 30); //高级修道院箱子。 / Noble Abbey Box.
					break;
				} */
				spawn(801967, 905.3781f, 895.2461f, 411.57785f, (byte) 75); //Sauro Supply Base Exit.
				spawn(802181, 906.9721f, 889.6604f, 411.59854f, (byte) 0); //Sauro Supply Base Opportunity Bundle.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Sauro Supply Base>");
			break;
		}
    }
	/**
	 * 移除相关物品。
	 * Remove related items.
	 *
	 * @param player 玩家 / player
	 */
	
	public void removeItems(Player player) {
        Storage storage = player.getInventory();
        storage.decreaseByItemId(185000176, storage.getItemCountByItemId(185000176)); //Red Storeroom Key.
		storage.decreaseByItemId(185000177, storage.getItemCountByItemId(185000177)); //Blue Storeroom Key.
        storage.decreaseByItemId(185000178, storage.getItemCountByItemId(185000178)); //Green Storeroom Key.
		storage.decreaseByItemId(185000179, storage.getItemCountByItemId(185000179)); //Danuar Stone Room Key.
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
	
    /**
     * 副本销毁时清理资源。
     * Clean up resources when the instance is destroyed.
     */
    @Override
    public void onInstanceDestroy() {
        doors.clear();
    }
}
