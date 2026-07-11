package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员突袭刷怪命令：在目标周围按半径/数量刷出 NPC，可定时删除。
 * Admin assault-spawn command: spawns NPCs around a target by radius/count, with optional despawn.
 *
 * @author ginho1
 */
public class Assault extends AdminCommand {

	/**
	 * 注册 {@code //assault} 命令。
	 * Registers the {@code //assault} command.
	 */
	public Assault() {
		super("assault");
	}

	/**
	 * 执行突袭刷怪：解析半径、数量、NPC 列表/预设与消失时间。
	 * Executes assault spawn: parses radius, amount, NPC list/preset, and despawn time.
	 *
	 * admin
	 * @param params 参数：半径、数量、NPC/预设、消失秒数 / radius, amount, npc/preset, despawn secs
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length > 4 || params.length < 3) {
			onFail(admin, null);
			return;
		}
		int radius;
		int amount;
		int despawnTime = 0;
		try {
			radius = Math.abs(Integer.parseInt(params[0]));
			amount = Integer.parseInt(params[1]);
			if(params.length == 4)
				despawnTime = Math.abs(Integer.parseInt(params[3]));
		}
		catch(NumberFormatException e) {
			PacketSendUtility.sendMessage(admin, "You should only input integers as radius, amount and despawn time.");
			return;
		}

		if (radius > 100) {
			PacketSendUtility.sendMessage(admin, "Radius can't be higher than 100.");
			return;
		}

		if(amount < 1 || amount > 100) {
			PacketSendUtility.sendMessage(admin, "Amount should be between 1-100.");
			return;
		}

		if( despawnTime > 60*60 ) {
			PacketSendUtility.sendMessage(admin, "You can't have a despawn time longer than 1hr.");
			return;
		}

		List<Integer> idList = new ArrayList<Integer>();
		if((params[2]).equals("tier20")) {
			idList.add(210799);
			idList.add(211961);
			idList.add(213831);
			idList.add(253739);
			idList.add(210566);
			idList.add(210745);
		}
		else if(params[2].equals("tier30")) {
			idList.add(210997);
			idList.add(213831);
			idList.add(213547);
			idList.add(253739);
			idList.add(210942);
			idList.add(212631);
		}
		else if(params[2].equals("balaur4")) {
			idList.add(210997);
			idList.add(255704);
			idList.add(211962);
			idList.add(213240);
			idList.add(214387);
			idList.add(213547);
		}
		else if(params[2].equals("balaur5")) {
			idList.add(250187);
			idList.add(250187);
			idList.add(250187);
			idList.add(250182);
			idList.add(250182);
			idList.add(250182);
			idList.add(250187);
		}
		else if(params[2].equals("dredgion")) {
			idList.add(258236);
			idList.add(258238);
			idList.add(258243);
			idList.add(258241);
			idList.add(258237);
			idList.add(258240);
			idList.add(258239);
			idList.add(258242);
			idList.add(250187);
			idList.add(250182);
		}
		else
		{
			for(String npcId : params[2].split(",")) {
				try {
					idList.add(Integer.parseInt(npcId));
				}
				catch(NumberFormatException e) {
					PacketSendUtility.sendMessage(admin, "You should only input integers as NPC ids.");
					return;
				}
			}
			if(idList.size() == 0)
				return;
		}

		Creature target;
		if(admin.getTarget() != null)
			target = (Creature) admin.getTarget();
		else
			target = (Creature) admin;

		float x = target.getX();
		float y = target.getY();
		float z = target.getZ();
		byte heading = target.getHeading();
		int worldId = target.getWorldId();

		int templateId;
		SpawnTemplate spawn = null;

		float interval = (float) (Math.PI * 2.0f / amount);
		float x1;
		float y1;
		int spawnCount = 0;

		VisibleObject visibleObject;
		List<VisibleObject> despawnList = new ArrayList<VisibleObject>();//will hold the list of spawned mobs

		for( int i = 0; amount > i; i++) {
			templateId = idList.get((int)(Math.random() * idList.size()));
			x1 = (float)(Math.cos( interval * i ) * radius);
			y1 = (float)(Math.sin( interval * i ) * radius);
			spawn = SpawnEngine.addNewSpawn(worldId, templateId, x + x1 , y + y1, z, heading, 0);

			if(spawn == null) {
				PacketSendUtility.sendMessage(admin, "There is no npc: " + templateId);
				return;
			}
			else {
				visibleObject = SpawnEngine.spawnObject(spawn, 1);

				if(despawnTime > 0)
					despawnList.add(visibleObject);

				spawnCount++;
			}
		}

		if( despawnTime > 0 ) {
			PacketSendUtility.sendMessage(admin, "Despawn time active: " + despawnTime + "sec" );
			despawnThem(admin, despawnList, despawnTime);
		}

		PacketSendUtility.sendMessage(admin, spawnCount + " npc have been spawned.");
	}

	/**
	 * 在指定秒数后删除本轮刷出的 NPC。
	 * Deletes the spawned NPCs after the given number of seconds.
	 *
	 * admin
	 * @param despawnList 待删除可见对象列表 / list of visible objects to despawn
	 * delay in seconds
	 */
	private void despawnThem(final Player admin, final List<VisibleObject> despawnList, final int despawnTime) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				int despawnCount = 0;
				for(VisibleObject visObj : despawnList)	{
					if(visObj != null && visObj.isSpawned()) {
						visObj.getController().delete();
						despawnCount++;
					}
				}
				PacketSendUtility.sendMessage(admin, despawnCount + " npc have been deleted.");
			}
		}, despawnTime * 1000);
	}

	/**
	 * 参数错误时输出 {@code //assault} 用法。
	 * Prints {@code //assault} usage on invalid arguments.
	 *
	 * admin
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		String syntax = "Syntax: //assault <radius> <amount> <npc_id1, npc_id2,...| tier20 | tier30 |...> <despawn time in secs>";
		PacketSendUtility.sendMessage(player, syntax);
	}
}