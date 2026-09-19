package com.aionemu.gameserver.questEngine.e2e;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionDirectoryLoader;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定“上交确认页不得成为死端”的全库客户端合同：任务自身客户端 HTML 中
 * check_user_item_ok(10000) 页的唯一按钮是 HACTION_FINISH_DIALOG(1008)（本地关闭，不会回传任务动作）时，
 * 只要该 NPC 在目标节点本来还有续接页，上交分支就必须直接下发该续接页，
 * 不能停在 10000 页等玩家重新对话；REPAIRED_BRANCHES 记录本批已接线的分支作为正向锁定。
 * Locks the catalog-wide client contract that a hand-over confirmation page must not be a dead end:
 * when the quest's own client HTML renders the only check_user_item_ok(10000) button as
 * HACTION_FINISH_DIALOG(1008) - a local close that never returns a quest action - and the same NPC has a
 * continuation page in the target node, the hand-over branch must show that page directly instead of
 * stopping on page 10000 and forcing the player to re-open the dialogue. REPAIRED_BRANCHES records the
 * branches wired by this batch as a positive lock.
 */
class QuestHandoverContinuationAuditTest {
	private static final Path CLIENT_MAPPING = Path.of("docs/quest/client-dialog-mapping");
	private static final int CONFIRMATION_PAGE = QuestDialogPage.CHECK_USER_ITEM_OK.id();
	/** 客户端把 10000 页做成纯本地关闭的任务数下界；低于该值说明客户端表未装载。 / Lower bound of quests whose confirmation page is a local close. */
	private static final int MINIMUM_FAMILY_QUESTS = 100;

	/**
	 * 本批已接线的上交分支：quest / source / target / npc / 续接页。
	 * Hand-over branches wired by this batch: quest / source / target / npc / continuation page.
	 */
	private static final String REPAIRED_BRANCHES = """
			2372	started	reward	798079	5
			10504	s3	reward	804706	10002
			13968	started	reward	835217	5
			15689	started	reward	806696	5
			15690	started	reward	806696	5
			15691	started	reward	806696	5
			16838	started	reward	806566	5
			16838	k1	reward	806566	5
			18742	started	reward	804707	5
			18975	started	reward	805215	5
			18976	started	reward	805215	5
			18977	started	reward	805215	5
			18978	started	reward	805215	5
			19010	started	reward	203788	5
			19016	started	reward	203790	5
			19022	started	reward	203793	5
			19028	started	reward	203792	5
			19034	started	reward	203786	5
			21027	started	reward	799254	5
			23968	started	reward	835220	5
			25689	started	reward	806697	5
			25690	started	reward	806697	5
			25691	started	reward	806697	5
			26838	started	reward	806575	5
			28742	started	reward	804732	5
			28975	started	reward	805218	5
			28976	started	reward	805218	5
			28977	started	reward	805218	5
			28978	started	reward	805218	5
			29010	started	reward	204104	5
			29016	started	reward	204106	5
			29022	started	reward	204110	5
			29028	started	reward	204108	5
			29034	started	reward	204102	5
			80723	started	reward	833543	5
			80795	started	reward	833543	5
			80849	started	reward	833788	5
			80850	started	reward	833788	5
			80851	started	reward	833796	5
			80852	started	reward	833796	5
			80886	started	reward	834242	5
			80958	s1	reward	222002	5
			""";

	@Test
	void clientLocalCloseConfirmationPagesContinueInTheSameDialogue() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		List<String> violations = new ArrayList<>();
		int family = 0;
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition definition = compiled.definition();
			if (!HandoverContinuationContract.closesLocally(oracle, definition.id())) {
				continue;
			}
			family++;
			// 规则 1：本 NPC 对话本来还能续接时，任何仍下发 10000 页的分支都是死端。
			// Rule 1: while this NPC's dialogue has a continuation, any branch that still shows page 10000 is a dead end.
			for (QuestTransition transition : definition.transitions()) {
				if (!displays(transition, CONFIRMATION_PAGE)
					|| !(transition.event() instanceof QuestEvent.TalkToNpc talk)) {
					continue;
				}
				List<Integer> continuationPages = HandoverContinuationContract.sameNpcEntryPages(definition,
					transition.targetNode(), talk.npcId());
				if (continuationPages.isEmpty()) {
					// 该 NPC 的对话在此结束（下一步在别的 NPC、区域或道具），确认页保持终端页。
					// The dialogue really ends here (another NPC, zone or item comes next), so the
					// confirmation page legitimately stays terminal.
					continue;
				}
				violations.add("quest " + definition.id() + ": " + transition.sourceNode() + " -> "
					+ transition.targetNode() + " by NPC " + talk.npcId() + " still shows page "
					+ CONFIRMATION_PAGE + " although the same dialogue continues on " + continuationPages);
			}
		}
		// 规则 2：本批修复的分支必须仍然直接下发各自的续接页。
		// Rule 2: every branch repaired by this batch must still show its own continuation page.
		Map<Integer, List<RepairedBranch>> repaired = repairedBranches();
		int linked = 0;
		for (Map.Entry<Integer, List<RepairedBranch>> entry : repaired.entrySet()) {
			QuestDefinition definition = catalog.findExecutable(entry.getKey())
				.orElseThrow(() -> new IllegalStateException("missing quest " + entry.getKey()))
				.definition();
			for (RepairedBranch branch : entry.getValue()) {
				boolean wired = definition.transitions().stream()
					.filter(candidate -> branch.source().equals(candidate.sourceNode())
						&& branch.target().equals(candidate.targetNode()))
					.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.npcId() == branch.npcId())
					.anyMatch(candidate -> displays(candidate, branch.pageId()));
				if (wired) {
					linked++;
				} else {
					violations.add("quest " + entry.getKey() + ": " + branch.source() + " -> "
						+ branch.target() + " by NPC " + branch.npcId() + " must display continuation page "
						+ branch.pageId());
				}
			}
		}
		assertTrue(family >= MINIMUM_FAMILY_QUESTS,
			"the client mapping must expose the local-close confirmation family, family=" + family);
		assertEquals(repaired.values().stream().mapToInt(List::size).sum(), linked,
			"every repaired hand-over branch must stay wired");
		assertEquals(List.of(), violations,
			"client-local-close confirmation pages must continue in the same dialogue");
	}

	@Test
	void confirmationPageButtonsComeFromTheClientMapping() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(CLIENT_MAPPING);
		assertTrue(HandoverContinuationContract.closesLocally(oracle, 10501), "quest 10501 renders check_user_item_ok as a local close");
		assertTrue(HandoverContinuationContract.closesLocally(oracle, 10504), "quest 10504 renders check_user_item_ok as a local close");
		assertFalse(HandoverContinuationContract.closesLocally(oracle, 15010),
			"quest 15010 confirmation page opens the reward window itself and is not a local close");
		assertFalse(oracle.visibleActions(15010, CONFIRMATION_PAGE).isEmpty(),
			"quest 15010 confirmation page must stay in the client mapping");
	}

	private static boolean displays(QuestTransition transition, int pageId) {
		return transition.afterCommit().stream().anyMatch(action ->
			action instanceof AfterCommitAction.ShowQuestDialog show && show.dialogId() == pageId);
	}

	private static Map<Integer, List<RepairedBranch>> repairedBranches() {
		Map<Integer, List<RepairedBranch>> result = new LinkedHashMap<>();
		for (String line : REPAIRED_BRANCHES.strip().split("\n")) {
			String[] fields = line.strip().split("\t");
			if (fields.length != 5) {
				throw new IllegalStateException("invalid repaired branch line: " + line);
			}
			result.computeIfAbsent(Integer.parseInt(fields[0]), ignored -> new ArrayList<>())
				.add(new RepairedBranch(fields[1], fields[2], Integer.parseInt(fields[3]),
					Integer.parseInt(fields[4])));
		}
		return result;
	}

	/** 一条已接线的上交分支。 / One wired hand-over branch. */
	private record RepairedBranch(String source, String target, int npcId, int pageId) {
	}
}
