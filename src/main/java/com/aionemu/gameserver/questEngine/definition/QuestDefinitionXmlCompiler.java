package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Strict XML front end for the canonical definition IR. */
public final class QuestDefinitionXmlCompiler {
	private QuestDefinitionXmlCompiler() {
	}

	public static CompiledQuestDefinition compile(InputStream input) {
		try (InputStream schemaStream = QuestDefinitionXmlCompiler.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quest_definition.xsd")) {
			if (schemaStream == null) {
				throw new QuestCompilationException("SCHEMA_MISSING", "quest definition schema is not packaged");
			}
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			factory.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
					.newSchema(new StreamSource(schemaStream)));
			var builder = factory.newDocumentBuilder();
			builder.setErrorHandler(new DefaultHandler() {
				@Override
				public void warning(SAXParseException exception) throws SAXException {
					throw exception;
				}

				@Override
				public void error(SAXParseException exception) throws SAXException {
					throw exception;
				}

				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					throw exception;
				}
			});
			Document document = builder.parse(input);
			return QuestDefinitionCompiler.compile(parseDefinition(document.getDocumentElement()));
		} catch (QuestCompilationException e) {
			throw e;
		} catch (Exception e) {
			throw new QuestCompilationException("INVALID_XML", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
		}
	}

	private static QuestDefinition parseDefinition(Element root) {
		if (!"quest-definition".equals(root.getTagName())) {
			fail("INVALID_ROOT", "expected quest-definition");
		}
		int id = integer(root, "id");
		int version = integer(root, "version");
		QuestOwnership ownership = enumValue(QuestOwnership.class, root, "ownership");

		List<EvidenceRef> evidence = new ArrayList<>();
		Element evidenceElement = child(root, "evidence");
		if (evidenceElement != null) {
			for (Element item : children(evidenceElement, "ref")) {
				evidence.add(new EvidenceRef(attribute(item, "source"), attribute(item, "locator"),
					attribute(item, "statement")));
			}
		}

		Element metadataElement = requiredChild(root, "metadata");
		QuestMetadata metadata = parseMetadata(metadataElement);
		ProgressLayout progress = parseProgress(child(root, "progress"));
		List<QuestNode> nodes = parseNodes(child(root, "nodes"));
		List<QuestTransition> transitions = parseTransitions(child(root, "transitions"));
		return new QuestDefinition(id, version, ownership, evidence, metadata, progress, nodes, transitions);
	}

	private static QuestMetadata parseMetadata(Element element) {
		Set<String> races = parseIdSet(child(element, "races"), "race");
		Set<String> classes = parseIdSet(child(element, "classes"), "class");
		Element genderElement = child(element, "gender");
		String gender = genderElement == null ? "" : attribute(genderElement, "id");
		RepeatPolicy repeat = RepeatPolicy.once();
		Set<String> repeatCycles = Set.of();
		Element repeatElement = child(element, "repeat");
		if (repeatElement != null) {
			repeat = new RepeatPolicy(integer(repeatElement, "max-repeat-count"),
				longInteger(repeatElement, "cooldown-seconds"), bool(repeatElement, "daily"), bool(repeatElement, "weekly"));
			repeatCycles = parseWhitespaceSet(attributeOrDefault(repeatElement, "cycles", ""));
		}
		Set<Integer> prerequisites = new HashSet<>();
		Element prerequisitesElement = child(element, "prerequisites");
		if (prerequisitesElement != null) {
			for (Element prerequisite : children(prerequisitesElement, "quest")) {
				prerequisites.add(integer(prerequisite, "id"));
			}
		}
		List<QuestItemRequirement> items = parseItems(child(element, "items"));
		List<QuestItemRequirement> inventoryItems = parseItems(child(element, "inventory-items"));
		List<QuestItemRequirement> questWorkItems = parseItems(child(element, "work-items"));
		List<QuestReward> rewards = parseRewards(child(element, "rewards"));
		List<QuestReward> extendedRewards = parseRewards(child(element, "extended-rewards"));
		List<QuestDrop> drops = parseDrops(child(element, "drops"));
		List<QuestBonus> bonuses = parseBonuses(child(element, "bonuses"));
		List<QuestKill> kills = parseKills(child(element, "kills"));
		List<QuestStartCondition> startConditions = parseStartConditions(child(element, "start-conditions"));
		Map<String, List<QuestReward>> classRewards = parseClassRewards(child(element, "class-rewards"));
		return new QuestMetadata(attribute(element, "name"), integer(element, "display-name-id"),
			integer(element, "min-level"), integer(element, "max-level"), races, attribute(element, "category"),
			repeat, prerequisites, items, rewards, drops, classes, gender,
			integerOrDefault(element, "rank", 0), integerOrDefault(element, "max-count-limited-quest", 1),
			integerOrDefault(element, "count-recover-limited-quest", 1), booleanOrDefault(element, "cannot-share", false),
			booleanOrDefault(element, "cannot-giveup", false), booleanOrDefault(element, "bounty-reward", false),
			integerOrDefault(element, "use-class-reward", 0), nullableInteger(element, "combine-skill"),
			nullableInteger(element, "combine-skill-point"), booleanOrDefault(element, "timer", false), repeatCycles,
			integerOrDefault(element, "npc-faction-id", 0), attributeOrDefault(element, "mentor-type", "NONE"),
			attributeOrDefault(element, "target-type", "NONE"), integerOrDefault(element, "title-id", 0),
			inventoryItems, questWorkItems, extendedRewards, bonuses, kills, startConditions, classRewards);
	}

	private static Set<String> parseIdSet(Element parent, String childName) {
		Set<String> values = new HashSet<>();
		if (parent != null) {
			for (Element item : children(parent, childName)) {
				values.add(attribute(item, "id"));
			}
		}
		return values;
	}

	private static Set<String> parseWhitespaceSet(String value) {
		if (value == null || value.isBlank()) {
			return Set.of();
		}
		Set<String> result = new HashSet<>();
		for (String token : value.trim().split("\\s+")) {
			result.add(token);
		}
		return result;
	}

	private static List<QuestItemRequirement> parseItems(Element parent) {
		List<QuestItemRequirement> result = new ArrayList<>();
		if (parent != null) {
			for (Element item : children(parent, "item")) {
				result.add(new QuestItemRequirement(integer(item, "id"), integer(item, "count")));
			}
		}
		return result;
	}

	private static List<QuestReward> parseRewards(Element parent) {
		List<QuestReward> result = new ArrayList<>();
		if (parent != null) {
			for (Element reward : children(parent, "reward")) {
				result.add(new QuestReward(attribute(reward, "kind"), integer(reward, "id"), longInteger(reward, "amount")));
			}
		}
		return result;
	}

	private static List<QuestDrop> parseDrops(Element parent) {
		List<QuestDrop> result = new ArrayList<>();
		if (parent != null) {
			for (Element drop : children(parent, "drop")) {
				boolean eachMember = bool(drop, "each-member");
				QuestDropScope scope = drop.hasAttribute("scope")
					? enumValue(QuestDropScope.class, drop, "scope")
					: (eachMember ? QuestDropScope.GROUP : QuestDropScope.NONE);
				result.add(new QuestDrop(integer(drop, "npc-id"), integer(drop, "item-id"), integer(drop, "chance"),
					eachMember, integer(drop, "collecting-step"), scope));
			}
		}
		return result;
	}

	private static List<QuestBonus> parseBonuses(Element parent) {
		List<QuestBonus> result = new ArrayList<>();
		if (parent != null) {
			for (Element bonus : children(parent, "bonus")) {
				result.add(new QuestBonus(attribute(bonus, "type"), nullableInteger(bonus, "level"),
					nullableInteger(bonus, "skill")));
			}
		}
		return result;
	}

	private static List<QuestKill> parseKills(Element parent) {
		List<QuestKill> result = new ArrayList<>();
		if (parent != null) {
			for (Element kill : children(parent, "kill")) {
				List<Integer> npcIds = new ArrayList<>();
				for (Element npc : children(kill, "npc")) {
					npcIds.add(integer(npc, "id"));
				}
				result.add(new QuestKill(integer(kill, "sequence"), npcIds));
			}
		}
		return result;
	}

	private static List<QuestStartCondition> parseStartConditions(Element parent) {
		List<QuestStartCondition> result = new ArrayList<>();
		if (parent != null) {
			for (Element condition : children(parent, "condition")) {
				result.add(new QuestStartCondition(attribute(condition, "type"), integer(condition, "quest-id"),
					integerOrDefault(condition, "reward-mode", 0)));
			}
		}
		return result;
	}

	private static Map<String, List<QuestReward>> parseClassRewards(Element parent) {
		Map<String, List<QuestReward>> result = new LinkedHashMap<>();
		if (parent != null) {
			for (Element classElement : children(parent, "class")) {
				result.put(attribute(classElement, "id"), parseRewards(classElement));
			}
		}
		return result;
	}

	private static ProgressLayout parseProgress(Element element) {
		if (element == null) {
			return ProgressLayout.empty();
		}
		ProgressLayout.Builder builder = new ProgressLayout.Builder();
		for (Element field : children(element, "bit-field")) {
			builder.add(new BitField(attribute(field, "name"), integer(field, "offset"), integer(field, "width"),
				integer(field, "min"), integer(field, "max"), enumValue(PersistenceMode.class, field, "persistence"),
				enumValue(ProgressScope.class, field, "scope")));
		}
		return builder.build();
	}

	private static List<QuestNode> parseNodes(Element element) {
		if (element == null) {
			return List.of();
		}
		List<QuestNode> nodes = new ArrayList<>();
		for (Element node : children(element, "node")) {
			Element projection = requiredChild(node, "project");
			java.util.Map<String, Integer> variables = new java.util.LinkedHashMap<>();
			Element vars = child(projection, "vars");
			if (vars != null) {
				for (Element value : children(vars, "var")) {
					variables.put(attribute(value, "name"), integer(value, "value"));
				}
			}
			nodes.add(new QuestNode(attribute(node, "label"), new NodeProjection(
				enumValue(QuestStatus.class, projection, "status"), variables)));
		}
		return nodes;
	}

	private static List<QuestTransition> parseTransitions(Element element) {
		if (element == null) {
			return List.of();
		}
		List<QuestTransition> transitions = new ArrayList<>();
		for (Element transition : children(element, "transition")) {
			Element event = onlyChild(requiredChild(transition, "event"));
			QuestEvent parsedEvent = parseEvent(event);
			List<QuestCondition> conditions = new ArrayList<>();
			Element conditionsElement = child(transition, "conditions");
			if (conditionsElement != null) {
				for (Element condition : children(conditionsElement, null)) {
					conditions.add(parseCondition(condition));
				}
			}
			List<QuestAction> actions = new ArrayList<>();
			Element actionsElement = child(transition, "actions");
			if (actionsElement != null) {
				for (Element action : children(actionsElement, null)) {
					actions.add(parseAction(action));
				}
			}
			List<AfterCommitAction> afterCommit = new ArrayList<>();
			Element afterElement = child(transition, "after-commit");
			if (afterElement != null) {
				for (Element action : children(afterElement, null)) {
					afterCommit.add(parseAfterCommitAction(action));
				}
			}
			Integer priority = transition.hasAttribute("priority") ? integer(transition, "priority") : null;
			String source = transition.hasAttribute("source") ? attribute(transition, "source") : null;
			QuestShadowCoverageRequirement shadowCoverage = transition.hasAttribute("shadow-coverage")
				? enumValue(QuestShadowCoverageRequirement.class, transition, "shadow-coverage")
				: QuestShadowCoverageRequirement.PRODUCTION_REQUIRED;
			transitions.add(new QuestTransition(parsedEvent, conditions, actions, attribute(transition, "target"),
				afterCommit, priority, source, shadowCoverage));
		}
		return transitions;
	}

	private static QuestEvent parseEvent(Element element) {
		return switch (element.getTagName()) {
			case "talk-to-npc" -> parseTalkEvent(element);
			case "kill-npc" -> new QuestEvent.KillNpc(integer(element, "npc-id"));
			case "attack-npc" -> new QuestEvent.AttackNpc(integer(element, "npc-id"));
			case "use-item" -> new QuestEvent.UseItem(integer(element, "item-id"));
			case "collect-item" -> new QuestEvent.CollectItem(integer(element, "item-id"), integer(element, "count"));
			case "item-play" -> new QuestEvent.ItemPlay(integer(element, "item-id"), integer(element, "animation-millis"));
			case "house-item-use" -> new QuestEvent.HouseItemUse(integer(element, "item-id"));
			case "get-item" -> new QuestEvent.GetItem(integer(element, "item-id"));
			case "level-up" -> new QuestEvent.LevelUp();
			case "zone-mission-end" -> new QuestEvent.ZoneMissionEnd();
			case "die" -> new QuestEvent.Die();
			case "log-out" -> new QuestEvent.LogOut();
			case "abandon" -> new QuestEvent.Abandon();
			case "enter-world" -> new QuestEvent.EnterWorld();
			case "enter-zone" -> new QuestEvent.EnterZone(attribute(element, "zone"));
			case "leave-zone" -> new QuestEvent.LeaveZone(attribute(element, "zone"));
			case "pass-flying-ring" -> new QuestEvent.PassFlyingRing(attribute(element, "ring"));
			case "movie-end" -> new QuestEvent.MovieEnd(integer(element, "movie-id"));
			case "quest-timer-end" -> new QuestEvent.QuestTimerEnd();
			case "invisible-timer-end" -> new QuestEvent.InvisibleTimerEnd();
			case "kill-ranked" -> new QuestEvent.KillRanked(integer(element, "rank-id"));
			case "kill-in-world" -> new QuestEvent.KillInWorld(integer(element, "world-id"));
			case "use-skill" -> new QuestEvent.UseSkill(integer(element, "skill-id"));
			case "fail-craft" -> new QuestEvent.FailCraft(integer(element, "item-id"));
			case "equip-item" -> new QuestEvent.EquipItem(integer(element, "item-id"));
			case "can-act" -> new QuestEvent.CanAct(integer(element, "template-id"), attribute(element, "action-type"));
			case "dredgion-reward" -> new QuestEvent.DredgionReward();
			case "kamar-reward" -> new QuestEvent.KamarReward();
			case "ophidan-reward" -> new QuestEvent.OphidanReward();
			case "bastion-reward" -> new QuestEvent.BastionReward();
			case "bonus-apply" -> new QuestEvent.BonusApply(attribute(element, "bonus-type"));
			case "add-aggro-list" -> new QuestEvent.AddAggroList(integer(element, "npc-id"));
			case "at-distance" -> new QuestEvent.AtDistance(integer(element, "npc-id"));
			case "protect-end" -> new QuestEvent.ProtectEnd();
			case "protect-fail" -> new QuestEvent.ProtectFail();
			case "enter-wind-stream" -> new QuestEvent.EnterWindStream(integer(element, "teleport-id"));
			case "ride-action" -> new QuestEvent.RideAction(integer(element, "item-id"));
			case "creativity-point" -> new QuestEvent.CreativityPoint();
			case "npc-reach-target" -> new QuestEvent.NpcReachTarget();
			case "npc-lost-target" -> new QuestEvent.NpcLostTarget();
			default -> fail("UNKNOWN_EVENT", element.getTagName());
		};
	}

	private static QuestEvent parseTalkEvent(Element element) {
		if (element.hasAttribute("dialog") && element.hasAttribute("dialog-id")) {
			return fail("AMBIGUOUS_DIALOG_EVENT", "declare dialog or dialog-id, not both");
		}
		Integer dialogId = null;
		if (element.hasAttribute("dialog")) {
			dialogId = enumValue(QuestDialog.class, element, "dialog").id();
		} else if (element.hasAttribute("dialog-id")) {
			dialogId = integer(element, "dialog-id");
		}
		return new QuestEvent.TalkToNpc(integer(element, "npc-id"), dialogId);
	}

	private static QuestCondition parseCondition(Element element) {
		return switch (element.getTagName()) {
			case "status-is" -> new QuestCondition.StatusIs(enumValue(QuestStatus.class, element, "status"));
			case "has-item" -> new QuestCondition.HasItem(integer(element, "item-id"), integer(element, "count"));
			case "variable-is" -> new QuestCondition.QuestVariableIs(attribute(element, "field"), integer(element, "value"));
			case "recipe-known" -> new QuestCondition.RecipeKnown(integer(element, "recipe-id"),
				booleanOrDefault(element, "expected", false));
			case "can-grant-craft-skill" -> new QuestCondition.CanGrantCraftSkill(
				integer(element, "skill-id"), integer(element, "target-level"));
			case "pvp-victim-level-delta" -> new QuestCondition.PvpVictimLevelDelta(
				integer(element, "minimum"), integer(element, "maximum"));
			case "pvp-recipient-in-zone" -> new QuestCondition.PvpRecipientInZone(
				attribute(element, "zone"));
			case "start-eligible" -> new QuestCondition.StartEligible();
			default -> fail("UNKNOWN_CONDITION", element.getTagName());
		};
	}

	private static QuestAction parseAction(Element element) {
		return switch (element.getTagName()) {
			case "remove-item" -> new QuestAction.RemoveItem(integer(element, "item-id"), integer(element, "count"));
			case "set-variable" -> new QuestAction.SetVariable(attribute(element, "field"), integer(element, "value"));
			case "set-status" -> new QuestAction.SetStatus(enumValue(QuestStatus.class, element, "status"));
			case "grant-reward" -> parseGrantReward(element);
			case "complete-quest" -> new QuestAction.CompleteQuest(integer(element, "reward-index"));
			case "learn-recipe" -> new QuestAction.LearnRecipe(integer(element, "recipe-id"),
				enumValue(QuestRecipeOwnership.class, element, "ownership"));
			case "forget-recipe" -> new QuestAction.ForgetRecipe(integer(element, "recipe-id"));
			case "grant-craft-skill" -> new QuestAction.GrantCraftSkill(integer(element, "skill-id"),
				integer(element, "target-level"), booleanOrDefault(element, "auto-learn-recipes", false));
			default -> fail("UNKNOWN_ACTION", element.getTagName());
		};
	}

	private static AfterCommitAction parseAfterCommitAction(Element action) {
		return switch (action.getTagName()) {
			case "close-dialog" -> new AfterCommitAction.CloseDialog();
			case "sync-quest-state" -> new AfterCommitAction.SyncQuestState(
				enumValue(QuestStateSyncMode.class, action, "mode"));
			case "refresh-player-stats" -> new AfterCommitAction.RefreshPlayerStats();
			case "show-quest-dialog" -> new AfterCommitAction.ShowQuestDialog(integer(action, "dialog-id"));
			case "show-quest-selection-dialog" -> new AfterCommitAction.ShowQuestSelectionDialog(
				integer(action, "dialog-id"));
			case "teleport-player-current-or-default" -> new AfterCommitAction.TeleportPlayer(
				QuestInstanceTarget.currentOrDefault(), integer(action, "world-id"),
				floatValue(action, "x"), floatValue(action, "y"), floatValue(action, "z"),
				byteValue(action, "heading"));
			case "teleport-player-fixed-instance" -> new AfterCommitAction.TeleportPlayer(
				QuestInstanceTarget.fixed(integer(action, "instance-id")), integer(action, "world-id"),
				floatValue(action, "x"), floatValue(action, "y"), floatValue(action, "z"),
				byteValue(action, "heading"));
			case "play-movie" -> new AfterCommitAction.PlayMovie(integer(action, "movie-id"));
			case "spawn-npc-current-or-default" -> new AfterCommitAction.SpawnNpc(attribute(action, "slot"),
				integer(action, "template-id"), new QuestSpawnLocation.Fixed(integer(action, "world-id"),
					QuestInstanceTarget.currentOrDefault(),
					floatValue(action, "x"), floatValue(action, "y"), floatValue(action, "z"),
					byteValue(action, "heading")));
			case "spawn-npc-fixed-instance" -> new AfterCommitAction.SpawnNpc(attribute(action, "slot"),
				integer(action, "template-id"), new QuestSpawnLocation.Fixed(integer(action, "world-id"),
					QuestInstanceTarget.fixed(integer(action, "instance-id")),
					floatValue(action, "x"), floatValue(action, "y"), floatValue(action, "z"),
					byteValue(action, "heading")));
			case "spawn-npc-at-player" -> new AfterCommitAction.SpawnNpc(attribute(action, "slot"),
				integer(action, "template-id"), new QuestSpawnLocation.PlayerPosition(byteValue(action, "heading")));
			case "despawn-npc" -> new AfterCommitAction.DespawnNpc(attribute(action, "slot"));
			case "start-follow" -> new AfterCommitAction.StartFollow(attribute(action, "slot"));
			case "stop-follow" -> new AfterCommitAction.StopFollow(attribute(action, "slot"));
			case "attack-target" -> new AfterCommitAction.AttackTarget(attribute(action, "slot"));
			case "start-walking" -> new AfterCommitAction.StartWalking(attribute(action, "slot"));
			case "broadcast-npc-emotion" -> new AfterCommitAction.BroadcastNpcEmotion(
				attribute(action, "slot"), enumValue(QuestNpcEmotion.class, action, "emotion"));
			case "watch-follow-zone" -> new AfterCommitAction.WatchFollowZone(
				attribute(action, "slot"), attribute(action, "zone"));
			case "start-quest-timer" -> new AfterCommitAction.StartQuestTimer(
				integer(action, "seconds"), parseTimerPolicy(action));
			case "start-invisible-timer" -> new AfterCommitAction.StartInvisibleTimer(
				integer(action, "seconds"), parseTimerPolicy(action));
			case "cancel-quest-timer" -> new AfterCommitAction.CancelQuestTimer(
				new QuestTimerPolicy.Identity(attribute(action, "timer-id"),
					enumValue(QuestTimerPolicy.Scope.class, action, "scope")));
			default -> fail("UNKNOWN_AFTER_COMMIT_ACTION", action.getTagName());
		};
	}

	private static QuestTimerPolicy parseTimerPolicy(Element action) {
		return new QuestTimerPolicy(
			new QuestTimerPolicy.Identity(attribute(action, "timer-id"),
				enumValue(QuestTimerPolicy.Scope.class, action, "scope")),
			enumValue(QuestTimerPolicy.Persistence.class, action, "persistence"),
			enumValue(QuestTimerPolicy.OverwritePolicy.class, action, "overwrite"),
			enumValue(QuestTimerPolicy.Delivery.class, action, "delivery"));
	}

	private static QuestAction parseGrantReward(Element element) {
		String kind = attribute(element, "kind");
		try {
			QuestRewardKind.fromWire(kind);
		} catch (IllegalArgumentException e) {
			return fail("UNKNOWN_REWARD_KIND", kind);
		}
		QuestRewardAmountMode mode = element.hasAttribute("amount-mode")
			? enumValue(QuestRewardAmountMode.class, element, "amount-mode")
			: QuestRewardAmountMode.EXACT;
		return new QuestAction.GrantReward(kind, integer(element, "id"), longInteger(element, "amount"), mode);
	}

	private static Element requiredChild(Element parent, String name) {
		Element element = child(parent, name);
		if (element == null) {
			fail("MISSING_ELEMENT", name);
		}
		return element;
	}

	private static Element onlyChild(Element parent) {
		List<Element> elements = children(parent, null);
		if (elements.size() != 1) {
			return fail("INVALID_ELEMENT_COUNT", parent.getTagName() + " requires exactly one typed child");
		}
		return elements.get(0);
	}

	private static Element child(Element parent, String name) {
		for (Element element : children(parent, null)) {
			if (name.equals(element.getTagName())) {
				return element;
			}
		}
		return null;
	}

	private static List<Element> children(Element parent, String name) {
		List<Element> result = new ArrayList<>();
		if (parent == null) {
			return result;
		}
		NodeList childNodes = parent.getChildNodes();
		for (int index = 0; index < childNodes.getLength(); index++) {
			Node node = childNodes.item(index);
			if (node instanceof Element element && (name == null || name.equals(element.getTagName()))) {
				result.add(element);
			}
		}
		return result;
	}

	private static String attribute(Element element, String name) {
		if (!element.hasAttribute(name) || element.getAttribute(name).isBlank()) {
			fail("MISSING_ATTRIBUTE", element.getTagName() + "." + name);
		}
		return element.getAttribute(name);
	}

	private static String attributeOrDefault(Element element, String name, String defaultValue) {
		return element.hasAttribute(name) && !element.getAttribute(name).isBlank() ? element.getAttribute(name) : defaultValue;
	}

	private static int integer(Element element, String name) {
		try {
			return Integer.parseInt(attribute(element, name));
		} catch (NumberFormatException e) {
			fail("INVALID_INTEGER", element.getTagName() + "." + name);
			return 0;
		}
	}

	private static int integerOrDefault(Element element, String name, int defaultValue) {
		return element.hasAttribute(name) && !element.getAttribute(name).isBlank() ? integer(element, name) : defaultValue;
	}

	private static Integer nullableInteger(Element element, String name) {
		return element.hasAttribute(name) && !element.getAttribute(name).isBlank() ? integer(element, name) : null;
	}

	private static long longInteger(Element element, String name) {
		try {
			return Long.parseLong(attribute(element, name));
		} catch (NumberFormatException e) {
			fail("INVALID_INTEGER", element.getTagName() + "." + name);
			return 0;
		}
	}

	private static float floatValue(Element element, String name) {
		try {
			return Float.parseFloat(attribute(element, name));
		} catch (NumberFormatException e) {
			fail("INVALID_FLOAT", element.getTagName() + "." + name);
			return 0;
		}
	}

	private static byte byteValue(Element element, String name) {
		try {
			return Byte.parseByte(attribute(element, name));
		} catch (NumberFormatException e) {
			fail("INVALID_BYTE", element.getTagName() + "." + name);
			return 0;
		}
	}

	private static boolean bool(Element element, String name) {
		String value = attribute(element, name);
		if (!"true".equals(value) && !"false".equals(value)) {
			fail("INVALID_BOOLEAN", element.getTagName() + "." + name);
		}
		return Boolean.parseBoolean(value);
	}

	private static boolean booleanOrDefault(Element element, String name, boolean defaultValue) {
		return element.hasAttribute(name) && !element.getAttribute(name).isBlank() ? bool(element, name) : defaultValue;
	}

	private static <T extends Enum<T>> T enumValue(Class<T> type, Element element, String name) {
		try {
			return Enum.valueOf(type, attribute(element, name).toUpperCase().replace('-', '_'));
		} catch (IllegalArgumentException e) {
			fail("INVALID_ENUM", element.getTagName() + "." + name);
			return null;
		}
	}

	private static <T> T fail(String code, String message) {
		throw new QuestCompilationException(code, message);
	}
}
