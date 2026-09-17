# EnorialsHUD 1.1.16 — feature inventory và codebase review

## Phạm vi và mức tin cậy

- Artifact: `EnorialsHUD.jar`, SHA-256 `9F9ED00F25178DE1433D2C193EB5D7A36CF44F77A7CF028872E65300DF6BA49F`.
- Decompiler: CFR 0.152; 399 file Java thuộc namespace `sk.enorials`, 3.046 resource.
- Bytecode target: Java 17 (class major 61).
- JAR có chữ ký hợp lệ về integrity, nhưng signer dùng certificate tự ký nên không có trust chain công khai.
- Các nhận xét hành vi từ source decompile là `DERIVED_SOURCE`; chưa có Paper server/dependency matrix để runtime-test.
- CFR không dựng hoàn chỉnh bốn method (`HudAccess.raw`, `PackManager.lambda$startBuild$0`, `PartyMapTracker.run`, `GuiMinimapPacketSender.iconComponent`) và cảnh báo một số cấu trúc try/catch. Không xem những đoạn này là source nguyên bản.

## Kết luận nhanh

Đây là một plugin bundle khá tham vọng và có nhiều ý tưởng tốt: HUD tùy biến, menu/social UX, party/friends/trade, minimap/fullscreen map/radar, GPS/quest navigation và resource-pack compiler đa phiên bản đều nằm trong một JAR. Điểm nổi bật nhất là compatibility pipeline theo protocol/resource-pack profile, transaction journal của trading, bounded work queue và degradation khi optional integration vắng mặt.

Rủi ro lớn nhất không nằm ở thuật toán đơn lẻ mà ở độ rộng của compatibility surface: direct NMS/CraftBukkit/Netty, shader/resource-pack nhiều đời client, reflection vào nhiều plugin và metadata release không đồng nhất. Muốn vận hành production cần khóa exact server build và test một matrix cụ thể; `api-version: 1.19` không chứng minh binary compatibility với mọi server từ 1.19 trở lên.

## Nhận diện artifact

- Plugin chính: `EnorialsHUD`, version `1.1.16`, main `sk.enorials.hud.EnorialsHUD`, load `POSTWORLD` (`resources/plugin.yml`).
- Đây là **một plugin bundle**. Main class tự khởi tạo Party, Minimap và GPS tại `src/sk/enorials/hud/EnorialsHUD.java:407-413`.
- Bundle cung cấp các tên tương thích `EnorialsParty`, `EnorialsMinimap`, `EnorialsGPS`, `EnorialsAnchorHUD`.
- Module metadata nhúng có version riêng và không phải ba plugin phải cài độc lập: GPS `2.0.0`, Minimap `26.2-migration-gui-1`, Party `1.2.17...mc26.2`.
- Public commands đăng ký trong descriptor chính chỉ gồm `/enohud` và `/gpsapi`.

## Feature map

### HUD và giao diện

- HUD player/target/party/GPS, health/mana/food/armor, effect icons và timer.
- Portrait từ skin, target portrait, compact mode, layout và global offset.
- Sáu theme được thấy trong menu: Enorials, Frostbound, Corsair, Inferno, Wildwood, Astral.
- Hotbar overlay, popup artwork, graphical inventory menu và admin menu.
- Condition engine theo effect, gamemode, placeholder và progress.
- API cho plugin khác cung cấp stat, target, marker, popup và điều khiển UI.

### Quest và hội thoại

- Quest log tùy biến, size setting, quest sidebar interception.
- BetonQuest dialogue window, portrait, pages/replies.
- Quest markers trên minimap và route GPS đến quest/NPC.
- BetonQuest action/condition/objective; GPS hook cho start/stop/reach/mode change.

### Minimap

- Vanilla-client minimap qua resource pack, font/shader, map/entity packet và GUI transport.
- Renderer terrain `vanilla`/`flat`, circle/square, left/right, ba size, zoom, fog và danger ring.
- Fullscreen map 5×3 segment mặc định.
- Personal/public/timed markers, category/icon/range; death marker tự xóa khi quay lại gần.
- Radar hostile/neutral/friendly, mặc định radius 64, vertical range 15, max 48 entity, update 2 tick.
- SQLite cho settings/markers, terrain cache có time budget, data retention cleanup.

### GPS/navigation

- ActionBar/BossBar/Compass, chỉ một mode active; fallback khi mode bị disable.
- GUI target picker, chat input, player-head picker, POI search/pagination, favorites, quest routes.
- Persist/resume route; VFX/sound; high/low indicator, distance, compact HUD, target beacon.
- Restriction theo allowed/denied world và blocked cuboid.
- Target: player, Bukkit/Mythic mob, Citizens/FancyNpcs/ZNPCsPlus/Shopkeepers/Denizen NPC, tọa độ, warp Essentials/CMI, WorldGuard region, Towny spawn, hologram, vanilla/custom block và generic entity qua tag/metadata/PDC.
- Bukkit service API, lifecycle events và `/gpsapi` cho automation/profiling.

### Party, friends và social

- Private/public party, browse/join, invite/accept/decline, promote/kick/votekick/disband.
- Friendly-fire prevention; shared EXP; locator/actionbar/compass.
- Party HUD, mana/spell resource, buff/event status.
- Friends request/remove/presence, SQLite persistence.
- Party/friend/trade prompt có Accept/Decline artwork.
- BetonQuest party hook và MythicDungeons party-provider bridge.

### Trading

- Trade item và tiền qua Vault nếu economy provider tồn tại.
- Range/visibility/permission checks, request cooldown, session timeout.
- Offer revision, lock/unlock, double confirmation và inventory observation chống thay đổi giữa lúc xác nhận.
- Write-ahead-style JSONL transaction journal; nếu economy operation rơi vào trạng thái không chắc chắn, khóa player liên quan để admin reconcile thay vì retry mù.

### Resource pack và compatibility

- Merge/copy/build pack, giới hạn file count và uncompressed size.
- Delivery qua `server.properties`, direct URL, static folder hoặc local HTTP host `127.0.0.1:25599` cho reverse proxy.
- Managed pack theo client profile, fixed single pack, SHA-1 và pack-load guard.
- Protocol profile từ client 1.17 đến 26.3; format resource pack từ 7 đến 97.1.
- Legacy/modern shader, font, spacing, item model và vanilla bar conversion.
- Optional GitHub upload/cleanup cho generated packs; default tắt.

## Commands và quyền chính

- `/enohud`: menu trung tâm, HUD settings, party, map, GPS, effects, friends, markers, trade và access admin.
- HUD admin/player command có `packcheck`, reload/build pack, layout, popup, pack URL và avatar refresh.
- Party: create/open/join/invite/accept/decline/leave/kick/promote/votekick/friend/hud.
- Minimap Brigadier tree: enable, position/size/shape, fog/danger ring, preset, marker/POI, fullscreen và pack build.
- `/gpsapi`: start/stop/mode, dynamic route add/remove/clear/list, perf/perf reset.
- Permission surface được chia tương đối kỹ theo HUD, target/effects/quest/conversation, party/friend/map, minimap/fullscreen/marker/radar, GPS target type và admin.
- `access.yml` là permission layer nội bộ có default, first-join grant và per-player override; bundle mặc định grant `enorials.hud.all` cho player.

## Optional integrations

ViaVersion/ViaBackwards, Vault, PlaceholderAPI, BetonQuest, MythicMobs, Citizens, Sentinel, ModelEngine, EliteMobs, Denizen, ZNPCsPlus, MMOCore, MMOItems, EcoBosses, ItemsAdder, Oraxen, Nexo, Essentials, CMI, WorldGuard, Towny, DecentHolograms, Shopkeepers, FancyNpcs và MythicCrucible. Code còn có MythicDungeons bridge.

ProtocolLib chỉ được detect/log trong Party module; transport thực tế dùng Adventure/Bukkit resource-pack API và direct CraftBukkit/NMS/Netty.

## Điểm mạnh kỹ thuật

- Module hóa theo domain (`hud`, `minimap`, `gps`, `partyhud`) trong một lifecycle owner, thay vì nhiều singleton plugin độc lập.
- Optional integration thường dùng soft-depend + reflection/capability check, giảm hard linkage khi plugin vắng mặt.
- Pack build có queue giới hạn concurrency, debounce, immutable content-addressed filename và cleanup.
- HTTP pack host chỉ cho GET/HEAD, path phải khớp SHA-1 ZIP, dùng `NOFOLLOW_LINKS`, bounded queue và fixed worker count (`ResourcePackHost.java:86-115`).
- ZIP/resource-pack ingest có kiểm tra traversal, duplicate/case collision, file count và byte budget (`BetonQuestPack.java:145-167`).
- Minimap terrain cache có budget theo tick thay vì build vô hạn trên main thread.
- Trade flow xử lý failure của withdraw/deposit/refund bằng durable journal và trạng thái REVIEW (`TradeMoney.java:31-66`, `TradeJournal.java:75-106`).
- Lifecycle có cleanup cho nhiều service, tasks, HTTP executor, database và packet sender.
- Permission được check ở các command tree quan trọng; admin/resource-pack operation có gate riêng.

## Findings

> Findings dưới đây dựa trên decompiled source. Cần đối chiếu source gốc và runtime trước khi sửa.

### [High] GPS có thể làm lộ vị trí player vanish/hidden

Chat target lấy player bằng exact name rồi start `PlayerTarget` mà không gọi `viewer.canSee(target)` (`ChatInputService.java:131-145`). `PlayerTarget.resolve()` trả live location mỗi lần update cũng không re-check visibility/consent (`PlayerTarget.java:41-46`). Trong khi GUI picker có filter visibility, hai entry path không nhất quán. Quyền `enorials.gps.track.player` lại nằm trong bundle `enorials.hud.all`, và access mặc định grant bundle này.

Tác động: player biết exact name có thể theo dõi staff đang vanish/hidden, kể cả khi visibility đổi sau khi route bắt đầu. Cần centralized authorization trong `NavigationManager`/`PlayerTarget` ở mọi resolve: `canSee`, target-world policy, opt-out/consent và stop route ngay khi authorization mất.

### [High] Compatibility contract không đồng nhất

- `META-INF/enorials-release.properties` ghi `minecraft=1.21`.
- GPS README tuyên bố Paper `1.19.x`, `1.20.x`, `1.21.x`.
- Module metadata/resource/code lại dùng `26.2` và `26.3`; protocol map tới 26.3.
- Main plugin dùng trực tiếp nhiều NMS/CraftBukkit class (`MapId`, `DataComponents`, packet constructors, CraftPlayer/CraftWorld) mà không có exact-version adapter module.

Tác động: operator có thể cài trên server chỉ vì thấy `api-version: 1.19`, rồi gặp linkage error hoặc packet mismatch. Cần công bố exact Paper/Purpur build + Java matrix, tách NMS adapter theo target, và fail-fast với thông báo build fingerprint trước khi khởi tạo module.

### [Medium] Shutdown không cô lập lỗi giữa các component

`EnorialsHUD.onDisable()` gọi tuần tự hàng loạt `close()/onDisable()` mà không guard riêng (`EnorialsHUD.java:430-492`). Chỉ cần một close ném exception thì các component phía sau, gồm HTTP host, GPS/minimap/party, render task và client profiles, có thể không được đóng.

Khuyến nghị: shutdown coordinator chạy mọi cleanup trong `try/catch`, thu thập suppressed errors, cleanup theo thứ tự ngược startup và luôn clear static/global references trong `finally`.

### [Medium] Startup là chuỗi lớn, thiếu rollback/degraded boundary rõ ràng

`onEnable()` khởi tạo pack hosting, render, party, minimap, GPS và integrations tuần tự (`EnorialsHUD.java:380-426`). Failure trong một module sau khi module trước đã đăng ký listeners/tasks/resources có thể để lại partial state cho tới khi platform hoàn tất disable path.

Khuyến nghị: model lifecycle `STARTING -> READY|DEGRADED -> STOPPING`, đăng ký cleanup ngay sau mỗi acquisition, hoặc dùng component stack để rollback theo thứ tự ngược.

### [High] Hot render path có thể parse YAML từ disk theo từng player mỗi 100 ms

`ListenerManager.getCachedHungerSlots()` chỉ cache 100 ms (`ListenerManager.java:433-447`). Khi cache miss, fallback đầu tiên là `YamlConfiguration.loadConfiguration(data.yml)` của plugin hunger khác (`ListenerManager.java:450-459`, `521-531`). HUD idle render mặc định vẫn chạy định kỳ, nên nếu integration này active, chi phí có thể trở thành disk parse × player × khoảng 100 ms trên main thread.

Khuyến nghị: integration adapter/event hoặc cache snapshot dùng chung theo file timestamp; tuyệt đối không parse file trong per-player render loop.

### [High] Persistence HUD ghi toàn bộ YAML đồng bộ trên mỗi thay đổi

`StorageManager.setTheme/setPreference/setMarkerSize/setQuestLogSize/setPartySize/setLayout/...` gọi `save()` ngay; `save()` serialize toàn bộ `data.yml` (`StorageManager.java:68-173`, `200-206`). Khi player count/data file tăng, click menu có thể gây I/O trên server thread và latency spike.

Khuyến nghị: immutable in-memory state + dirty flag/debounce, snapshot rồi ghi async/atomic temp-file move; flush bounded khi disable.

### [Medium] Netty passenger handler không có lifecycle remove rõ ràng

Legacy minimap thêm handler cố định tên `passenger_rewriter` vào channel khi `PlayerJoinEvent` (`EnorialsMinimap.java:374-381`). Quit chỉ xóa entry khỏi map, không remove handler khỏi pipeline (`EnorialsMinimap.java:384-391`); startup cũng không inject player đã online. Channel đóng khi quit nên restart bình thường ít rủi ro, nhưng hot reload/PlugMan có thể gây handler cũ, duplicate-name exception hoặc missing rewrite.

Khuyến nghị: coi full process restart là contract; đồng thời implement idempotent `install/remove` trên đúng Netty event loop và cleanup tất cả online channel khi disable.

### [High] Race khi shutdown có thể ghi rỗng friends SQLite

`FriendSaveScheduler` có delayed async save nhưng không có API cancel/drain; task tự xóa khỏi `TASKS` trước khi gọi save (`FriendSaveScheduler.java:28-39`). `FriendService.shutdown()` save rồi clear map (`FriendService.java:205-207`). Một async task đã bắt đầu sau final save có thể snapshot map rỗng; `FriendSqlite.saveAll()` xóa toàn bảng trước khi insert snapshot (`FriendSqlite.java:51-74`). Bukkit hủy scheduled task khi disable không dừng được task async đã chạy.

Khuyến nghị: closing flag + cancel-and-join scheduler theo service; chặn queue/save mới; final snapshot/save và clear cùng lifecycle lock, chỉ clear sau khi worker chắc chắn dừng.

### [Medium] GPS nhận NaN/Infinity và tọa độ ngoài world bounds

Chat parser dùng `Double.parseDouble` nhưng không `Double.isFinite`/bounds check (`ChatInputService.java:189-206`); `/gpsapi` có path tương tự. Java chấp nhận `NaN` và `Infinity`, nên chúng có thể đi vào per-tick distance/vector/render math và tạo session lỗi kéo dài.

Khuyến nghị: một shared coordinate parser, bắt buộc finite, X/Z trong world border hard bounds và Y trong min/max của world.

### [Medium] Radar sort toàn bộ entity trước khi áp giới hạn

`EntityRadar` materialize mọi living entity trong bounding box rồi sort toàn list; chỉ sau đó mới dừng ở `maxEntities` (`EntityRadar.java:74-96`). Mob farm đông × nhiều player có thể tạo `O(players × entities log entities)` trên main thread mỗi 1–10 tick.

Khuyến nghị: bounded nearest-N heap/selection, reuse buffers, spatial batching và profiling với mật độ entity xấu nhất.

### [Medium] Secret có thể được đặt trực tiếp trong config

GitHub upload đọc `upload.github.token` từ YAML trước, rồi mới fallback `GITHUB_TOKEN` (`GitHubUploader.java:44-51`). Default tắt, nhưng operator dễ lưu PAT trong thư mục plugin/backup/support bundle.

Khuyến nghị: bỏ token field hoặc chỉ cho phép `env:`/secret provider; redact config diagnostics; scope PAT tối thiểu. Đây là operational security risk, không phải evidence token bị leak trong artifact.

### [Low] Close của pack worker có thể block main server thread tối đa 30 giây

`BoundedWorkQueue.close()` gọi `shutdownNow()` rồi `awaitTermination(30s)` (`BoundedWorkQueue.java:117-134`). Nếu chạy trong Bukkit disable thread, một worker kẹt I/O có thể kéo dài shutdown/reload.

Khuyến nghị: budget shutdown ngắn hơn, cooperative cancellation, I/O timeout và log unfinished job identity.

### [Low] Metadata/module docs dễ gây hiểu sai operational model

JAR giữ `plugin.yml`/README/version của các module cũ dù chúng được nhúng như `HudModule`. Điều này hữu ích cho migration nhưng dễ khiến admin tưởng phải cài thêm plugin con hoặc tin nhầm compatibility range.

Khuyến nghị: đánh dấu rõ `embedded migration metadata`, tạo một compatibility document duy nhất được generate từ release matrix.

## Đánh giá tổng thể

- Ý tưởng/sản phẩm: rất mạnh; breadth gần một client UI framework cho RPG server.
- Kiến trúc domain: tốt, đặc biệt ở pack pipeline, API surface, trade safety và compatibility helpers.
- Maintainability: trung bình-khá; nhiều reflection/NMS, lifecycle owner quá lớn và compatibility logic dày.
- Production readiness: có nhiều defensive pattern, nhưng chỉ nên đánh giá “ready” sau exact-version smoke/load tests. Không có build files/source dependency graph/test suite trong artifact để tái build hoặc xác minh runtime.

## Validation còn thiếu

1. Start/stop/restart trên exact Paper/Purpur target.
2. Matrix client native + ViaVersion/ViaBackwards từ các profile được công bố.
3. Pack accept/decline/fail/download hash và reverse-proxy HTTP behavior.
4. 50–200 players với HUD tick, radar, minimap cache, avatars và pack rebuild.
5. Optional dependency absent/present/version mismatch.
6. Trade disconnect/crash/economy failure/reconciliation.
7. World unload, player quit, entity retirement và shutdown khi worker đang chạy.
8. License/SBOM scan cho SQLite JDBC, ORMLite và bundled assets.

## License note

`META-INF/LICENSE.txt` và `META-INF/EULA.md` của vendor không cấp quyền phát hành lại bản decompile/modified JAR và có điều khoản hạn chế reverse engineering cho mục đích redistribution/repackaging. Thư mục này nên chỉ dùng cho audit nội bộ; không publish/fork/copy implementation sang plugin khác nếu chưa có phép bằng văn bản hoặc legal review. Third-party notices cho ORMLite, SQLite JDBC và VPP đã được bundle, đây là điểm compliance tốt.

## Unresolved questions

- Exact server build/runtime intended for JAR này là 1.21.x, 26.2 hay 26.3?
- Có source/build files và test matrix gốc để đối chiếu các method CFR không dựng hoàn chỉnh không?
