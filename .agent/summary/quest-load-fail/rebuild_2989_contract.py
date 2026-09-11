#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
BASE_PATH = QUEST_DIR / "1989.xml"
TARGET_PATH = QUEST_DIR / "2989.xml"

REWARD_BLOCK = re.compile(
    r'    <transition source="reward" target="complete">.*?    </transition>',
    re.S,
)


def replace_once(content: str, pattern: str, replacement: str) -> str:
    result, count = re.subn(pattern, replacement, content, count=1, flags=re.S)
    if count != 1:
        raise ValueError(f"pattern did not match exactly once: {pattern}")
    return result


def main() -> None:
    original = TARGET_PATH.read_text(encoding="utf-8")
    metadata_match = re.search(r"  <metadata\b.*?  </metadata>", original, re.S)
    if metadata_match is None:
        raise ValueError("2989 metadata block missing")
    metadata = metadata_match.group(0)

    reward_blocks = [block for block in REWARD_BLOCK.findall(original) if 'npc-id="204146"' in block]
    if len(reward_blocks) != 55:
        raise ValueError(f"expected 55 reward-owner completion transitions, found {len(reward_blocks)}")

    content = BASE_PATH.read_text(encoding="utf-8")
    content = replace_once(content, r"<quest-definition id=\"1989\"", '<quest-definition id="2989"')
    content = replace_once(content, r"  <metadata\b.*?  </metadata>", metadata)

    for source, target in (
        ("203771", "204146"),
        ("203704", "204056"),
        ("203705", "204057"),
        ("203706", "204058"),
        ("203707", "204059"),
        ("801214", "801222"),
        ("801215", "801223"),
    ):
        content = content.replace(f'npc-id="{source}"', f'npc-id="{target}"')

    content = content.replace('page="SELECT5_3_1_1"', 'page="SELECT5_3"', 1)
    content = content.replace('page="SELECT5_3_2"', 'page="SELECT5_3_3"')
    content = content.replace('page="SELECT5_4_1_1"', 'page="SELECT5_4"', 1)
    content = content.replace('page="SELECT5_4_2"', 'page="SELECT5_4_3"')

    content = replace_once(
        content,
        r"    <transition source=\"started\" target=\"stage-complete\">\n"
        r"      <event>\n"
        r"        <dialog type=\"TALK_TO_NPC\" npc-id=\"801222\" action=\"SETPRO1\"/>",
        """    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="801222" action="SELECT5_3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5_3_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="801222" action="SELECT5_3_2"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5_3_2"/>
      </after-commit>
    </transition>
    <transition source="started" target="stage-complete">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="801222" action="SETPRO1"/>""",
    )
    content = replace_once(
        content,
        r"    <transition source=\"started\" target=\"stage-complete\">\n"
        r"      <event>\n"
        r"        <dialog type=\"TALK_TO_NPC\" npc-id=\"801223\" action=\"SETPRO1\"/>",
        """    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="801223" action="SELECT5_4_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5_4_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="801223" action="SELECT5_4_2"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5_4_2"/>
      </after-commit>
    </transition>
    <transition source="started" target="stage-complete">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="801223" action="SETPRO1"/>""",
    )
    content = content.replace('<play-movie movie-id="105"/>', '<play-movie movie-id="137"/>')

    content, removed = re.subn(
        r"\n    <npc-complete npc-id=\"204146\" source=\"reward-(?:combat|class)\".*?</npc-complete>",
        "",
        content,
        flags=re.S,
    )
    if removed != 2:
        raise ValueError(f"expected two generated reward blocks, removed {removed}")

    reward_transitions = []
    for block in reward_blocks:
        reward_transitions.append(block.replace('source="reward"', 'source="reward-combat"', 1))
        reward_transitions.append(block.replace('source="reward"', 'source="reward-class"', 1))

    previews = """    <transition source="reward-combat" target="reward-combat">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204146" action="USE_OBJECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="reward-class" target="reward-class">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204146" action="USE_OBJECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
"""
    closing = "  </transitions>"
    if content.count(closing) != 1:
        raise ValueError("2989 transitions closing tag missing")
    content = content.replace(closing, previews + "\n".join(reward_transitions) + "\n" + closing, 1)
    TARGET_PATH.write_text(content, encoding="utf-8")


if __name__ == "__main__":
    main()
