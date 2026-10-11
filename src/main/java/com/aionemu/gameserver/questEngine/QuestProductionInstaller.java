package com.aionemu.gameserver.questEngine;

import java.util.Set;

import com.aionemu.gameserver.model.templates.quest.QuestNpc;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogRegistry;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeDispatcher;

/**
 * 生产 typed owner 安装器：把已校验目录的事件接线经 {@link QuestEngine} 注册面
 * 安装进任务注册表，并回传待发布的分发器。
 * Production typed-owner installer: wires the validated catalog's event transitions through the
 * {@link QuestEngine} registration facade into the quest registry and returns the dispatcher to
 * publish.
 */
final class QuestProductionInstaller {

	private QuestProductionInstaller() {
	}

	/**
	 * 按 transition 事件类型经引擎注册面逐个安装兴趣面；
	 * 分发器由调用方在全部接线完成后发布。
	 * Installs the interest faces per transition event type through the engine's registration facade;
	 * the dispatcher is published by the caller only after all wiring completed.
	 */
	static QuestRuntimeDispatcher install(QuestEngine engine,
			QuestEngine.PreparedProductionDefinitions prepared) {
		QuestCatalogRegistry catalog = prepared.catalog();
		QuestRuntimeDispatcher dispatcher = prepared.dispatcher();
		for (CompiledQuestDefinition definition : catalog.executables()) {
			for (var transition : definition.definition().transitions()) {
				if (transition.event() instanceof QuestEvent.TalkToNpc talk) {
					QuestNpc questNpc = engine.registerQuestNpc(talk.npcId());
					questNpc.addOnTalkEvent(definition.id());
					if (transition.sourceNode() != null && definition.definition().nodes().stream()
							.filter(node -> node.label().equals(transition.sourceNode()))
							.map(QuestNode::projection)
							.anyMatch(projection -> projection.status() == QuestStatus.NONE)) {
						questNpc.addOnQuestStart(definition.id());
					}
				} else if (transition.event() instanceof QuestEvent.KillNpc(int npcId1)) {
					engine.registerQuestNpc(npcId1).addOnKillEvent(definition.id());
				} else if (transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)) {
					for (int npcId : npcIds) {
						engine.registerQuestNpc(npcId).addOnKillEvent(definition.id());
					}
				} else if (transition.event() instanceof QuestEvent.AttackNpc attack) {
					engine.registerQuestNpc(attack.npcId()).addOnAttackEvent(definition.id());
				} else if (transition.event() instanceof QuestEvent.CanAct canAct) {
					engine.registerCanAct(definition.id(), canAct.templateId());
				} else if (transition.event() instanceof QuestEvent.UseItem use) {
					engine.registerQuestItem(use.itemId(), definition.id());
				} else if (transition.event() instanceof QuestEvent.GetItem(int id)) {
					// Keep the obtain-event index in sync with the typed catalog. The
					// runtime dispatcher remains authoritative; this index only lets
					// the legacy loop skip typed owners safely.
					engine.registerGetingItem(id, definition.id());
				} else if (transition.event() instanceof QuestEvent.CollectItem collect) {
					// Collection routes share the item-obtain ingress while retaining
					// their own event type for count matching.
					engine.registerGetingItem(collect.itemId(), definition.id());
				} else if (transition.event() instanceof QuestEvent.PassFlyingRing ring) {
					engine.registerOnPassFlyingRings(ring.ring(), definition.id());
				} else if (transition.event() instanceof QuestEvent.EnterWindStream) {
					engine.registerOnEnterWindStream(definition.id());
				} else if (transition.event() instanceof QuestEvent.AtDistance atDistance) {
					engine.registerQuestNpc(atDistance.npcId()).addOnAtDistanceEvent(definition.id());
				} else if (transition.event() instanceof QuestEvent.Die) {
					engine.registerOnDie(definition.id());
				} else if (transition.event() instanceof QuestEvent.LogOut) {
					engine.registerOnLogOut(definition.id());
				} else if (transition.event() instanceof QuestEvent.NpcReachTarget) {
					engine.registerAddOnReachTargetEvent(definition.id());
				} else if (transition.event() instanceof QuestEvent.NpcLostTarget) {
					engine.registerAddOnLostTargetEvent(definition.id());
				} else if (transition.event() instanceof QuestEvent.MovieEnd(int movieId)) {
					engine.registerOnMovieEndQuest(movieId, definition.id());
				} else if (transition.event() instanceof QuestEvent.ZoneMissionEnd) {
					engine.registerOnEnterZoneMissionEnd(definition.id());
				} else if (transition.event() instanceof QuestEvent.InvisibleTimerEnd) {
					engine.registerOnInvisibleTimerEnd(definition.id());
				} else if (transition.event() instanceof QuestEvent.EquipItem(int itemId)) {
					engine.registerOnEquipItem(itemId, definition.id());
				}
			}
		}
		return dispatcher;
	}
}
