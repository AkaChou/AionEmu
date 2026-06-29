/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.curingzone.CuringObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.curingzones.CuringTemplate;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.knownlist.Visitor;

@Slf4j
public class CuringZoneService {

	private static volatile ObjectProvider<CuringZoneService> instanceProvider;
	private List<CuringObject> curingObjects = new ArrayList<CuringObject>();

	public CuringZoneService() {
		for (CuringTemplate t : DataManager.CURING_OBJECTS_DATA.getCuringObject()) {
			CuringObject obj = new CuringObject(t, 0);
			obj.spawn();
			curingObjects.add(obj);
		}
		log.info("spawned Curing Zones");
		startTask();
	}

	private void startTask() {
		GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {

			public void run() {
				for (final CuringObject obj : curingObjects)
					obj.getKnownList().doOnAllPlayers(new Visitor<Player>() {
						public void visit(Player player) {
							if ((MathUtil.isIn3dRange(obj, player, obj.getRange()))
									&& (!player.getEffectController().hasAbnormalEffect(8751))) {
								GameEngineServices.skillEngine().getSkill(player, 8751, 1, player).useNoAnimationSkill();
							}
						}
					});
			}
		}, 1000, 1000);
	}

	public static final CuringZoneService getInstance() {
		ObjectProvider<CuringZoneService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	public static void setInstanceProvider(ObjectProvider<CuringZoneService> instanceProvider) {
		CuringZoneService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {

		protected static final CuringZoneService instance = new CuringZoneService();
	}
}
