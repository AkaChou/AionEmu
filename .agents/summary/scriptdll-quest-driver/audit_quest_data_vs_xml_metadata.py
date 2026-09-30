#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
对账在仓 quest_data.xml（真端 quest.xml 的 JAXB 派生）与 quest_definition XML 元数据。

目的：为"真端元数据层"（QuestTemplate → QuestMetadata 映射器）提供全量差异台账。
  - quest_definition XML 的元数据已被此前 systemic-goal 门禁与真端 quest.xml 对齐
    （start-metadata / prerequisite / drop / reward-value 等轴），因此差异优先怀疑
    quest_data.xml 陈旧；差异行会给出 XML 侧值，供刷新 quest_data 时复核真端原文。
  - 输出：quest-data-vs-xml-metadata-audit.tsv（quest_id, axis, quest_data, xml, note）
  - 汇总：stdout 按轴统计。

只读两个在仓数据文件，不读外部真端包；刷新方向的真端复核用
audit_retail_quest_xml_refresh.py（另脚本）。
"""
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
QUEST_DATA = os.path.join(REPO, "src/main/resources/aion/data/static_data/quest_data/quest_data.xml")
PROD_DIR = os.path.join(REPO, "src/main/resources/aion/data/static_data/quest/definitions/quests")
OUT_TSV = os.path.join(os.path.dirname(os.path.abspath(__file__)), "quest-data-vs-xml-metadata-audit.tsv")

UNLIMITED = "UNLIMITED"

# ---------------------------------------------------------------- quest_data 侧


def parse_quest_data():
    quests = {}
    for _, elem in ET.iterparse(QUEST_DATA, events=("end",)):
        if elem.tag != "quest":
            continue
        quests[int(elem.get("id"))] = parse_quest_data_entry(elem)
        elem.clear()
    return quests


def attrs_int(elem, name, default=None):
    raw = elem.get(name)
    if raw is None or raw == "":
        return default
    return int(raw)


def parse_quest_data_entry(q):
    d = {}
    d["name"] = q.get("name") or ""
    d["display_name_id"] = attrs_int(q, "nameId", 0)
    d["min_level"] = attrs_int(q, "minlevel_permitted", 0)
    d["max_level"] = attrs_int(q, "maxlevel_permitted", 0)  # 0 => UNLIMITED
    races = set()
    for token in (q.get("race_permitted") or "").split():
        races.add({"pc_light": "ELYOS", "pc_dark": "ASMODIANS"}.get(token, token.upper()))
    d["races"] = races
    d["category"] = (q.get("category") or "QUEST").upper()
    max_repeat = attrs_int(q, "max_repeat_count", 1)
    if max_repeat is None or max_repeat < 1:
        max_repeat = 1
    reward_repeat = attrs_int(q, "reward_repeat_count", None)
    if reward_repeat is None:
        reward_repeat = max_repeat
    cooldown = attrs_int(q, "quest_cooltime", 0) or 0
    cycles = frozenset((q.get("repeat_cycle") or "").split())
    d["repeat"] = (max_repeat, reward_repeat, cooldown, cycles)

    # start_conditions：finished → prerequisites；其余 → start_conditions 轴
    prereqs = set()
    conds = []
    sc = q.find("start_conditions")
    if sc is not None:
        for child in sc:
            tag = child.tag
            if tag == "finished":
                for token in (child.get("quest_id") or "").split():
                    prereqs.add(int(token))
            else:
                text_ids = (child.text or "").split() if child.text else []
                if not text_ids:
                    text_ids = [str(attrs_int(child, "quest_id", 0))]
                for token in text_ids:
                    conds.append((tag, int(token)))
    d["prerequisites"] = prereqs
    d["start_conditions"] = sorted(conds)

    items = []
    ci = q.find("collect_items")
    if ci is not None:
        for it in ci.findall("collect_item"):
            items.append((int(it.get("item_id")), int(it.get("count", "1"))))
    d["items"] = items

    # 奖励块：第一块为普通奖励，其余块单独登记
    reward_blocks = q.findall("rewards")
    d["rewards"] = rewards_of(reward_blocks[0]) if reward_blocks else []
    d["extra_reward_blocks"] = len(reward_blocks) - 1
    ext = q.find("extended_rewards")
    d["extended_rewards"] = rewards_of(ext) if ext is not None else []

    drops = []
    for dr in q.findall("quest_drop"):
        drops.append((int(dr.get("npc_id")), int(dr.get("item_id")), int(dr.get("chance", "100")),
                      dr.get("drop_each_member", "0") in ("1", "true"),
                      int(dr.get("collecting_step", "0"))))
    d["drops"] = sorted(drops)

    d["classes"] = frozenset(c.text.strip() for c in q.findall("class_permitted") if c.text)
    g = q.find("gender_permitted")
    d["gender"] = (g.text or "").strip() if g is not None else ""

    d["rank"] = attrs_int(q, "rank", 0) or 0
    d["max_count_limited"] = max(1, attrs_int(q, "max_count_limited_quest", 1) or 1)
    d["count_recover_limited"] = max(1, attrs_int(q, "count_recover_limited_quest", 1) or 1)
    d["cannot_share"] = q.get("cannot_share") in ("true", "1")
    d["cannot_giveup"] = q.get("cannot_giveup") in ("true", "1")
    d["bounty_reward"] = q.get("bounty_reward") in ("true", "1")
    uc = attrs_int(q, "use_class_reward", 0) or 0
    d["use_class_reward"] = uc
    d["combine_skill"] = attrs_int(q, "combineskill", None)
    d["combine_skillpoint"] = attrs_int(q, "combine_skillpoint", None)
    d["timer"] = q.get("timer") in ("true", "1")
    d["npc_faction_id"] = attrs_int(q, "npcfaction_id", 0) or 0
    d["mentor_type"] = (q.get("mentor_type") or "NONE").upper()
    d["target_type"] = (q.get("target_type") or "NONE").upper()
    d["title_id"] = attrs_int(q, "titleId", 0) or 0

    inv = []
    ii = q.find("inventory_items")
    if ii is not None:
        for it in ii.findall("inventory_item"):
            inv.append((int(it.get("item_id")), int(it.get("count", "1"))))
    d["inventory_items"] = sorted(inv)

    work = []
    wi = q.find("quest_work_items")
    if wi is not None:
        for it in wi.findall("quest_work_item"):
            work.append((int(it.get("item_id")), int(it.get("count", "1"))))
    d["quest_work_items"] = sorted(work)

    bonuses = []
    for b in q.findall("bonus"):
        bonuses.append((b.get("type"), attrs_int(b, "level", 0) or 0, attrs_int(b, "skill", 0) or 0))
    d["bonuses"] = sorted(bonuses)

    kills = []
    for k in q.findall("quest_kill"):
        npcs = tuple(sorted(int(n) for n in (k.get("npc_ids") or "").split() if n.strip()))
        kills.append((int(k.get("seq", "0")), npcs))
    d["kills"] = sorted(kills)

    class_rewards = {}
    for tag, cls in (("fighter_selectable_reward", "FIGHTER"), ("knight_selectable_reward", "KNIGHT"),
                     ("ranger_selectable_reward", "RANGER"), ("assassin_selectable_reward", "ASSASSIN"),
                     ("wizard_selectable_reward", "WIZARD"), ("elementalist_selectable_reward", "ELEMENTALIST"),
                     ("priest_selectable_reward", "PRIEST"), ("chanter_selectable_reward", "CHANTER"),
                     ("gunslinger_selectable_reward", "GUNSLINGER"), ("songweaver_selectable_reward", "SONGWEAVER"),
                     ("aethertech_selectable_reward", "AETHERTECH")):
        block = q.find(tag)
        if block is not None:
            class_rewards[cls] = rewards_of(block, selectable_as_selectable=False)
    d["class_rewards"] = {k: v for k, v in sorted(class_rewards.items())}
    return d


def rewards_of(block, selectable_as_selectable=True):
    """quest_data 奖励块 → [(kind, id, amount)]（属性在前，子项在后，稳定序）。"""
    out = []
    a = block.attrib
    if a.get("exp"):
        out.append(("EXP", 0, int(a["exp"])))
    if a.get("gold"):
        out.append(("GOLD", 0, int(a["gold"])))
    if a.get("ap"):
        out.append(("AP", 0, int(a["ap"])))
    if a.get("gp"):
        out.append(("GP", 0, int(a["gp"])))
    if a.get("dp"):
        out.append(("DP", 0, int(a["dp"])))
    if a.get("cp"):
        out.append(("CP", 0, int(a["cp"])))
    if a.get("exp_boost"):
        out.append(("EXP_BOOST", 0, int(a["exp_boost"])))
    if a.get("abyss_op"):
        out.append(("ABYSS_OP", 0, int(a["abyss_op"])))
    if a.get("title"):
        out.append(("TITLE", int(a["title"]), 1))
    for it in block.findall("reward_item"):
        out.append(("ITEM", int(it.get("item_id")), int(it.get("count", "1"))))
    for it in block.findall("selectable_reward_item"):
        out.append(("SELECTABLE_ITEM" if selectable_as_selectable else "ITEM",
                    int(it.get("item_id")), int(it.get("count", "1"))))
    return out


# ---------------------------------------------------------------- XML 侧


def parse_xml_metadata(path):
    root = ET.parse(path).getroot()
    m = root.find("metadata")
    if m is None:
        return None
    d = {}
    d["name"] = m.get("name") or ""
    d["display_name_id"] = int(m.get("display-name-id", "0"))
    d["min_level"] = int(m.get("min-level", "0"))
    raw_max = m.get("max-level", "2147483647")
    d["max_level"] = UNLIMITED if int(raw_max) >= 2147483647 else int(raw_max)
    races_el = m.find("races")
    d["races"] = {r.get("id") for r in races_el.findall("race")} if races_el is not None else set()
    d["category"] = (m.get("category") or "QUEST").upper()
    rep = m.find("repeat")
    if rep is not None:
        max_repeat = int(rep.get("max-repeat-count", "1"))
        reward_repeat = int(rep.get("reward-repeat-count", str(max_repeat)))
        cooldown = int(rep.get("cooldown-seconds", "0") or 0)
        cycles = frozenset((rep.get("cycles") or "").split())
        if rep.get("daily") == "true":
            cycles = frozenset(set(cycles) | {"ALL"})
        d["repeat"] = (max_repeat, reward_repeat, cooldown, cycles)
    else:
        d["repeat"] = (1, 1, 0, frozenset())
    d["prerequisites"] = {int(x.get("id")) for x in (m.find("prerequisites") or [])} \
        if m.find("prerequisites") is not None else set()
    conds = []
    sc = m.find("start-conditions")
    if sc is not None:
        for c in sc.findall("condition"):
            conds.append((c.get("type"), int(c.get("quest-id"))))
    d["start_conditions"] = sorted(conds)
    d["items"] = [(int(x.get("id")), int(x.get("count"))) for x in (m.find("items") or [])] \
        if m.find("items") is not None else []
    d["rewards"] = xml_rewards(m, "rewards")
    d["extended_rewards"] = xml_rewards(m, "extended-rewards")
    d["extra_reward_blocks"] = 0
    drops = []
    dl = m.find("drops")
    if dl is not None:
        for dr in dl.findall("drop"):
            drops.append((int(dr.get("npc-id")), int(dr.get("item-id")), int(dr.get("chance")),
                          dr.get("each-member") == "true", int(dr.get("collecting-step", "0"))))
    d["drops"] = sorted(drops)
    d["classes"] = {c.get("id") for c in (m.find("classes") or [])} if m.find("classes") is not None else set()
    g = m.find("gender")
    d["gender"] = (g.get("id") or "") if g is not None else ""
    d["rank"] = int(m.get("rank", "0"))
    d["max_count_limited"] = max(1, int(m.get("max-count-limited-quest", "1")))
    d["count_recover_limited"] = max(1, int(m.get("count-recover-limited-quest", "1")))
    d["cannot_share"] = m.get("cannot-share") == "true"
    d["cannot_giveup"] = m.get("cannot-giveup") == "true"
    d["bounty_reward"] = m.get("bounty-reward") == "true"
    d["use_class_reward"] = int(m.get("use-class-reward", "0"))
    d["combine_skill"] = int(m.get("combine-skill")) if m.get("combine-skill") else None
    d["combine_skillpoint"] = int(m.get("combine-skill-point")) if m.get("combine-skill-point") else None
    d["timer"] = m.get("timer") == "true"
    d["npc_faction_id"] = int(m.get("npc-faction-id", "0"))
    d["mentor_type"] = (m.get("mentor-type") or "NONE").upper()
    d["target_type"] = (m.get("target-type") or "NONE").upper()
    d["title_id"] = int(m.get("title-id", "0"))
    d["inventory_items"] = sorted((int(x.get("id")), int(x.get("count"))) for x in (m.find("inventory-items") or [])) \
        if m.find("inventory-items") is not None else []
    d["quest_work_items"] = sorted((int(x.get("id")), int(x.get("count"))) for x in (m.find("work-items") or [])) \
        if m.find("work-items") is not None else []
    bonuses = []
    bl = m.find("bonuses")
    if bl is not None:
        for b in bl.findall("bonus"):
            bonuses.append((b.get("type"), int(b.get("level", "0") or 0), int(b.get("skill", "0") or 0)))
    d["bonuses"] = sorted(bonuses)
    kills = []
    kl = m.find("kills")
    if kl is not None:
        for k in kl.findall("kill"):
            kills.append((int(k.get("sequence")), tuple(sorted(int(n.get("id")) for n in k.findall("npc")))))
    d["kills"] = sorted(kills)
    class_rewards = {}
    cr = m.find("class-rewards")
    if cr is not None:
        for c in cr.findall("class"):
            class_rewards[c.get("id")] = [
                (r.get("kind"), int(r.get("id")), int(r.get("amount"))) for r in c.findall("reward")]
    d["class_rewards"] = {k: v for k, v in sorted(class_rewards.items())}
    return d


def xml_rewards(metadata, tag):
    """XML rewards/extended-rewards（含 reward-groups）→ [(kind, id, amount)]。"""
    shorthand = metadata.find(tag)
    out = []
    if shorthand is not None:
        for r in shorthand.findall("reward"):
            out.append((r.get("kind"), int(r.get("id")), int(r.get("amount"))))
        return out
    groups = metadata.find(tag + "-groups")
    if groups is not None:
        for g in groups.findall("group"):
            for r in g.findall("reward"):
                out.append((r.get("kind"), int(r.get("id")), int(r.get("amount"))))
    return out


# ---------------------------------------------------------------- 对账

AXES = ["name", "display_name_id", "min_level", "max_level", "races", "category", "repeat",
        "prerequisites", "start_conditions", "items", "rewards", "extra_reward_blocks",
        "extended_rewards", "drops", "classes", "gender", "rank", "max_count_limited",
        "count_recover_limited", "cannot_share", "cannot_giveup", "bounty_reward",
        "use_class_reward", "combine_skill", "combine_skillpoint", "timer", "npc_faction_id",
        "mentor_type", "target_type", "title_id", "inventory_items", "quest_work_items",
        "bonuses", "kills", "class_rewards"]


def canon_qd_max(max_level):
    return UNLIMITED if max_level in (0, 998, 999) else max_level


def canon_rewards_order(rewards):
    """奖励排序键：kind 优先级（元数据顺序无行为差时按值比较）。"""
    order = {"EXP": 0, "GOLD": 1, "KINAH": 2, "AP": 3, "GP": 4, "DP": 5, "CP": 6,
             "EXP_BOOST": 7, "ABYSS_OP": 8, "TITLE": 9, "ITEM": 10, "SELECTABLE_ITEM": 11,
             "RANDOM": 12}
    return sorted(rewards, key=lambda r: (order.get(r[0], 99), r[1], r[2]))


def main():
    quest_data = parse_quest_data()
    diffs = []
    checked = 0
    xml_only = []
    for fn in sorted(os.listdir(PROD_DIR)):
        if not fn.endswith(".xml"):
            continue
        qid = int(fn[:-4])
        xml = parse_xml_metadata(os.path.join(PROD_DIR, fn))
        if xml is None:
            continue
        checked += 1
        qd = quest_data.get(qid)
        if qd is None:
            xml_only.append(qid)
            continue
        for axis in AXES:
            xv = xml[axis]
            qv = qd[axis]
            if axis == "max_level":
                qv = canon_qd_max(qv)
            elif axis == "rewards":
                qv, xv = canon_rewards_order(qv), canon_rewards_order(xv)
            elif axis == "extended_rewards":
                qv, xv = canon_rewards_order(qv), canon_rewards_order(xv)
            elif axis in ("races", "classes", "prerequisites"):
                qv, xv = set(qv), set(xv)
            elif axis in ("items", "inventory_items", "quest_work_items"):
                qv, xv = sorted(qv), sorted(xv)
            if qv != xv:
                diffs.append((qid, axis, fmt(qv), fmt(xv)))
    with open(OUT_TSV, "w", encoding="utf-8") as fh:
        fh.write("# quest_data.xml vs quest_definition metadata 全量对账\n")
        fh.write("# 列：quest_id\taxis\tquest_data\txml\tnote\n")
        for qid, axis, qv, xv in diffs:
            fh.write(f"{qid}\t{axis}\t{qv}\t{xv}\t\n")
    by_axis = Counter(axis for _, axis, _, _ in diffs)
    quests_hit = len({qid for qid, _, _, _ in diffs})
    print(f"checked={checked} diffs={len(diffs)} quests_with_diff={quests_hit} "
          f"quest_data_missing={len(xml_only)}")
    for axis, n in by_axis.most_common():
        print(f"  {axis}\t{n}")
    if xml_only:
        sample = ",".join(str(q) for q in xml_only[:20])
        print(f"quest_data 缺失任务（前 20）: {sample}")


def fmt(value):
    if isinstance(value, frozenset):
        value = set(value)
    if isinstance(value, (set, list, tuple)):
        text = str(sorted(value) if isinstance(value, set) else list(value))
        return text.replace("\t", " ")
    return str(value)


if __name__ == "__main__":
    sys.exit(main())
