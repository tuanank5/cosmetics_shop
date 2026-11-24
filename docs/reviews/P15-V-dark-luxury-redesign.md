# P15-V · Dark Luxury Storefront + ScrollTrigger

**23/11/2026 · Đã triển khai frontend, dừng để owner nghiệm thu thị giác.** Home/Catalog chuyển sang dark editorial; ScrollTrigger3.15.1 được xác minh và chỉ tải trên Home. GSAP Core và adapter C19 giữ nguyên byte. **Video thương phẩm thật còn thiếu: không chấm PASS cho video production.**

Maven JDK21: **154 test =120 PASS /0 FAIL /0 ERROR /34 SKIPPED**, package JAR thành công. Chrome:60 layout records,20 tổ hợp CTA,13 kiểm media/motion; hồi quy B2:160, B4:320, navigation:100, footer:60 tổ hợp. Không cộng record Chrome vào test Maven.

## 1. Phạm vi và bảo toàn

Đã đối chiếu AGENTS, README/SRS/Business Rules/ERD/DECISIONS, Design System, audit P15-V, B2/B4, reference analysis và GSAP C19. Owner phê duyệt đổi storefront beige sang đen/gold muted và mở rộng motion Home. Admin giữ U1 sáng. [Design System cập nhật](../ui/design-system.md) · [Quyết định](../DECISIONS.md)

Working tree có nhiều thay đổi chưa commit. Đầu kỳ lưu Git inventory,190 SHA-256 source và snapshot các file chính tại target/dark-luxury. Cuối kỳ198 file: **9 file cũ đổi,8 mới,181 file cũ giữ hash**. Java production, repository/entity/DTO, Flyway, Security, checkout/cart template, checkout.js, Core và home-motion.js không đổi. [Preservation](../ui/screenshots/dark-luxury/preservation.json)

Không reset/clean/stash/commit/push; không seed/migration/gateway/IPN hoặc ghi beautyshop. P01_MARIADB_TEST=false. Không thêm framework, bundler, package.json, CDN hoặc webfont. Không đổi coverage component để chấm thẩm mỹ.

## 2. Thiết kế đã triển khai

**Palette:** nền #0A0A0B, surface #151518, surface-alt #1D1D22, chữ #F5F2ED, chữ phụ #B9B6B0, separator rgba(245,242,237,.12), gold #A88A5A. Burgundy #4B1F2D chỉ rất ít trong visual. CTA sáng/badge giỏ sáng có chữ tối. Bề mặt UI storefront không còn beige; màu tự nhiên của ảnh dữ liệu giữ nguyên.

Tái dùng custom properties U1 trong storefront.css, dưới root có header data-storefront. Marker render theo path không bắt đầu /admin, không đổi Security hoặc suy quyền từ JS. Public/Customer chrome, kể cả palette checkout/cart, nhận dark theme; Admin giữ U1. Các form/view-state/nghiệp vụ không đổi.

**Typography:** editorial dùng Palatino Linotype/Palatino/Georgia/serif; card, giá và UI dùng Segoe UI/Arial/sans-serif. Đây là system-font stack với fallback; không khẳng định đã tải Cormorant/Playfair/Inter/Manrope. Không thêm font request/license phân phối/font swap. Render đa OS chưa nghiệm thu. Serif được dành cho headline, giảm ở card/footer; tên đầy đủ, không line-clamp.

Contrast sRGB: chữ chính/nền17,72:1, muted/surface9,01:1, muted/surface-alt8,30:1. Control border mạnh hơn separator; focus3px gold. Separator12% chỉ trang trí. [Các cặp màu](../ui/screenshots/dark-luxury/theme-contrast.json). Không coi phép tính này là chứng nhận WCAG toàn site.

**Home:**3 section lớn min-height100svh, section đầu trừ chiều cao header; một heading ngắn/CTA nổi bật mỗi section. Cuộn native, không hijack/pin/snap. Poster là3 SVG trừu tượng do BeautyShop tự tạo (nocturne/form/light), không phải ảnh mỹ phẩm/chiến dịch hoặc video thật. Bỏ hình chai/hũ CSS; ba hook C19 còn dưới dạng vệt sáng trang trí để giữ nguyên adapter.

ScrollTrigger chỉ animate .story-poster: scale1.04→1, opacity.88→1, scrub.4, từ top-bottom đến bottom-top. H1/CTA không bị ẩn hoặc đưa vào timeline. gsap.matchMedia revert khi reduce; pagehide revert/pageshow persisted tạo lại context. Plugin thiếu/lỗi vẫn có poster và nội dung. Pilot C19 cũ380ms vẫn hoạt động trên ba hook.

**Catalog:**4 cột từ1100 CSS px, tablet/mobile2 cột, dưới340 CSS px về1 cột để đọc ở zoom200%. Media4:5/contain, brand nhỏ, tên/giá/tồn rõ, card phẳng không viền hộp nặng. Placeholder trung thực “BeautyShop / Chưa có ảnh sản phẩm”. Filter là một disclosure/form GET phía trên grid, mặc định đóng; hash mở đúng categories/brands/skin-type. No-JS mở native details được. Giữ q/brand/category/skinType/sort/page/size, count/chips/Page; không AJAX hoặc tự submit select.

**Wishlist:** heart là link mở đúng PDP#pdp-actions, accessible label “Xem [tên] để lưu yêu thích”. Không one-click save vì projection card không có productId; không tạo field/POST giả hoặc báo đã lưu. PDP dùng POST wishlist/CSRF hiện có khi Customer, Guest có đăng nhập; Admin ẩn icon.

Header giảm độ nặng search/viền, giữ native menu/role/current. Footer tối, nhóm UI sans, link/keyboard đúng role. PDP chỉ nhận palette, một ít typography/radius và anchor wishlist; không viết lại SKU/form mua.

## 3. Media: cấu trúc frontend, hành vi và mức nghiệm thu

Không sửa backend/projection/DTO/schema để thêm video. Template nhận **optional view map từ nguồn đáng tin**, hiện chỉ được cấp ở test/fixture:

```text
catalogPreviewMedia[productSlug] = { thumbnailUrl, previewVideoUrl, videoPosterUrl }
homeEditorialMedia[opening|ritual|discovery] = { previewVideoUrl }
```

Card ưu tiên thumbnailUrl, rồi videoPosterUrl, rồi ảnh sản phẩm hiện có. Không có metadata thì ảnh/fallback như cũ, không render/request video. Đây là view hook, không phải field HTTP/backend mới. Production hiện chưa cấp map/video URL; nối dữ liệu media thật cần nguồn có quyền và phạm vi đọc dữ liệu được duyệt riêng.

Video muted/loop/playsinline/preload=none; HTML chưa có src. JS chỉ chấp nhận URL cùng origin dưới /img/ hoặc /uploads/sku/, không tải CDN.

- Catalog: hover mouse/fine-pointer mới load/play và fade180ms. Unhover pause/unload, lần sau bắt đầu lại; mỗi trang chỉ một video hoạt động. Out-of-view/tab hidden/pagehide sẽ dừng. Desktop có nút play/pause bằng keyboard; focus đơn thuần không tự phát. Mobile, reduce và save-data giữ poster. Lỗi404/decode/play rejection giữ poster/link, không ném exception không xử lý.
- Home: IntersectionObserver chọn section có ít nhất55% diện tích trong viewport; một video hoạt động. Có pause/play, người dùng pause thì không tự bật lại theo cuộn. Ra khỏi viewport/đổi tab/reduce/save-data/pagehide sẽ dừng. Khi nút bị ẩn bởi reduce, focus trả về CTA/link, không để focus trong hidden control.
- No-JS/reduced-motion: poster luôn có trong HTML; video không src, không tự phát. Nếu video thật mang thông tin ngoài hình trang trí, cần caption/transcript và rà quyền riêng.

**PASS UI/fixture:** layout/palette, poster, runtime ScrollTrigger/cleanup, keyboard/no-JS/responsive và các regression. **PARTIAL:** hover video/Home video vì chỉ có clip QA, chưa có asset thương phẩm/chiến dịch và production feed. Wishlist card là link PDP, chưa one-click save. **BLOCKED/NOT VERIFIED:** F01/MariaDB thật, media thật, visual comparison trực tiếp Byredo, CSP/performance production, MoMo merchant/IPN. Không nâng C21 hoặc dùng24/30 để chấm độ giống.

## 4. Artifact, license và dung lượng

Đã băm **D:\Downloads\ScrollTrigger.min.js trước tích hợp**, khớp SHA-256:

```text
B5BB360661F478F0FF6FFA11500D8CE5E2B6AAF6F7288237CDCA3AAC9548035A
```

Header xác nhận3.15.1/copyright2026 GreenSock/license URL. Sao nguyên44.683 byte vào vendor, không sửa/minify. [Provenance](../../src/main/resources/static/js/vendor/ScrollTrigger-3.15.1.provenance.md) · [License notice](../../src/main/resources/static/js/vendor/ScrollTrigger-3.15.1.LICENSE-NOTICE.txt) · [Nguồn tagged owner xác nhận](https://github.com/greensock/GSAP/blob/3.15.1/dist/ScrollTrigger.min.js).

Áp dụng GSAP Standard “No Charge” License, không phải MIT; giữ notices và điều kiện về visual animation builder cạnh tranh. File notice giữ thông báo và link điều khoản, không phải toàn bộ license offline. [License chính thức](https://gsap.com/community/standard-license/). Curl trực tiếp ban đầu lỗi7; không khẳng định đã tự tải lại upstream/npm tarball. Dùng đúng artifact/hash owner cung cấp.

Core vẫn SHA256 **3D1BEA374AB1B703CC5C4105358EEC63C4465EFDC573931DC116E34A0A190C1B**; home-motion.js vẫn **E804B020D6FCDA54A2617D7552A68A7FBEFA859EFFDD7A214B2E377D34FFE88C**. JAR khớp source.

Node26.13.0, raw/gzip9/Brotli11: plugin44.683/18.028/16.248 byte; home-story4.484/1.548/1.340; catalog-preview3.492/1.209/1.021. Tổng4 script Home **48.765 byte gzip**. Pilot Core+adapter cũ vẫn29.189; phạm vi mới tăng19.576 byte gzip. **Tổng Home vượt35KiB của pilot cũ**; không báo vẫn trong budget cũ, owner cần nghiệm thu chi phí mở rộng. Catalog chỉ thêm1.209 byte gzip, không GSAP. [Phép đo/hash](../ui/screenshots/dark-luxury/bundle-sizes.json)

Không nới CSP/Security. Đã kiểm version3.15.1/3 trigger và các trang ngoài Home không tải vendor. CSP production chưa xác minh mới. Không công bố FPS/LCP/INP/CLS/Lighthouse chưa đo; gzip offline không phải transfer thực tế.

## 5. File đã sửa

7 production file hiện hữu:

- [home/index.html](../../src/main/resources/templates/home/index.html):3 chapter/poster/media slot, CTA/discovery thật, include riêng Home.
- [product/list.html](../../src/main/resources/templates/product/list.html):eyebrow/filter disclosure/script preview; GET/chips/Page giữ nguyên.
- [product-card.html](../../src/main/resources/templates/fragments/product-card.html):optional media, fallback, heart/link và nút preview không lồng trong link card.
- [header.html](../../src/main/resources/templates/fragments/header.html):marker theme theo path, không đổi role/route/form.
- [product/detail.html](../../src/main/resources/templates/product/detail.html):chỉ thêm id=pdp-actions.
- [storefront.css](../../src/main/resources/static/css/storefront.css#L521):token override/theme, story/grid/card/header/footer/reflow. Không sửa tokens.css.
- [app.js](../../src/main/resources/static/js/app.js):initial filter open theo hash; submit guard, confirm, SKU/cart fallback giữ nguyên.

8 file mới: [home-story.js](../../src/main/resources/static/js/home-story.js), [catalog-preview.js](../../src/main/resources/static/js/catalog-preview.js), [3 SVG](../../src/main/resources/static/img/editorial/), ScrollTrigger minified/provenance/license-notice.

2 test hiện hữu: [BootstrapPagesTest](../../src/test/java/com/beautyshop/controller/BootstrapPagesTest.java) cập nhật heading và optional/deferred Home media; [GiaoDienPagesTest](../../src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java) kiểm optional preview/thumbnail, link PDP và không POST wishlist giả. Checkout/integrity/B2 tests cũ tiếp tục chạy.

Docs: báo cáo này, Design System, DECISIONS, roadmap và evidence. Footer template/head/Java production/checkout/cart/security/DB không thuộc diff; chỉ palette chung ảnh hưởng storefront chrome.

## 6. Maven và JAR

Temurin21.0.13/Maven3.9.11, P01_MARIADB_TEST=false. Lượt cuối **154=120 PASS+0 FAIL+0 ERROR+34 SKIPPED**,22 lớp, có unit/MVC/service doubles/H2 SELECT.34 skipped là MariaDB integration, chưa có test DB độc lập được duyệt. B4 trước redesign118 PASS/34 SKIPPED là lịch sử; hai test mới đưa PASS lên120.

```powershell
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$env:Path="$env:JAVA_HOME/bin;$env:Path"
$env:P01_MARIADB_TEST='false'
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' test
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' -DskipTests package
node --check src/main/resources/static/js/home-story.js
node --check src/main/resources/static/js/catalog-preview.js
node --check src/main/resources/static/js/app.js
git diff --check
```

Tất cả đạt. JAR target/beautyshop-0.0.1-SNAPSHOT.jar; đối chiếu entry template/CSS/JS/vendor/license khớp source. [Maven summary](../ui/screenshots/dark-luxury/maven-summary.json) · [Tests](../ui/screenshots/dark-luxury/tests-final.log) · [Package](../ui/screenshots/dark-luxury/package-final.log) · [JAR/hash](../ui/screenshots/dark-luxury/jar-assets.json). Log Maven xuất vào docs đã ẩn mật khẩu test tự sinh.

## 7. Chrome và hồi quy

Chrome158/Windows, profile/CDP cô lập theo chấp thuận owner.5 viewport **360×800,390×844,768×1024,1366×768,1920×1080**, zoom browser thật100%/200%, không CSS zoom. Fixture dùng MVC/Thymeleaf/Security thật, repository bộ nhớ, loại DataSource/JPA/Flyway. POST/login chỉ tới fixture, không beautyshop.

- **60 layout records:**Home/Catalog/PDP×5 viewport×2 zoom×JS on/off,0 overflow/JS exception. Home3 poster, plugin3.15.1/3 trigger; reduce→0/revert, bật lại→3. Catalog/PDP không GSAP. [Measurements](../ui/screenshots/dark-luxury/measurements.json)
- **20 CTA combinations/60 CTA hit-tests:**JS on/off,5 viewport,2 zoom; từng CTA3 chapter không bị che, cao≥56px, không overflow. [CTA](../ui/screenshots/dark-luxury/cta-checks.json)
- **13 media checks:**hover/unhover/unload, đúng tile video không đổi chiều cao; keyboard play/pause;404 fallback; mobile/no-JS poster; reduce runtime/focus recovery; offscreen pause; đổi tab thật xác nhận document.hidden/paused; một Home video; tiến/lùi và refresh giữa trang khôi phục767px/3 trigger; plugin bị chặn vẫn poster. [Media](../ui/screenshots/dark-luxury/media-checks.json)
- **B2:160/160 records+27 checks:**NULL/rỗng/trắng/PNG/404/decode/cached failure, count/chips/clear-all/query/size/page/history/no-JS. Assertion display fallback đổi từ grid sang khác none vì layout mới dùng flex; giữ ratio/visibility/error checks. [B2](../ui/screenshots/dark-luxury/commerce/catalog-regression/measurements.json)
- **B4:320/320 records+24 checks:**quote/no-voucher/invalid/F07/requote/address/empty, COD/MoMo OFF/ON/mock/pending, errors/no-JS/role. Hai Enter nhanh vẫn1 POST COD và1 MOMO. [B4](../ui/screenshots/dark-luxury/commerce/measurements.json) · [Rapid submit](../ui/screenshots/dark-luxury/commerce/rapid-checks.json)
- **Navigation100/100+30 checks:**Guest/Customer/Admin, current/badge/menu/resize/keyboard/touch/no-JS. Guest0→1→5→12; login giữ2 SKU/12 đơn vị/4.425.000₫ trên fixture. [Navigation](../ui/screenshots/dark-luxury/commerce/regression/measurements.json)
- **Footer60/60:**6 trang theo role×5 viewport×2 zoom, màu/focus keyboard/reflow. Admin vẫn nền rgb(250,247,243), public/Customer rgb(10,10,11).6 GET theo role xác nhận Cart/Checkout/Account/Admin không tải GSAP/ScrollTrigger. [Role/assets/footer](../ui/screenshots/dark-luxury/assets-role.json)

Không cộng các records thành test Maven. Các lượt browser cuối đều0 Runtime.exceptionThrown;404 cố ý là lỗi tài nguyên fixture. Không suy fixture thành transaction/locking/idempotency MariaDB.

### Lỗi đã xử lý trong QA

Harness ghi video đầu lỗi encode RangeError, đã chuyển FileReader. Keyboard probe đầu trỏ nút hidden ở card không có video, đã sửa đúng selector. Lượt navigation đầu Customer sẵn2 đơn vị nên merge14 thay vì expected12; đã restart fixture với regression-mode trước khi bean giỏ tạo, chạy lại100/100, không sửa service để khớp test.

Kiểm ảnh cuối phát hiện footer thừa kế bg tối vào chữ/focus. Đã sửa foreground/focus scoped, chụp lại gallery và kiểm60 tổ hợp. Probe focus đầu chỉ gọi focus() nên :focus-visible chưa kích hoạt; lượt cuối dùng Tab/Shift+Tab CDP, kiểm outline solid3px. Kết quả công bố là lượt hoàn tất, không lấy lượt dở dang làm PASS.

## 8. Ảnh và video

298PNG gồm các bộ regression;2WebM từ screencast Chrome thật, re-encode10fps bằng MediaRecorder để review khung hình, không dùng đo FPS/thời gian thực. Before sao nguyên từ audit P15-V tương ứng baseline Home/Catalog, không giả render bản cũ. Ảnh sản phẩm fixture là screenshot FAQ/placeholder kỹ thuật, không dùng nghiệm thu ảnh thương phẩm.

- Home desktop: [Trước](../ui/screenshots/dark-luxury/before-home-1366.png) · [Sau](../ui/screenshots/dark-luxury/after-home-1366-100-js.png) · [Toàn trang](../ui/screenshots/dark-luxury/after-home-1366-full.png).
- Home mobile: [Trước](../ui/screenshots/dark-luxury/before-home-390.png) · [Sau](../ui/screenshots/dark-luxury/after-home-390-100-js.png) · [Chapter2](../ui/screenshots/dark-luxury/after-home-390-chapter2.png) · [Chapter3](../ui/screenshots/dark-luxury/after-home-390-chapter3.png).
- Catalog desktop: [Trước](../ui/screenshots/dark-luxury/before-catalog-1366.png) · [Sau](../ui/screenshots/dark-luxury/after-catalog-1366-100-js.png) · [Card4 cột](../ui/screenshots/dark-luxury/after-card-1366.png).
- Catalog mobile: [Trước](../ui/screenshots/dark-luxury/before-catalog-390.png) · [Sau](../ui/screenshots/dark-luxury/after-catalog-390-100-js.png) · [Card2 cột](../ui/screenshots/dark-luxury/after-card-390.png).
- Video/card: [Trước](../ui/screenshots/dark-luxury/before-card-1366.png) · [Playing](../ui/screenshots/dark-luxury/hover-fixture-playing-1366.png) · [Unhover](../ui/screenshots/dark-luxury/hover-fixture-stopped-1366.png) · [Mobile poster](../ui/screenshots/dark-luxury/hover-fixture-mobile-poster.png) · [No-JS](../ui/screenshots/dark-luxury/hover-fixture-nojs-1366.png).
- Accessibility: [Home390/200%](../ui/screenshots/dark-luxury/after-home-390-200-js.png) · [Catalog390/200% no-JS](../ui/screenshots/dark-luxury/after-catalog-390-200-nojs.png) · [Reduced](../ui/screenshots/dark-luxury/hover-fixture-reduced-1366.png) · [Plugin blocked](../ui/screenshots/dark-luxury/scrolltrigger-blocked-static.png) · [Footer](../ui/screenshots/dark-luxury/after-footer-390.png).
- Video evidence: [Home cuộn tiến/lùi](../ui/screenshots/dark-luxury/home-scroll-motion.webm) · [Card hover/unhover — QA fixture](../ui/screenshots/dark-luxury/card-hover-QA-fixture.webm).

Clip QA tự tạo trong target, ghi **QA VIDEO FIXTURE**, không đóng gói làm media sản phẩm. Screencast card chứng minh engine, không chứng minh video mỹ phẩm thật. Home screencast quay poster/ScrollTrigger thực tế, không gọi là campaign video. Không dùng asset/logo/text/font Byredo.

## 9. Giới hạn, rollback và dừng

**F01/MariaDB vẫn BLOCKED**,34 integration tests skipped. Chưa xác minh gộp giỏ/lưu đơn/voucher nguyên tử DB thật, merchant/IPN/Sandbox, CSP production, mobile Safari/thiết bị vật lý, screen reader hoặc field performance. Không tuyên bố production-ready/WCAG toàn site.

Ảnh render Byredo chưa có từ audit P15-V; thiết kế dựa brief owner, không tuyên bố pixel comparison hoặc phần trăm giống Byredo. Cần owner duyệt6 màn hình cùng viewport và media có quyền trước khi nối nội dung thật. Video production còn PARTIAL dù engine fixture đạt.

Rollback đúng hunk dark/story/card/marker và8 file mới theo snapshot/hash, giữ U1–U2/B1/B2/B4 đã có; không checkout nguyên file về HEAD. Fixture QA đã đóng, credential Admin tạm xóa sau kiểm. [Tất cả bằng chứng](../ui/screenshots/dark-luxury/)

**DỪNG sau redesign phase để nghiệm thu trực quan. Không commit/push hoặc thêm chức năng ngoài phạm vi.**
