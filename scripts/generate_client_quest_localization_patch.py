#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import xml.etree.ElementTree as ET
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path


DEFAULT_OUTPUT_DIR = Path("patch/L10N/CHS/Data/Strings")


@dataclass(frozen=True)
class DictionaryPatch:
    filename: str
    entry_name: str
    broken_body: str
    fixed_body: str
    affected_quests: tuple[int, ...]


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


def main() -> None:
    args = parse_args()
    for path in generate_patch(args.strings_dir, args.output_dir):
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        print(f"{path} sha256={digest}")


if __name__ == "__main__":
    main()
