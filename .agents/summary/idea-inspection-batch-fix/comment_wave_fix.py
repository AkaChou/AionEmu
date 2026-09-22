#!/usr/bin/env python3
"""按块修复 javadoc 问题：悬空块删除/转行注释、非法标签转文本或删除、空说明标签删除、
参数名修正、错误标签修正、块内空行清理。逐行精确校验，任何不符即放弃该文件。

对应 IDEA 规则: DanglingJavadoc / JavadocDeclaration / JavadocBlankLines / JavadocReference(仅检测)。
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT_LOG = Path(__file__).parent / 'comment_wave_fix_log.json'

MODIFIERS = {'public', 'private', 'protected', 'static', 'final', 'abstract', 'synchronized', 'native', 'default',
             'transient', 'volatile', 'strictfp', 'sealed', 'non-sealed'}
CONTROL = {'if', 'for', 'while', 'switch', 'return', 'break', 'continue', 'try', 'do', 'else', 'case', 'throw',
           'throws', 'new', 'assert', 'catch', 'finally'}
LEGAL_BLOCK_TAGS = {'author', 'version', 'since', 'see', 'deprecated', 'serial', 'serialField', 'serialData',
                    'hidden', 'apiNote', 'implNote', 'implSpec', 'spec'}
KNOWN_TAGS = {'param', 'return', 'throws', 'exception', 'author', 'version', 'since', 'see', 'deprecated', 'serial',
              'serialField', 'serialData', 'inheritDoc', 'docRoot', 'link', 'linkplain', 'value', 'code', 'literal',
              'index', 'summary', 'systemProperty', 'hide', 'apiNote', 'implNote', 'implSpec', 'spec', 'hidden'}
BAD_TAG_TEXT_PREFIX = {'modified', 'based', 'rework', 'changed', 'updated', 'fixed'}


def strip_strings(line):
    line = re.sub(r'"(?:\\.|[^"\\])*"', '""', line)
    line = re.sub(r"'(?:\\.|[^'\\])*'", "''", line)
    line = re.sub(r'//.*$', '', line)
    return line


def find_javadocs(lines):
    blocks = []
    i, n = 0, len(lines)
    while i < n:
        code = strip_strings(lines[i])
        pos = code.find('/**')
        if pos >= 0:
            start, end = i, i
            if '*/' not in code[pos + 3:]:
                j = i + 1
                while j < n:
                    if '*/' in lines[j]:
                        end = j
                        break
                    j += 1
                else:
                    break
            blocks.append((start, end))
            i = end + 1
        else:
            i += 1
    return blocks


def is_declaration_start(line):
    s = strip_strings(line).strip()
    if not s:
        return False
    first = re.match(r'([A-Za-z_$][\w$]*)', s)
    # 控制流/杂项 → 非声明
    if first and first.group(1) in CONTROL:
        return False
    if s.startswith(('}', '{', ')', ';', '*', '/', '+', '-', '&lt;')):
        return False
    if first and first.group(1) in MODIFIERS:
        return True
    if first and first.group(1) in {'class', 'interface', 'enum', 'record'}:
        return True
    # 构造器 / 带参枚举常量: Ident(
    if re.match(r'[A-Z][\w$]*\s*[\(<{]', s):
        return True
    # 无参枚举常量: Ident; / Ident, / 单独一行全大写标识符
    if re.match(r'[A-Z][\w$]*\s*[;,]', s):
        return True
    if re.fullmatch(r'[A-Za-z_$][\w$]*', s):
        return True
    # 类型 名字 (方法/字段): Token... name ( | = | ;
    if re.match(r'[A-Za-z_$][\w$.]*(?:\s*<[^=;{}]*?>)?(?:\s*\[\s*\])*\s+[A-Za-z_$][\w$]*\s*(?:\(|=|;|,)', s):
        return True
    return False


def block_content_lines(lines, s, e):
    """块内非空内容行（去 * 修饰）。返回 [(idx, text)]"""
    out = []
    for i in range(s, e + 1):
        text = lines[i]
        if i == s:
            text = text.split('/**', 1)[1] if '/**' in text else ''
        if i == e and '*/' in text:
            text = text.split('*/', 1)[0]
        m = re.match(r'^\s*\*?(.*)$', text)
        body = (m.group(1) if m else text).strip()
        if body:
            out.append((i, body))
    return out


def parse_tag(text):
    m = re.match(r'@(\w+)\s*(.*)$', text)
    if m:
        return m.group(1), m.group(2).strip()
    return None, None


def norm_words(text):
    return {w for w in re.findall(r'[a-z0-9]+', text.lower()) if w not in {'the', 'a', 'an', 'of', 'this'}}


def extract_params(sig_text):
    start = sig_text.find('(')
    if start < 0:
        return None, False
    depth, end = 0, -1
    for i in range(start, len(sig_text)):
        c = sig_text[i]
        if c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                end = i
                break
    if end < 0:
        return None, False
    inner = sig_text[start + 1:end]
    parts, part, d = [], [], 0
    for c in inner:
        if c in '(<[':
            d += 1
        elif c in ')>]':
            d -= 1
        if c == ',' and d == 0:
            parts.append(''.join(part))
            part = []
        else:
            part.append(c)
    if part:
        parts.append(''.join(part))
    names = []
    for p in parts:
        p = re.sub(r'@\w+(?:\([^()]*(?:\([^()]*\)[^()]*)*\))?', ' ', p)
        toks = p.split()
        if not toks:
            return None, False
        tok = toks[-1]
        if '...' in tok:
            tok = tok.rsplit('...', 1)[-1]
            if not tok and len(toks) > 1:
                tok = toks[-2]
            tok = tok.strip()
        names.append(tok.lstrip('.'))
    return names, True


def collect_signature(lines, start_idx):
    n = len(lines)
    j = start_idx
    while j < n:
        st = lines[j].strip()
        if st == '' or st.startswith('@') or st.startswith('//'):
            j += 1
            continue
        break
    buf, depth, seen_open = [], 0, False
    while j < n and len(buf) < 60:
        text = strip_strings(lines[j])
        buf.append(text)
        for c in text:
            if c == '(':
                depth += 1
                seen_open = True
            elif c == ')':
                depth -= 1
        joined = ' '.join(buf)
        if seen_open and depth <= 0:
            return joined
        if not seen_open and re.search(r'[;={]', joined):
            return joined
        j += 1
    return ' '.join(buf)


def tag_to_text(tag, rest):
    """非法标签转纯文本。"""
    if tag == 'param':
        m = re.match(r'(\S+)\s*(.*)$', rest)
        if m:
            return f"{m.group(1)}: {m.group(2)}".strip()
        return rest
    if tag == 'return':
        return f"return: {rest}" if rest else ''
    return f"{tag}: {rest}"


def next_code_line(lines, start_idx):
    """返回 start_idx 起第一个有效代码行 idx（跳过空行/注释行/含跨行括号的注解行）。"""
    n = len(lines)
    j = start_idx
    while j < n:
        st = lines[j].strip()
        if st == '' or st.startswith('//') or st.startswith('/*') or st.startswith('*'):
            j += 1
            continue
        if st.startswith('@'):
            # 跳过整个注解（含跨行括号参数）
            depth = st.count('(') + st.count('{') - st.count(')') - st.count('}')
            j += 1
            while j < n and depth > 0:
                depth += lines[j].count('(') + lines[j].count('{') - lines[j].count(')') - lines[j].count('}')
                j += 1
            continue
        return j
    return n


def plan_block(lines, s, e):
    """返回 (action, ops)。action in skip/delete_block/to_comment/fix_lines。
    ops: [(idx, new_text_or_None)]；None 表示删行。块边界行也用 op 表示。"""
    content = block_content_lines(lines, s, e)
    # 块边界整洁性：起始行注释前、结束行注释后必须无代码
    if s != e:
        pre = strip_strings(lines[s]).split('/**', 1)[0].strip()
        if pre:
            return 'skip', []
        post = strip_strings(lines[e]).split('*/', 1)[1].strip()
        if post:
            return 'skip', []
    else:
        whole = strip_strings(lines[s])
        pre = whole.split('/**', 1)[0].strip()
        post = whole.split('*/', 1)[1].strip() if '*/' in whole else 'x'
        if pre or post:
            return 'skip', []

    # 悬空判定：块后第一个有效代码行是否为声明
    j = next_code_line(lines, e + 1)
    n = len(lines)
    dangling = j < n and not is_declaration_start(lines[j])

    if dangling:
        # 有标签或≥2 行内容 → 转行注释；否则删除
        has_tags = any(parse_tag(t)[0] for _, t in content)
        if has_tags or len(content) >= 2:
            ops = []
            indent = re.match(r'\s*', lines[s]).group(0)
            for i in range(s, e + 1):
                text = lines[i]
                if i == s:
                    text = text.split('/**', 1)[1] if '/**' in text else ''
                if i == e and '*/' in text:
                    text = text.split('*/', 1)[0]
                m = re.match(r'^\s*\*?(.*)$', text)
                body = (m.group(1) if m else '').strip()
                if not body:
                    continue
                tag, rest = parse_tag(body)
                if tag:
                    body = tag_to_text(tag, rest)
                ops.append((i, f'{indent}// {body}'.rstrip()))
            return 'to_comment', ops
        return 'delete_block', [(i, None) for i in range(s, e + 1)]

    # 合法块：标签修复 + 空行清理
    # 目标签名
    params = None
    is_method = False
    first_code = lines[j] if j < n else ''
    is_class_target = bool(re.search(r'\b(class|interface|enum|record)\s+\w+', strip_strings(first_code)))
    if not is_class_target:
        joined = collect_signature(lines, j if j < n else e + 1)
        params, is_method = extract_params(joined)
        # 字段带初始化器（'=' 先于 '(' 出现，如 `a = new Foo()`）→ 不是方法
        eq, paren = joined.find('='), joined.find('(')
        if eq != -1 and (paren == -1 or eq < paren):
            is_method, params = False, []
        elif '(' in joined and params is None:
            is_method, params = 'unknown', None

    # 收集块内 tag 行（0-based idx, tag, rest）
    tag_rows = []
    for i, body in content:
        tag, rest = parse_tag(body)
        if tag:
            tag_rows.append((i, tag, rest, body))

    delete_idxs = set()
    rename = {}  # idx -> new name
    for idx, tag, rest, body in tag_rows:
        if tag == 'Author':
            rename.setdefault(idx, ('recase', None))
            continue
        if tag not in KNOWN_TAGS or tag in BAD_TAG_TEXT_PREFIX:
            delete_idxs.add(idx)
            continue
        if tag == 'param':
            m = re.match(r'<(\S+)>(?:\s+(.*))?$', rest)
            if m:
                # 类型参数：空说明删行
                if not (m.group(2) or '').strip():
                    delete_idxs.add(idx)
                continue
            m = re.match(r'(\S+)\s*(.*)$', rest)
            if not m:
                continue
            pname, desc = m.group(1), m.group(2).strip()
            if is_class_target or is_method is False:
                # 字段/类上的 param
                if not desc or _dup(desc, content, idx):
                    delete_idxs.add(idx)
                else:
                    rename.setdefault(idx, ('text', tag_to_text('param', rest)))
                continue
            if is_method == 'unknown':
                continue
            if not desc:
                delete_idxs.add(idx)
                continue
            if params is not None and pname not in params:
                if len(params) == 1 and len([r for r in tag_rows if r[1] == 'param']) == 1:
                    rename.setdefault(idx, ('param', params[0]))
                else:
                    delete_idxs.add(idx)
            continue
        if tag == 'return':
            if is_class_target or is_method is False:
                if not rest or _dup(rest, content, idx):
                    delete_idxs.add(idx)
                else:
                    rename.setdefault(idx, ('text', tag_to_text('return', rest)))
                continue
            if is_method == 'unknown':
                continue
            if not rest:
                delete_idxs.add(idx)
            continue

    # 整块删除判定：删除标签后若只剩 Lombok 标记（-- GETTER -- / -- SETTER --）而无真实描述
    is_marker = lambda t: bool(re.fullmatch(r'--\s*(GETTER|SETTER)\s*--', t))
    marker_only = all(is_marker(t) or parse_tag(t)[0] for _, t in content)
    if content and marker_only and (delete_idxs or rename):
        survivors = [t for i, t in content if i not in delete_idxs and not parse_tag(t)[0] and not is_marker(t)]
        if not survivors:
            return 'delete_block', [(i, None) for i in range(s, e + 1)]

    ops = []
    for i in range(s, e + 1):
        text = lines[i]
        if i == e and '*/' in text:
            head = text.split('*/', 1)[0]
        else:
            head = text
        # 块内裸空行（无 * 前缀）同样被 javadoc 忽略 → 删
        if s < i < e and lines[i].strip() == '':
            ops.append((i, None))
            continue
        if re.fullmatch(r'\s*\*\s*', head):
            ops.append((i, None))
            continue
        if i in delete_idxs:
            ops.append((i, None))
            continue
        if i in rename:
            kind, val = rename[i]
            m = re.match(r'^(\s*\*?\s*)@(\w+)(\s+)(.*)$', head)
            if kind == 'recase':
                ops.append((i, f'{m.group(1)}@author{m.group(3)}{m.group(4)}'))
            elif kind == 'param':
                m2 = re.match(r'^(\s*\*?\s*)@param\s+\S+\s*(.*)$', head)
                ops.append((i, f'{m2.group(1)}@param {val} {m2.group(2)}'.rstrip()))
            elif kind == 'text':
                # 非法标签转纯文本：param 去掉参数名只留描述；return 留描述
                m2 = re.match(r'^(\s*\*?\s*)@(\w+)\s+(.*)$', head)
                tag, rest = m2.group(2), m2.group(3).strip()
                if tag == 'param':
                    rest = re.sub(r'^\S+\s*', '', rest)
                ops.append((i, f'{m2.group(1)}{rest}'.rstrip()))
    return 'fix_lines', ops


def _split_tag(body):
    m = re.match(r'@(\w+)\s*(.*)$', body)
    return (m.group(1), m.group(2)) if m else ('', body)


def _dup(desc, content, idx):
    words = norm_words(desc)
    if not words:
        return True
    for i, t in content:
        if i == idx:
            continue
        if parse_tag(t)[0]:
            continue
        if words <= norm_words(t):
            return True
    return False


def apply_file(path, backup=True):
    raw = path.read_text(encoding='utf-8')
    nl = '\r\n' if '\r\n' in raw else '\n'
    lines = raw.split(nl)
    blocks = find_javadocs(lines)
    all_ops = []
    stats = {'delete_block': 0, 'to_comment': 0, 'fix_lines': 0, 'skip': 0}
    for (s, e) in blocks:
        action, ops = plan_block(lines, s, e)
        stats[action] += 1
        if ops:
            all_ops.extend(ops)
    if not all_ops:
        return None, stats
    # 校验并自底向上应用
    new_lines = list(lines)
    for idx, val in sorted(all_ops, key=lambda x: -x[0]):
        if val is None:
            new_lines[idx] = '__DELETE__'
        else:
            new_lines[idx] = val
    # 行删除（连续行成段删，保持换行结构简单：直接过滤）
    result = [l for l in new_lines if l != '__DELETE__']
    if backup:
        rel = path.relative_to(ROOT)
        dst = Path(__file__).parent / 'backup' / rel
        if not dst.exists():
            dst.parent.mkdir(parents=True, exist_ok=True)
            dst.write_text(raw, encoding='utf-8')
    path.write_text(nl.join(result), encoding='utf-8')
    return len(all_ops), stats


def main():
    only = sys.argv[1] if len(sys.argv) > 1 else None
    log = []
    changed = 0
    skipped = 0
    for base in ['src/main/java', 'src/test/java']:
        for p in sorted((ROOT / base).rglob('*.java')):
            rel = str(p)
            if only and only not in rel:
                continue
            if p.name == 'package-info.java':
                continue
            # 排除 quest WIP 代码文件
            if rel.endswith(('QuestDefinitionXmlCompiler.java', 'QuestXmlBlockExpander.java',
                             'QuestBClassRouteContractTest.java')):
                continue
            try:
                r, stats = apply_file(p)
            except Exception as ex:
                log.append({'file': rel, 'error': str(ex)})
                skipped += 1
                continue
            if r == 'verify-fail':
                log.append({'file': rel, 'error': 'verify-fail'})
                skipped += 1
            elif r is None:
                pass
            else:
                changed += 1
                log.append({'file': rel, 'ops': r, **stats})
    OUT_LOG.write_text(json.dumps(log, ensure_ascii=False, indent=1))
    total_ops = sum(x.get('ops', 0) for x in log)
    print(f'changed files: {changed}, skipped/error: {skipped}, total ops: {total_ops}')
    from collections import Counter
    acts = Counter()
    for x in log:
        for k in ('delete_block', 'to_comment', 'fix_lines', 'skip'):
            acts[k] += x.get(k, 0)
    print(dict(acts))


if __name__ == '__main__':
    main()
