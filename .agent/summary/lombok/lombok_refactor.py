#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
lombok_refactor.py — 将平凡 getter/setter 安全替换为 Lombok 字段级注解。
Safely replace trivial getters/setters with field-level Lombok annotations.

严格转换标准（全部满足才转换） / Strict conversion criteria (ALL must hold):
  1. 文件仅含一个类型声明（无嵌套类/枚举/接口、无枚举常量类体），非 record、非 @interface、无文本块
  2. 方法为 public、非 static、非 final、非 synchronized、无任何注解
  3. 方法体恰好一条语句：getter 为 return [this.]field; setter 为 [this.]field = param;
  4. 方法名、返回/参数类型与字段完全一致，且与 Lombok 生成签名一致
     （boolean 字段 → isX()；Boolean/其他类型 → getX()；is 前缀 boolean 字段按 Lombok 规则）
  5. 字段非 static；setter 仅用于非 final 字段；不使用带参 Lombok 注解（如 @Getter(lazy=true)）
  6. 被删方法的 javadoc 在字段无文档时迁移到字段上，避免文档丢失
  7. 字段已有等价 Lombok 注解或类级注解已覆盖时，仅删除方法，不重复加注解

实现要点：所有结构分析在"掩码文本"上进行（注释与字符串字面量被替换为等长空格，
偏移不变），避免注释/字符串中的 ; { } @ 等字符干扰解析。
All structural analysis runs on a "masked" text where comments and string/char
literals are replaced by length-preserving spaces, so offsets stay valid.

用法 / Usage:
  python3 lombok_refactor.py scan <dir>... [--files ...] [--limit N]   仅分析
  python3 lombok_refactor.py apply <dir>... [--files ...] [--limit N]  应用转换
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

# ---------------------------------------------------------------------------
# 正则 / Regexes
# ---------------------------------------------------------------------------

FIELD_RE = re.compile(
    r"^(?P<indent>[ \t]*)"
    r"(?:(?:@[\w.]+(?:\([^)]*\))?)\s+)*"
    r"(?P<mods>(?:(?:private|protected|public|final|static|volatile|transient)\s+)*)"
    r"(?P<type>[\w$<>\[\],.?]+(?:\s*<[^\n;{}()]*>)?(?:\[\])*)"
    r"\s+(?P<name>[a-zA-Z_$][\w$]*)"
    r"\s*(?P<init>=[^;]*)?;\s*$",
    re.MULTILINE,
)

METHOD_RE = re.compile(
    r"^(?P<indent>[ \t]*)"
    r"public\s+"
    r"(?P<mods>(?:(?:static|final|synchronized|abstract|native|strictfp)\s+)*)"
    r"(?P<ret>[\w$<>\[\],.?]+(?:\s*<[^\n;{}()]*>)?(?:\[\])*)"
    r"\s+(?P<name>(?:get|is|set)[A-Z][\w$]*)"
    r"\s*\((?P<params>[^)]*)\)"
    r"\s*(?:throws\s+[\w.,\s]+)?\{",
    re.MULTILINE,
)

JAVADOC_RE = re.compile(r"/\*\*.*?\*/", re.DOTALL)

# 类中所有方法声明（用于 Lombok 同名+同参数个数冲突检测）
# All method declarations (for Lombok same-name+same-param-count conflict check)
ALL_METHOD_RE = re.compile(
    r"^[ \t]*"
    r"(?:public|protected|private)\s+"
    r"(?:(?:static|final|synchronized|abstract|native|strictfp|default)\s+)*"
    r"[\w$<>\[\],.?]+(?:\s*<[^\n;{}()]*>)?(?:\[\])*"
    r"\s+([a-zA-Z_$][\w$]*)\s*"
    r"\(([^)]*)\)"
    r"\s*(?:throws\s+[\w.,\s]+)?\{",
    re.MULTILINE,
)
BLOCK_COMMENT_RE = re.compile(r"/\*.*?\*/", re.DOTALL)
LINE_COMMENT_RE = re.compile(r"//[^\n]*")
TYPE_DECL_RE = re.compile(r"\b(?P<kw>class|enum|interface|record)\s+[\w$]+")
ANN_RE = re.compile(r"@([\w.]+)")
MASK_RE = re.compile(
    r'"(?:\\.|[^"\\\n])*"'       # 字符串字面量 / string literal
    r"|'(?:\\.|[^'\\\n])*'"      # 字符字面量 / char literal
    r"|/\*.*?\*/"                # 块注释 / block comment
    r"|//[^\n]*",                # 行注释 / line comment
    re.DOTALL,
)


def mask_code(text: str) -> str:
    """注释/字面量替换为等长空格（保留换行），保持偏移不变 / Mask comments & literals, preserving offsets."""

    def repl(m: re.Match) -> str:
        return "".join("\n" if c == "\n" else " " for c in m.group(0))

    return MASK_RE.sub(repl, text)


def strip_comments(s: str) -> str:
    s = BLOCK_COMMENT_RE.sub("", s)
    s = LINE_COMMENT_RE.sub("", s)
    return s


def strip_annotations(s: str) -> str:
    """移除注解（含带括号参数的多行注解）/ Remove annotations incl. multi-line ones with args."""
    out = []
    i = 0
    n = len(s)
    while i < n:
        if s[i] == "@" and i + 1 < n and (s[i + 1].isalpha() or s[i + 1] == "_"):
            j = i + 1
            while j < n and (s[j].isalnum() or s[j] in "._$"):
                j += 1
            k = j
            while k < n and s[k] in " \t\n":
                k += 1
            if k < n and s[k] == "(":
                depth = 0
                while k < n:
                    if s[k] == "(":
                        depth += 1
                    elif s[k] == ")":
                        depth -= 1
                        if depth == 0:
                            k += 1
                            break
                    k += 1
                j = k
            i = j
        else:
            out.append(s[i])
            i += 1
    return "".join(out)


def lombok_getter_name(fname: str, ftype: str) -> str:
    """按 Lombok 规则计算 getter 名 / Lombok getter name for a field."""
    if ftype == "boolean":
        if fname.startswith("is") and len(fname) > 2 and fname[2].isupper():
            return fname  # boolean isRunning -> isRunning()
        return "is" + fname[0].upper() + fname[1:]
    return "get" + fname[0].upper() + fname[1:]


def lombok_setter_name(fname: str, ftype: str) -> str:
    """按 Lombok 规则计算 setter 名 / Lombok setter name for a field."""
    if ftype == "boolean" and fname.startswith("is") and len(fname) > 2 and fname[2].isupper():
        base = fname[2:]  # boolean isRunning -> setRunning(..)
    else:
        base = fname
    return "set" + base[0].upper() + base[1:]


def norm_type(t: str) -> str:
    return re.sub(r"\s+", "", t)


def find_matching_brace(text: str, open_idx: int) -> int | None:
    """在掩码文本上做括号配对 / Brace matching on masked text."""
    depth = 0
    i = open_idx
    n = len(text)
    while i < n:
        c = text[i]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return None


def member_depths(masked: str) -> tuple[list[int], list[int]]:
    """
    计算每行行首的括号深度与行起始偏移（基于掩码文本）。
    Return (line_start_offsets, depth_at_each_line_start) for the masked text.
    类成员（字段/方法）位于深度 1；方法体/局部块内为更深。
    """
    starts: list[int] = [0]
    depths: list[int] = [0]
    depth = 0
    for m in re.finditer(r"\n|[{}]", masked):
        tok = m.group(0)
        if tok == "{":
            depth += 1
        elif tok == "}":
            depth -= 1
        else:  # 换行：记录下一行行首的深度
            starts.append(m.end())
            depths.append(depth)
    return starts, depths


def depth_at_line(line_starts: list[int], depths: list[int], pos: int) -> int:
    """pos 所在行行首的深度 / brace depth at the start of the line containing pos."""
    import bisect

    idx = bisect.bisect_right(line_starts, pos) - 1
    return depths[idx] if idx >= 0 else 0


class MemberRegion:
    """声明上方紧邻的 javadoc/注解区域 / Attached javadoc/annotation region above a declaration."""

    def __init__(self, text: str, masked: str, decl_start: int, floor: int):
        prev = max(masked.rfind("}", 0, decl_start), masked.rfind(";", 0, decl_start), floor)
        region_start = prev + 1
        region = text[region_start:decl_start]
        # 若区域起点是上一成员同一行的行尾内容（如行尾注释），先跳过该行，
        # 避免删除区间错误地覆盖上一成员的声明行。
        # If the region begins mid-line (trailing content of the previous member,
        # e.g. a same-line comment), skip that line before locating the attached block.
        nl = region.find("\n")
        if nl != -1 and region[:nl].strip():
            region = region[nl + 1 :]
            region_start += nl + 1
        self.region = region
        self.region_start = region_start
        code_part = strip_annotations(strip_comments(self.region))
        self.annotations = {a.split(".")[-1] for a in ANN_RE.findall(strip_comments(self.region))}
        jds = list(JAVADOC_RE.finditer(self.region))
        self.javadoc: str | None = None
        if jds:
            tail = strip_annotations(strip_comments(self.region[jds[-1].end() :]))
            if not tail.strip():
                self.javadoc = jds[-1].group(0)
        m = re.search(r"\S", self.region)
        if m:
            self.block_start = text.rfind("\n", 0, self.region_start + m.start()) + 1
        else:
            self.block_start = decl_start
        self.has_code = bool(code_part.strip())


def analyze_file(path: Path) -> dict:
    text = path.read_text(encoding="utf-8")
    masked = mask_code(text)
    result = {"file": str(path), "conversions": [], "skipped": [], "rejected": False}

    def reject(reason: str) -> dict:
        result["rejected"] = True
        result["reason"] = reason
        return result

    # 全局拒绝条件 / Whole-file rejection criteria
    if '"""' in text:
        return reject("text block")
    if re.search(r"\brecord\s+[\w$]+\s*\(", masked):
        return reject("record")
    if "@interface" in masked:
        return reject("annotation type")
    if re.search(r"@(Getter|Setter|Data|Value)\s*\(", masked):
        return reject("lombok annotation with args (e.g. lazy)")

    decls = list(TYPE_DECL_RE.finditer(masked))
    if len(decls) != 1:
        return reject(f"type declarations: {len(decls)}")
    decl = decls[0]
    class_brace = masked.find("{", decl.end())
    if class_brace == -1:
        return reject("no class body")

    # 枚举常量区含类体（常量特定实现）→ 拒绝 / enum constant-specific bodies -> reject
    if decl.group("kw") == "enum":
        semi = masked.find(";", class_brace)
        if semi != -1 and "{" in masked[class_brace + 1 : semi]:
            return reject("enum constant class body")

    # 类级 Lombok 注解 / class-level lombok annotations
    cls_region = MemberRegion(text, masked, decl.start(), -1)
    cls_has_getter = bool(cls_region.annotations & {"Getter", "Data"})
    cls_has_setter = bool(cls_region.annotations & {"Setter", "Data"})

    # 行深度表（类成员=1，方法体内部>1）/ per-line brace depths (members=1)
    line_starts, line_depths = member_depths(masked)

    # 字段表 / field table
    fields: dict[str, dict] = {}
    for fm in FIELD_RE.finditer(masked):
        if depth_at_line(line_starts, line_depths, fm.start()) != 1:
            continue  # 方法体内的变量声明不是字段 / declarations inside bodies are not fields
        name = fm.group("name")
        reg = MemberRegion(text, masked, fm.start(), class_brace)
        inline = {a.split(".")[-1] for a in ANN_RE.findall(strip_comments(text[fm.start() : fm.end()]))}
        fields[name] = {
            "name": name,
            "type": fm.group("type").strip(),
            "mods": fm.group("mods").split(),
            "start": fm.start(),
            "line_start": fm.start(),
            "indent": fm.group("indent"),
            "annotations": reg.annotations | inline,
            "has_javadoc": reg.javadoc is not None,
        }

    # 全部方法的 (名称, 参数个数) 签名表 / signature table of all methods: (name, param_count)
    def param_count(params: str) -> int:
        p = params.strip()
        if not p:
            return 0
        # 去掉泛型实参避免逗号误计 / strip generic args to avoid counting their commas
        prev = None
        while prev != p:
            prev = p
            p = re.sub(r"<[^<>]*>", "", p)
        return p.count(",") + 1

    method_sigs: list[tuple[str, int, int]] = []
    for am in ALL_METHOD_RE.finditer(masked):
        method_sigs.append((am.group(1), param_count(am.group(2)), am.start()))

    for mm in METHOD_RE.finditer(masked):
        if depth_at_line(line_starts, line_depths, mm.start()) != 1:
            continue  # 方法体/匿名类内部的同形方法不是类成员 / not a class member
        name = mm.group("name")
        mods = (mm.group("mods") or "").split()

        def skip(reason: str) -> None:
            result["skipped"].append({"method": name, "reason": reason})

        reg = MemberRegion(text, masked, mm.start(), class_brace)
        if reg.annotations:
            skip(f"annotated: {sorted(reg.annotations)}")
            continue
        if "static" in mods:
            skip("static")
            continue
        if "final" in mods:
            skip("final method")
            continue
        if "synchronized" in mods:
            skip("synchronized")
            continue

        open_idx = mm.end() - 1
        close_idx = find_matching_brace(masked, open_idx)
        if close_idx is None:
            skip("unbalanced braces")
            continue
        body = text[open_idx + 1 : close_idx].strip()

        if name.startswith("set"):
            bm = re.fullmatch(r"(?:this\.)?([a-zA-Z_$][\w$]*)\s*=\s*([a-zA-Z_$][\w$]*)\s*;", body, re.DOTALL)
            kind = "setter"
        else:
            bm = re.fullmatch(r"return\s+(?:this\.)?([a-zA-Z_$][\w$]*)\s*;", body, re.DOTALL)
            kind = "getter"
        if not bm:
            skip("non-trivial body")
            continue
        if kind == "getter" and mm.group("params").strip():
            skip("getter with parameters (not an accessor)")
            continue

        field_name = bm.group(1)
        target = fields.get(field_name)
        if target is None:
            skip(f"no field '{field_name}'")
            continue
        if "static" in target["mods"]:
            skip("static field")
            continue

        if kind == "getter":
            expected = lombok_getter_name(field_name, target["type"])
            if name != expected:
                skip(f"name mismatch: lombok would generate '{expected}' for field '{field_name}'")
                continue
            if norm_type(mm.group("ret")) != norm_type(target["type"]):
                skip(f"return type '{mm.group('ret')}' != field type '{target['type']}'")
                continue
        else:
            expected = lombok_setter_name(field_name, target["type"])
            if name != expected:
                skip(f"name mismatch: lombok would generate '{expected}' for field '{field_name}'")
                continue
            if "final" in target["mods"]:
                skip("final field with setter")
                continue
            params = [p.strip() for p in mm.group("params").split(",") if p.strip()]
            if len(params) != 1:
                skip("setter param count != 1")
                continue
            pm = re.match(r"(?:final\s+)?([\w$<>\[\],.?]+(?:\s*<[^\n;{}()]*>)?(?:\[\])*)\s+([a-zA-Z_$][\w$]*)$", params[0])
            if not pm:
                skip("unparseable setter param")
                continue
            ptype, pname = pm.group(1), pm.group(2)
            if pname != bm.group(2):
                skip("assigned value is not the param")
                continue
            if norm_type(ptype) != norm_type(target["type"]):
                skip(f"param type '{ptype}' != field type '{target['type']}'")
                continue

        # Lombok 冲突规则：类中已存在同名且同参数个数的方法时，Lombok 不会生成
        # Lombok skips generation when another method with same name & param count exists
        cand_pc = 0 if kind == "getter" else 1
        if any(sname == name and spc == cand_pc and sstart != mm.start() for sname, spc, sstart in method_sigs):
            skip(f"lombok conflict: another '{name}' with {cand_pc} param(s) exists")
            continue

        # 删除区间 / removal span
        remove_start = reg.block_start
        remove_end = close_idx + 1
        if text[remove_end : remove_end + 1] == "\n":
            remove_end += 1
        if text[remove_end : remove_end + 1] == "\n":
            remove_end += 1  # 吸收一个后续空行 / absorb one trailing blank line
        # 注意：不做 remove_start 前移，避免与相邻转换的删除区间重叠
        # NOTE: never shift remove_start backwards - it can overlap the previous removal span

        needs_annotation = not (
            (kind == "getter" and (cls_has_getter or "Getter" in target["annotations"] or "Data" in target["annotations"]))
            or (kind == "setter" and (cls_has_setter or "Setter" in target["annotations"] or "Data" in target["annotations"]))
        )
        result["conversions"].append(
            {
                "kind": kind,
                "method_name": name,
                "field_name": field_name,
                "remove_start": remove_start,
                "remove_end": remove_end,
                "javadoc": reg.javadoc,
                "move_javadoc": reg.javadoc is not None and not target["has_javadoc"],
                "needs_annotation": needs_annotation,
                "annotation": "@Getter" if kind == "getter" else "@Setter",
                "field_start": target["start"],
                "field_line_start": target["line_start"],
                "field_indent": target["indent"],
            }
        )

    return result


def apply_file(path: Path, analysis: dict) -> str:
    text = path.read_text(encoding="utf-8")
    convs = analysis["conversions"]
    if not convs:
        return text

    by_field: dict[str, list[dict]] = {}
    for c in convs:
        by_field.setdefault(c["field_name"], []).append(c)

    edits: list[tuple[int, int, str]] = []
    removals = sorted((c["remove_start"], c["remove_end"]) for c in convs)
    # 防御性检查：删除区间不得重叠，否则放弃整个文件 / defensive: refuse overlapping removal spans
    for (s1, e1), (s2, e2) in zip(removals, removals[1:]):
        if s2 < e1:
            print(f"OVERLAP GUARD: skip {path} (overlapping removal spans)")
            return text
    for c in convs:
        edits.append((c["remove_start"], c["remove_end"], ""))

    for fname, clist in by_field.items():
        line_start = clist[0]["field_line_start"]
        indent = clist[0]["field_indent"]
        anns = []
        if any(c["kind"] == "getter" and c["needs_annotation"] for c in clist):
            anns.append(f"{indent}@Getter\n")
        if any(c["kind"] == "setter" and c["needs_annotation"] for c in clist):
            anns.append(f"{indent}@Setter\n")
        jd = next((c["javadoc"] for c in clist if c.get("move_javadoc")), None)
        jd_text = ""
        if jd:
            jd_lines = jd.split("\n")
            parts = []
            for l in jd_lines:
                s = l.strip()
                if not s:
                    parts.append("")
                elif s.startswith("*"):
                    parts.append(indent + " " + s)
                else:
                    parts.append(indent + s)
            jd_text = "\n".join(parts) + "\n"
        insertion = jd_text + "".join(anns)
        if insertion:
            edits.append((line_start, line_start, insertion))

    for start, end, repl in sorted(edits, key=lambda e: e[0], reverse=True):
        text = text[:start] + repl + text[end:]

    # 清理意外产生的 3+ 连续空行 / collapse accidental 3+ blank lines
    text = re.sub(r"\n{4,}", "\n\n\n", text)
    # 清理右大括号前的空行（删除类末尾方法后残留）/ collapse blank lines left before closing braces
    text = re.sub(r"\n{2,}([ \t]*})", r"\n\1", text)

    need_getter = any(c["kind"] == "getter" and c["needs_annotation"] for c in convs)
    need_setter = any(c["kind"] == "setter" and c["needs_annotation"] for c in convs)
    imports_to_add = []
    if need_getter and not re.search(r"^import\s+lombok\.Getter;", text, re.MULTILINE):
        imports_to_add.append("import lombok.Getter;\n")
    if need_setter and not re.search(r"^import\s+lombok\.Setter;", text, re.MULTILINE):
        imports_to_add.append("import lombok.Setter;\n")
    if imports_to_add:
        existing = list(re.finditer(r"^import\s+[^;]+;\n", text, re.MULTILINE))
        if existing:
            pos = existing[-1].end()
            text = text[:pos] + "".join(imports_to_add) + text[pos:]
        else:
            pm = re.search(r"^package\s+[^;]+;\n", text, re.MULTILINE)
            pos = pm.end()
            text = text[:pos] + "\n" + "".join(imports_to_add) + text[pos:]

    return text


def analyze_ctors(path: Path) -> dict:
    """
    识别可替换为 Lombok 构造器注解的构造器。
    Identify constructors replaceable with Lombok constructor annotations.

    严格标准（全部满足才转换） / Strict criteria (ALL must hold):
      1. 类非 record/@interface/enum/接口，且类上无 @Data/@Value/@Builder 及已有 Lombok 构造器注解
         （这些注解会在显式构造器消失后自行生成构造器，删除手写版会改变签名/可见性）
      2. 类中无 @NonNull 字段（Lombok 生成的构造器会加空检查，语义变化）
      3. 构造器体仅由直接赋值语句组成：this.field = param;（或 field = param; 且 param != field）
      4. 赋值集合与目标注解的参数集合完全一致且按字段声明顺序：
         - 无参且体为空 → @NoArgsConstructor
         - 恰好覆盖全部非 static 字段 → @AllArgsConstructor
         - 恰好覆盖全部"未初始化 final 非 static"字段 → @RequiredArgsConstructor
      5. 参数类型与字段类型完全一致；构造器可见性通过 access 参数复现
    """
    text = path.read_text(encoding="utf-8")
    masked = mask_code(text)
    result = {"file": str(path), "conversions": [], "skipped": [], "rejected": False}

    def reject(reason: str) -> dict:
        result["rejected"] = True
        result["reason"] = reason
        return result

    if '"""' in text:
        return reject("text block")
    stripped_mask = mask_code(text)
    if re.search(r"\brecord\s+[\w$]+\s*\(", stripped_mask):
        return reject("record")
    if "@interface" in stripped_mask:
        return reject("annotation type")

    decls = list(TYPE_DECL_RE.finditer(masked))
    if len(decls) != 1:
        return reject(f"type declarations: {len(decls)}")
    decl = decls[0]
    if decl.group("kw") in ("enum", "interface"):
        return reject(f"constructor annotations unsupported on {decl.group('kw')}")

    class_brace = masked.find("{", decl.end())
    cls_region = MemberRegion(text, masked, decl.start(), -1)
    if cls_region.annotations & {"Data", "Value", "Builder", "SuperBuilder", "NoArgsConstructor", "RequiredArgsConstructor", "AllArgsConstructor"}:
        return reject(f"class already has lombok ctor-affecting annotations: {sorted(cls_region.annotations)}")

    # 有序字段表（含无修饰符的包级私有字段）/ ordered field table (incl. package-private fields)
    line_starts, line_depths = member_depths(masked)
    ordered: list[dict] = []
    for fm in FIELD_RE.finditer(masked):
        if depth_at_line(line_starts, line_depths, fm.start()) != 1:
            continue  # 方法体内的变量声明不是字段 / declarations inside bodies are not fields
        name = fm.group("name")
        reg = MemberRegion(text, masked, fm.start(), class_brace)
        inline = {a.split(".")[-1] for a in ANN_RE.findall(strip_comments(text[fm.start() : fm.end()]))}
        ordered.append(
            {
                "name": name,
                "type": fm.group("type").strip(),
                "mods": fm.group("mods").split(),
                "has_init": fm.group("init") is not None,
                "annotations": reg.annotations | inline,
            }
        )
    if any("NonNull" in f["annotations"] for f in ordered):
        return reject("class has @NonNull fields (generated ctor would add null checks)")

    non_static = [f for f in ordered if "static" not in f["mods"]]
    finals_no_init = [f for f in non_static if "final" in f["mods"] and not f["has_init"]]

    # 类名 / class name
    cname = decl.group(0).split()[-1]
    ctor_re = re.compile(
        rf"^[ \t]*(?P<vis>(?:public|protected|private)\s+)?(?P<name>{re.escape(cname)})\s*\((?P<params>[^)]*)\)\s*(?:throws\s+[\w.,\s]+)?\{{",
        re.MULTILINE,
    )

    def split_params(p: str) -> list[str]:
        parts: list[str] = []
        depth = 0
        cur = ""
        for ch in p:
            if ch == "<":
                depth += 1
            elif ch == ">":
                depth -= 1
            if ch == "," and depth == 0:
                parts.append(cur)
                cur = ""
            else:
                cur += ch
        if cur.strip():
            parts.append(cur)
        return parts

    for cm in ctor_re.finditer(masked):
        if depth_at_line(line_starts, line_depths, cm.start()) != 1:
            continue  # 方法体/内部块中的同名方法不是构造器 / not a class member
        cname_m = cm.group("name")
        vis = (cm.group("vis") or "").strip()

        def skip(reason: str) -> None:
            result["skipped"].append({"constructor": f"{cname_m}({len(split_params(cm.group('params')))} args)", "reason": reason})

        reg = MemberRegion(text, masked, cm.start(), class_brace)
        if reg.annotations:
            skip(f"annotated: {sorted(reg.annotations)}")
            continue

        open_idx = cm.end() - 1
        close_idx = find_matching_brace(masked, open_idx)
        if close_idx is None:
            skip("unbalanced braces")
            continue
        body_code = strip_comments(text[open_idx + 1 : close_idx]).strip()

        # 解析赋值语句 / parse assignment statements
        assigned: list[tuple[str, str, str]] = []  # (field, param, param_type)
        params = split_params(cm.group("params"))
        parsed_params: list[tuple[str, str]] = []  # (type, name)
        ok = True
        for p in params:
            pm = re.match(r"(?:final\s+)?([\w$<>\[\],.?]+(?:\s*<[^\n;{}()]*>)?(?:\[\])*)\s+([a-zA-Z_$][\w$]*)$", p.strip())
            if not pm:
                ok = False
                skip("unparseable param")
                break
            parsed_params.append((pm.group(1), pm.group(2)))
        if not ok:
            continue

        stmts = [s.strip() for s in body_code.split(";") if s.strip()]
        if len(stmts) != len(parsed_params):
            skip("statement count != param count (empty ctor must have no params)")
            continue
        for stmt, (ptype, pname) in zip(stmts, parsed_params):
            am = re.fullmatch(
                r"(?P<this>this\.)?(?P<field>[a-zA-Z_$][\w$]*)\s*=\s*(?:this\.)?(?P<val>[a-zA-Z_$][\w$]*)",
                stmt,
                re.DOTALL,
            )
            if not am:
                ok = False
                skip(f"non-trivial statement: {stmt[:40]!r}")
                break
            fname, val = am.group("field"), am.group("val")
            if val != pname:
                ok = False
                skip(f"rhs is not the param: {stmt[:40]!r}")
                break
            # 裸形式 field = field 是参数自赋值（不触碰字段）；this.f = p 才是真赋值
            # bare form `f = p` with f == p assigns the param to itself, not the field
            if am.group("this") is None and fname == val:
                ok = False
                skip("self-assignment (param shadows field)")
                break
            assigned.append((fname, pname, ptype))
        if not ok:
            continue

        # 分类 / classify
        fields_by_name = {f["name"]: f for f in ordered}
        if not assigned and not parsed_params:
            annotation, target = "NoArgsConstructor", []
        else:
            assigned_names = [a[0] for a in assigned]
            target = None
            if assigned_names == [f["name"] for f in non_static] and all(
                norm_type(a[2]) == norm_type(fields_by_name[a[0]]["type"]) for a in assigned
            ):
                annotation, target = "AllArgsConstructor", non_static
            elif assigned_names == [f["name"] for f in finals_no_init] and all(
                norm_type(a[2]) == norm_type(fields_by_name[a[0]]["type"]) for a in assigned
            ):
                annotation, target = "RequiredArgsConstructor", finals_no_init
            else:
                skip("assigned set != all-fields / non-initialized-finals in declaration order")
                continue
            if not assigned_names:
                skip("empty assignment with params")
                continue

        # 注：经 Maven 环境实证，Lombok 显式构造器注解与类中保留的其他显式构造器可共存，
        # 不存在 setter 那样的"同名同参数个数跳过生成"规则，故此处无需冲突守卫。
        # NOTE: verified under Maven - explicit ctor annotations generate even when other
        # explicit constructors remain, so no same-signature guard is needed here.

        remove_start = reg.block_start
        remove_end = close_idx + 1
        if text[remove_end : remove_end + 1] == "\n":
            remove_end += 1
        if text[remove_end : remove_end + 1] == "\n":
            remove_end += 1

        result["conversions"].append(
            {
                "annotation": annotation,
                "visibility": vis or "package",
                "ctor_name": cname_m,
                "param_count": len(parsed_params),
                "assigned_fields": [a[0] for a in assigned],
                "remove_start": remove_start,
                "remove_end": remove_end,
                "class_decl_line_start": text.rfind("\n", 0, decl.start()) + 1,
                "javadoc": reg.javadoc,
            }
        )

    # 同一文件多注解去重：NoArgsConstructor/RequiredArgs/AllArgs 各至多一个
    seen: set[str] = set()
    unique: list[dict] = []
    for c in result["conversions"]:
        if c["annotation"] in seen:
            result["skipped"].append({"constructor": c["ctor_name"], "reason": f"duplicate {c['annotation']} mapping"})
            continue
        seen.add(c["annotation"])
        unique.append(c)
    result["conversions"] = unique
    return result


def apply_ctors(path: Path, analysis: dict) -> str:
    text = path.read_text(encoding="utf-8")
    convs = analysis["conversions"]
    if not convs:
        return text

    edits: list[tuple[int, int, str]] = [(c["remove_start"], c["remove_end"], "") for c in convs]

    # 类声明行前插入注解 / insert annotations above the class declaration
    by_ann: dict[str, dict] = {}
    for c in convs:
        by_ann[c["annotation"]] = c
    class_indent = ""
    ann_lines = []
    order = ["NoArgsConstructor", "RequiredArgsConstructor", "AllArgsConstructor"]
    for ann in order:
        if ann not in by_ann:
            continue
        c = by_ann[ann]
        vis = c["visibility"]
        if vis == "public":
            ann_lines.append(f"@{ann}\n")
        else:
            level = {"private": "PRIVATE", "protected": "PROTECTED", "package": "PACKAGE"}[vis]
            ann_lines.append(f"@{ann}(access = AccessLevel.{level})\n")
    if ann_lines:
        # 使用类声明行的缩进 / reuse class declaration indentation
        pos = convs[0]["class_decl_line_start"]
        line = text[pos : text.find("\n", pos)]
        class_indent = re.match(r"[ \t]*", line).group(0)
        edits.append((pos, pos, "".join(class_indent + l for l in ann_lines)))

    for start, end, repl in sorted(edits, key=lambda e: e[0], reverse=True):
        text = text[:start] + repl + text[end:]

    text = re.sub(r"\n{4,}", "\n\n\n", text)
    text = re.sub(r"\n{2,}([ \t]*})", r"\n\1", text)

    # 补 import / add imports
    needed = {"NoArgsConstructor": "lombok.NoArgsConstructor", "RequiredArgsConstructor": "lombok.RequiredArgsConstructor", "AllArgsConstructor": "lombok.AllArgsConstructor"}
    imports_to_add = []
    for ann, imp in needed.items():
        if ann in by_ann and not re.search(rf"^import\s+{re.escape(imp)};", text, re.MULTILINE):
            imports_to_add.append(f"import {imp};\n")
    uses_access = any(c["visibility"] != "public" for c in convs)
    if uses_access and not re.search(r"^import\s+lombok\.AccessLevel;", text, re.MULTILINE):
        imports_to_add.append("import lombok.AccessLevel;\n")
    if imports_to_add:
        existing = list(re.finditer(r"^import\s+[^;]+;\n", text, re.MULTILINE))
        if existing:
            pos = existing[-1].end()
            text = text[:pos] + "".join(imports_to_add) + text[pos:]
        else:
            pm = re.search(r"^package\s+[^;]+;\n", text, re.MULTILINE)
            pos = pm.end()
            text = text[:pos] + "\n" + "".join(imports_to_add) + text[pos:]

    return text


def collect_java_files(dirs: list[str], only_files: list[str] | None, excludes: list[str]) -> list[Path]:
    if only_files:
        return [Path(f) for f in only_files]
    files: list[Path] = []
    for d in dirs:
        for f in sorted(Path(d).rglob("*.java")):
            if any(str(f).find(x) != -1 for x in excludes):
                continue
            files.append(f)
    return files


def main() -> None:
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)
    mode = sys.argv[1]
    args = sys.argv[2:]
    limit = None
    only_files = None
    excludes: list[str] = []
    dirs: list[str] = []
    i = 0
    while i < len(args):
        if args[i] == "--limit":
            limit = int(args[i + 1])
            i += 2
        elif args[i] == "--exclude":
            excludes.append(args[i + 1])
            i += 2
        elif args[i] == "--files":
            i += 1
            only_files = []
            while i < len(args) and not args[i].startswith("--"):
                only_files.append(args[i])
                i += 1
        else:
            dirs.append(args[i])
            i += 1

    files = collect_java_files(dirs, only_files, excludes)
    if limit:
        files = files[:limit]

    ctor_mode = mode.endswith("-ctors")
    analyze_fn = analyze_ctors if ctor_mode else analyze_file
    apply_fn = apply_ctors if ctor_mode else apply_file
    apply_mode = mode.startswith("apply")
    report_name = "lombok_ctors_report.json" if ctor_mode else "lombok_refactor_report.json"

    report = []
    total_conv = 0
    changed_files = 0
    skip_reasons: dict[str, int] = {}
    reject_reasons: dict[str, int] = {}
    for f in files:
        try:
            analysis = analyze_fn(f)
        except Exception as e:  # noqa: BLE001 - 分析失败必须跳过并记录 / must skip & record
            report.append({"file": str(f), "error": str(e)})
            continue
        for s in analysis["skipped"]:
            key = s["reason"].split(":")[0]
            skip_reasons[key] = skip_reasons.get(key, 0) + 1
        if analysis["rejected"]:
            reject_reasons[analysis["reason"]] = reject_reasons.get(analysis["reason"], 0) + 1
        if analysis["rejected"] or analysis["conversions"] or analysis["skipped"]:
            report.append(analysis)
        if apply_mode and analysis["conversions"] and not analysis["rejected"]:
            new_text = apply_fn(f, analysis)
            f.write_text(new_text, encoding="utf-8")
        if analysis["conversions"]:
            changed_files += 1
            total_conv += len(analysis["conversions"])

    summary = {
        "mode": mode,
        "files_scanned": len(files),
        "files_with_conversions": changed_files,
        "constructors_converted" if ctor_mode else "methods_converted": total_conv,
        "skip_reasons": dict(sorted(skip_reasons.items(), key=lambda kv: -kv[1])),
        "reject_reasons": dict(sorted(reject_reasons.items(), key=lambda kv: -kv[1])),
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    out = Path(__file__).parent / report_name
    out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"report: {out}")


if __name__ == "__main__":
    main()
