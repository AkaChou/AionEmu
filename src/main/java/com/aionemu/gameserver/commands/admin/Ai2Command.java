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
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;

/**
 * 管理员 AI2 调试命令：开关日志、查看/设置目标 NPC 的 AI 状态与事件。
 * Admin AI2 debug command: toggles logs and inspects/sets target NPC AI state and events.
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
	 * @param params 参数：子命令与附加参数 / subcommand and extra args
	 */
	@Override
	public void execute(Player player, String... params) {
        String param0 = params[0];

		switch (param0) {
			case "createlog": {
				boolean oldValue = AIConfig.ONCREATE_DEBUG;
				AIConfig.ONCREATE_DEBUG = !oldValue;
				PacketSendUtility.sendMessage(player, "New createlog value: " + !oldValue);
				return;
			}
			case "eventlog": {
				boolean oldValue = AIConfig.EVENT_DEBUG;
				AIConfig.EVENT_DEBUG = !oldValue;
				PacketSendUtility.sendMessage(player, "New eventlog value: " + !oldValue);
				return;
			}
			case "movelog": {
				boolean oldValue = AIConfig.MOVE_DEBUG;
				AIConfig.MOVE_DEBUG = !oldValue;
				PacketSendUtility.sendMessage(player, "New movelog value: " + !oldValue);
				return;
			}
			case "say":
				log.info(I18n.get("log.f71e557037c3", params[1]));
				break;
		}

        VisibleObject target = player.getTarget();

		if (target == null || !(target instanceof Npc npc)) {
			PacketSendUtility.sendMessage(player, "Select target first (Npc only)");
			return;
		}

		switch (param0) {
			case "info":
				PacketSendUtility.sendMessage(player, "Ai name: " + npc.getAi2().getName());
				PacketSendUtility.sendMessage(player, "Ai state: " + npc.getAi2().getState());
				PacketSendUtility.sendMessage(player, "Ai substate: " + npc.getAi2().getSubState());
				return;
			case "log":
				boolean oldValue = npc.getAi2().isLogging();
				((AbstractAI) npc.getAi2()).setLogging(!oldValue);
				PacketSendUtility.sendMessage(player, "New log value: " + !oldValue);
				return;
			case "print":
				AIEventLog eventLog = ((AbstractAI) npc.getAi2()).getEventLog();
				Iterator<AIEventType> iterator = eventLog.iterator();
				while (iterator.hasNext()) {
					PacketSendUtility.sendMessage(player, "EVENT: " + iterator.next().name());
				}
				return;
		}

		String param1 = params[1];
		switch (param0) {
			case "set": {
				String aiName = param1;
				GameEngineServices.ai2Engine().setupAI(aiName, npc);
				// 新装配的 AI 实例停留在 CREATED，只有 SPAWNED 事件会把它推进 IDLE；缺少这一步会让该 NPC
				// 收不到 ATTACK / CREATURE_SEE 等事件，变成完全木桩（与 RetailDirectPortalEngine 的做法保持一致）。
				// A freshly attached AI stays in CREATED until the SPAWNED event moves it to IDLE; without it the NPC
				// stops receiving ATTACK / CREATURE_SEE and behaves like a statue (same as RetailDirectPortalEngine).
				npc.getAi2().onGeneralEvent(AIEventType.SPAWNED);
				break;
			}
			case "event": {
				AIEventType eventType = AIEventType.valueOf(param1.toUpperCase());
				if (eventType != null) {
					npc.getAi2().onGeneralEvent(eventType);
				}
				break;
			}
			case "event2": {
				AIEventType eventType = AIEventType.valueOf(param1.toUpperCase());
				Creature creature = (Creature) com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(Integer.valueOf(params[2]));
				if (eventType != null) {
					npc.getAi2().onCreatureEvent(eventType, creature);
				}
				break;
			}
			case "state": {
				AIState state = AIState.valueOf(param1.toUpperCase());
				((NpcAI2) npc.getAi2()).setStateIfNot(state);
				if (params.length > 2) {
					AISubState substate = AISubState.valueOf(params[2]);
					((NpcAI2) npc.getAi2()).setSubStateIfNot(substate);
				}
				break;
			}
		}
	}

	/**
	 * 参数错误时输出 {@code //ai2} 用法。
	 * Prints {@code //ai2} usage on invalid arguments.
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //ai2 <set|event|event2|info|log|print|createlog|eventlog|movelog>");
	}
}
