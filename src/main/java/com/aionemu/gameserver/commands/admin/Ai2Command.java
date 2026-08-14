package com.aionemu.gameserver.commands.admin;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.*;
import com.aionemu.gameserver.ai2.event.AIEventLog;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

/**
 * 管理员 AI2 调试命令：开关日志、查看/设置目标 NPC 的 AI 状态与事件。
 * Admin AI2 debug command: toggles logs and inspects/sets target NPC AI state and events.
 *
 * @author ATracer
 */
@Slf4j
public class Ai2Command extends AdminCommand {

	/**
	 * 注册 {@code //ai2} 命令。
	 * Registers the {@code //ai2} command.
	 */
	public Ai2Command() {
		super("ai2");
	}

	/**
	 * 执行 AI2 调试：支持全局日志开关与目标 NPC 的 info/log/set/event 等操作。
	 * Executes AI2 debug: global log toggles and target-NPC info/log/set/event ops.
	 *
	 * @param params 参数：子命令与附加参数 / subcommand and extra args
	 */
	@Override
	public void execute(Player player, String... params) {
		/**
		 * 非目标类命令（全局日志开关）。
		 * Non target commands
		 */
		String param0 = params[0];

		if (param0.equals("createlog")) {
			boolean oldValue = AIConfig.ONCREATE_DEBUG;
			AIConfig.ONCREATE_DEBUG = !oldValue;
			PacketSendUtility.sendMessage(player, "New createlog value: " + !oldValue);
			return;
		}

		if (param0.equals("eventlog")) {
			boolean oldValue = AIConfig.EVENT_DEBUG;
			AIConfig.EVENT_DEBUG = !oldValue;
			PacketSendUtility.sendMessage(player, "New eventlog value: " + !oldValue);
			return;
		}

		if (param0.equals("movelog")) {
			boolean oldValue = AIConfig.MOVE_DEBUG;
			AIConfig.MOVE_DEBUG = !oldValue;
			PacketSendUtility.sendMessage(player, "New movelog value: " + !oldValue);
			return;
		}

		if (param0.equals("say")) {
			log.info(I18n.get("log.f71e557037c3", params[1]));
		}

		/**
		 * 目标类命令（需选中 NPC）。
		 * Target commands
		 */
		VisibleObject target = player.getTarget();

		if (target == null || !(target instanceof Npc)) {
			PacketSendUtility.sendMessage(player, "Select target first (Npc only)");
			return;
		}
		Npc npc = (Npc) target;

		if (param0.equals("info")) {
			PacketSendUtility.sendMessage(player, "Ai name: " + npc.getAi2().getName());
			PacketSendUtility.sendMessage(player, "Ai state: " + npc.getAi2().getState());
			PacketSendUtility.sendMessage(player, "Ai substate: " + npc.getAi2().getSubState());
			return;
		}

		if (param0.equals("log")) {
			boolean oldValue = npc.getAi2().isLogging();
			((AbstractAI) npc.getAi2()).setLogging(!oldValue);
			PacketSendUtility.sendMessage(player, "New log value: " + !oldValue);
			return;
		}

		if (param0.equals("print")) {
			AIEventLog eventLog = ((AbstractAI) npc.getAi2()).getEventLog();
			Iterator<AIEventType> iterator = eventLog.iterator();
			while (iterator.hasNext()) {
				PacketSendUtility.sendMessage(player, "EVENT: " + iterator.next().name());
			}
			return;
		}

		String param1 = params[1];
		if (param0.equals("set")) {
			String aiName = param1;
			GameEngineServices.ai2Engine().setupAI(aiName, npc);
		}
		else if (param0.equals("event")) {
			AIEventType eventType = AIEventType.valueOf(param1.toUpperCase());
			if (eventType != null) {
				npc.getAi2().onGeneralEvent(eventType);
			}
		}
		else if (param0.equals("event2")) {
			AIEventType eventType = AIEventType.valueOf(param1.toUpperCase());
			Creature creature = (Creature) com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(Integer.valueOf(params[2]));
			if (eventType != null) {
				npc.getAi2().onCreatureEvent(eventType, creature);
			}
		}
		else if (param0.equals("state")) {
			AIState state = AIState.valueOf(param1.toUpperCase());
			((NpcAI2) npc.getAi2()).setStateIfNot(state);
			if (params.length > 2) {
				AISubState substate = AISubState.valueOf(params[2]);
				((NpcAI2) npc.getAi2()).setSubStateIfNot(substate);
			}
		}
	}

	/**
	 * 参数错误时输出 {@code //ai2} 用法。
	 * Prints {@code //ai2} usage on invalid arguments.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //ai2 <set|event|event2|info|log|print|createlog|eventlog|movelog>");
	}
}
