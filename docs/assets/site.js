/* ================================================================
   How Tomcat Works 教程 · 共享脚本：侧边栏、目录、代码高亮、主题
   ================================================================ */
// 保持经典 <script> 而不是 type="module"：模块脚本在 file:// 下会被浏览器拦截，
// 而这些页面经常是直接双击打开的。
(() => {
  'use strict';

  const CHAPTERS = [
    { n: 1,  t: '一个简单的 Web 服务器',  d: 'HTTP 报文、Socket 与 ServerSocket', part: '第一部分 · 起步' },
    { n: 2,  t: '一个简单的 Servlet 容器', d: 'Servlet 接口、类加载器与外观模式' },
    { n: 3,  t: '连接器',                d: '解析请求行、请求头、Cookie 和参数', part: '第二部分 · 连接器' },
    { n: 4,  t: 'Tomcat 默认连接器',      d: '处理器池、keep-alive 与线程协作' },
    { n: 5,  t: '容器',                  d: '四种容器、管道（Pipeline）与阀（Valve）', part: '第三部分 · 容器的组件' },
    { n: 6,  t: '生命周期',              d: 'Lifecycle 接口与事件监听器' },
    { n: 7,  t: '日志记录器',             d: 'Logger 接口与三种实现' },
    { n: 8,  t: '载入器',                d: '类加载器、WebappClassLoader 与自动重载' },
    { n: 9,  t: 'Session 管理',          d: 'Manager、Session 与 Store' },
    { n: 10, t: '安全',                  d: 'Realm、Principal、登录配置与认证器' },
    { n: 11, t: 'StandardWrapper',       d: 'Servlet 的加载、单线程模型与过滤器链', part: '第四部分 · 标准实现' },
    { n: 12, t: 'StandardContext',       d: '配置、映射、重载与后台线程' },
    { n: 13, t: 'Host 与 Engine',         d: '虚拟主机与顶层容器' },
    { n: 14, t: 'Server 与 Service',      d: '一键启动、一键关闭整个容器' },
    { n: 15, t: 'Digester',              d: '把 XML 配置文件变成 Java 对象', part: '第五部分 · 启动、部署与管理' },
    { n: 16, t: '关闭钩子',               d: '无论怎么退出都能执行清理代码' },
    { n: 17, t: 'Tomcat 的启动',          d: 'Bootstrap、Catalina 与启动脚本' },
    { n: 18, t: '部署器',                 d: 'HostConfig 如何发现并部署 Web 应用' },
    { n: 19, t: 'Manager Servlet',       d: 'ContainerServlet：让 Servlet 触碰容器内部' },
    { n: 20, t: '基于 JMX 的管理',         d: 'MBean、MBeanServer 与 Commons Modeler' },
    { n: 21, t: 'ServletContext 与 RequestDispatcher', d: 'forward、include 与三种作用域', part: '第六部分 · 补充：现代 Servlet 容器' },
    { n: 22, t: '静态资源、错误页与字符编码', d: 'DefaultServlet、ETag/304/Range、error-page 与 UTF-8' },
    { n: 23, t: 'Coyote 与 NIO 连接器', d: 'Acceptor、Poller、Worker 与 CoyoteAdapter' },
    { n: 24, t: '异步 Servlet 与非阻塞 IO', d: 'AsyncContext、SSE 与 ReadListener' },
    { n: 25, t: '可插拔性与 Spring Boot', d: '注解、SCI、web-fragment 与嵌入式启动' },
    { n: 26, t: 'TLS、HTTP/2 与 WebSocket', d: 'SNI/ALPN、多路复用、协议升级与帧' },
    { n: 27, t: '虚拟线程', d: 'Java 21 虚拟线程、钉住与 Tomcat 11' },
    { n: 28, t: '运维：调优、监控与加固', d: '线程估算、JMX 指标、重载泄漏与安全清单' },
    { n: 29, t: '整体架构：分层与边界', d: '五条架构原则，套用到一个订单服务', part: '第七部分 · 设计篇' },
    { n: 30, t: '扩展点设计', d: '四种扩展形态、七条标准与插件隔离' },
    { n: 31, t: '设计模式实战', d: '十二个模式：在 Tomcat 里找到，在业务里用上' },
    { n: 32, t: '并发、隔离与演进', d: '线程归属、舱壁、被推翻的设计与评审清单' },
    { n: 33, t: '综合：一个请求的完整旅程', d: '启动与请求的全过程、类与章节速查', part: '第八部分 · 综合与练习' },
    { n: 34, t: '动手：插件化任务调度器', d: '把设计篇的原则用在一个非 Web 系统上' }
  ];

  const pad = (n) => String(n).padStart(2, '0');
  const href = (n) => `ch${pad(n)}.html`;

  const cur = Number.parseInt(document.body.dataset.chapter || '0', 10);

  /* ---------- 侧边栏 ---------- */
  const side = document.getElementById('sidebar');
  if (side) {
    const brand = '<a class="brand" href="index.html"><span class="flame">&#128049;</span>' +
      '<span>How Tomcat Works<small>从 Socket 到 Catalina · Java 17 对照</small></span></a>';
    const links = CHAPTERS.map(({ n, t, part }) =>
      (part ? `<div class="part">${part}</div>` : '') +
      `<a class="ch${n === cur ? ' active' : ''}" href="${href(n)}"><span class="n">${n}</span><span>${t}</span></a>`
    );
    side.innerHTML = brand + links.join('');
    const active = side.querySelector('a.ch.active');
    if (active) setTimeout(() => active.scrollIntoView({ block: 'center' }), 0);
  }

  /* ---------- 移动端菜单 ---------- */
  const btn = Object.assign(document.createElement('button'), {
    id: 'menu-btn',
    type: 'button',
    innerHTML: '&#9776;',
    onclick: () => document.body.classList.toggle('nav-open')
  });
  btn.setAttribute('aria-label', '目录');
  document.body.appendChild(btn);
  document.addEventListener('click', (e) => {
    if (document.body.classList.contains('nav-open') &&
        side && !side.contains(e.target) && e.target !== btn) {
      document.body.classList.remove('nav-open');
    }
  });

  /* ---------- 主题切换 ---------- */
  const THEME_KEY = 'tomcat-doc-theme';
  const tbtn = Object.assign(document.createElement('button'), {
    id: 'theme-btn',
    type: 'button',
    innerHTML: '&#9789;',
    onclick: () => {
      const root = document.documentElement;
      const now = root.getAttribute('data-theme');
      const dark = now ? now === 'dark'
        : window.matchMedia('(prefers-color-scheme: dark)').matches;
      const next = dark ? 'light' : 'dark';
      root.setAttribute('data-theme', next);
      try { localStorage.setItem(THEME_KEY, next); } catch {}
    }
  });
  tbtn.setAttribute('aria-label', '切换深浅色');
  document.body.appendChild(tbtn);
  try {
    const saved = localStorage.getItem(THEME_KEY);
    if (saved) document.documentElement.setAttribute('data-theme', saved);
  } catch {}

  /* ---------- 箭头 marker（全局一次） ---------- */
  const defs = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  defs.setAttribute('width', '0');
  defs.setAttribute('height', '0');
  defs.setAttribute('style', 'position:absolute');
  // 注意：SVG marker 的内容不会从引用它的元素继承 color，
  // 所以这里直接用 CSS 变量填色，而不是 currentColor。
  const marker = (id, fill) =>
    `<marker id="${id}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">` +
    `<path d="M0,0 L10,5 L0,10 z" fill="${fill}"/></marker>`;
  defs.innerHTML = `<defs>${marker('ar', 'var(--fg-faint)')}${marker('ar-a', 'var(--accent)')}` +
    `${marker('ar-b', 'var(--blue)')}${marker('ar-g', 'var(--green)')}${marker('ar-r', 'var(--red)')}${marker('ar-p', 'var(--purple)')}</defs>`;
  document.body.appendChild(defs);

  /* ---------- 章节内目录 ---------- */
  const main = document.querySelector('main');
  const slot = document.getElementById('chapter-toc');
  if (slot && main) {
    const hs = [...main.querySelectorAll('h2')];
    if (hs.length > 2) {
      const items = hs.map((h, i) => {
        if (!h.id) h.id = `sec-${i + 1}`;
        return `<li><a href="#${h.id}">${h.textContent}</a></li>`;
      });
      slot.className = 'toc';
      slot.innerHTML = `<div class="h">本章目录</div><ol>${items.join('')}</ol>`;
    }
  }

  /* ---------- 代码块：语言标签 + 复制 + 高亮 ---------- */
  const KW = ('package|import|public|private|protected|class|interface|enum|record|extends|implements|' +
    'new|return|if|else|for|while|do|try|catch|finally|throw|throws|switch|case|default|break|continue|' +
    'static|final|abstract|synchronized|volatile|transient|native|void|boolean|byte|char|short|int|long|' +
    'float|double|var|this|super|null|true|false|instanceof|sealed|non-sealed|permits|yield|assert|' +
    'const|let|function|async|await|typeof|of|in').split('|');

  // 每一段用正则字面量写，取 .source 拼起来，省掉字符串里的双重转义
  const RE_JAVA = new RegExp([
    /(\/\*[\s\S]*?\*\/|\/\/[^\n]*)/.source,                                           // 1 注释
    /("""[\s\S]*?"""|'(?:\\[\s\S]|[^\\'\n])*'|"(?:\\[\s\S]|[^\\"\n])*")/.source,        // 2 字符串
    String.raw`\b(${KW.join('|')})\b`,                                                // 3 关键字
    /\b([A-Z][A-Za-z0-9_]*)\b/.source,                                                // 4 类型
    /(@[A-Za-z]+|\b\d+(?:\.\d+)?[LlFfDd]?\b)/.source,                                  // 5 注解 / 数字
    /\b([a-zA-Z_$][\w$]*)(?=\()/.source                                               // 6 方法调用
  ].join('|'), 'g');

  const RE_XML = /(<!--[\s\S]*?-->)|("[^"\n]*")|(<\/?[\w:.-]+|\/?>|<\?[\w-]+|\?>)/g;

  const RE_SH = /(#[^\n]*)|('(?:\\[\s\S]|[^\\'])*'|"(?:\\[\s\S]|[^\\"])*")|\b(java|javac|mvn|curl|telnet|cd|mkdir|export|set|echo|kill|jps|jconsole|sh|bash)\b/g;

  const SH_CLASSES = ['tk-cm', 'tk-st', 'tk-kw'];
  const XML_CLASSES = ['tk-cm', 'tk-st', 'tk-kw'];
  const JAVA_CLASSES = ['tk-cm', 'tk-st', 'tk-kw', 'tk-tp', 'tk-nm', 'tk-fn'];
  // 这些语言没有专门的规则，借用 shell 的注释 / 字符串规则就够了
  const SH_LIKE = new Set(['bash', 'sh', 'shell', 'properties']);
  const PLAIN = new Set(['text', 'http', 'out']);

  const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

  function paint(src, re, classes) {
    let out = '';
    let last = 0;
    // 所有分支都至少匹配一个字符，matchAll 不会卡在零宽匹配上
    for (const m of src.matchAll(re)) {
      out += esc(src.slice(last, m.index));
      const g = m.findIndex((v, i) => i > 0 && v !== undefined);
      out += `<span class="${classes[g - 1]}">${esc(m[g])}</span>`;
      last = m.index + m[0].length;
    }
    return out + esc(src.slice(last));
  }

  const LABEL = { java: 'Java', xml: 'XML', http: 'HTTP', out: '输出', text: 'text', bash: 'shell', sh: 'shell' };

  document.querySelectorAll('.code').forEach((box) => {
    const pre = box.querySelector('pre');
    if (!pre) return;
    const { lang = '', file = '' } = box.dataset;

    const bar = document.createElement('div');
    bar.className = 'bar';
    bar.innerHTML = `<span class="tag">${esc(file || LABEL[lang] || lang || 'code')}</span>`;
    const cp = Object.assign(document.createElement('button'), {
      className: 'copy',
      type: 'button',
      textContent: '复制'
    });
    cp.onclick = () => {
      navigator.clipboard?.writeText(pre.textContent);
      cp.textContent = '已复制';
      setTimeout(() => { cp.textContent = '复制'; }, 1400);
    };
    bar.appendChild(cp);
    box.insertBefore(bar, pre);

    const code = pre.textContent.replace(/^\n/, '').replace(/\s+$/, '');
    if (PLAIN.has(lang)) pre.innerHTML = esc(code);
    else if (lang === 'xml') pre.innerHTML = paint(code, RE_XML, XML_CLASSES);
    else if (SH_LIKE.has(lang)) pre.innerHTML = paint(code, RE_SH, SH_CLASSES);
    else pre.innerHTML = paint(code, RE_JAVA, JAVA_CLASSES);
  });

  /* ---------- 上一章 / 下一章 ---------- */
  const pager = document.getElementById('pager');
  if (pager && cur) {
    const prev = CHAPTERS.find((c) => c.n === cur - 1);
    const next = CHAPTERS.find((c) => c.n === cur + 1);
    const prevLink = prev
      ? `<a class="prev" href="${href(prev.n)}"><span>&larr; 上一章</span>第 ${prev.n} 章 · ${prev.t}</a>`
      : '<a class="prev" href="index.html"><span>&larr; 返回</span>教程首页</a>';
    const nextLink = next
      ? `<a class="next" href="${href(next.n)}"><span>下一章 &rarr;</span>第 ${next.n} 章 · ${next.t}</a>`
      : '<a class="next" href="index.html"><span>完结 &rarr;</span>回到教程首页</a>';
    pager.className = 'pager';
    pager.innerHTML = prevLink + nextLink;
  }

  /* ---------- 首页目录 ---------- */
  const grid = document.getElementById('toc-grid');
  if (grid) {
    grid.className = 'toc-grid';
    grid.innerHTML = CHAPTERS.map(({ n, t, d }) =>
      `<a class="toc-card" href="${href(n)}">` +
      `<div class="n">第 ${n} 章</div>` +
      `<div class="t">${t}</div>` +
      `<div class="d">${d}</div></a>`
    ).join('');
  }
})();
