#!/usr/bin/env python3
"""P0c-57 台账更新（锚点式、幂等、fail-closed：锚点不唯一/不存在/已改过即拒）。"""
from __future__ import annotations

import sys
from pathlib import Path

LEDGER = Path(__file__).resolve().parent / "GOAL-retail-driver-progress.zh-CN.md"

NEXT_FACE = "- **下一面（P0c-57 收口后，续片 34 候选）：阶段角色扩散 `XML_ONLY_STAGE_ROLE_SPREAD_PENDING`（28 项 / 25 任务，`p0c42-blocked-rows.tsv` 第 33 行）**——接取域已由 P0c-57 用「块级根因 + owner 双侧唯一」收口（判例可复用：块轴普查先行 + 客户端模板索引定 owner + 六守卫），阶段域同病同法但判据另有三个不同点：①页族是 `SELECT2..SELECT9[_n]`/`SETPRO<k>`（与阶梯轴 `apply_talk_ladder` 声明一致性同域，**不得**与阶梯裁定表冲突，P0c-55 的 owner 逐跳相接判例 QE-080 适用）；②11106 `SELECT3_1/SELECT3_2` 在 `talk_npc1/talk_npc2` 双份（P0c-55 未动，本面内）；③已知坑照搬：块级 `B NPC_START` 不是 owner 权威（owner 用客户端 `progress` 列 + 真端 `talk_npc<k>` 双侧对拍）。**同面未收（登记在案，勿丢）**：接取侧 `ACCEPT_ENTRY_PAGE_WRONG_PENDING`（7 条：35017@799806 / 45010@799848 / 45017@799849 / 45024@799842·799843 / 45026@799842·799843——其 `QUEST_SELECT` 行下发的是阶段页；且 35017/45010/45017 客户端 `start_npc_ids` 为空，与 P0c-57 暂缓面 14 项 `_faction_` 同任务相交，判据须与势力声明轴合议）；P0c-57 暂缓 19/34 项（12 任务 `_faction_` 声明未解 + 5 任务 owner 缺失/变体差 3218/4218/19070/19071/21110）与块轴未动 5 任务（2266/3006/35011/4970/26990）仍留阻塞表按原因跟踪；`GATE_VS_ITEM_CHECK_PENDING`（21033/21455）另形。**当前切片（P0c-57 已收口）**：接取入口角色收窄 18 任务 37 块 + 8 任务 15 行，登记表 `5bf47fde…` → `0ae2d924…`，T3 `T3-230841.log` 身份集 105==105 零新增。"

CURRENT_SLICE = """- 已完成：**P0c-57（SimpleTalk 链车道 / 续片 33）接取入口角色收窄：18 个任务的每链 NPC `NPC_START` 块按真端/客户端 owner 剪除（`XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING` 收口）**：（编号消歧：本片独占 `P0c-57` 前缀；并发 DataDriven 车道已取 `P0c-58`（challenge 哨兵轴），唯一键 = lane + 续片号。）①**口径复算 + 块级根因**——登记口径 34 项 / 25 任务先复算（RECOMPUTED == REGISTERED，QE-072）；根因在**块级**：遗留 XML 把真端步骤行（`talk_npc<k>`）误读成起始行、每链 NPC 各写一个 `<dialog type="NPC_START">` 块 ⇒ `RetailSimpleTalkDefinitionCompiler.acceptFlowChain` 每块合成一条接取流，链上每个 NPC 都成接取人（HEAD `1484.xml` 自证：`NPC_START npc-id="204045"` 紧贴注释 `retail 步骤0:type="TALK" ids="204045"`；客户端任务书接取相位只有 owner 798127 一行有按钮 ⇒ 扩散在客户端无对应物）。②**两轴普查**——块轴 27 任务（18 重复入口 37 块 / 1 变体差 2266 / 5 owner 缺失 3218/4218/4970/21110/26990 / 3 `_faction_` 未解 3006/35011/35026）+ 物品轴 34 项（15 PRUNE_CANDIDATE / 5 GUARD_FAIL / 14 OWNER_UNRESOLVED `_faction_`）；坑：**块级 `B NPC_START` 不是 owner 权威**（owner 用客户端 `start_npc_ids`；35017/45010/45017 客户端 start 为空）。③**六守卫 G1..G6 fail-closed**——owner 双侧唯一且相等（真端 `acquired_npc_name` == 客户端 `start_npc_ids`）/ 被剪 NPC 是链上 NPC 且 ≠ owner / 接取投影 ⊆ owner / owner 侧有接取族行 / 不得剪 `QUEST_SELECT` 入口行 / 只剪 `source == unaccepted`；装载轴（与 P0c-42 接取入口表互斥、retention==RETAIL_TABLE、真端名字/ID 逐字对拍、每任务恰一 owner）+ 逐行 `accept_pruned_route` 过滤 + `P0C57-ACCEPT-KEEP` 收口守卫（剪后恰剩 owner 一块）。④**落地**——登记表 5052→5000 行（净 −52 = 37 块 + 15 条 `R:unaccepted` 行），`5bf47fde…` → **`0ae2d924…`**（415369 字节）；保真重跑逐字节相同 + 24 张裁定表零漂移；爆炸半径**恰 18 任务 added=0 removed=52**。⑤**冻结面**——指纹外科重冻 18 值（声明面 == 实测面 == 裁定表三面互证，267 行逐字节不动，冻结 `13a5c40e…` 双副本）；IR −353/+0；审计 `unreachedOrEvidence=10` pre/post 同值，diff 仅 18 条头行计数变小（被剪全为 `PAGE_ACTION_MATCHED` 非致命行）。⑥**门禁**——净树 **20/20**（链门 2 + 序审计 17 + 客户端契约 1，契约门 fatal 0）；T1 `gates/T1-225105.log` **75 例 1F**（lane `20035` 唯一身份，源行 423→430 为并发 lane 对该测试类自身编辑）；T2（18 id，`gates/T2-225730.log`）**97 例 7F**（`20035` + `QuestMultistepChainContractTest` 1514×5 + `JournalRewardRowRepairContractTest` 15613×1——六条 1514/15613 身份逐字见装机前 `T3-221706.log` 与 P0c-55 基线 `T3-221828.log` ⇒ 车道既有）；**T3（`gates/T3-230841.log`）2015 例 117F/22E/1S vs P0c-55 基线（2013 例同值）class.method 身份集 105==105，ADDED 0 / REMOVED 0**（例数 +2 为两轮间并入的 lane 新测试类）。⑦**残留**——19/34 项暂缓（12 任务 `35010…45026` `_faction_` 声明未解 + 5 任务 3218/4218/19070/19071/21110 owner 缺失/变体差）、块轴 2266/3006/35011/4970/26990 未动、`ACCEPT_ENTRY_PAGE_WRONG_PENDING` 7 条另轴（其入口行 `source != unaccepted`，G6 剪不到）、运行时/客户端目检 PENDING（未启服）。报告 `reports/2026-09-26-P0c57-accept-entrance-role-narrowing.zh-CN.md`；机械 `p0c57_accept_axis_recon.py` / `p0c57_accept_axis_census.py` / `p0c57_accept_entrance_census.py` / `p0c57_blast_radius.py` / `p0c57_refreeze_fingerprints.py` / `p0c57_ledger_update.py`；产物 `p0c57-registry-pre.tsv` / `p0c57-accept-axis-{recon,census,decisions}.tsv` / `p0c57-accept-entrance-{census,decisions}.tsv` / `p0c57-blast-radius.txt` / `p0c57-ir-{pre,post}.txt` / `p0c57-audit-{pre,post}.txt` / `p0c57-fingerprints-dump.tsv` / `p0c57-nettree-gate.log`；探针源码归档 `P0c57AcceptProbeTest.java.txt` / `P0c57AuditProbeTest.java.txt`（树内 `.java`/`.class` 已删）。"""

EVIDENCE_ROW = """| 2026-09-26 | **P0c-57 门禁（保真 / 净树 / T1 / T2 / T3 / 指纹 / IR / 审计）** | `p0c42_builder_fidelity_check.py` + `-Dtest='RetailSimpleTalkChainGateTest,QuestDialogOrderAuditTest,QuestClientContractGateTest'` + `run_quest_gates.sh T1` + `run_quest_gates.sh T2 1484 2271 2480 2538 2646 2663 2914 2954 3037 3041 3087 3093 3966 4052 11010 11103 11460 16990` + `QUEST_FORK_COUNT=2 run_quest_gates.sh T3` + `p0c57_refreeze_fingerprints.py` | `FIDELITY_OK` 415369 字节逐字节 + `REGISTRY_MD5 0ae2d924…` + 24 表零漂移；净树 `p0c57-nettree-gate.log` 20/20 绿（契约门 fatal 0）；T1 `gates/T1-225105.log` 75 例 1F（lane `20035`）；T2 `gates/T2-225730.log` 97 例 7F（`20035` + 1514×5 + 15613×1，六身份逐字见装机前 `T3-221706.log` ⇒ 车道既有）；**T3 `gates/T3-230841.log` 2015 例 117F/22E/1S vs P0c-55 基线 `T3-221828.log`（2013 例同值）身份集 105==105，ADDED 0 / REMOVED 0**；指纹 dump 与冻结 285 行逐键对拍 changed 恰 18 / added=removed=0 / 声明面==实测面==裁定表，冻结 `13a5c40e…` 双副本；IR −353/+0；审计 pre/post `unreachedOrEvidence=10` 同值 |"""

CHANGELOG_ROW = """- **2026-09-26 续片 33 / P0c-57（SimpleTalk 链车道）：接取入口角色收窄 —— 18 任务的每链 NPC `NPC_START` 块按真端/客户端 owner 剪除（`XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING` 收口）**：①**口径复算 + 块级根因**——34 项 / 25 任务先复算（RECOMPUTED == REGISTERED，QE-072）；根因：遗留 XML 把真端步骤行（`talk_npc<k>`）误读成起始行、每链 NPC 各写一个 `NPC_START` 块 ⇒ `acceptFlowChain` 每块合成接取流，链上每个 NPC 都成接取人（HEAD `1484.xml` 注释自证；客户端接取相位只有 owner 一行有按钮 ⇒ 扩散在客户端无对应物）。②**两轴普查**——块轴 27 任务（18 重复入口 37 块 / 2266 变体差 / 3218/4218/4970/21110/26990 owner 缺失 / 3006/35011/35026 `_faction_` 未解）+ 物品轴 34 项（15 采纳 / 5 守卫拦 / 14 `_faction_` 未解）；坑：块级 `B NPC_START` 不是 owner 权威（owner 用客户端 `start_npc_ids`）。③**六守卫 G1..G6**（owner 双侧唯一且相等 / 链上 NPC≠owner / 接取投影 ⊆ owner / owner 有接取行 / 不得剪 `QUEST_SELECT` / 只剪 `source=unaccepted`）+ 装载轴互斥 P0c-42 接取入口表 + `P0C57-ACCEPT-KEEP` 收口守卫（剪后恰剩 owner 一块）。④**落地**——登记表 5052→5000 行（净 −52 = 37 块 + 15 条 `R:unaccepted` 行），`5bf47fde…` → `0ae2d924…`；保真逐字节 + 24 表零漂移；爆炸半径恰 18 任务 added=0 removed=52。⑤**冻结面**——指纹重冻 18 值（声明面 == 实测面 == 裁定表，267 行不动，`13a5c40e…`）；IR −353/+0；审计 `unreachedOrEvidence=10` 同值仅头行计数变小。⑥**门禁**——净树 20/20、T1 75 例 1F（lane `20035`）、T2 97 例 7F（`20035` + 1514×5 + 15613×1 六身份逐字见装机前基线 ⇒ 车道既有）、**T3 2015 例 117F/22E/1S 身份集 105==105 ADDED 0 / REMOVED 0**。⑦**残留**——19/34 项暂缓（`_faction_` 声明未解 12 任务 + owner 缺失/变体差 5 任务）、`ACCEPT_ENTRY_PAGE_WRONG_PENDING` 7 条另轴、运行时目检 PENDING（未启服）。报告 `reports/2026-09-26-P0c57-accept-entrance-role-narrowing.zh-CN.md`。"""

DOD_OLD = "；保真 `FIDELITY_OK` 419803 字节 + 23 表零漂移"
DOD_APPEND = DOD_OLD + """；**P0c-57 时点（接取入口收窄 18 任务后）**：净树 **20/20**、客户端契约门 fatal 0、T1 `gates/T1-225105.log` **75 例 1F**（lane `20035`，源行 423→430 为 lane 自身编辑）、T2 `gates/T2-225730.log`（18 id）**97 例 7F**（`20035` + 1514×5 + 15613×1，六身份逐字见装机前基线 ⇒ 车道既有）、T3 `gates/T3-230841.log` **2015 例 117F/22E/1S** 对 P0c-55 基线 `T3-221828` 身份集 **105==105 ADDED 0 / REMOVED 0**、保真 `FIDELITY_OK` 415369 字节 + 24 表零漂移"""

FAMILY_APPEND = """；**P0c-57 接取入口角色收窄**：18 任务 37 块 + 8 任务 15 条 `R:unaccepted` 行剪除（登记表 **`0ae2d924…`**，5000 行；爆炸半径 added=0 removed=52 恰 18 任务；指纹 18 值重冻 **`13a5c40e…`**、审计 `unreachedOrEvidence=10` 同值、T3 身份集 105==105 零新增）——`XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING` 收口（19/34 项暂缓带原因 + 7 条入口页错位另轴）"""

# (tag, old, new, mode) — mode: 'line' 整行替换（old 为行前缀）；'insert_before' 在 old 前插入 new；'replace' 子串替换；'append_end' 文件尾追加
EDITS = [
    ("next-face", "- **下一面（P0c-55 收口后，续片 33 候选）：接取轴两登记项", NEXT_FACE, "line"),
    ("current-slice", "- 已完成：**P0c-55（SimpleTalk 链车道 / 续片 32）阶段腿逐阶段重建", CURRENT_SLICE + "\n", "insert_before"),
    ("dod", "T3 = `gates/T3-224433.log`（待填））", "T3 = `gates/T3-224433.log`（待填）" + DOD_APPEND, "replace"),
    ("family", "——审计 9 任务未达 **34 → 0**、全表 **1348 → 1314**（`ONLY_POST = 0`）、冻结指纹 **`aaf2b9c0…`**；**族内 retired / `XML_RETENTION` 计数不变**（本轴只修登记表形态与下发路由，不涉退役与归属翻转） |",
     "——审计 9 任务未达 **34 → 0**、全表 **1348 → 1314**（`ONLY_POST = 0`）、冻结指纹 **`aaf2b9c0…`**" + FAMILY_APPEND + "；**族内 retired / `XML_RETENTION` 计数不变**（本轴只修登记表形态与下发路由，不涉退役与归属翻转） |", "replace"),
    ("evidence", "| 2026-09-26 | **P0c-57 接取轴侦察（下一面预取证据，只读）**", EVIDENCE_ROW + "\n", "insert_before_after"),
]


def fail(msg: str) -> int:
    print(f"ABORT: {msg}")
    return 1


def main() -> int:
    text = LEDGER.read_text(encoding="utf-8")
    lines = text.splitlines()
    if any(tag in text for tag in ("P0c-57（SimpleTalk 链车道 / 续片 33）接取入口角色收窄：18 个任务的每链", "ACCEPT_ENTRANCE_ROLE_NARROW_CLOSED")):
        # 台账正文允许阻塞表先行写入；这里只防本脚本重复执行
        if "p0c57_ledger_update" in text or "续片 33 / P0c-57" in text:
            print("ABORT: ledger already updated by p0c57_ledger_update")
            return 1

    # 1) 下一面整行替换
    hits = [i for i, ln in enumerate(lines) if ln.startswith(EDITS[0][1])]
    if len(hits) != 1:
        return fail(f"next-face anchor hits={len(hits)}")
    lines[hits[0]] = NEXT_FACE

    # 2) 当前切片 bullet 插入（P0c-55 行前）
    hits = [i for i, ln in enumerate(lines) if ln.startswith("- 已完成：**P0c-55（SimpleTalk 链车道 / 续片 32）阶段腿逐阶段重建")]
    if len(hits) != 1:
        return fail(f"current-slice anchor hits={len(hits)}")
    lines.insert(hits[0], CURRENT_SLICE)

    # 3) DoD 时点
    if sum(1 for ln in lines if DOD_OLD in ln) != 1:
        return fail("dod anchor not unique")
    for i, ln in enumerate(lines):
        if DOD_OLD in ln:
            lines[i] = ln.replace(DOD_OLD, DOD_APPEND, 1)
            break

    # 4) 家族进度 SimpleTalk 行
    fam_old = "——审计 9 任务未达 **34 → 0**、全表 **1348 → 1314**（`ONLY_POST = 0`）、冻结指纹 **`aaf2b9c0…`**；**族内 retired / `XML_RETENTION` 计数不变**（本轴只修登记表形态与下发路由，不涉退役与归属翻转） |"
    hits = [i for i, ln in enumerate(lines) if fam_old in ln]
    if len(hits) != 1:
        return fail(f"family anchor hits={len(hits)}")
    lines[hits[0]] = lines[hits[0]].replace(fam_old,
        "——审计 9 任务未达 **34 → 0**、全表 **1348 → 1314**（`ONLY_POST = 0`）、冻结指纹 **`aaf2b9c0…`**" + FAMILY_APPEND + "；**族内 retired / `XML_RETENTION` 计数不变**（本轴只修登记表形态与下发路由，不涉退役与归属翻转） |", 1)

    # 5) 证据索引：在 P0c-57 侦察行之后插入门禁行
    hits = [i for i, ln in enumerate(lines) if ln.startswith("| 2026-09-26 | **P0c-57 接取轴侦察（下一面预取证据，只读）**")]
    if len(hits) != 1:
        return fail(f"evidence anchor hits={len(hits)}")
    lines.insert(hits[0] + 1, EVIDENCE_ROW)

    # 6) 变更日志追加到文件尾
    if not text.rstrip().endswith("reports/2026-09-26-P0c55-stage-leg-rebuild.zh-CN.md`。") and "变更日志" not in lines[-1]:
        pass  # 文件尾即变更日志列表，直接追加
    lines.append(CHANGELOG_ROW.rstrip())

    LEDGER.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("P0C57_LEDGER_OK 6 edits applied")
    return 0


if __name__ == "__main__":
    sys.exit(main())
