#!/usr/bin/env python3
"""检查 docs/ 下所有 HTML 页面的站内链接：目标文件存在、#锚点存在。

用法：python3 .github/scripts/check_links.py [docs 目录]
页面目录（chapter-toc）和侧边栏由 site.js 动态生成，这里额外检查 site.js 里 CHAPTERS 列出的每一章都有对应的文件。
"""
import re
import sys
from html.parser import HTMLParser
from pathlib import Path


class Collector(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids, self.links = set(), []

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if "id" in a:
            self.ids.add(a["id"])
        for key in ("href", "src"):
            if key in a and a[key]:
                self.links.append(a[key])


def main() -> int:
    docs = Path(sys.argv[1] if len(sys.argv) > 1 else "docs")
    pages = {p.name: p for p in docs.glob("*.html")}
    parsed = {}
    for name, path in pages.items():
        c = Collector()
        c.feed(path.read_text(encoding="utf-8"))
        parsed[name] = c

    errors = []
    for name, c in parsed.items():
        for link in c.links:
            if re.match(r"^(https?:|mailto:|data:|javascript:)", link):
                continue
            target, _, anchor = link.partition("#")
            target = target or name
            if target.endswith(".html"):
                if target not in parsed:
                    errors.append(f"{name}: 链接到不存在的页面 {link}")
                elif anchor and anchor not in parsed[target].ids:
                    errors.append(f"{name}: {target} 里没有锚点 #{anchor}")
            elif not (docs / target).exists():
                errors.append(f"{name}: 引用的文件不存在 {link}")

    site_js = (docs / "assets" / "site.js").read_text(encoding="utf-8")
    for n in re.findall(r"\{\s*n:\s*(\d+)", site_js):
        if f"ch{int(n):02d}.html" not in pages:
            errors.append(f"site.js: 第 {n} 章没有对应的页面")

    for e in errors:
        print("✗", e)
    print(f"检查了 {len(parsed)} 个页面、{sum(len(c.links) for c in parsed.values())} 个链接，发现 {len(errors)} 个问题")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
