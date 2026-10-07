#!/usr/bin/env python3
"""对拍 compact 技能数据的 neutral_to_npc 与真端 skill_base.xml 的 reserved14。

Cross-check the compact skill data's neutral_to_npc against reserved14 in the
retail skill_base.xml (<真端根>/Map/XML/skill_base.xml, UTF-16).

用途：确认「变形/变身中性」这一属性在两个数据源之间是否一一对应，
从而判断 quest 14114 的鹦鹉变身（skill 8197）在真端数据里是否本应中性。
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
COMPACT = REPO / "src/main/resources/aion/definitions/compact/skills"
# 同宿主目录约定：<真端根> = <仓库根>/../58Server（见 ENVIRONMENT.md）
RETAIL = REPO.parent / "58Server" / "Map" / "XML" / "skill_base.xml"

TAG = re.compile(r'<skill_template skill_id="(\d+)"')
EFFECT = re.compile(r'<(?P<kind>deform|polymorph)\b(?P<attrs>[^/>]*)/?>')


def compact_effects():
    """skill_id -> list of (kind, neutral_to_npc, transform_level)"""
    out = {}
    for part in sorted(COMPACT.glob("skill_templates_part_*.xml")):
        text = part.read_text(encoding="utf-8")
        for template in text.split("<skill_template ")[1:]:
            m = TAG.search("<skill_template " + template)
            if not m:
                continue
            skill_id = int(m.group(1))
            for e in EFFECT.finditer(template):
                attrs = e.group("attrs")
                neutral = 'neutral_to_npc="true"' in attrs
                lvl = re.search(r'transform_level="(\d+)"', attrs)
                out.setdefault(skill_id, []).append(
                    (e.group("kind"), neutral, lvl.group(1) if lvl else None))
    return out


def retail_effects():
    """skill_id -> {'deform'/'polymorph': [(index, reserved14, reserved16)]}"""
    out = {}
    with RETAIL.open(encoding="utf-16") as handle:
        for event, elem in ET.iterparse(handle, events=("end",)):
            if elem.tag != "skill_base":
                continue
            skill_id = int(elem.findtext("id"))
            fields = {}
            for child in elem:
                fields[child.tag] = (child.text or "").strip()
            kinds = []
            for idx in range(1, 6):
                etype = fields.get(f"effect{idx}_type")
                if etype in ("Deform", "Polymorph"):
                    kinds.append((etype.lower(), idx,
                                  fields.get(f"effect{idx}_reserved14"),
                                  fields.get(f"effect{idx}_reserved16")))
            out[skill_id] = kinds
            elem.clear()
    return out


def main():
    compact = compact_effects()
    retail = retail_effects()

    agree = mismatch = 0
    rows = []
    for skill_id, effects in sorted(retail.items()):
        for kind, idx, res14, res16 in effects:
            retail_neutral = res14 == "1"
            local = compact.get(skill_id)
            local_neutral = None
            if local:
                for lkind, lneutral, _ in local:
                    if lkind == kind:
                        local_neutral = lneutral
                        break
            if local_neutral is None:
                rows.append((skill_id, kind, idx, res14, res16, "MISSING"))
                continue
            if local_neutral == retail_neutral:
                agree += 1
            else:
                mismatch += 1
                rows.append((skill_id, kind, idx, res14, res16,
                             f"compact={local_neutral} retail(res14={res14})={retail_neutral}"))
    print(f"agree={agree} mismatch={mismatch}")
    for row in rows[:60]:
        print("\t".join(str(x) for x in row))


if __name__ == "__main__":
    sys.exit(main())
