"""Compare production quest start conditions with the retail client slot model.

Retail client fields `<family>_quest_condN` form slots: slots of one family are
OR alternatives, comma-separated ids inside one slot are ANDed, and different
families are jointly mandatory. Limiting families (noacquired/unfinished) pass a
slot when at least one listed id is unsatisfied, so their slots contribute an OR
of negated atoms.
"""
import collections
import itertools
import re
from pathlib import Path

QUEST_XML = Path("/Users/mc/IdeaProjects/58Server/Map/XML/quest.xml")
QUEST_DIR = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
FIELD_RE = re.compile(r"<(finished|acquired|noacquired|unfinished|equipped)_quest_cond(\d+)>([^<]+)</\1_quest_cond\2>")
XML_COND_RE = re.compile(r'<condition\s+type="(\w+)"\s+quest-id="(\d+)"[^>]*/>')
LIMITING = {"noacquired", "unfinished"}


def client_slots():
    text = QUEST_XML.read_text(encoding="utf-16-le", errors="replace")
    model = {}
    for block in re.findall(r"<quest>(.*?)</quest>", text, re.S):
        mid = re.search(r"<id>(\d+)</id>", block)
        if not mid:
            continue
        slots = collections.defaultdict(dict)
        for family, slot, value in FIELD_RE.findall(block):
            ids = []
            for token in value.split(","):
                token = token.strip().lstrip("Q").split(":")[0]
                if token.isdigit():
                    ids.append(int(token))
            ids = tuple(sorted(set(ids)))
            if ids:
                slots[family][int(slot)] = ids
        if slots:
            model[int(mid.group(1))] = slots
    return model


def canonical_groups(slots):
    """Return DNF groups (frozenset of (type, id)) equivalent to retail semantics."""
    family_options = []
    for family in sorted(slots):
        options = []
        if family in LIMITING:
            slot_ids = [slots[family][slot] for slot in sorted(slots[family])]
            for combo in itertools.product(*slot_ids):
                options.append(frozenset((family, quest_id) for quest_id in combo))
        else:
            for slot in sorted(slots[family]):
                options.append(frozenset((family, quest_id) for quest_id in slots[family][slot]))
        family_options.append(options)
    groups = set()
    for combo in itertools.product(*family_options):
        groups.add(frozenset().union(*combo))
    return groups


def xml_groups(text):
    block = re.search(r"<start-condition-groups>(.*?)</start-condition-groups>", text, re.S)
    if block:
        groups = set()
        for group in re.findall(r"<group>(.*?)</group>", block.group(1), re.S):
            groups.add(frozenset((t, int(q)) for t, q in XML_COND_RE.findall(group)))
        return groups, "groups"
    block = re.search(r"<start-conditions>(.*?)</start-conditions>", text, re.S)
    if block:
        conditions = frozenset((t, int(q)) for t, q in XML_COND_RE.findall(block.group(1)))
        return ({conditions} if conditions else set()), "shorthand"
    return set(), "none"


def main():
    model = client_slots()
    mismatches = []
    for path in sorted(QUEST_DIR.glob("*.xml")):
        quest_id = int(path.stem) if path.stem.isdigit() else None
        if quest_id is None or quest_id not in model:
            continue
        text = path.read_text(encoding="utf-8")
        declared, form = xml_groups(text)
        canonical = canonical_groups(model[quest_id])
        if declared != canonical and form != "none":
            multi_slot = {f: len(v) for f, v in model[quest_id].items() if len(v) > 1}
            mismatches.append((path.stem, form, len(declared), len(canonical), multi_slot, model[quest_id]))
    print(f"quests with client start conditions: {len(model)}")
    print(f"production files differing from retail slot semantics: {len(mismatches)}")
    for quest_id, form, declared, canonical, multi_slot, slots in mismatches:
        summary = {f: {s: list(ids) for s, ids in sorted(v.items())} for f, v in sorted(slots.items())}
        print(f"  {quest_id}: form={form} xml_groups={declared} retail_groups={canonical} multi_slot={multi_slot or '{}'}")
        print(f"      slots={summary}")


if __name__ == "__main__":
    main()
