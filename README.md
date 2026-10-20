# 每日新闻 · PCL Daily News

一个长在 Android 上的「每日新闻播报器」。界面照 PCL2 启动器的感觉做（浅色、圆角卡片、蓝色主色），每天定时抓新闻推到通知栏；另有一个可缩放的事件地图，把新闻按地名落到中国 / 世界两个视角上。

> 纯原生 Java + WebView，**零第三方依赖**、无 Gradle、无 AndroidX，整个 APK 只有 107 KB。

## 功能

| 模块 | 说明 |
| :-- | :-- |
| 新闻流 | 全部 / 科技 / 深度 / 要闻 / 天依 五个分类，卡片带配图（抓原文 og:image）、来源、时间 |
| 事件地图 | 中国 / 世界双视角，按地名把新闻落成光点；单指拖动、双指缩放、双击放大、点光点看该地事件 |
| 每日推送 | 默认 08:00 抓取并推送通知栏，通知里直接列前 5 条标题，可改时间 / 关推送 |
| 洛天依 | 官号投稿 + 百度资讯 + 必应 + Google News 多级兜底，全挂时用上次缓存（标「缓存」） |
| 设置 | 推送时间、通知开关、天依提醒、是否加载配图、测试通知 |

## 安装

```
release/pclnews-3.3.apk
```

- minSdk 26（Android 8.0+），targetSdk 34，包名 `com.pclnews`
- 首次打开要授予**通知权限**，否则无法每日推送
- 国产 ROM 请给本应用 **自启动 / 关闭电池优化**，否则闹钟会被杀

## 数据源

| 分类 | 来源 |
| :-- | :-- |
| 科技 | IT之家 RSS、cnBeta RSS |
| 深度 | Solidot、少数派 |
| 要闻 | 中国新闻网（即时新闻） |
| 天依 | B站官号 36081646、百度资讯（3 组关键词）、必应搜索、Google News |
| 配图 | 抓原文页 `og:image`，滚动到可见才加载（可在设置关闭） |

全部公开源，不需要任何 API Key。

## 地图是怎么做的

- 底图用 [阿里 DataV](https://datav.aliyun.com/) 的省级边界 GeoJSON（世界图用 echarts 的 world.json），经 **Douglas-Peucker 简化**后压成 SVG path：中国 12322 个点（2000×1520 画布）、世界 7935 个点（2400×1240 画布）。
- 投影：等距圆柱（Plate Carrée）。
- 定位：先匹配 104 个中国城市 → 省份映射到省会 → 63 个世界城市 → 58 个国家中心。
- 光点用 **HTML 绝对定位**（固定像素大小，不随缩放变形），地图轮廓用 SVG，两者共用同一套坐标换算。
- 缩放/平移靠改 `viewBox`，坐标层按百分比重新布局。

## 构建

不用 Gradle。整个 APK 在一台 Android 设备上就能编出来：

```sh
sh build.sh
```

需要一套离线工具链（JDK 21 + aapt2 + d8/R8 + apksig），脚本里默认路径是作者机器上的 `.probe` 目录，换环境改开头几个变量即可。

构建产物：`build/pclnews.apk`。

## 目录

```
pclnews/app/src/main/
├─ AndroidManifest.xml
├─ java/com/pclnews/
│  ├─ MainActivity.java   WebView + JS 桥（刷新 / 打开链接 / 抓图 / 设置）
│  ├─ Fetcher.java        HTTP、RSS/Atom 解析、百度资讯、必应、B站、og:image
│  ├─ Store.java          SharedPreferences 缓存与设置
│  ├─ Alarm.java          AlarmManager 每日定时（自我重排）
│  ├─ Notify.java         日报 / 天依两类通知
│  ├─ DailyReceiver.java  定时触发点（goAsync 保活抓取）
│  └─ BootReceiver.java   开机重排闹钟
├─ assets/
│  ├─ index.html          界面结构
│  ├─ style.css           浅色卡片风格
│  ├─ app.js              列表 / 地图 / 手势 / 设置
│  └─ mapdata.js          中国 + 世界地图 path（180 KB）
└─ res/                  主题、图标
```

## 已知限制

- 地图落点覆盖率约 3 成：科技类新闻标题里通常没有地名，会归入「未定位」（界面有计数）。
- B站投稿接口有风控（412 / -799），命中后 1 小时内不再请求，由百度资讯兜底。
- 通知依赖系统闹钟，被 ROM 杀进程后需要靠开机广播重排。

## License

MIT
