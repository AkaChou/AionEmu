package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestTitleRewardCoverageTest {
	private static final Path QUEST_DATA = Path.of(
		"src/main/resources/aion/data/static_data/quest_data/quest_data.xml");
	private static final Path PLAYER_TITLES = Path.of(
		"src/main/resources/aion/data/static_data/player_titles.xml");
	private static final Map<Integer, Integer> CLIENT_ONLY_TITLE_REWARDS = Map.of(
		18316, 207,
		18395, 113,
		28316, 218,
		28395, 132);

	@Test
	void catalogMatchesEveryKnownServerTitleQuest() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		TitleRewards expected = expectedTitleRewards();
		TitleRewards actual = catalogTitleRewards(catalog);

		assertEquals(179, expected.regular().size(), "unexpected regular title-owner count");
		assertEquals(3, expected.extended().size(), "unexpected extended title-owner count");
		assertEquals(182, ownerIds(expected).size(), "unexpected total title-owner count");
		assertEquals(expected.regular(), actual.regular(), "regular title rewards differ from server baseline");
		assertEquals(expected.extended(), actual.extended(), "extended title rewards differ from server baseline");
		assertEquals(174, expected.regular().keySet().stream()
			.filter(id -> catalog.findExecutable(id).isPresent()).count(),
			"unexpected executable regular title-owner count");
	}

	@Test
	void everyExecutableCompletionGrantsExactlyItsConfiguredRegularTitles() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> invalid = new ArrayList<>();

		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestMetadata metadata = compiled.definition().metadata();
			if (titleGrants(metadata.rewards()).isEmpty()) {
				continue;
			}
			List<QuestTransition> completions = compiled.definition().transitions().stream()
				.filter(transition -> completeAction(transition) != null).toList();
			if (completions.isEmpty()) {
				invalid.add(compiled.id() + ": no completion transition");
				continue;
			}
			for (QuestTransition completion : completions) {
				QuestAction.CompleteQuest complete = completeAction(completion);
				Set<TitleGrant> configured = titleGrants(rewardGroup(metadata, complete.rewardIndex()));
				Set<TitleGrant> granted = completion.actions().stream()
					.filter(QuestAction.GrantReward.class::isInstance)
					.map(QuestAction.GrantReward.class::cast)
					.filter(reward -> reward.rewardKind() == QuestRewardKind.TITLE)
					.map(reward -> new TitleGrant(reward.id(), reward.amount()))
					.collect(java.util.stream.Collectors.toSet());
				if (!configured.equals(granted)) {
					invalid.add(compiled.id() + ":" + completion.sourceNode() + "->"
						+ completion.targetNode() + " reward=" + complete.rewardIndex()
						+ " configured=" + configured + " granted=" + granted);
				}
			}
		}

		assertTrue(invalid.isEmpty(), "invalid title completion rewards: " + invalid);
	}

	@Test
	void everyConfiguredTitleExistsMatchesRaceAndHasValidRepeatSemantics() throws Exception {
		Map<Integer, String> titleRaces = titleRaces();
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> invalid = new ArrayList<>();

		for (QuestCatalogEntry entry : catalog.entries()) {
			QuestMetadata metadata = entry.metadata();
			for (QuestReward reward : allTitleRewards(metadata)) {
				String titleRace = titleRaces.get(reward.id());
				if (titleRace == null) {
					invalid.add(entry.id() + ": unknown title " + reward.id());
					continue;
				}
				Set<String> questRaces = metadata.permittedRaces();
				if (!"PC_ALL".equals(titleRace) && !questRaces.isEmpty()
						&& !questRaces.contains("PC_ALL") && !questRaces.contains(titleRace)) {
					invalid.add(entry.id() + ": title " + reward.id() + " race=" + titleRace
						+ " questRaces=" + questRaces);
				}
			}
			if (!titleGrants(metadata.extendedRewards()).isEmpty()
					&& metadata.repeatPolicy().rewardRepeatCount() <= 0) {
				invalid.add(entry.id() + ": extended title has no reward repeat count");
			}
		}

		assertTrue(invalid.isEmpty(), "invalid quest title rewards: " + invalid);
	}

	private static QuestAction.CompleteQuest completeAction(QuestTransition transition) {
		return transition.actions().stream().filter(QuestAction.CompleteQuest.class::isInstance)
			.map(QuestAction.CompleteQuest.class::cast).findFirst().orElse(null);
	}

	private static List<QuestReward> rewardGroup(QuestMetadata metadata, int rewardIndex) {
		List<QuestRewardGroup> groups = metadata.rewardGroups();
		if (groups.isEmpty()) {
			return List.of();
		}
		if (groups.size() == 1) {
			return groups.getFirst().rewards();
		}
		return rewardIndex < groups.size() ? groups.get(rewardIndex).rewards() : List.of();
	}

	private static Set<TitleGrant> titleGrants(List<QuestReward> rewards) {
		Set<TitleGrant> result = new HashSet<>();
		for (QuestReward reward : rewards) {
			if (isTitle(reward)) {
				result.add(new TitleGrant(reward.id(), reward.amount()));
			}
		}
		return Set.copyOf(result);
	}

	private static List<QuestReward> allTitleRewards(QuestMetadata metadata) {
		List<QuestReward> result = new ArrayList<>();
		metadata.rewards().stream().filter(QuestTitleRewardCoverageTest::isTitle).forEach(result::add);
		metadata.extendedRewards().stream().filter(QuestTitleRewardCoverageTest::isTitle).forEach(result::add);
		return result;
	}

	private static boolean isTitle(QuestReward reward) {
		return QuestRewardKind.fromWire(reward.kind()) == QuestRewardKind.TITLE;
	}

	private static TitleRewards catalogTitleRewards(QuestCatalog catalog) {
		Map<Integer, Set<Integer>> regular = new TreeMap<>();
		Map<Integer, Set<Integer>> extended = new TreeMap<>();
		for (QuestCatalogEntry entry : catalog.entries()) {
			putTitles(regular, entry.id(), entry.metadata().rewards());
			putTitles(extended, entry.id(), entry.metadata().extendedRewards());
		}
		return new TitleRewards(Map.copyOf(regular), Map.copyOf(extended));
	}

	private static void putTitles(Map<Integer, Set<Integer>> destination, int questId,
			List<QuestReward> rewards) {
		Set<Integer> titleIds = new HashSet<>();
		for (QuestReward reward : rewards) {
			if (isTitle(reward)) {
				titleIds.add(reward.id());
			}
		}
		if (!titleIds.isEmpty()) {
			destination.put(questId, Set.copyOf(titleIds));
		}
	}

	private static TitleRewards expectedTitleRewards() throws Exception {
		DocumentBuilderFactory factory = secureDocumentBuilderFactory();
		NodeList quests = factory.newDocumentBuilder().parse(QUEST_DATA.toFile()).getElementsByTagName("quest");
		Map<Integer, Set<Integer>> regular = new TreeMap<>();
		Map<Integer, Set<Integer>> extended = new TreeMap<>();
		for (int index = 0; index < quests.getLength(); index++) {
			Element quest = (Element) quests.item(index);
			int questId = Integer.parseInt(quest.getAttribute("id"));
			putXmlTitles(regular, questId, quest.getElementsByTagName("rewards"));
			putXmlTitles(extended, questId, quest.getElementsByTagName("extended_rewards"));
		}
		CLIENT_ONLY_TITLE_REWARDS.forEach((questId, titleId) -> regular.put(questId, Set.of(titleId)));
		return new TitleRewards(Map.copyOf(regular), Map.copyOf(extended));
	}

	private static void putXmlTitles(Map<Integer, Set<Integer>> destination, int questId,
			NodeList rewardGroups) {
		Set<Integer> titleIds = new HashSet<>();
		for (int index = 0; index < rewardGroups.getLength(); index++) {
			String title = ((Element) rewardGroups.item(index)).getAttribute("title");
			if (!title.isBlank() && Integer.parseInt(title) > 0) {
				titleIds.add(Integer.parseInt(title));
			}
		}
		if (!titleIds.isEmpty()) {
			destination.put(questId, Set.copyOf(titleIds));
		}
	}

	private static Set<Integer> ownerIds(TitleRewards rewards) {
		Set<Integer> result = new HashSet<>(rewards.regular().keySet());
		result.addAll(rewards.extended().keySet());
		return result;
	}

	private static Map<Integer, String> titleRaces() throws Exception {
		NodeList titles = secureDocumentBuilderFactory().newDocumentBuilder()
			.parse(PLAYER_TITLES.toFile()).getElementsByTagName("title");
		Map<Integer, String> result = new HashMap<>();
		for (int index = 0; index < titles.getLength(); index++) {
			Element title = (Element) titles.item(index);
			result.put(Integer.parseInt(title.getAttribute("id")), title.getAttribute("race"));
		}
		return result;
	}

	private static DocumentBuilderFactory secureDocumentBuilderFactory() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		return factory;
	}

	private record TitleRewards(Map<Integer, Set<Integer>> regular,
			Map<Integer, Set<Integer>> extended) {
	}

	private record TitleGrant(int id, long amount) {
	}
}
