package com.aionemu.gameserver.services.conquestservice;

import java.util.Map;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;
import com.aionemu.gameserver.model.conquest.ConquestLocation;

/**
 * 征服/供奉活动启动定时任务。
 * Start runnable for Conquest/Offering events.
 *
 * <p>广播各开放副本与稀有怪出现消息，并启动对应地点。
 * Broadcasts open-instance and rare-spawn messages, then starts the matching location.</p>
 *
 * @author Rinzler (Encom)
 */
public class ConquestStartRunnable implements Runnable {

	private final int id;

	/**
	 * 绑定目标地点 ID。
	 * Binds the target location id.
	 *
	 * @param id 地点 ID / location id
	 */
	public ConquestStartRunnable(int id) {
		this.id = id;
	}

	/**
	 * 广播消息并启动对应地点。
	 * Broadcasts messages and starts the matching location.
	 */
	@Override
	public void run() {
		// 术古皇帝宝库 4.7.5 现已开放！！！ / Shugo Emperor's Vault 4.7.5 is now open !!!
		GameLocationBootstrapServices.conquestService().emperorVaultMsg(id);
		// 皇帝特里利伦克保险箱 4.9.1 现已开放！！！ / Emperor Trillirunerk's Safe 4.9.1 is now open !!!
		GameLocationBootstrapServices.conquestService().trillirunerkSafeMsg(id);
		// 闷燃火神殿 5.1 现已开放！！！ / Smoldering Fire Temple 5.1 is now open !!!
		GameLocationBootstrapServices.conquestService().smolderingFireTempleMsg(id);
		// 库穆基洞穴 5.3 现已开放！！！ / Kumuki Cave 5.3 is now open !!!
		GameLocationBootstrapServices.conquestService().kumukiCaveMsg(id);
		// IDEventDefMsg 5.6 is now open !!!。 / IDEventDefMsg 5.6 is now open !!!
		GameLocationBootstrapServices.conquestService().IDEventDefMsg(id);
		// 提亚玛兰塔之眼现已开放！！！ / Tiamaranta's Eye is now open !!!
		GameLocationBootstrapServices.conquestService().tiamarantaMsg(id);
		// 征服/供奉：稀有怪物出现了！！！ / Conquest/Offering a rare monster appeared !!!
		GameLocationBootstrapServices.conquestService().conquestOfferingMsg(id);
		Map<Integer, ConquestLocation> locations = GameLocationBootstrapServices.conquestService().getConquestLocations();
		for (final ConquestLocation loc : locations.values()) {
			if (loc.getId() == id) {
				GameLocationBootstrapServices.conquestService().startConquest(loc.getId());
			}
		}
	}
}
