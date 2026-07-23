package com.aionemu.gameserver.instance.handlers.scripts.ophidanBridge;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Set;

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
				spawn(235780, 751.4241f, 527.29016f, 576.37476f, (byte) 33); //Fugitive Mazikin.
			break;
			case 2:
				spawn(235781, 751.4241f, 527.29016f, 576.37476f, (byte) 33); //Runaway Hirakiki.
			break;
			case 3:
				spawn(235782, 751.4241f, 527.29016f, 576.37476f, (byte) 33); //Escapee Asachin.
			break;
		} switch (Rnd.get(1, 4)) {
			case 1:
				spawn(235768, 318.23724f, 488.92276f, 607.64343f, (byte) 1); //Spirited Velkur.
			break;
			case 2:
				spawn(235769, 318.23724f, 488.92276f, 607.64343f, (byte) 1); //Velkur Aethercaster.
			break;
			case 3:
				spawn(235770, 318.23724f, 488.92276f, 607.64343f, (byte) 1); //Velkur Aetherpriest.
			break;
			case 4:
				spawn(235771, 318.23724f, 488.92276f, 607.64343f, (byte) 1); //Velkur Aetherknife.
			break;
		} switch (Rnd.get(1, 3)) {
			case 1:
				spawn(235721, 673.0f, 472.0f, 599.3125f, (byte) 0); //Post Defense Drakenclaw.
			break;
			case 2:
				spawn(235726, 673.0f, 472.0f, 599.3125f, (byte) 0); //Defense Spelltongue.
			break;
			case 3:
				spawn(235727, 673.0f, 472.0f, 599.3125f, (byte) 0); //Defense Swiftrunner.
			break;
		} switch (Rnd.get(1, 3)) {
			case 1:
				spawn(235728, 531.0988f, 437.3993f, 620.25f, (byte) 109); //North Defense Drakenclaw.
			break;
			case 2:
				spawn(235730, 531.0988f, 437.3993f, 620.25f, (byte) 109); //North Defense Ironscale.
			break;
			case 3:
				spawn(235731, 531.0988f, 437.3993f, 620.25f, (byte) 109); //North Defense Hidestitcher.
			break;
		} switch (Rnd.get(1, 5)) {
			case 1:
				spawn(235735, 608.1635f, 558.9905f, 590.57214f, (byte) 110); //South Defense Drakenclaw.
			break;
			case 2:
				spawn(235736, 608.1635f, 558.9905f, 590.57214f, (byte) 110); //South Defense Bard.
			break;
			case 3:
				spawn(235737, 608.1635f, 558.9905f, 590.57214f, (byte) 110); //South Defense Ironscale.
			break;
			case 4:
				spawn(235738, 608.1635f, 558.9905f, 590.57214f, (byte) 110); //South Defense Hidestitcher.
			break;
			case 5:
				spawn(235740, 608.1635f, 558.9905f, 590.57214f, (byte) 110); //South Defense Spelltongue.
			break;
		} switch (Rnd.get(1, 6)) {
			case 1:
				spawn(235742, 480.99368f, 524.84326f, 597.43713f, (byte) 10); //Post Defense Drakenclaw.
			break;
			case 2:
				spawn(235743, 480.99368f, 524.84326f, 597.43713f, (byte) 10); //Post Defense Bard.
			break;
			case 3:
				spawn(235745, 480.99368f, 524.84326f, 597.43713f, (byte) 10); //Post Defense Hidestitcher.
			break;
			case 4:
				spawn(235746, 480.99368f, 524.84326f, 597.43713f, (byte) 10); //Post Defense Gunner.
			break;
			case 5:
				spawn(235747, 480.99368f, 524.84326f, 597.43713f, (byte) 10); //Post Defense Spelltongue.
			break;
			case 6:
				spawn(235748, 480.99368f, 524.84326f, 597.43713f, (byte) 10); //Post Defense Swiftrunner.
			break;
		} switch (Rnd.get(1, 4)) {
			case 1:
				spawn(235772, 672.9581f, 468.63168f, 599.4349f, (byte) 1); //Hakara.
			break;
			case 2:
				spawn(235773, 672.9581f, 468.63168f, 599.4349f, (byte) 1); //Zubala.
			break;
			case 3:
				spawn(235774, 672.9581f, 468.63168f, 599.4349f, (byte) 1); //Visha.
			break;
			case 4:
				spawn(235775, 672.9581f, 468.63168f, 599.4349f, (byte) 1); //Bahapa.
			break;
		} switch (Rnd.get(1, 4)) {
			case 1:
				spawn(235776, 552.2419f, 512.9514f, 610.10693f, (byte) 26); //Hakara.
			break;
			case 2:
				spawn(235777, 552.2419f, 512.9514f, 610.10693f, (byte) 26); //Zubala.
			break;
			case 3:
				spawn(235778, 552.2419f, 512.9514f, 610.10693f, (byte) 26); //Visha.
			break;
			case 4:
				spawn(235779, 552.2419f, 512.9514f, 610.10693f, (byte) 26); //Bahapa.
			break;
		}
	}
	@Override
	public void onDropRegistered(Npc npc) {
		int npcId = npc.getNpcId();
		if (npcId != 802180) {
			return;
		}
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000051, 30)); //Major Ancient Crown.
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000052, 30)); //Greater Ancient Crown.
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000236, 50)); //Blood Mark.
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000237, 50)); //Ancient Coin.
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
			case 235768: //Spirited Velkur.
			case 235769: //Velkur Aethercaster.
			case 235770: //Velkur Aetherpriest.
			case 235771: //Velkur Aetherknife.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Ophidan Bridge>");
/* 				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 349.57327f, 495.25214f, 606.76013f, (byte) 91); //修道院箱子。 / Abbey Box.
					break;
					case 2:
					    spawn(702659, 349.57327f, 495.25214f, 606.76013f, (byte) 91); //高级修道院箱子。 / Noble Abbey Box.
					break;
				} */
				spawn(730868, 350.18478f, 490.73065f, 606.34015f, (byte) 1); //Ophidan Bridge Exit.
				spawn(802180, 350.39514f, 486.26636f, 606.75397f, (byte) 32); //Ophidan Bridge Opportunity Bundle.
            break;
			case 235786: //Steel Wall.
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
