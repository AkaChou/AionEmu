package com.aionemu.gameserver.instance.handlers.scripts.ophidanBridge;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.world.WorldMapInstance;

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
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 235768: //Spirited Velkur.
			case 235769: //Velkur Aethercaster.
			case 235770: //Velkur Aetherpriest.
			case 235771: //Velkur Aetherknife.
				spawn(802180, 350.39514f, 486.26636f, 606.75397f, (byte) 32); //Ophidan Bridge Opportunity Bundle.
			break;
		}
	}
}
