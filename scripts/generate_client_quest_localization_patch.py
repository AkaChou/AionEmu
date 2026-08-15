#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import re
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path


DEFAULT_OUTPUT_DIR = Path("patch/L10N/CHS/Data/Strings")
DEFAULT_DIALOG_OUTPUT_DIR = Path("patch/L10N/CHS/Data/Dialogs")
QUEST_DIALOG_NAME = re.compile(r"quest_q\d+\.html", re.IGNORECASE)


@dataclass(frozen=True)
class DictionaryPatch:
    filename: str
    entry_name: str
    broken_body: str
    fixed_body: str
    affected_quests: tuple[int, ...]


@dataclass(frozen=True)
class DialogReferencePatch:
    broken_reference: str
    expected_occurrences: int
    fixed_reference: str | None = None
    literal: str | None = None

    @property
    def broken_token(self) -> str:
        return f"[%dic:{self.broken_reference}]"

    @property
    def replacement(self) -> str:
        if (self.fixed_reference is None) == (self.literal is None):
            raise ValueError(
                f"{self.broken_reference!r} must define exactly one replacement type"
            )
        if self.fixed_reference is not None:
            return f"[%dic:{self.fixed_reference}]"
        return self.literal or ""


PATCHES = (
    DictionaryPatch(
        "client_strings_dic_people.xml",
        "STR_DIC_N_Hianu",
        "[%dic:STR_DIC_FLA15]\u7684\u730e\u4eba\u3002\u59ae\u7f8e\u96c5\u7684\u4e08\u592b\uff0c\u5bf9\u59bb\u5b50\u8fc7\u5ea6\u7684\u5e72\u6d89\u611f\u5230\u5f88\u538c\u70e6\u3002 ",
        "\u897f\u4e9a\u8bfa;[%dic:STR_DIC_FLA15]\u7684\u730e\u4eba\u3002\u59ae\u7f8e\u96c5\u7684\u4e08\u592b\uff0c\u5bf9\u59bb\u5b50\u8fc7\u5ea6\u7684\u5e72\u6d89\u611f\u5230\u5f88\u538c\u70e6\u3002 ",
        (1153,),
    ),
    DictionaryPatch(
        "client_strings_dic_item.xml",
        "STR_DIC_I_quest_23020a",
        "\u5728\u6c61\u67d3\u7684\u5fb7\u62c9\u574e\u8eab\u4e0a\u83b7\u5f97\u7684\u4f0a\u5fb7\u51dd\u80f6\u7bb1\u5b50\u788e\u7247\u3002\u597d\u50cf\u4f1a\u5bf9\u67e5\u660e\u9f99\u65cf\u53d7\u5230\u6c61\u67d3\u7684\u539f\u56e0\u6709\u6240\u5e2e\u52a9\u3002\u5728[%dic:STR_DIC_E_LDF5b_B2_VriZomb]\u8eab\u4e0a\u53ef\u4ee5\u83b7\u5f97\u3002",
        "\u6c61\u67d3\u7684\u4f0a\u5fb7\u51dd\u80f6\u7bb1\u5b50\u788e\u5757;\u5728\u6c61\u67d3\u7684\u5fb7\u62c9\u574e\u8eab\u4e0a\u83b7\u5f97\u7684\u4f0a\u5fb7\u51dd\u80f6\u7bb1\u5b50\u788e\u7247\u3002\u597d\u50cf\u4f1a\u5bf9\u67e5\u660e\u9f99\u65cf\u53d7\u5230\u6c61\u67d3\u7684\u539f\u56e0\u6709\u6240\u5e2e\u52a9\u3002\u5728[%dic:STR_DIC_E_LDF5b_B2_VriZomb]\u8eab\u4e0a\u53ef\u4ee5\u83b7\u5f97\u3002",
        (23020,),
    ),
    DictionaryPatch(
        "client_strings_dic_item.xml",
        "STR_DIC_I_noblemetal_d_q5303_65a",
        "\u4f69\u5c14\u519c\u9732\u6c34\uff1a\u4f69\u5c14\u519c\u53ea\u5728\u5305\u62ec\u4f69\u5c14\u519c\u5728\u5185\u7684\u9b54\u65cf\u4e13\u7528\u5730\u751f\u957f\uff0c\u7528\u5b83\u7684\u9732\u6c34\u805a\u96c6\u800c\u6210\u3002\u91c7\u96c6\u65f6\u4f3c\u4e4e\u9700\u8981\u796d\u575b\u73bb\u7483\u74f6\u3002",
        "\u4f69\u5c14\u519c\u9732\u6c34;\u4f69\u5c14\u519c\u53ea\u5728\u5305\u62ec\u4f69\u5c14\u519c\u5728\u5185\u7684\u9b54\u65cf\u4e13\u7528\u5730\u751f\u957f\uff0c\u7528\u5b83\u7684\u9732\u6c34\u805a\u96c6\u800c\u6210\u3002\u91c7\u96c6\u65f6\u4f3c\u4e4e\u9700\u8981\u796d\u575b\u73bb\u7483\u74f6\u3002",
        (25303, 25313),
    ),
)


DIALOG_REFERENCE_PATCHES = (
    DialogReferencePatch(
        "STR_DIC_E_DF6_Mission_Light_All", 1, "STR_DIC_E_DF6_Mission_Dark_All"
    ),
    DialogReferencePatch("STR_DIC_E_DP_Ri", 5, "STR_DIC_E_DP_RI"),
    DialogReferencePatch(
        "STR_DIC_E_LF3_work_production", 1, "STR_DIC_E_LF3_Work_production"
    ),
    DialogReferencePatch("STR_DIC_I_QUEST_13655a", 2, "STR_DIC_I_quest_13655a"),
    DialogReferencePatch("STR_DIC_I_QUEST_22018a", 2, "STR_DIC_I_quest_22018a"),
    DialogReferencePatch("STR_DIC_I_QUEST_23056A", 6, "STR_DIC_I_quest_23056a"),
    DialogReferencePatch("STR_DIC_I_QUEST_23300A", 1, "STR_DIC_I_quest_23300a"),
    DialogReferencePatch("STR_DIC_I_QUEST_23307A", 1, "STR_DIC_I_quest_23307a"),
    DialogReferencePatch("STR_DIC_I_QUEST_23317A", 1, "STR_DIC_I_quest_23317a"),
    DialogReferencePatch("STR_DIC_I_QUEST_23327A", 1, "STR_DIC_I_quest_23327a"),
    DialogReferencePatch("STR_DIC_I_QUEST_23336A", 1, "STR_DIC_I_quest_23336a"),
    DialogReferencePatch("STR_DIC_I_QUEST_23337A", 2, "STR_DIC_I_quest_23337a"),
    DialogReferencePatch("STR_DIC_I_QUEST_23341A", 1, "STR_DIC_I_quest_23341a"),
    DialogReferencePatch("STR_DIC_I_QUEST_23345A", 1, "STR_DIC_I_quest_23345a"),
    DialogReferencePatch("STR_DIC_I_QUEST_24113A", 2, "STR_DIC_I_quest_24113a"),
    DialogReferencePatch("STR_DIC_I_QUEST_41512a", 2, "STR_DIC_I_quest_41512a"),
    DialogReferencePatch("STR_DIC_I_QUEST_41594a", 2, "STR_DIC_I_quest_41594a"),
    DialogReferencePatch("STR_DIC_I_quest_212558a", 1, "STR_DIC_I_quest_22558a"),
    DialogReferencePatch("STR_DIC_I_quest_24062a", 1, "STR_DIC_I_QUEST_24062a"),
    DialogReferencePatch("STR_DIC_I_quest_9714a", 1, literal="美味年糕汤"),
    DialogReferencePatch("STR_DIC_I_quest_9715a", 1, literal="美味年糕汤"),
    DialogReferencePatch(
        "STR_DIC_LDF5a_DieVritra_E_LMDrM", 1, "STR_DIC_LDF5a_DieVritraL_E"
    ),
    DialogReferencePatch(
        "STR_DIC_LDF5a_DieYun_E_LMYW", 1, "STR_DIC_LDF5a_DieYunL_E"
    ),
    DialogReferencePatch("STR_DIC_LDF5a_Gram_E_LHM", 3, "STR_DIC_LDF5a_Gram_E"),
    DialogReferencePatch("STR_DIC_LDF5a_Meib_E_LHW", 2, "STR_DIC_LDF5a_Meib_E"),
    DialogReferencePatch(
        "STR_DIC_LDF5a_Mycenae_E_LHW", 1, "STR_DIC_N_LDF5a_Mycenae_E"
    ),
    DialogReferencePatch("STR_DIC_LDF5a_Rone_E_LHW", 1, literal="洛内"),
    DialogReferencePatch(
        "STR_DIC_LDF5a_Tauros_E_LHM", 1, "STR_DIC_N_LDF5a_Tauros_E"
    ),
    DialogReferencePatch(
        "STR_DIC_LDF5a_Thrasir_E_LHM", 1, "STR_DIC_LDF5a_Thrasir_E"
    ),
    DialogReferencePatch("STR_DIC_LDF5a_Torio_E_LMYM", 3, "STR_DIC_LDF5a_Torio_E"),
    DialogReferencePatch("STR_DIC_LDF5a_Trace4_E_LHM", 3, literal="扎坎"),
    DialogReferencePatch(
        "STR_DIC_LDF5a_YDeadBody_Q10086a", 1, literal="被伊德污染的卡伦护卫队"
    ),
    DialogReferencePatch("STR_DIC_LDF5b_Jin_E_LHW", 1, "STR_DIC_N_LDF5b_Jin_E"),
    DialogReferencePatch(
        "STR_DIC_M_LDF5B_E3_Shulack_Debuff_Mu_Nmd_64_An",
        3,
        "STR_DIC_M_LDF5b_E3_Shulack_Debuff_Mu_Nmd_64_An",
    ),
    DialogReferencePatch(
        "STR_DIC_M_LDF5_under_GhostRune_Q_01",
        10,
        "STR_DIC_M_LDF5_Under_GhostRune_Q_01",
    ),
    DialogReferencePatch(
        "STR_DIC_M_LDF5_under_GhostRune_Q_02",
        10,
        "STR_DIC_M_LDF5_Under_GhostRune_Q_02",
    ),
    DialogReferencePatch(
        "STR_DIC_M_LDF5_under_GhostRune_Q_03",
        10,
        "STR_DIC_M_LDF5_Under_GhostRune_Q_03",
    ),
    DialogReferencePatch(
        "STR_DIC_M_LDF5_under_GhostRune_Q_04",
        10,
        "STR_DIC_M_LDF5_Under_GhostRune_Q_04",
    ),
    DialogReferencePatch(
        "STR_DIC_M_LizardPet_DrakeD_43_An_tune",
        3,
        "STR_DIC_M_LizardPet_DrakeD_43_An_Tune",
    ),
    DialogReferencePatch(
        "STR_DIC_M_SouledstonebabyD_17_An",
        2,
        "STR_DIC_M_SouledstoneBabyD_17_An",
    ),
    DialogReferencePatch(
        "STR_DIC_NPC_event_event_Dirull", 1, "STR_DIC_N_event_Dirull"
    ),
    DialogReferencePatch("STR_DIC_NPC_event_hinjel", 2, "STR_DIC_NPC_event_Hinjel"),
    DialogReferencePatch(
        "STR_DIC_N_Corridoer_D_PangeaQuest_E",
        4,
        "STR_DIC_N_Corridor_D_PangeaQuest_E",
    ),
    DialogReferencePatch(
        "STR_DIC_N_Corridoer_L_PangeaQuest_E",
        3,
        "STR_DIC_N_Corridor_L_PangeaQuest_E",
    ),
    DialogReferencePatch("STR_DIC_N_DF6_Deser_E", 1, "STR_DIC_N_DF6_Wuste_E"),
    DialogReferencePatch("STR_DIC_N_ascalon", 3, "STR_DIC_N_Ascalon"),
    DialogReferencePatch("STR_DIC_W_LDF5A_E5", 2, "STR_DIC_W_LDF5a_E5"),
    DialogReferencePatch("STR_DIC_W_LDF5a_A2", 3, "STR_DIC_W_LDF5A_A2"),
    DialogReferencePatch("STR_DIC_W_LDF5a_A4", 5, "STR_DIC_W_LDF5A_A4"),
    DialogReferencePatch("STR_DIC_W_LDF5a_B6", 4, "STR_DIC_W_LDF5A_B6"),
    DialogReferencePatch("STR_N_Owlau_DF4_01", 3, "STR_DIC_N_Owlau_DF4_01"),
    DialogReferencePatch("str_dic_fla18", 2, "STR_DIC_FLA18"),
    DialogReferencePatch("str_dic_la30", 1, "STR_DIC_LA30"),
    DialogReferencePatch("str_dic_mla23", 1, "STR_DIC_MLA23"),
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate loose Aion 5.8 CHS string overrides for broken quest dictionary labels."
    )
    parser.add_argument(
        "--strings-dir",
        type=Path,
        required=True,
        help="Decoded L10N/CHS/Data/Strings directory from the authoritative client data.pak.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=DEFAULT_OUTPUT_DIR,
        help="Destination directory for the two patched XML override files.",
    )
    parser.add_argument(
        "--dialogs-dir",
        type=Path,
        help="Decoded Data/Dialogs directory. Defaults to the sibling of --strings-dir.",
    )
    parser.add_argument(
        "--dialog-output-dir",
        type=Path,
        default=DEFAULT_DIALOG_OUTPUT_DIR,
        help="Destination directory for patched quest HTML override files.",
    )
    return parser.parse_args()


def dictionary_bodies(path: Path) -> dict[str, str]:
    bodies: dict[str, str] = {}
    for entry in ET.parse(path).getroot().findall(".//string"):
        name = (entry.findtext("name") or "").strip()
        body = entry.find("body")
        if not name or body is None or body.text is None:
            continue
        if name in bodies:
            raise ValueError(f"duplicate dictionary entry {name!r} in {path}")
        bodies[name] = body.text
    return bodies


def patch_text(source: Path, patches: list[DictionaryPatch]) -> str:
    bodies = dictionary_bodies(source)
    raw = source.read_bytes()
    has_bom = raw.startswith(b"\xef\xbb\xbf")
    text = raw.decode("utf-8-sig")
    for patch in patches:
        current = bodies.get(patch.entry_name)
        if current == patch.fixed_body:
            continue
        if current != patch.broken_body:
            raise ValueError(
                f"unexpected body for {patch.entry_name!r} in {source}: {current!r}"
            )
        old = f"<body>{patch.broken_body}</body>"
        new = f"<body>{patch.fixed_body}</body>"
        if text.count(old) != 1:
            raise ValueError(f"expected one exact body for {patch.entry_name!r} in {source}")
        text = text.replace(old, new, 1)
    return ("\ufeff" if has_bom else "") + text


def generate_patch(strings_dir: Path, output_dir: Path) -> list[Path]:
    grouped: dict[str, list[DictionaryPatch]] = defaultdict(list)
    for patch in PATCHES:
        grouped[patch.filename].append(patch)

    output_dir.mkdir(parents=True, exist_ok=True)
    outputs = []
    for filename, patches in sorted(grouped.items()):
        source = strings_dir / filename
        if not source.is_file():
            raise FileNotFoundError(source)
        destination = output_dir / filename
        destination.write_text(patch_text(source, patches), encoding="utf-8", newline="")
        fixed = dictionary_bodies(destination)
        for patch in patches:
            if fixed.get(patch.entry_name) != patch.fixed_body:
                raise ValueError(f"failed to patch {patch.entry_name!r} in {destination}")
        outputs.append(destination)
    return outputs


def generate_dialog_patch(dialogs_dir: Path, output_dir: Path) -> list[Path]:
    patches_by_token = {patch.broken_token: patch for patch in DIALOG_REFERENCE_PATCHES}
    if len(patches_by_token) != len(DIALOG_REFERENCE_PATCHES):
        raise ValueError("duplicate broken dialog reference")
    token_pattern = re.compile("|".join(re.escape(token) for token in patches_by_token))
    counts: Counter[str] = Counter()
    pending: list[tuple[Path, bytes]] = []

    for source in sorted(dialogs_dir.rglob("*.html")):
        if not QUEST_DIALOG_NAME.fullmatch(source.name) or "unused" in source.parts:
            continue
        raw = source.read_bytes()
        has_bom = raw.startswith(b"\xef\xbb\xbf")
        text = raw.decode("utf-8-sig")

        def replace(match: re.Match[str]) -> str:
            patch = patches_by_token[match.group(0)]
            counts[patch.broken_reference] += 1
            return patch.replacement

        fixed = token_pattern.sub(replace, text)
        if fixed != text:
            relative = source.relative_to(dialogs_dir)
            encoded = fixed.encode("utf-8")
            if has_bom:
                encoded = b"\xef\xbb\xbf" + encoded
            pending.append((relative, encoded))

    for patch in DIALOG_REFERENCE_PATCHES:
        actual = counts[patch.broken_reference]
        if actual != patch.expected_occurrences:
            raise ValueError(
                f"unexpected reference count for {patch.broken_reference!r}: "
                f"expected {patch.expected_occurrences}, found {actual}"
            )

    outputs = []
    for relative, data in pending:
        destination = output_dir / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
        outputs.append(destination)
    return outputs


def validate_task_dialogs(
    strings_dir: Path,
    string_output_dir: Path,
    dialogs_dir: Path,
    dialog_output_dir: Path,
) -> tuple[int, int]:
    dictionary: dict[str, str] = {}
    for source in sorted(strings_dir.glob("client_strings_dic_*.xml")):
        overlay = string_output_dir / source.name
        for name, body in dictionary_bodies(overlay if overlay.is_file() else source).items():
            if name in dictionary:
                raise ValueError(f"duplicate dictionary entry {name!r} across string tables")
            dictionary[name] = body

    reference_pattern = re.compile(r"\[%dic:([^\]]+)\]")
    missing: dict[str, Path] = {}
    without_display_separator: dict[str, Path] = {}
    file_count = 0
    reference_count = 0
    for source in sorted(dialogs_dir.rglob("*.html")):
        if not QUEST_DIALOG_NAME.fullmatch(source.name) or "unused" in source.parts:
            continue
        relative = source.relative_to(dialogs_dir)
        overlay = dialog_output_dir / relative
        text = (overlay if overlay.is_file() else source).read_text(encoding="utf-8-sig")
        file_count += 1
        for reference in reference_pattern.findall(text):
            reference_count += 1
            body = dictionary.get(reference)
            if body is None:
                missing.setdefault(reference, relative)
            elif ";" not in body:
                without_display_separator.setdefault(reference, relative)

    if missing or without_display_separator:
        problems = []
        for reference, relative in sorted(missing.items()):
            problems.append(f"missing {reference!r} in {relative}")
        for reference, relative in sorted(without_display_separator.items()):
            problems.append(f"missing display separator for {reference!r} in {relative}")
        raise ValueError("invalid quest dialog dictionary references: " + "; ".join(problems))
    return file_count, reference_count


def main() -> None:
    args = parse_args()
    dialogs_dir = args.dialogs_dir or args.strings_dir.parent / "Dialogs"
    outputs = generate_patch(args.strings_dir, args.output_dir)
    outputs += generate_dialog_patch(dialogs_dir, args.dialog_output_dir)
    file_count, reference_count = validate_task_dialogs(
        args.strings_dir,
        args.output_dir,
        dialogs_dir,
        args.dialog_output_dir,
    )
    for path in outputs:
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        print(f"{path} sha256={digest}")
    print(f"validated {file_count} quest dialogs and {reference_count} dictionary references")


if __name__ == "__main__":
    main()
