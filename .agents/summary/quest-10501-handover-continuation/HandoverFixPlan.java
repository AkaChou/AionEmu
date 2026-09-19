import com.aionemu.gameserver.questEngine.definition.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 只读探针：生成“1008 本地关闭确认页 → 同 NPC 续接页”批处理计划。
 * Read-only probe that emits the batch plan "client-local-close confirmation page -> same-NPC continuation page".
 *
 * 判定规则:
 *   1. 任务的客户端 HTML 中 check_user_item_ok(10000) 页唯一按钮是 HACTION_FINISH_DIALOG(1008)（本地关闭，不回传任务动作）；
 *   2. 该任务某条 transition 的 after-commit 下发了页面 10000；
 *   3. 该 transition 的目标节点存在“同一 NPC + USE_OBJECT(-1) 打开对话”的入口路由，并显示页 P；
 *   → 修复建议: 该 transition 直接下发 P（玩家重新对话本来就会落到 P），计划行输出到标准输出。
 *
 * 用法: java ... HandoverFixPlan <plan.tsv>
 */
public final class HandoverFixPlan {
	public static void main(String[] args) throws Exception {
		Map<Integer, List<String[]>> client = new HashMap<>();
		List<String> lines = Files.readAllLines(
			Path.of("docs/quest/client-dialog-mapping/quest-dialog-action-details.csv"), StandardCharsets.UTF_8);
		for (int i = 1; i < lines.size(); i++) {
			String[] f = lines.get(i).split(",", -1);
			if (f.length < 16 || !"10000".equals(f[6])) {
				continue;
			}
			client.computeIfAbsent(Integer.parseInt(f[0]), key -> new ArrayList<>()).add(new String[] {f[12], f[13]});
		}
		Map<Integer, String> pageNames = new HashMap<>();
		for (QuestDialogPage page : QuestDialogPage.values()) {
			pageNames.putIfAbsent(page.id(), page.name());
		}
		Map<Integer, String> actionNames = new HashMap<>();
		for (QuestDialogAction action : QuestDialogAction.values()) {
			actionNames.putIfAbsent(action.id(), action.name());
		}
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(HandoverFixPlan.class.getClassLoader());
		StringBuilder plan = new StringBuilder();
		StringBuilder review = new StringBuilder();
		for (CompiledQuestDefinition compiled : catalog.executables()) {
			QuestDefinition def = compiled.definition();
			List<String[]> buttons = client.get(def.id());
			if (buttons == null || buttons.isEmpty()) {
				continue;
			}
			Set<String> actionIds = new TreeSet<>();
			for (String[] button : buttons) {
				actionIds.add(button[0]);
			}
			if (!actionIds.equals(Set.of("1008"))) {
				continue;
			}
			boolean emitted = false;
			for (QuestTransition transition : def.transitions()) {
				boolean showsOkPage = transition.afterCommit().stream().anyMatch(action ->
					action instanceof AfterCommitAction.ShowQuestDialog show && show.dialogId() == QuestDialogPage.CHECK_USER_ITEM_OK.id());
				if (!showsOkPage || !(transition.event() instanceof QuestEvent.TalkToNpc talk)) {
					continue;
				}
				Set<Integer> entries = new TreeSet<>();
				for (QuestTransition candidate : def.transitions()) {
					if (!candidate.sourceNode().equals(transition.targetNode())) {
						continue;
					}
					if (!(candidate.event() instanceof QuestEvent.TalkToNpc open)
						|| open.npcId() != talk.npcId() || open.dialogId() == null
						|| open.dialogId() != QuestDialogAction.USE_OBJECT.id()) {
						continue;
					}
					for (AfterCommitAction action : candidate.afterCommit()) {
						if (action instanceof AfterCommitAction.ShowQuestDialog show) {
							entries.add(show.dialogId());
						}
						if (action instanceof AfterCommitAction.ShowQuestSelectionDialog show) {
							entries.add(show.dialogId());
						}
					}
				}
				if (entries.isEmpty()) {
					continue;
				}
				for (int entry : entries) {
					String pageName = pageNames.get(entry);
					if (pageName == null) {
						throw new IllegalStateException("quest " + def.id() + " entry page " + entry + " has no XML name");
					}
					String actionName = talk.dialogId() == null ? "" : actionNames.get(talk.dialogId());
					if (actionName == null) {
						throw new IllegalStateException("quest " + def.id() + " dialog " + talk.dialogId() + " has no XML name");
					}
					plan.append(def.id()).append('\t').append(transition.sourceNode()).append('\t')
						.append(transition.targetNode()).append('\t').append(talk.npcId()).append('\t')
						.append(talk.dialogId()).append('\t').append(actionName).append('\t').append(entry)
						.append('\t').append(pageName).append('\n');
					emitted = true;
				}
			}
			if (!emitted) {
				review.append(def.id()).append('\t').append(buttons.get(0)[1]).append('\n');
			}
		}
		StringBuilder out = new StringBuilder("# quest\tsource\ttarget\tnpc\tdialog\tdialog_action\tto_page\tto_page_name\n");
		out.append(plan);
		out.append("# REVIEW quest\theader_button\n").append(review);
		if (args.length == 1) {
			Files.writeString(Path.of(args[0]), out.toString(), StandardCharsets.UTF_8);
		}
		int planRows = plan.toString().isBlank() ? 0 : plan.toString().strip().split("\n").length;
		int reviewRows = review.toString().isBlank() ? 0 : review.toString().strip().split("\n").length;
		System.out.println("PLAN_ROWS=" + planRows + " REVIEW_ROWS=" + reviewRows);
	}
}
