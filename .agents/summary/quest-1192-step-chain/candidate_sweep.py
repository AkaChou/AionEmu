#!/usr/bin/env python3
"""候选复核：把「客户端推进动作无路由」与「行/状态缺行」交叉，逐候选判定当前 HEAD 状态。

输入（同目录，由两个审计脚本生成）：
  unrouted-progress-actions.tsv  客户端 HACTION_SETPRO<n>/SELECT<n>_<m> 在任务 XML 中无路由
  audit-output.tsv               客户端 quest_summary 行 ↔ START/REWARD var0 状态投影
输出：
  candidate-sweep.tsv            逐候选判定（仍缺 / 噪声）+ 页面引用证据 + 最近提交
"""

from __future__ import annotations

import csv
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

BASE = Path(__file__).resolve().parent
REPO = BASE.parents[2]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
DIALOG = Path("/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs")

CROSS_STATES = {"ROW_WITHOUT_STATE", "BOTH_MISALIGNED"}
CROSS_SHAPES = {"MISSING_TAIL_ROWS", "INTERIOR_GAP"}
PAGE_RE = re.compile(r'<HtmlPage name="([^"]+)">(.*?)</HtmlPage>', re.S)


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8") as handle:
        return list(csv.DictReader(handle, delimiter="\t"))


def client_page_index(quest_id: int) -> tuple[dict[str, str], dict[str, str]]:
    """返回 action -> 所在页、page -> 该页按钮动作清单。"""
    action_page: dict[str, str] = {}
    page_actions: dict[str, str] = {}
    for name in (f"QUEST_Q{quest_id}.html", f"quest_q{quest_id}.html"):
        path = DIALOG / name
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        for match in PAGE_RE.finditer(text):
            page = match.group(1).upper()
            actions = sorted(set(re.findall(r"HACTION_([A-Z0-9_]+)", match.group(2))))
            page_actions[page] = " ".join(actions)
            for action in actions:
                action_page.setdefault(action, page)
        break
    return action_page, page_actions


def xml_facts(quest_id: int) -> dict[str, object]:
    path = QUESTS / f"{quest_id}.xml"
    raw = path.read_text(encoding="utf-8")
    root = ET.fromstring(raw)
    nodes_element = root.find("nodes")
    nodes = []
    for node in (nodes_element if nodes_element is not None else []):
        variables = {v.get("name"): v.get("value") for v in node.findall("var")}
        nodes.append({"label": node.get("label"), "status": node.get("status"),
                      "var0": variables.get("var0")})
    states = [f"{n['label']}:{n['status']}@{n['var0']}" for n in nodes
              if n["status"] in ("START", "REWARD")]
    actions = sorted(set(re.findall(r'action="([A-Z0-9_]+)"', raw)))
    pages = {value.upper() for value in re.findall(r'page="([A-Za-z0-9_]+)"', raw)}
    fields = sorted(set(re.findall(r'field="([A-Za-z0-9_]+)"', raw)))
    last = subprocess.run(["git", "log", "-1", "--format=%h %ad %s", "--date=short", "--",
                           str(path)], capture_output=True, text=True, cwd=REPO).stdout.strip()
    return {"states": states, "actions": actions, "pages": pages, "fields": fields, "last": last}


def main() -> int:
    unrouted = {int(r["quest_id"]): r for r in read_tsv(BASE / "unrouted-progress-actions.tsv")}
    audit = {int(r["quest_id"]): r for r in read_tsv(BASE / "audit-output.tsv")}
    old_cross = ({int(r["quest_id"]) for r in read_tsv(BASE / "audit-cross.tsv")}
                 if (BASE / "audit-cross.tsv").exists() else set())

    rows = []
    for quest_id in sorted(unrouted):
        audit_row = audit.get(quest_id)
        if audit_row is None:
            continue
        missing = unrouted[quest_id]["missing_progress_actions"].split()
        facts = xml_facts(quest_id)
        action_page, _page_actions = client_page_index(quest_id)
        missing_pages = {a: action_page.get(a, "?") for a in missing}
        # 页面被 XML 引用 = 服务端会下发该页，按钮点下去却没有路由 → 硬卡死
        shown_but_unrouted = [a for a in missing if missing_pages[a] != "?" and missing_pages[a] in facts["pages"]]
        never_referenced = [a for a in missing if missing_pages[a] != "?" and missing_pages[a] not in facts["pages"]]
        is_cross = (audit_row["row_state_verdict"] in CROSS_STATES
                    or audit_row["shape"] in CROSS_SHAPES)
        client_rows = int(audit_row["client_rows"])
        if client_rows <= 1:
            klass = "噪声_单行"
            reason = f"客户端只有 {client_rows} 行，无多步链"
        elif not facts["states"]:
            klass = "噪声_无START/REWARD节点"
            reason = f"XML 只有元数据/其它状态轴（fields={','.join(facts['fields']) or '-'}）"
        elif not facts["pages"]:
            klass = "噪声_无对话页路由"
            reason = "XML 未引用任何 SHOW_QUEST_PAGE（过场/自动任务）"
        elif shown_but_unrouted:
            klass = "仍缺_页面已下发但无路由"
            reason = f"{len(shown_but_unrouted)} 个动作所在页面已由 XML 下发但动作无路由：" + " ".join(shown_but_unrouted)
        else:
            klass = "仍缺_客户端页面从未下发"
            reason = f"{len(never_referenced)} 个动作所在页面 XML 从未下发（错页/跳页）：" + " ".join(never_referenced)
        rows.append({
            "quest_id": quest_id,
            "class": klass,
            "row_status": "行状态未收口" if is_cross else "行状态已收口",
            "client_rows": client_rows,
            "missing_actions": " ".join(missing),
            "missing_action_pages": " ".join(f"{a}->{missing_pages[a]}" for a in missing),
            "shown_but_unrouted": " ".join(shown_but_unrouted),
            "page_never_shown": " ".join(never_referenced),
            "shape": audit_row["shape"],
            "row_state_verdict": audit_row["row_state_verdict"],
            "visible_state_var0": audit_row["visible_state_var0"],
            "rows_without_state": audit_row["rows_without_state"],
            "reward_var0": audit_row["reward_var0"],
            "xml_states": " ".join(facts["states"]),
            "xml_fields": " ".join(facts["fields"]),
            "last_commit": facts["last"],
            "reason": reason,
        })

    columns = list(rows[0]) if rows else []
    with (BASE / "candidate-sweep.tsv").open("w", encoding="utf-8") as handle:
        handle.write("\t".join(columns) + "\n")
        for row in rows:
            handle.write("\t".join(str(row[c]) for c in columns) + "\n")

    classes: dict[str, int] = {}
    for row in rows:
        classes[row["class"]] = classes.get(row["class"], 0) + 1
    cross = [r for r in rows if r["row_status"] == "行状态未收口"]
    still = [r["quest_id"] for r in rows if r["class"].startswith("仍缺")]
    print(f"当前无路由推进动作任务：{len(unrouted)}；1192 是否仍在：{1192 in unrouted}")
    print(f"交叉候选（行状态未收口）：{len(cross)}；行状态已收口但仍无路由：{len(rows) - len(cross)}")
    print(f"仍有路由缺陷：{len(still)}；噪声：{len(rows) - len(still)}")
    print("分类：" + "  ".join(f"{k}={v}" for k, v in sorted(classes.items(), key=lambda i: -i[1])))
    print("旧 67 候选中：行状态已收口 "
          f"{len([r for r in rows if r['quest_id'] in old_cross and r['row_status'] == '行状态已收口'])}，"
          f"仍缺行状态 {len([r for r in rows if r['quest_id'] in old_cross and r['row_status'] == '行状态未收口'])}")
    hard = [str(r["quest_id"]) for r in rows if r["class"] == "仍缺_页面已下发但无路由"]
    print(f"硬卡死风险（页面已下发但按钮无路由）{len(hard)}：{' '.join(hard)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
