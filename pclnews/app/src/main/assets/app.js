(function () {
  'use strict';
  var PC = window.PC || {};
  var $ = function (id) { return document.getElementById(id); };
  var pad = function (n) { return n < 10 ? '0' + n : '' + n; };
  var CATS = { all: '全部', tech: '科技', deep: '深度', news: '要闻', lty: '天依', map: '地图' };

  var state = {
    items: [], cat: 'all', updated: 0, scope: 'cn', pick: null,
    imgs: {}, imgOn: true, lastErr: '', loading: false
  };

  /* ---------- 地名库 ---------- */
  var CITIES = [
  ["北京",116.41,39.90],
  ["上海",121.47,31.23],
  ["广州",113.26,23.13],
  ["深圳",114.06,22.55],
  ["杭州",120.15,30.27],
  ["南京",118.80,32.06],
  ["苏州",120.62,31.32],
  ["成都",104.07,30.57],
  ["重庆",106.55,29.56],
  ["武汉",114.30,30.59],
  ["西安",108.94,34.34],
  ["天津",117.20,39.08],
  ["长沙",112.94,28.23],
  ["郑州",113.62,34.75],
  ["青岛",120.38,36.07],
  ["济南",117.00,36.65],
  ["合肥",117.28,31.86],
  ["福州",119.30,26.08],
  ["厦门",118.09,24.48],
  ["南昌",115.89,28.68],
  ["沈阳",123.43,41.80],
  ["大连",121.62,38.92],
  ["哈尔滨",126.53,45.80],
  ["长春",125.32,43.82],
  ["石家庄",114.51,38.04],
  ["太原",112.55,37.87],
  ["呼和浩特",111.75,40.84],
  ["银川",106.23,38.49],
  ["兰州",103.83,36.06],
  ["西宁",101.78,36.62],
  ["乌鲁木齐",87.62,43.82],
  ["拉萨",91.11,29.65],
  ["昆明",102.83,24.88],
  ["贵阳",106.63,26.65],
  ["南宁",108.37,22.82],
  ["海口",110.20,20.04],
  ["三亚",109.51,18.25],
  ["香港",114.17,22.32],
  ["澳门",113.55,22.20],
  ["台北",121.52,25.03],
  ["无锡",120.31,31.49],
  ["宁波",121.55,29.87],
  ["温州",120.70,28.00],
  ["佛山",113.12,23.02],
  ["东莞",113.75,23.02],
  ["珠海",113.58,22.27],
  ["泉州",118.68,24.87],
  ["扬州",119.41,32.39],
  ["徐州",117.28,34.26],
  ["洛阳",112.45,34.62],
  ["烟台",121.45,37.46],
  ["唐山",118.18,39.63],
  ["包头",109.84,40.66],
  ["绵阳",104.68,31.47],
  ["桂林",110.29,25.27],
  ["遵义",106.93,27.73],
  ["大理",100.23,25.60],
  ["喀什",75.99,39.47],
  ["保定",115.46,38.87],
  ["临沂",118.35,35.05],
  ["赣州",114.93,25.83],
  ["汕头",116.68,23.35],
  ["柳州",109.41,24.31],
  ["宜昌",111.29,30.69],
  ["襄阳",112.14,32.04],
  ["常州",119.97,31.81],
  ["南通",120.86,32.01],
  ["绍兴",120.58,30.00],
  ["嘉兴",120.75,30.75],
  ["金华",119.65,29.09],
  ["台州",121.42,28.66],
  ["芜湖",118.38,31.33],
  ["南阳",112.53,32.99],
  ["淄博",118.05,36.81],
  ["威海",122.12,37.51],
  ["齐齐哈尔",123.92,47.35],
  ["呼伦贝尔",119.76,49.22],
  ["雄安",115.97,39.05],
  ["延安",109.49,36.60],
  ["敦煌",94.66,40.14],
  ["秦皇岛",119.60,39.94],
  ["大同",113.30,40.08],
  ["鄂尔多斯",109.78,39.61],
  ["马鞍山",118.51,31.67],
  ["淮南",117.02,32.63],
  ["泰安",117.09,36.20],
  ["济宁",116.59,35.41],
  ["潍坊",119.16,36.71],
  ["东营",118.50,37.46],
  ["丹东",124.40,40.00],
  ["延边",129.51,42.89],
  ["阿克苏",80.26,41.17],
  ["和田",79.93,37.11],
  ["伊犁",81.32,43.92],
  ["克拉玛依",84.89,45.58],
  ["攀枝花",101.72,26.58],
  ["宜宾",104.63,28.77],
  ["泸州",105.44,28.87],
  ["南充",106.11,30.84],
  ["敦煌",94.66,40.14],
  ["香格里拉",99.70,27.83],
  ["九寨沟",103.92,33.26],
  ["延吉",129.51,42.89],
  ["伊宁",81.32,43.92]
  ];
  var COUNTRIES = [
  ["美国",-98,39],
  ["日本",138,36.5],
  ["英国",-2,53.5],
  ["法国",2.5,47],
  ["德国",10,51],
  ["俄罗斯",95,60],
  ["韩国",127.5,36.5],
  ["印度",78,22],
  ["巴西",-51,-11],
  ["澳大利亚",134,-25],
  ["加拿大",-106,56],
  ["意大利",12.5,42.5],
  ["西班牙",-3.7,40.4],
  ["荷兰",5.5,52.2],
  ["以色列",35,31.5],
  ["阿联酋",54,24],
  ["新加坡",103.8,1.4],
  ["泰国",101,15],
  ["越南",106,16],
  ["印度尼西亚",113,-2],
  ["马来西亚",102,4],
  ["菲律宾",122,12],
  ["土耳其",35,39],
  ["埃及",30,27],
  ["南非",24,-29],
  ["墨西哥",-102,23],
  ["阿根廷",-64,-34],
  ["智利",-71,-35],
  ["瑞士",8,46.8],
  ["瑞典",15,62],
  ["挪威",10,62],
  ["丹麦",10,56],
  ["芬兰",26,64],
  ["波兰",20,52],
  ["乌克兰",32,49],
  ["伊朗",53,32],
  ["沙特阿拉伯",45,24],
  ["卡塔尔",51,25],
  ["新西兰",174,-41],
  ["爱尔兰",-8,53],
  ["比利时",4.5,50.5],
  ["奥地利",14,47.5],
  ["葡萄牙",-8,39.5],
  ["捷克",15.5,49.8],
  ["希腊",22,39],
  ["匈牙利",19,47],
  ["哈萨克斯坦",68,48],
  ["巴基斯坦",69,30],
  ["孟加拉国",90,24],
  ["斯里兰卡",81,7.5],
  ["尼泊尔",84,28],
  ["缅甸",96,21],
  ["柬埔寨",105,12.5],
  ["老挝",103,18],
  ["蒙古",103,46],
  ["朝鲜",127,40],
  ["台湾",121,23.7]
  ];
  CITIES.sort(function (a, b) { return b[0].length - a[0].length; });
  COUNTRIES.sort(function (a, b) { return b[0].length - a[0].length; });
  var CITY_MAP = {}; CITIES.forEach(function (c) { CITY_MAP[c[0]] = c; });
  var PROV = {
    '广东': '广州', '江苏': '南京', '浙江': '杭州', '山东': '济南', '河南': '郑州', '湖北': '武汉',
    '湖南': '长沙', '四川': '成都', '福建': '福州', '安徽': '合肥', '河北': '石家庄', '山西': '太原',
    '陕西': '西安', '辽宁': '沈阳', '吉林': '长春', '黑龙江': '哈尔滨', '江西': '南昌', '广西': '南宁',
    '云南': '昆明', '贵州': '贵阳', '甘肃': '兰州', '青海': '西宁', '宁夏': '银川', '新疆': '乌鲁木齐',
    '西藏': '拉萨', '内蒙古': '呼和浩特', '海南': '海口'
  };

  function locate(it) {
    var txt = (it.t || '') + ' ' + (it.d || '').slice(0, 40);
    var city = null, country = null, wcity = null, i;
    for (i = 0; i < CITIES.length; i++) {
      if (txt.indexOf(CITIES[i][0]) >= 0) { city = CITIES[i]; break; }
    }
    if (!city) {
      for (var p in PROV) {
        if (txt.indexOf(p) >= 0 && CITY_MAP[PROV[p]]) { city = CITY_MAP[PROV[p]]; break; }
      }
    }
    for (i = 0; i < COUNTRIES.length; i++) {
      if (txt.indexOf(COUNTRIES[i][0]) >= 0) { country = COUNTRIES[i]; break; }
    }
    for (i = 0; i < WORLD_CITIES.length; i++) {
      if (txt.indexOf(WORLD_CITIES[i][0]) >= 0) { wcity = WORLD_CITIES[i]; break; }
    }
    if (!city && !country && !wcity) return null;
    return { city: city, country: country, wcity: wcity };
  }

  function project(scope, lon, lat) {
    if (scope === 'world') return [ (lon + 180) / 360 * 2400, (90 - lat) / 180 * 1240 ];
    return [ (lon - 72) / 65 * 2000, (54.6 - lat) / 37.6 * 1520 ];
  }

  /* ---------- 工具 ---------- */
  function esc(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }
  function fmt(ts) {
    if (!ts) return '';
    var d = new Date(ts), now = new Date(), diff = (now - d) / 1000;
    if (diff < 120) return '刚刚';
    if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前';
    if (d.toDateString() === now.toDateString()) return Math.floor(diff / 3600) + ' 小时前';
    return (d.getMonth() + 1) + '/' + d.getDate() + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
  }

  function cardHtml(it) {
    var u = esc(it.u), fresh = it.p && (Date.now() - it.p) < 12 * 3600 * 1000;
    var showThumb = state.imgOn;
    return '<article class="card" data-u="' + u + '">'
      + (showThumb ? '<div class="thumb" data-u="' + u + '"><i></i></div>' : '')
      + '<div class="card-body">'
      + '<div class="card-t">' + esc(it.t) + '</div>'
      + (it.d ? '<div class="card-d">' + esc(it.d) + '</div>' : '')
      + '<div class="card-m"><b>' + esc(it.s || CATS[it.c] || '') + '</b>'
      + '<span class="sep"></span><span>' + fmt(it.p) + '</span>'
      + (it.o ? '<span class="cache">缓存</span>' : '')
      + (fresh ? '<span class="new"></span>' : '')
      + '</div></div></article>';
  }

  /* ---------- 列表 ---------- */
  function renderFeed() {
    var list = state.cat === 'all' ? state.items
      : state.items.filter(function (i) { return i.c === state.cat; });
    $('feed').innerHTML = list.map(cardHtml).join('');
    var on = !list.length;
    $('empty').className = 'empty' + (on ? ' on' : '');
    if (on) {
      if (state.cat === 'lty') {
        $('emptyT').textContent = '天依这边暂时没有内容';
        $('emptyD').textContent = '已尝试百度资讯 / 必应 / B站 / Google，被限流时会用上次缓存兜底，点右上角再刷新一次';
      } else {
        $('emptyT').textContent = '还没有内容';
        $('emptyD').textContent = '点右上角刷新按钮抓取';
      }
    }
    var d = new Date();
    var wd = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()];
    $('pageTitle').textContent = state.cat === 'all' ? '每日新闻' : CATS[state.cat];
    $('pageDate').textContent = (d.getMonth() + 1) + ' 月 ' + d.getDate() + ' 日 ' + wd
      + (state.updated ? ' · ' + fmt(state.updated) + '更新 · ' + state.items.length + ' 条' : '');
    observeThumbs();
  }

  /* ---------- 图片懒加载 ---------- */
  var observer = null;
  function observeThumbs() {
    var thumbs = [].slice.call(document.querySelectorAll('.thumb:not([data-done])'));
    if (!thumbs.length) return;
    if (typeof IntersectionObserver === 'undefined' || !PC.ogImage) {
      thumbs.forEach(function (t) { requestImg(t); });
      return;
    }
    if (!observer) {
      observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (en) {
          if (en.isIntersecting) { observer.unobserve(en.target); requestImg(en.target); }
        });
      }, { rootMargin: '220px 0px' });
    }
    thumbs.forEach(function (t) { observer.observe(t); });
  }
  function requestImg(t) {
    if (t.getAttribute('data-done')) return;
    t.setAttribute('data-done', '1');
    var u = t.getAttribute('data-u');
    var cached = state.imgs[u];
    if (cached) { applyImg(u, cached); return; }
    try { if (PC.ogImage) PC.ogImage(u); } catch (e) { }
  }
  function applyImg(u, url) {
    if (!url) return;
    state.imgs[u] = url;
    var nodes = document.querySelectorAll('.thumb[data-u="' + u.replace(/"/g, '\\"') + '"]');
    for (var i = 0; i < nodes.length; i++) {
      nodes[i].classList.add('on', 'ok');
      var inner = nodes[i].querySelector('i');
      if (inner) inner.style.backgroundImage = 'url("' + url.replace(/"/g, '\\"') + '")';
    }
  }
  window.onImg = function (u, url) { try { applyImg(u, url); } catch (e) { } };

  /* ---------- 地图 ---------- */
  var WORLD_CITIES = [
    ["纽约", -74.0, 40.7], ["华盛顿", -77.0, 38.9], ["硅谷", -122.0, 37.4], ["洛杉矶", -118.2, 34.1],
    ["波士顿", -71.1, 42.4], ["西雅图", -122.3, 47.6], ["伦敦", -0.1, 51.5], ["巴黎", 2.35, 48.85],
    ["柏林", 13.4, 52.5], ["慕尼黑", 11.6, 48.1], ["阿姆斯特丹", 4.9, 52.4], ["布鲁塞尔", 4.35, 50.85],
    ["苏黎世", 8.5, 47.4], ["日内瓦", 6.1, 46.2], ["斯德哥尔摩", 18.1, 59.3], ["奥斯陆", 10.7, 59.9],
    ["赫尔辛基", 24.9, 60.2], ["哥本哈根", 12.6, 55.7], ["都柏林", -6.3, 53.3], ["马德里", -3.7, 40.4],
    ["巴塞罗那", 2.2, 41.4], ["罗马", 12.5, 41.9], ["米兰", 9.2, 45.5], ["维也纳", 16.4, 48.2],
    ["布拉格", 14.4, 50.1], ["华沙", 21.0, 52.2], ["雅典", 23.7, 38.0], ["伊斯坦布尔", 29.0, 41.0],
    ["莫斯科", 37.6, 55.8], ["圣彼得堡", 30.3, 59.9], ["东京", 139.7, 35.7], ["大阪", 135.5, 34.7],
    ["首尔", 127.0, 37.5], ["台北", 121.5, 25.0], ["新加坡", 103.8, 1.35], ["曼谷", 100.5, 13.8],
    ["吉隆坡", 101.7, 3.1], ["雅加达", 106.8, -6.2], ["马尼拉", 121.0, 14.6], ["河内", 105.8, 21.0],
    ["胡志明市", 106.7, 10.8], ["新德里", 77.2, 28.6], ["孟买", 72.8, 19.1], ["班加罗尔", 77.6, 13.0],
    ["迪拜", 55.3, 25.2], ["阿布扎比", 54.4, 24.5], ["利雅得", 46.7, 24.7], ["特拉维夫", 34.8, 32.1],
    ["开罗", 31.2, 30.0], ["内罗毕", 36.8, -1.3], ["约翰内斯堡", 28.0, -26.2], ["开普敦", 18.4, -33.9],
    ["悉尼", 151.2, -33.9], ["墨尔本", 145.0, -37.8], ["奥克兰", 174.8, -36.9], ["多伦多", -79.4, 43.7],
    ["温哥华", -123.1, 49.3], ["蒙特利尔", -73.6, 45.5], ["墨西哥城", -99.1, 19.4], ["圣保罗", -46.6, -23.5],
    ["布宜诺斯艾利斯", -58.4, -34.6], ["圣地亚哥", -70.7, -33.5], ["圣克拉拉", -121.9, 37.3]
  ];
  WORLD_CITIES.sort(function (a, b) { return b[0].length - a[0].length; });

  function aggregate() {
    if (!window.MAPDATA) return { groups: [], other: 0 };
    var map = {}, other = 0;
    state.items.forEach(function (it) {
      var loc = locate(it), pick = null;
      if (loc) {
        if (state.scope === 'world') pick = loc.wcity || loc.city || loc.country;
        else pick = loc.city;
      }
      if (!pick) { other++; return; }
      var key = pick[0];
      if (!map[key]) map[key] = { name: pick[0], lon: pick[1], lat: pick[2], items: [] };
      map[key].items.push(it);
    });
    var groups = Object.keys(map).map(function (k) { return map[k]; });
    groups.sort(function (a, b) { return b.items.length - a.items.length || a.name.localeCompare(b.name); });
    return { groups: groups, other: other };
  }

  /* ---- 视野 ---- */
  var vt = { x: 0, y: 0, w: 2000, h: 1520, baseW: 2000, baseH: 1520, scope: '' };
  var lastGroups = [];

  function applyBox() {
    $('mapSvg').setAttribute('viewBox', vt.x.toFixed(0) + ' ' + vt.y.toFixed(0) + ' ' + vt.w.toFixed(0) + ' ' + vt.h.toFixed(0));
    layoutPins();
  }
  function resetView() {
    vt.x = 0; vt.y = 0; vt.w = vt.baseW; vt.h = vt.baseH;
    applyBox();
  }
  function clampView() {
    var minW = vt.baseW / 12;
    if (vt.w < minW) { var k = minW / vt.w; vt.x -= (vt.baseW / 2 - vt.x) * (k - 1); vt.y -= (vt.baseH / 2 - vt.y) * (k - 1); vt.w *= k; vt.h *= k; }
    if (vt.w > vt.baseW) { var k2 = vt.baseW / vt.w; vt.x -= (vt.baseW / 2 - vt.x) * (k2 - 1); vt.y -= (vt.baseH / 2 - vt.y) * (k2 - 1); vt.w *= k2; vt.h *= k2; }
    var padX = vt.w * 0.3, padY = vt.h * 0.3;
    vt.x = Math.max(-padX, Math.min(vt.baseW - vt.w + padX, vt.x));
    vt.y = Math.max(-padY, Math.min(vt.baseH - vt.h + padY, vt.y));
  }

  /* ---- 点/标签定位（固定像素大小，不随缩放变形） ---- */
  function layoutPins() {
    var layer = $('pinLayer');
    if (!layer || !lastGroups.length) return;
    var nodes = layer.children || [];
    for (var i = 0; i < lastGroups.length && i < nodes.length; i++) {
      var g = lastGroups[i], el = nodes[i];
      var p = project(state.scope, g.lon, g.lat);
      var lx = (p[0] - vt.x) / vt.w * 100;
      var ly = (p[1] - vt.y) / vt.h * 100;
      if (lx < -3 || lx > 103 || ly < -3 || ly > 103) { el.style.display = 'none'; continue; }
      el.style.display = '';
      el.style.left = lx.toFixed(3) + '%';
      el.style.top = ly.toFixed(3) + '%';
    }
  }

  function renderMap() {
    var M = window.MAPDATA;
    if (!M) {
      $('mapList').innerHTML = '<div class="empty on" style="padding:40px 20px"><div class="empty-t">地图数据没加载出来</div><div class="empty-d">重新安装一次 App 试试</div></div>';
      return;
    }
    var scope = state.scope, view = scope === 'world' ? M.world : M.cn;
    if (vt.scope !== scope) {
      vt.scope = scope; vt.baseW = view.w; vt.baseH = view.h;
      vt.x = 0; vt.y = 0; vt.w = view.w; vt.h = view.h;
    }
    $('mapCanvas').style.aspectRatio = view.w + ' / ' + view.h;
    $('mapSvg').setAttribute('viewBox', vt.x + ' ' + vt.y + ' ' + vt.w + ' ' + vt.h);
    $('mapLand').setAttribute('d', view.path);

    var agg = aggregate();
    lastGroups = agg.groups;
    var html = '', shown = 0;
    for (var i = 0; i < lastGroups.length; i++) {
      var g = lastGroups[i], n = g.items.length, on = state.pick === g.name;
      var showLabel = shown < 10 || on;
      if (showLabel) shown++;
      html += '<div class="pin' + (n > 1 ? ' multi' : '') + (on ? ' on' : '')
        + '" data-c="' + esc(g.name) + '"><i class="dot"></i>'
        + (showLabel ? '<b class="lb">' + esc(g.name) + (n > 1 ? ' ' + n : '') + '</b>' : '')
        + '</div>';
    }
    $('pinLayer').innerHTML = html;
    layoutPins();

    $('mapCount').textContent = agg.groups.length + ' 处' + (agg.other ? ' · ' + agg.other + ' 条未定位' : '');

    var arr = state.pick
      ? (lastGroups.filter(function (g) { return g.name === state.pick; })[0] || { items: [] }).items
      : lastGroups.reduce(function (a, g) { return a.concat(g.items); }, []);
    arr = arr.slice().sort(function (a, b) { return (b.p || 0) - (a.p || 0); });
    $('mapListTitle').textContent = state.pick ? state.pick : (scope === 'world' ? '全部国际 / 地区事件' : '全部城市事件');
    $('mapListN').textContent = arr.length + ' 条';
    $('mapList').innerHTML = arr.length ? arr.map(cardHtml).join('')
      : '<div class="empty on" style="padding:34px 20px"><div class="empty-t">这一屏还没有落点</div>'
        + '<div class="empty-d">新闻标题里没出现地名就会归到「未定位」，换个模式或刷新试试</div></div>';
    observeThumbs();
  }

  /* ---- 手势 ---- */
  function setupGestures() {
    var svg = $('mapSvg'), box = $('mapCanvas'), layer = $('pinLayer');
    var dragging = false, moved = false, lx = 0, ly = 0, pinch0 = 0;

    function boxScale() {
      var r = box.getBoundingClientRect();
      return Math.min(r.width / vt.w, r.height / vt.h) || 1;
    }
    function toSvg(cx, cy) {
      var r = box.getBoundingClientRect();
      var s = Math.min(r.width / vt.w, r.height / vt.h) || 1;
      return { x: vt.x + (cx - r.left) / s, y: vt.y + (cy - r.top) / s };
    }
    function zoomAt(anchor, k) {
      var nw = Math.max(vt.baseW / 12, Math.min(vt.baseW, vt.w * k));
      var real = nw / vt.w;
      vt.x = anchor.x - (anchor.x - vt.x) * real;
      vt.y = anchor.y - (anchor.y - vt.y) * real;
      vt.w = nw; vt.h = vt.h * real;
      clampView(); applyBox();
    }

    box.addEventListener('touchstart', function (e) {
      moved = false;
      if (e.touches.length === 1) {
        dragging = true; lx = e.touches[0].clientX; ly = e.touches[0].clientY;
      } else if (e.touches.length === 2) {
        dragging = false;
        pinch0 = Math.hypot(e.touches[0].clientX - e.touches[1].clientX, e.touches[0].clientY - e.touches[1].clientY);
      }
    }, { passive: true });

    box.addEventListener('touchmove', function (e) {
      if (e.touches.length === 1 && dragging) {
        var dx = e.touches[0].clientX - lx, dy = e.touches[0].clientY - ly;
        if (Math.abs(dx) + Math.abs(dy) > 3) moved = true;
        var s = boxScale();
        vt.x -= dx / s; vt.y -= dy / s;
        lx = e.touches[0].clientX; ly = e.touches[0].clientY;
        clampView(); applyBox();
        e.preventDefault();
      } else if (e.touches.length === 2 && pinch0 > 0) {
        var d = Math.hypot(e.touches[0].clientX - e.touches[1].clientX, e.touches[0].clientY - e.touches[1].clientY);
        if (d > 0) {
          var mid = toSvg((e.touches[0].clientX + e.touches[1].clientX) / 2, (e.touches[0].clientY + e.touches[1].clientY) / 2);
          zoomAt(mid, pinch0 / d);
          pinch0 = d; moved = true;
        }
        e.preventDefault();
      }
    }, { passive: false });

    box.addEventListener('touchend', function () { dragging = false; pinch0 = 0; }, { passive: true });

    layer.addEventListener('click', function (e) {
      if (moved) { moved = false; return; }
      var pin = e.target.closest ? e.target.closest('.pin') : null;
      if (!pin) return;
      var c = pin.getAttribute('data-c');
      state.pick = (state.pick === c) ? null : c;
      renderMap();
    });

    box.addEventListener('dblclick', function (e) {
      var m = toSvg(e.clientX, e.clientY);
      if (vt.w > vt.baseW * 0.55) zoomAt(m, 0.45); else resetView();
    });

    $('mapReset').addEventListener('click', function (e) { e.stopPropagation(); resetView(); });
  }

  /* ---------- 统一渲染 ---------- */
  function render() {
    var isMap = state.cat === 'map';
    $('mapwrap').classList.toggle('on', isMap);
    $('feed').style.display = isMap ? 'none' : '';
    $('empty').style.display = isMap ? 'none' : '';
    if (isMap) renderMap(); else renderFeed();
    renderTabs();
  }
  function renderTabs() {
    var tabs = document.querySelectorAll('.tab');
    for (var i = 0; i < tabs.length; i++) {
      tabs[i].classList.toggle('active', tabs[i].getAttribute('data-cat') === state.cat);
    }
  }

  /* ---------- 数据 ---------- */
  function ingest(payload, keepPick) {
    if (!payload) return;
    if (payload.items && payload.items.length) state.items = payload.items;
    if (payload.updated) state.updated = payload.updated;
    if (!keepPick) state.pick = null;
    render();
    if ($('tipbar').classList.contains('on')) return;
  }
  window.onNews = function (p) {
    loading(false);
    if (p && p.ok === false) {
      state.lastErr = p.msg || '抓取失败';
      $('tipbar').className = 'tipbar on';
      $('tipbar').textContent = '抓取失败：' + state.lastErr + '（下拉不了就点右上角 ⟳ 再试）';
    } else {
      $('tipbar').className = 'tipbar';
    }
    ingest(p, false);
  };
  window.onLoading = function () { loading(true); };

  function loading(on) {
    state.loading = on;
    var b = $('btnRefresh');
    if (!b) return;
    if (on) b.classList.add('spin'); else b.classList.remove('spin');
  }

  /* ---------- 事件绑定 ---------- */
  $('tabs').addEventListener('click', function (e) {
    var el = e.target.closest('.tab');
    if (!el) return;
    state.cat = el.getAttribute('data-cat');
    if (state.cat !== 'map') state.pick = null;
    render();
    if (state.cat === 'map' && window.MAPDATA) renderMap();
  });

  $('feed').addEventListener('click', function (e) {
    var card = e.target.closest('.card');
    if (card && PC.open) PC.open(card.getAttribute('data-u'));
  });
  $('mapList').addEventListener('click', function (e) {
    var card = e.target.closest('.card');
    if (card && PC.open) PC.open(card.getAttribute('data-u'));
  });
  $('mapseg').addEventListener('click', function (e) {
    var el = e.target.closest('.seg');
    if (!el) return;
    state.scope = el.getAttribute('data-scope');
    state.pick = null;
    var segs = document.querySelectorAll('.seg');
    for (var i = 0; i < segs.length; i++) segs[i].classList.toggle('on', segs[i] === el);
    renderMap();
  });
  $('btnRefresh').addEventListener('click', function () { if (PC.refresh) PC.refresh(); });

  /* ---------- 设置 ---------- */
  function openSheet() {
    $('sheet').classList.add('on');
    if (PC.setModal) PC.setModal(true);
    try {
      var s = JSON.parse(PC.settings ? PC.settings() : '{}');
      $('inHour').value = s.hour == null ? 8 : s.hour;
      $('inMin').value = s.minute == null ? 0 : s.minute;
      $('swNotify').checked = s.notify !== false;
      $('swLty').checked = s.lty !== false;
      $('swImg').checked = s.img !== false;
      syncChips();
    } catch (e) { }
  }
  function closeSheet() {
    $('sheet').classList.remove('on');
    if (PC.setModal) PC.setModal(false);
  }
  window.closeModal = closeSheet;
  function syncChips() {
    var h = parseInt($('inHour').value, 10) || 0, m = parseInt($('inMin').value, 10) || 0;
    var chips = document.querySelectorAll('.chip');
    for (var i = 0; i < chips.length; i++) {
      chips[i].classList.toggle('on',
        parseInt(chips[i].getAttribute('data-h'), 10) === h && parseInt(chips[i].getAttribute('data-m'), 10) === m);
    }
  }
  $('btnSettings').addEventListener('click', openSheet);
  $('btnCloseSheet').addEventListener('click', closeSheet);
  $('sheet').addEventListener('click', function (e) { if (e.target === $('sheet')) closeSheet(); });
  $('chips').addEventListener('click', function (e) {
    var c = e.target.closest('.chip');
    if (!c) return;
    $('inHour').value = c.getAttribute('data-h');
    $('inMin').value = c.getAttribute('data-m');
    syncChips();
  });
  $('btnTest').addEventListener('click', function () { if (PC.testNotify) PC.testNotify(); });
  $('swImg').addEventListener('change', function () {
    state.imgOn = $('swImg').checked;
    render();
  });
  $('btnSave').addEventListener('click', function () {
    var h = parseInt($('inHour').value, 10), m = parseInt($('inMin').value, 10);
    if (isNaN(h) || h < 0 || h > 23) h = 8;
    if (isNaN(m) || m < 0 || m > 59) m = 0;
    if (PC.save) PC.save(h, m, $('swNotify').checked, $('swLty').checked, $('swImg').checked);
    closeSheet();
  });

  /* ---------- 启动 ---------- */
  function boot() {
    try {
      var cached = PC.cached ? JSON.parse(PC.cached()) : null;
      if (cached) ingest(cached, false);
    } catch (e) { state.lastErr = 'cache:' + e.message; }
    if (!state.items.length && PC.refresh) PC.refresh();
    else setTimeout(function () { if (PC.refresh) PC.refresh(); }, 400);
  }
  setupGestures();
  boot();
})();
