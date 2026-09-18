package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 任务影片循环、重复对话及伪推进缺陷的全局回归门禁。
 * Global regression gate for movie playback loops, repeated dialog deadlocks, and progress stalls.
 */
class QuestMovieAndDialogLoopRegressionTest {

	@Test
	void barunaLaboratoryMovieLoopsAdvanceToStep1() throws Exception {
		// 16942 天族巴鲁纳次元研究所
		QuestDefinition def16942 = definition(16942).definition();
		assertNode(def16942, "s1", Map.of("var0", 1));
		QuestTransition trans16942 = def16942.transitions().stream()
			.filter(t -> "started".equals(t.sourceNode()) && "s1".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 206361
				&& Integer.valueOf(QuestDialogAction.SETPRO1.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow();
		assertTrue(trans16942.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable set && "var0".equals(set.field()) && set.value() == 1));
		assertTrue(trans16942.afterCommit().stream().anyMatch(a -> a instanceof AfterCommitAction.PlayMovie movie && movie.movieId() == 899));

		// 26942 魔族巴鲁纳次元研究所
		QuestDefinition def26942 = definition(26942).definition();
		assertNode(def26942, "s1", Map.of("var0", 1));
		QuestTransition trans26942 = def26942.transitions().stream()
			.filter(t -> "started".equals(t.sourceNode()) && "s1".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 206362
				&& Integer.valueOf(QuestDialogAction.SETPRO1.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow();
		assertTrue(trans26942.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable set && "var0".equals(set.field()) && set.value() == 1));
		assertTrue(trans26942.afterCommit().stream().anyMatch(a -> a instanceof AfterCommitAction.PlayMovie movie && movie.movieId() == 900));
	}

	@Test
	void quest14047OnlyPlaysMovieOnActualKillStage() throws Exception {
		QuestDefinition def14047 = definition(14047).definition();
		// 非目标击杀阶段禁止错配 214599 循环播放电影 422
		for (String invalidNode : List.of("started", "s1", "s2", "s3", "s4", "s6")) {
			boolean hasBadKill = def14047.transitions().stream()
				.anyMatch(t -> invalidNode.equals(t.sourceNode())
					&& ((t.event() instanceof QuestEvent.KillNpc kill && kill.npcId() == 214599)
					|| (t.event() instanceof QuestEvent.KillNpcSet killSet && killSet.npcIds().contains(214599))));
			assertFalse(hasBadKill, "Quest 14047 must not trigger kill-npc 214599 on node " + invalidNode);
		}
		// 目标阶段 s5 -> s6 正确推进并播放电影 422
		QuestTransition validKill = def14047.transitions().stream()
			.filter(t -> "s5".equals(t.sourceNode()) && "s6".equals(t.targetNode())
				&& ((t.event() instanceof QuestEvent.KillNpc kill && kill.npcId() == 214599)
				|| (t.event() instanceof QuestEvent.KillNpcSet killSet && killSet.npcIds().contains(214599))))
			.findFirst().orElseThrow();
		assertTrue(validKill.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable set && "var0".equals(set.field()) && set.value() == 6));
		assertTrue(validKill.afterCommit().stream().anyMatch(a -> a instanceof AfterCommitAction.PlayMovie movie && movie.movieId() == 422));
	}

	@Test
	void quest14112AdvancesToArmedOnSetpro1() throws Exception {
		QuestDefinition def = definition(14112).definition();
		QuestTransition talk = def.transitions().stream()
			.filter(t -> "started".equals(t.sourceNode()) && "armed".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc ttn && ttn.npcId() == 203148
				&& Integer.valueOf(QuestDialogAction.SETPRO1.id()).equals(ttn.dialogId()))
			.findFirst().orElseThrow();
		assertTrue(talk.actions().stream().anyMatch(a -> a instanceof QuestAction.GiveItem gi && gi.itemId() == 182215455));
		assertTrue(talk.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable sv && "var5".equals(sv.field()) && sv.value() == 0));
	}

	@Test
	void quest24155AdvancesToArmedOnSetpro2() throws Exception {
		QuestDefinition def = definition(24155).definition();
		QuestTransition talk = def.transitions().stream()
			.filter(t -> "started".equals(t.sourceNode()) && "armed".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc ttn && ttn.npcId() == 204785
				&& Integer.valueOf(QuestDialogAction.SETPRO2.id()).equals(ttn.dialogId()))
			.findFirst().orElseThrow();
		assertTrue(talk.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable sv && "var5".equals(sv.field()) && sv.value() == 0));
	}

	@Test
	void quest28301GrantsDeviceAndAdvancesToReward() throws Exception {
		QuestDefinition def = definition(28301).definition();
		for (int npcId : List.of(799530, 730373, 730374)) {
			QuestTransition pickUp = def.transitions().stream()
				.filter(t -> "k7".equals(t.sourceNode()) && "reward".equals(t.targetNode())
					&& t.event() instanceof QuestEvent.TalkToNpc ttn && ttn.npcId() == npcId
					&& Integer.valueOf(QuestDialogAction.SETPRO2.id()).equals(ttn.dialogId()))
				.findFirst().orElseThrow();
			assertTrue(pickUp.actions().stream().anyMatch(a -> a instanceof QuestAction.GiveItem gi && gi.itemId() == 182212110));
			assertTrue(pickUp.afterCommit().stream().anyMatch(a -> a instanceof AfterCommitAction.ShowQuestDialog sqd && sqd.dialogId() == QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()));
		}
	}

	@Test
	void quest50010SingleLineAcceptsAndAdvancesToV2() throws Exception {
		QuestDefinition def = definition(50010).definition();
		QuestTransition singleLine = def.transitions().stream()
			.filter(t -> "unaccepted".equals(t.sourceNode()) && "v2".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc ttn && ttn.npcId() == 202549
				&& Integer.valueOf(QuestDialogAction.SETPRO2.id()).equals(ttn.dialogId()))
			.findFirst().orElseThrow();
		assertTrue(singleLine.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable sv && "var0".equals(sv.field()) && sv.value() == 2));
	}

	@Test
	void quest15301And25301AcceptanceAdvancesToStarted() throws Exception {
		for (int qid : List.of(15301, 25301)) {
			QuestDefinition def = definition(qid).definition();
			int npcId = qid == 15301 ? 805327 : 805339;
			QuestTransition accept = def.transitions().stream()
				.filter(t -> "unaccepted".equals(t.sourceNode()) && "started".equals(t.targetNode())
					&& t.event() instanceof QuestEvent.TalkToNpc ttn && ttn.npcId() == npcId
					&& Integer.valueOf(QuestDialogAction.QUEST_ACCEPT_1.id()).equals(ttn.dialogId()))
				.findFirst().orElseThrow();
			assertTrue(accept.afterCommit().stream().anyMatch(a -> a instanceof AfterCommitAction.ShowQuestDialog sqd && sqd.dialogId() == QuestDialogPage.QUEST_ACCEPT_1.id()));
		}
	}

	@Test
	void quest1423AdvancesToRewardOnSetpro1() throws Exception {
		QuestDefinition def = definition(1423).definition();
		QuestTransition report = def.transitions().stream()
			.filter(t -> "started".equals(t.sourceNode()) && "reward".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc ttn && ttn.npcId() == 203983
				&& Integer.valueOf(QuestDialogAction.SETPRO1.id()).equals(ttn.dialogId()))
			.findFirst().orElseThrow();
		assertTrue(report.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable sv && "var0".equals(sv.field()) && sv.value() == 1));
	}

	@Test
	void executableQuestsHaveNoPureMovieLoops() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		java.util.List<String> pureLoops = new java.util.ArrayList<>();

		for (CompiledQuestDefinition compiled : catalog.executables()) {
			int qid = compiled.id();
			if (qid == 24053) {
				continue; // 24053 是已记录的旧 handler fallback 特例
			}
			QuestDefinition def = compiled.definition();
			boolean hasMovieEnd = def.transitions().stream().anyMatch(t -> t.event() instanceof QuestEvent.MovieEnd);

			for (QuestTransition t : def.transitions()) {
				if (t.sourceNode() == null || t.targetNode() == null) {
					continue;
				}
				if (!t.sourceNode().equals(t.targetNode())) {
					continue;
				}
				if (!t.actions().isEmpty()) {
					continue;
				}
				if (hasMovieEnd) {
					continue;
				}
				boolean playsMovie = t.afterCommit().stream().anyMatch(AfterCommitAction.PlayMovie.class::isInstance);
				if (!playsMovie) {
					continue;
				}
				boolean showsPage = t.afterCommit().stream().anyMatch(AfterCommitAction.ShowQuestDialog.class::isInstance);
				if (!showsPage) {
					pureLoops.add("Quest " + qid + " node " + t.sourceNode() + " has pure movie loop without continuation or progress");
				}
			}
		}

		assertTrue(pureLoops.isEmpty(), "Pure movie loops found: " + pureLoops);
	}

	@Test
	void fatalImpossibleDropStepsAreEliminated() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		java.util.List<String> violations = new java.util.ArrayList<>();

		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition def = compiled.definition();
			java.util.Set<Integer> validVars = new java.util.HashSet<>();
			for (QuestNode node : def.nodes()) {
				Integer val = node.projection().variables().get("var0");
				if (val != null) {
					validVars.add(val);
				}
			}

			for (QuestDrop drop : def.metadata().drops()) {
				int step = drop.collectingStep();
				if (step != 0 && !validVars.contains(step)) {
					violations.add("Quest " + compiled.id() + " drop npc=" + drop.npcId() + " step=" + step + " not in " + validVars);
				}
			}
		}

		assertTrue(violations.isEmpty(), "Impossible drop collecting-steps found: " + violations);
	}

	@Test
	void quests2372And4907And24202And24203DropsStepCorrected() throws Exception {
		for (int qid : List.of(2372, 4907, 24202, 24203)) {
			QuestDefinition def = definition(qid).definition();
			assertTrue(def.metadata().drops().stream().allMatch(d -> d.collectingStep() == 0),
				"Quest " + qid + " drops must have collectingStep=0");
		}
	}

	@Test
	void multiTierQuestsNeverDeclareDeadRewardGroups() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		java.util.List<String> violations = new java.util.ArrayList<>();

		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition def = compiled.definition();
			List<QuestRewardGroup> groups = def.metadata().rewardGroups();
			if (groups.size() <= 1) {
				continue;
			}
			java.util.Set<Integer> covered = new java.util.TreeSet<>();
			for (QuestTransition t : def.transitions()) {
				boolean completes = t.actions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance);
				if (!completes) {
					continue;
				}
				for (QuestAction action : t.actions()) {
					if (action instanceof QuestAction.CompleteQuest completion) {
						covered.add(completion.rewardIndex());
					}
				}
				List<String> inlineGrants = t.actions().stream()
					.filter(QuestAction.GrantReward.class::isInstance)
					.map(QuestAction.GrantReward.class::cast)
					.map(grant -> grant.kind() + "|" + grant.id() + "|" + grant.amount())
					.sorted()
					.toList();
				if (!inlineGrants.isEmpty()) {
					for (int index = 0; index < groups.size(); index++) {
						if (groupSignature(groups.get(index)).equals(inlineGrants)) {
							covered.add(index);
						}
					}
				}
			}
			for (int index = 0; index < groups.size(); index++) {
				if (!covered.contains(index)) {
					violations.add("Quest " + compiled.id() + " declares " + groups.size()
						+ " reward groups but tier " + index + " is never granted (covered=" + covered + ")");
				}
			}
		}

		assertTrue(violations.isEmpty(), "Dead reward groups found: " + violations);
	}

	@Test
	void startedQuestTimersHaveCancelOrExpiryRoute() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		java.util.List<String> violations = new java.util.ArrayList<>();

		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition def = compiled.definition();
			java.util.Set<String> cancelled = new java.util.HashSet<>();
			java.util.List<String> startedVisible = new java.util.ArrayList<>();
			java.util.List<String> startedInvisible = new java.util.ArrayList<>();
			boolean endsVisible = false;
			boolean endsInvisible = false;

			for (QuestTransition t : def.transitions()) {
				if (t.event() instanceof QuestEvent.QuestTimerEnd) {
					endsVisible = true;
				} else if (t.event() instanceof QuestEvent.InvisibleTimerEnd) {
					endsInvisible = true;
				}
				for (AfterCommitAction after : t.afterCommit()) {
					if (after instanceof AfterCommitAction.CancelQuestTimer cancel) {
						cancelled.add(cancel.identity().timerId());
					} else if (after instanceof AfterCommitAction.StartQuestTimer start) {
						startedVisible.add(start.policy().identity().timerId());
					} else if (after instanceof AfterCommitAction.StartInvisibleTimer start) {
						startedInvisible.add(start.policy().identity().timerId());
					}
				}
			}

			for (String timerId : startedVisible) {
				if (!cancelled.contains(timerId) && !endsVisible) {
					violations.add("Quest " + compiled.id() + " starts visible timer '" + timerId
						+ "' without a cancel action or quest-timer-end route");
				}
			}
			for (String timerId : startedInvisible) {
				if (!cancelled.contains(timerId) && !endsInvisible) {
					violations.add("Quest " + compiled.id() + " starts invisible timer '" + timerId
						+ "' without a cancel action or invisible-timer-end route");
				}
			}
		}

		assertTrue(violations.isEmpty(), "Unterminated quest timers found: " + violations);
	}

	@Test
	void quest2230StopsWagerCountdownOnHandIn() throws Exception {
		QuestDefinition def = definition(2230).definition();
		QuestTransition handIn = def.transitions().stream()
			.filter(t -> "started".equals(t.sourceNode()) && "reward".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow();
		assertTrue(handIn.afterCommit().stream().anyMatch(after ->
			after instanceof AfterCommitAction.CancelQuestTimer cancel
				&& QuestTimerPolicy.VISIBLE_TIMER_ID.equals(cancel.identity().timerId())),
			"Quest 2230 must stop the 1800s visible wager timer once the fangs are handed in");
	}

	@Test
	void rewardWindowTierMappingCoversEveryClientWindow() {
		int[] clientPages = {5, 6, 7, 8, 45, 46};
		for (int tier = 0; tier < clientPages.length; tier++) {
			assertEquals(clientPages[tier], QuestDialogPage.rewardWindowForTier(tier).orElseThrow().id(),
				"tier " + tier + " must map to client reward window page " + clientPages[tier]);
		}
		assertTrue(QuestDialogPage.rewardWindowForTier(6).isEmpty(),
			"the client declares exactly six reward windows; tier 7 has no page");
		assertTrue(QuestDialogPage.rewardWindowForTier(-1).isEmpty());
	}

	@Test
	void synthesisedAndDeclaredPreviewUseTheTableDrivenTierPage() throws Exception {
		// 未显式声明预览时由编译器合成：第 5 档必须是客户端页面 45，而不是线性推算出的 9。
		// Synthesised previews must use the tier table: tier 5 is client page 45, never 5 + 4 = 9.
		assertRewardPreviewPages(compileTierFixture(4, false).definition(), 45);
		// npc-complete 显式声明预览时同样按 complete-reward-index 取页。
		// A declared npc-complete preview must use its own complete-reward-index page.
		assertRewardPreviewPages(compileTierFixture(1, true).definition(), 6);
	}

	@Test
	void npcCompletePreviewsOpenTheirOwnTierWindowAcrossTheCatalog() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		var builder = factory.newDocumentBuilder();
		java.util.List<String> violations = new java.util.ArrayList<>();
		int previewedBlocks = 0;
		try (var files = Files.newDirectoryStream(
				Path.of("src/main/resources/aion/data/static_data/quest_definition/quests"), "*.xml")) {
			for (Path file : files) {
				var document = builder.parse(file.toFile());
				org.w3c.dom.NodeList blocks = document.getElementsByTagName("npc-complete");
				if (blocks.getLength() == 0) {
					continue;
				}
				int questId = Integer.parseInt(document.getDocumentElement().getAttribute("id"));
				java.util.Optional<CompiledQuestDefinition> compiled = catalog.findExecutable(questId);
				if (compiled.isEmpty()) {
					continue;
				}
				QuestDefinition definition = compiled.get().definition();
				for (int index = 0; index < blocks.getLength(); index++) {
					Element block = (Element) blocks.item(index);
					boolean previewed = block.getElementsByTagName("preview").getLength() > 0
						|| !block.getAttribute("preview-dialog-ids").isBlank();
					if (!previewed) {
						continue;
					}
					previewedBlocks++;
					int tier = Integer.parseInt(block.getAttribute("complete-reward-index"));
					int npcId = Integer.parseInt(block.getAttribute("npc-id"));
					String source = block.getAttribute("source");
					int expectedPage = QuestDialogPage.rewardWindowForTier(tier)
						.orElseThrow(() -> new AssertionError("tier " + tier + " has no client reward window"))
						.id();
					boolean routed = definition.transitions().stream().anyMatch(transition ->
						source.equals(transition.sourceNode())
							&& transition.event() instanceof QuestEvent.TalkToNpc talk
							&& talk.npcId() == npcId
							&& (Integer.valueOf(-1).equals(talk.dialogId())
								|| Integer.valueOf(QuestDialogAction.SELECT_QUEST_REWARD.id()).equals(talk.dialogId()))
							&& transition.afterCommit().stream().anyMatch(after ->
								after instanceof AfterCommitAction.ShowQuestDialog page
									&& page.dialogId() == expectedPage));
					if (!routed) {
						violations.add("Quest " + questId + " npc-complete(source=" + source + ", npc=" + npcId
							+ ") tier " + tier + " preview must open page " + expectedPage);
					}
				}
			}
		}
		assertTrue(previewedBlocks > 0, "expected at least one previewed npc-complete block");
		assertTrue(violations.isEmpty(),
			"npc-complete previews opening another tier's client window: " + violations);
	}

	@Test
	void multiTierHandInWindowsMatchTheirGrantedTier() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		java.util.List<String> violations = new java.util.ArrayList<>();

		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition definition = compiled.definition();
			List<QuestRewardGroup> groups = definition.metadata().rewardGroups();
			if (groups.size() < 2) {
				continue;
			}
			java.util.Map<String, java.util.Set<Integer>> entryPages = new java.util.HashMap<>();
			java.util.Map<String, java.util.Set<Integer>> inlineTiers = new java.util.HashMap<>();
			java.util.Map<String, java.util.Set<Integer>> persistedTiers = new java.util.HashMap<>();
			for (QuestTransition transition : definition.transitions()) {
				if (transition.targetNode() != null) {
					for (AfterCommitAction after : transition.afterCommit()) {
						if (after instanceof AfterCommitAction.ShowQuestDialog page
								&& rewardTierOf(page.dialogId()) >= 0) {
							entryPages.computeIfAbsent(transition.targetNode(), ignored -> new java.util.TreeSet<>())
								.add(page.dialogId());
						}
					}
				}
				if (transition.sourceNode() == null) {
					continue;
				}
				List<QuestAction.GrantReward> grants = transition.actions().stream()
					.filter(QuestAction.GrantReward.class::isInstance)
					.map(QuestAction.GrantReward.class::cast)
					.toList();
				if (!grants.isEmpty()) {
					List<String> signature = grants.stream()
						.map(grant -> grant.kind() + "|" + grant.id() + "|" + grant.amount())
						.sorted()
						.toList();
					for (int index = 0; index < groups.size(); index++) {
						if (groupSignature(groups.get(index)).equals(signature)) {
							inlineTiers.computeIfAbsent(transition.sourceNode(), ignored -> new java.util.TreeSet<>())
								.add(index);
						}
					}
				}
				for (QuestAction action : transition.actions()) {
					if (action instanceof QuestAction.CompleteQuest completion) {
						persistedTiers.computeIfAbsent(transition.sourceNode(), ignored -> new java.util.TreeSet<>())
							.add(completion.rewardIndex());
					}
				}
			}
			for (java.util.Map.Entry<String, java.util.Set<Integer>> entry : entryPages.entrySet()) {
				if (entry.getValue().size() != 1) {
					continue;
				}
				int entryPage = entry.getValue().iterator().next();
				java.util.Set<Integer> granted = inlineTiers.getOrDefault(entry.getKey(), java.util.Set.of());
				if (granted.isEmpty()) {
					granted = persistedTiers.getOrDefault(entry.getKey(), java.util.Set.of());
				}
				if (granted.size() != 1) {
					continue;
				}
				int tier = granted.iterator().next();
				java.util.Optional<QuestDialogPage> window = QuestDialogPage.rewardWindowForTier(tier);
				if (window.isPresent() && window.get().id() != entryPage) {
					violations.add("Quest " + compiled.id() + " reward node " + entry.getKey()
						+ " opens page " + entryPage + " but grants tier " + (tier + 1)
						+ " (page " + window.get().id() + ")");
				}
			}
		}
		assertTrue(violations.isEmpty(), "Reward windows and granted tiers diverge: " + violations);
	}

	private static int rewardTierOf(int pageId) {
		for (int tier = 0; tier < 6; tier++) {
			if (QuestDialogPage.rewardWindowForTier(tier).orElseThrow().id() == pageId) {
				return tier;
			}
		}
		return -1;
	}

	private static void assertRewardPreviewPages(QuestDefinition definition, int expectedPage) {
		List<QuestTransition> previews = definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& (Integer.valueOf(-1).equals(talk.dialogId())
					|| Integer.valueOf(QuestDialogAction.SELECT_QUEST_REWARD.id()).equals(talk.dialogId())))
			.toList();
		assertFalse(previews.isEmpty(), "expected a reward preview route");
		for (QuestTransition preview : previews) {
			assertTrue(preview.afterCommit().stream().anyMatch(after ->
					after instanceof AfterCommitAction.ShowQuestDialog page && page.dialogId() == expectedPage),
				"reward preview " + preview.event() + " must open page " + expectedPage
					+ " but was " + preview.afterCommit());
		}
	}

	private static CompiledQuestDefinition compileTierFixture(int tier, boolean declaredPreview) throws Exception {
		StringBuilder groups = new StringBuilder();
		for (int index = 0; index <= tier; index++) {
			groups.append("      <group><reward kind=\"ITEM\" id=\"18805").append(100 + index)
				.append("\" amount=\"1\"/></group>\n");
		}
		String preview = declaredPreview
			? "      <preview actions=\"USE_OBJECT SELECT_QUEST_REWARD\"/>\n" : "";
		String xml = """
			<quest-definition id="990777" version="1">
			  <metadata name="tier" display-name-id="0" min-level="1" max-level="2147483647" category="QUEST">
			    <races><race id="ELYOS"/></races>
			    <reward-groups>
			%s    </reward-groups>
			  </metadata>
			  <progress>
			    <bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
			  </progress>
			  <nodes>
			    <node label="unaccepted" status="NONE"><var name="var0" value="0"/></node>
			    <node label="started" status="START"><var name="var0" value="0"/></node>
			    <node label="reward" status="REWARD"><var name="var0" value="0"/></node>
			    <node label="complete" status="COMPLETE"><var name="var0" value="0"/></node>
			  </nodes>
			  <transitions>
			    <transition source="unaccepted" target="started">
			      <event><dialog type="TALK_TO_NPC" npc-id="700001" action="QUEST_ACCEPT_SIMPLE"/></event>
			      <conditions><start-eligible/></conditions>
			      <after-commit><close-dialog/></after-commit>
			    </transition>
			    <transition source="started" target="reward">
			      <event><kill-npc npc-id="210001"/></event>
			      <after-commit><sync-quest-state mode="PACKET_ONLY"/></after-commit>
			    </transition>
			    <npc-complete npc-id="700001" source="reward" target="complete" complete-reward-index="%d"
			        actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" finish="SELECTION_DIALOG">
			%s    </npc-complete>
			  </transitions>
			</quest-definition>
			""".formatted(groups.toString(), tier, preview);
		return QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	private static List<String> groupSignature(QuestRewardGroup group) {
		return group.rewards().stream()
			.map(reward -> reward.kind() + "|" + reward.id() + "|" + reward.amount())
			.sorted()
			.toList();
	}

	@Test
	void quest50023TiersUseTheirOwnClientRewardWindow() throws Exception {
		QuestDefinition def = definition(50023).definition();
		assertEquals(2, def.metadata().rewardGroups().size(), "Quest 50023 must declare 2 reward groups");
		assertEquals(188051780, def.metadata().rewardGroups().get(0).rewards().get(0).id());
		assertEquals(188051782, def.metadata().rewardGroups().get(1).rewards().get(0).id());

		// 档位 1（1 个线索）展示 select_quest_reward1；档位 2（3 个线索）展示 select_quest_reward2。
		// Tier 1 (1 clue) opens select_quest_reward1; tier 2 (3 clues) opens select_quest_reward2.
		assertRewardWindow(def, QuestDialogAction.SELECTED_QUEST_REWARD1, 1, QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		assertRewardWindow(def, QuestDialogAction.SELECTED_QUEST_REWARD2, 3, QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW2);
	}

	private static void assertRewardWindow(QuestDefinition definition, QuestDialogAction action, int clueCount,
			QuestDialogPage expectedPage) {
		QuestTransition tier = definition.transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(action.id()).equals(talk.dialogId())
				&& candidate.conditions().stream().anyMatch(condition ->
					condition instanceof QuestCondition.HasItem hasItem && hasItem.count() == clueCount))
			.findFirst().orElseThrow(() -> new AssertionError("missing tier route for action " + action));
		assertTrue(tier.afterCommit().stream().anyMatch(after ->
			after instanceof AfterCommitAction.ShowQuestDialog page && page.dialogId() == expectedPage.id()),
			"tier route for " + action + " must open " + expectedPage);
	}

	private static void assertNode(QuestDefinition definition, String label, Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(QuestStatus.START, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private CompiledQuestDefinition definition(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = getClass().getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
