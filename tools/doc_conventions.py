"""Documentation conventions: padded tables, wiki links, typography, headings, carriers.

    python -B tools/doc_conventions.py tables FILE...         report padded tables whose columns no longer line up
    python -B tools/doc_conventions.py tables FILE... --fix   re-pad them in place
    python -B tools/doc_conventions.py links [FILE...]        wiki links and the rules docs' wiki references resolve
    python -B tools/doc_conventions.py typography [FILE...]   the wiki's Chinese typography
    python -B tools/doc_conventions.py headings [FILE...]     no numbered heading, no H1 repeating the file name, no §
    python -B tools/doc_conventions.py carriers [FILE...]     no fact on the wrong carrier (wiki《维护说明#写作与来源》)

Tables. A padded table is one whose separator row has a run of more than three dashes. Every column is
padded to its widest cell, measured in display width (East Asian wide and fullwidth characters count 2,
everything else 1, ambiguous ones such as · ≈ – counting 1), each cell written as "| " + text + padding + " ".
The separator spans the width plus two dashes, keeping the table's own style and any alignment colons: tight
(|-----|, the rules files and READMEs) or spaced (| ----- |, the Obsidian wiki, two characters wider). Compact
tables (|---|---:|) are left as they are. A pipe inside a code span or escaped as \\| does not split a cell.
Exit code 1 in report mode means at least one table is out of line.

Prose checks. links, typography and headings read each file's prose: the leading YAML frontmatter and fenced code
are skipped, and code spans, wiki links, Markdown link targets and URLs are masked, so only the writer's own text is
judged; each problem prints as path:line and the exit code is 1 when any is found. Without FILE, links reads every
wiki page and rules doc (rules_docs: the rules files, skills and subagent definitions of every project), typography
every wiki page, headings every wiki page, rules doc and README. links resolves a wiki page's [[page#heading|alias]]
(embeds and same-page [[#heading]] too; block references only to their page) against the vault's page names,
attachment names and headings, and a rules doc's 《page#heading》 likewise. typography holds the wiki's writing rules
(wiki《维护说明#写作与来源》): a space between Chinese and Latin letters or digits, full-width punctuation next to
Chinese, 约 instead of a bare ~, and – in number ranges; version strings and dates are no ranges. carriers reads every
wiki page, code spans included, for facts the carrier table puts elsewhere: a test class named outside the two
maintenance pages (TEST_PAGES; a page lists the behavior no test covers instead), a dated 「Alex 定」 (history is Git's),
and a Java name in a code span on a 玩家向 page.
"""
import argparse
import re
import sys
import unicodedata
from pathlib import Path

import project_runtime as env


# ----- tables ------------------------------------------------------------------------------------

def display_width(text):
    return sum(2 if unicodedata.east_asian_width(char) in ('W', 'F') else 1 for char in text)


def split_cells(line):
    """The cells of a table row, without the outer pipes; pipes in code spans or escaped stay put."""
    cells, current, in_code, previous = [], [], False, ''
    for char in line.strip()[1:]:
        if char == '`':
            in_code = not in_code
        if char == '|' and not in_code and previous != '\\':
            cells.append(''.join(current))
            current = []
        else:
            current.append(char)
        previous = char
    if ''.join(current).strip():
        cells.append(''.join(current))
    return cells


def is_separator(cells):
    return bool(cells) and all(cell.strip() and set(cell.strip()) <= set('-:') for cell in cells)


def tables(lines):
    """(start, end) line index ranges of every table: a header, a separator, then its rows."""
    found, index = [], 0
    while index < len(lines):
        if lines[index].lstrip().startswith('|') and index + 1 < len(lines) \
                and is_separator(split_cells(lines[index + 1])):
            end = index + 2
            while end < len(lines) and lines[end].lstrip().startswith('|'):
                end += 1
            found.append((index, end))
            index = end
        else:
            index += 1
    return found


def is_padded(separator_cells):
    return any(cell.strip().count('-') > 3 for cell in separator_cells)


def aligned(rows):
    """The rows re-padded so every column lines up, separator (rows[1]) included."""
    cells = [split_cells(row) for row in rows]
    columns = max(len(row) for row in cells)
    cells = [row + [''] * (columns - len(row)) for row in cells]
    widths = [max(display_width(row[column].strip()) for number, row in enumerate(cells) if number != 1)
              for column in range(columns)]
    spaced = cells[1][0].startswith(' ')
    out = []
    for number, row in enumerate(cells):
        if number == 1:
            parts = []
            for column, cell in enumerate(row):
                text = cell.strip()
                left, right = text.startswith(':'), text.endswith(':')
                rule = (':' if left else '') + '-' * (widths[column] + 2 - left - right) + (':' if right else '')
                parts.append(f' {rule} ' if spaced else rule)
            out.append('|' + '|'.join(parts) + '|')
        else:
            out.append('|' + '|'.join(f' {cell.strip()}{" " * (widths[column] - display_width(cell.strip()))} '
                                      for column, cell in enumerate(row)) + '|')
    return out


def lines_up(rows):
    """True when every column already lines up, even padded wider than its widest cell: all content
    rows give a column the same width, each cell fits with a space on both sides, and the separator
    spans it (tight) or spans it plus its two spaces (spaced)."""
    cells = [split_cells(row.strip()) for row in rows]
    if len({len(row) for row in cells}) != 1:
        return False
    content = [row for number, row in enumerate(cells) if number != 1]
    for column in range(len(cells[0])):
        widths = {display_width(row[column]) for row in content}
        if len(widths) != 1 or any(not row[column].startswith(' ') or display_width(row[column].strip()) + 2
                                   > display_width(row[column]) for row in content):
            return False
        separator = display_width(cells[1][column])
        if separator not in (widths.pop(), display_width(cells[1][column].strip()) + 2):
            return False
        if cells[1][column].startswith(' ') and separator != display_width(content[0][column]) + 2:
            return False
    return True


def align_tables(text):
    """(fixed text, starting line numbers of the padded tables that were out of line)."""
    newline = '\r\n' if '\r\n' in text else '\n'
    lines = text.split(newline)
    off = []
    for start, end in tables(lines):
        rows = lines[start:end]
        if not is_padded(split_cells(rows[1])) or lines_up(rows):
            continue
        indent = rows[0][:len(rows[0]) - len(rows[0].lstrip())]
        fixed = [indent + row for row in aligned([row.strip() for row in rows])]
        if fixed != rows:
            off.append(start + 1)
            lines[start:end] = fixed
    return newline.join(lines), off



# ----- prose -------------------------------------------------------------------------------------

CJK = '㐀-䶿一-鿿'
MASK = '\x00'
FENCE = re.compile(r'\s*(`{3,}|~{3,})')
WIKI_LINK = re.compile(r'!?\[\[((?:[^\]\\]|\\.)+?)\]\]')
MASKED = [re.compile(r'(`+).+?\1'), WIKI_LINK, re.compile(r'\]\([^)]*\)'), re.compile(r'<?https?://[^\s>)]+>?')]


def prose(text):
    """(line number, line) of every line outside the leading YAML frontmatter and fenced code blocks."""
    lines = text.splitlines()
    start = 0
    if lines and lines[0].strip() == '---':
        start = next((index + 1 for index in range(1, len(lines)) if lines[index].strip() == '---'), 0)
    fence = None
    for index in range(start, len(lines)):
        marker = FENCE.match(lines[index])
        if fence is None and marker:
            fence = marker.group(1)
        elif fence is not None:
            if marker and marker.group(1)[0] == fence[0] and len(marker.group(1)) >= len(fence):
                fence = None
        else:
            yield index + 1, lines[index]


def masked(line):
    """The line with code spans, wiki links, Markdown link targets and URLs each replaced by MASK."""
    for pattern in MASKED:
        line = pattern.sub(MASK, line)
    return line


def display(path):
    path = Path(path).resolve()
    return str(path.relative_to(env.WORKSPACE_ROOT)) if path.is_relative_to(env.WORKSPACE_ROOT) else str(path)


def wiki_pages(root=None):
    root = Path(root or env.WIKI_ROOT)
    return sorted(path for path in root.rglob('*.md')
                  if not any(part.startswith('.') for part in path.relative_to(root).parts))


# Skills kept verbatim from their upstream (alex-constitution names the commit), so its own conventions stand.
UPSTREAM_SKILLS = {'i-have-adhd'}
# AGENTS.md holds the rules; CLAUDE.md imports it and adds Claude Code's own section.
RULES_FILES = ('AGENTS.md', 'CLAUDE.md')


def rules_docs(projects=None):
    """The rules files, skills and subagent definitions of every project checked out, upstream skills aside."""
    found = []
    for project in projects or env.PROJECTS.values():
        found += [project.root / name for name in RULES_FILES if (project.root / name).is_file()]
        found += [path for path in sorted(project.root.glob('.claude/skills/*/SKILL.md'))
                  if path.parent.name not in UPSTREAM_SKILLS]
        found += sorted(project.root.glob('.claude/agents/*.md'))
    return found


def readmes(projects=None):
    return [path for project in projects or env.PROJECTS.values() for path in sorted(project.root.glob('README*.md'))]


# ----- links -------------------------------------------------------------------------------------

RULES_REFERENCE = re.compile(r'《([^》\n]+)》')


class Vault:
    """The wiki's page names, attachment names and, read on demand, each page's headings."""

    def __init__(self, root=None):
        self.root = Path(root or env.WIKI_ROOT)
        self.pages, self.files, self.cache = {}, set(), {}
        self.paths, self.name_paths = {}, {}
        for path in self.root.rglob('*'):
            if path.is_file() and not any(part.startswith('.') for part in path.relative_to(self.root).parts):
                if path.suffix == '.md':
                    self.pages.setdefault(path.stem, path)
                    self.paths[path.relative_to(self.root).with_suffix('').as_posix()] = path
                    self.name_paths.setdefault(path.stem, []).append(path)
                else:
                    self.files.add(path.name)

    def headings(self, page):
        if page not in self.cache:
            path = self.paths.get(page, self.pages.get(page))
            text = path.read_text(encoding='utf-8')
            self.cache[page] = {re.sub(r'\s+#+\s*$', '', line.lstrip('#')).strip()
                                for _, line in prose(text) if re.match(r'#{1,6}\s', line)}
        return self.cache[page]

    def problem(self, target, here=None):
        """Why link target `target` ("page#heading|alias") does not resolve, else None; `here` is the linking page."""
        page, _, heading = target.replace('\\|', '|').split('|', 1)[0].partition('#')
        page = page.strip()
        page = page[:-3] if page.endswith('.md') else page
        if page and '/' not in page and len(self.name_paths.get(page, [])) > 1:
            return f'ambiguous wiki page "{page}"; use its vault-relative path'
        if page and page not in self.paths and page not in self.pages:
            return None if page in self.files else f'no wiki page or file "{page}"'
        page = page or here
        heading = heading.rsplit('#', 1)[-1].strip()
        if not heading or heading.startswith('^') or page is None:
            return None
        return None if heading in self.headings(page) else f'"{page}" has no heading "{heading}"'


def link_problems(path, vault):
    """Each unresolved [[link]] of a wiki page, or 《reference》 of any other file, as "path:line: ..."."""
    path = Path(path).resolve()
    in_wiki = path.is_relative_to(vault.root.resolve())
    found = []
    for number, line in prose(path.read_text(encoding='utf-8')):
        if in_wiki:
            line = re.sub(r'(`+).+?\1', MASK, line)
            targets = [match.group(1) for match in WIKI_LINK.finditer(line)]
        else:
            targets = [match.group(1) for match in RULES_REFERENCE.finditer(masked(line))]
        for target in targets:
            problem = vault.problem(target, path.relative_to(vault.root.resolve()).with_suffix('').as_posix() if in_wiki else None)
            if problem:
                found.append(f'{display(path)}:{number}: link {target}: {problem}')
    return found


# ----- typography --------------------------------------------------------------------------------

# Lookahead groups let neighboring hits overlap (了3次 is two); ASCII classes, since \w also matches Chinese.
TYPOGRAPHY = [
    (re.compile(f'(?=([{CJK}][A-Za-z0-9]|[A-Za-z0-9][{CJK}]))'), 'space between Chinese and Latin letters or digits'),
    (re.compile(f'(?=([{CJK}][,;:?!()]|[,;:?!()][{CJK}]))'), 'full-width punctuation next to Chinese'),
    (re.compile(r'(?<!~)(~)(?![~/])'), '约 instead of a bare ~'),
    (re.compile(r'(?<![A-Za-z0-9_.\-–])(\d+(?:\.\d+)?\s?-\s?\d+(?:\.\d+)?)(?![A-Za-z0-9_.\-])(?<!\d{4}-\d{2})'),
     '– in a number range'),
]


def typography_problems(path):
    found = []
    for number, line in prose(Path(path).read_text(encoding='utf-8')):
        line = masked(line)
        for pattern, rule in TYPOGRAPHY:
            for match in pattern.finditer(line):
                found.append(f'{display(path)}:{number}: typography: {rule} ({match.group(1)!r})')
    return found


# ----- headings ----------------------------------------------------------------------------------

NUMBERED = re.compile(r'#{1,6}\s+(?:\d+(?:\.\d+)*[.、)）](?!\d)|\d+\.\d+\s|[一二三四五六七八九十]+[、.．]'
                      r'|第[一二三四五六七八九十百\d]+[章节部篇])')


def heading_problems(path):
    path, found = Path(path), []
    for number, line in prose(path.read_text(encoding='utf-8')):
        where = f'{display(path)}:{number}'
        if NUMBERED.match(line):
            found.append(f'{where}: heading: numbered ({line.strip()})')
        if re.match(r'#\s', line) and re.sub(r'\s+#+\s*$', '', line[1:]).strip() == path.stem:
            found.append(f'{where}: heading: the H1 repeats the file name, which Obsidian already shows as the title')
        if '§' in masked(line):
            found.append(f'{where}: heading: a cross-reference names the heading, never §')
    return found


# ----- carriers ----------------------------------------------------------------------------------

TEST_PAGES = {'维护说明', 'Gu World 维护说明'}
TEST_CLASS = re.compile(r'(?<![\w.])([A-Z]\w*?(?:GameTests|Test))\b')
TEST_CONCEPTS = {'GameTest'}
DATED_CALL = re.compile(r'(Alex ?\d{4}-\d{2}-\d{2})')
JAVA_NAME = re.compile(r'`((?:[a-z]\w*\.)*[A-Z][a-z0-9]+[A-Z][A-Za-z0-9]*(?:[.#][A-Za-z]\w*)*)`')


def carrier_problems(path, root=None):
    path, root = Path(path).resolve(), Path(root or env.WIKI_ROOT).resolve()
    player = path.is_relative_to(root) and path.relative_to(root).parts[0] == '玩家向'
    found = []
    for number, line in prose(path.read_text(encoding='utf-8')):
        where, line = f'{display(path)}:{number}', MASKED[3].sub(MASK, line)
        if path.stem not in TEST_PAGES:
            found += [f'{where}: carriers: a test class outside 维护说明; list the untested behavior instead ({name})'
                      for name in TEST_CLASS.findall(line) if name not in TEST_CONCEPTS]
        found += [f'{where}: carriers: 「Alex 定」 carries no date ({call})' for call in DATED_CALL.findall(line)]
        if player:
            found += [f'{where}: carriers: a Java name on a 玩家向 page ({name})' for name in JAVA_NAME.findall(line)]
    return found


def main_prose(args):
    vault = Vault()
    checks = {'links': (lambda path: link_problems(path, vault), lambda: wiki_pages() + rules_docs()),
              'typography': (typography_problems, wiki_pages),
              'headings': (heading_problems, lambda: wiki_pages() + rules_docs() + readmes()),
              'carriers': (carrier_problems, wiki_pages)}
    check, defaults = checks[args.command]
    problems = [problem for path in (args.files or defaults()) for problem in check(path)]
    for problem in problems:
        print(problem)
    return 1 if problems else 0


def main_tables(args):
    bad = False
    for name in args.files:
        path = Path(name)
        text = path.read_bytes().decode('utf-8')
        fixed, off = align_tables(text)
        for line in off:
            print(f'{path}:{line}: padded table out of line' + (' (fixed)' if args.fix else ''))
        if off and args.fix:
            path.write_bytes(fixed.encode('utf-8'))
        bad = bad or bool(off)
    return 1 if bad and not args.fix else 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    commands = parser.add_subparsers(dest='command', required=True)
    tables_parser = commands.add_parser('tables', help='check or re-pad padded Markdown tables')
    tables_parser.add_argument('files', nargs='+')
    tables_parser.add_argument('--fix', action='store_true', help='re-pad out-of-line tables in place')
    for name, summary in [('links', 'check that wiki links and the rules docs\' wiki references resolve'),
                          ('typography', 'check the wiki\'s Chinese typography'),
                          ('headings', 'check for numbered headings, H1s repeating the file name and §'),
                          ('carriers', 'check for test classes, dated calls and Java names on the wrong wiki page')]:
        commands.add_parser(name, help=summary).add_argument('files', nargs='*')
    args = parser.parse_args(argv)
    for stream in (sys.stdout, sys.stderr):
        stream.reconfigure(encoding='utf-8')
    return {'tables': main_tables}.get(args.command, main_prose)(args)


if __name__ == '__main__':
    sys.exit(main())
