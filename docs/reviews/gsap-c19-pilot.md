# BeautyShop · GSAP Core pilot C19

Ngày 22–23/11/2026. **Hiện tại: gate dependency đạt, đã tích hợp pilot C19 và kiểm thử trên Chrome Headless/fixture; chờ owner nghiệm thu.** Tổng JS bổ sung **29.189 byte gzip**, Maven **61 PASS / 0 FAIL / 0 ERROR / 34 SKIPPED**, JAR đóng gói thành công. [Kết quả tiếp tục và bằng chứng](#ket-qua-tiep-tuc). Chưa tuyên bố production-ready.

**Các mục 1–6 dưới đây lưu lịch sử lần chạy bị chặn ngày 22/11**, trước khi owner cung cấp file GSAP chính thức. Trạng thái hiện hành nằm ở mục 7.

## 1. Phạm vi và working tree

Đã đọc AGENTS.md, README, SRS, Business Rules, ERD, DECISIONS, báo cáo công nghệ frontend, Design System U1 và đầy đủ hai báo cáo P15 hiện hành. Kiểm tra template Home, head chung và Security_Config để xác định điểm tích hợp và CSP. Working tree có nhiều thay đổi U1–U2/P15 chưa commit; không reset/clean, không commit/push.

Home hiện có đúng ba span trang trí trong `.shop-hero-art[aria-hidden=true]`: `.shop-hero-arch`, `.shop-hero-vessel`, `.shop-hero-jar`. H1 và CTA nằm ở vùng nội dung khác. Đây là mục tiêu pilot được duyệt, chưa có animation mới. [Home](../../src/main/resources/templates/home/index.html) · [Design System](../ui/design-system.md)

## 2. Giai đoạn 1: nguồn, phiên bản và license

Phiên bản ứng viên cố định là **GSAP 3.15.1**, chưa được đưa vào runtime. Metadata tại tag `3.15.1` khai báo cùng version, entry CommonJS `dist/gsap.js`, repository GreenSock/GSAP và Standard “no charge” license. Chưa xác minh được dist-tag stable/latest hoặc integrity của tarball từ registry; không coi metadata source là chứng nhận artifact đã tải. [Metadata tagged](https://raw.githubusercontent.com/greensock/GSAP/3.15.1/package.json)

Đã đọc license hiện hành trên website GSAP: cho phép sử dụng thương mại trên website/giao diện, có hạn chế đối với công cụ tạo animation trực quan cạnh tranh với Webflow và yêu cầu giữ thông báo sở hữu. Đây là nhận định phạm vi sử dụng website của pilot; chưa đối chiếu license/header nằm trong artifact vì không tải được file. Không gán nhãn MIT cho GSAP. Khi có artifact, phải giữ nguyên notice và đối chiếu license đi kèm trước khi phân phối. [Standard GSAP License](https://gsap.com/community/standard-license/)

Hai lần tải trực tiếp trong lượt pilot đều thất bại trước khi nhận artifact:

| Nguồn thử | URL | Kết quả thực tế |
|---|---|---|
| npm registry, tarball version cố định | `https://registry.npmjs.org/gsap/-/gsap-3.15.1.tgz` | curl exit **7**, không kết nối được cổng 443; không có tarball local. |
| Repository upstream, dist tại tag cố định | `https://raw.githubusercontent.com/greensock/GSAP/3.15.1/dist/gsap.min.js` | curl exit **7**, không kết nối được cổng 443; không có core local. |

Web reader đọc được metadata tagged và license, nhưng endpoint metadata npm `https://registry.npmjs.org/gsap/3.15.1` không truy cập được qua công cụ này. Nội dung web reader không được dùng để dựng lại một file minified hoặc giả lập checksum của artifact. Không chuyển sang CDN runtime hoặc một nguồn không xác minh để vượt điều kiện dừng.

Lệnh tải đã chạy, timeout giới hạn 15 giây mỗi nguồn:

```powershell
curl.exe --fail --location --connect-timeout 5 --max-time 15 --output target/gsap-c19/gsap-3.15.1.tgz https://registry.npmjs.org/gsap/-/gsap-3.15.1.tgz
curl.exe --fail --location --connect-timeout 5 --max-time 15 --output target/gsap-c19/gsap-3.15.1.min.js https://raw.githubusercontent.com/greensock/GSAP/3.15.1/dist/gsap.min.js
```

Log gốc lưu cục bộ tại `target/gsap-c19/registry-download.log` và `target/gsap-c19/github-download.log`. Đây là output kiểm tra trong thư mục build, không phải dependency được phân phối.

### Checksum và ngân sách

| Thành phần | SHA-256 | Raw | gzip | Brotli |
|---|---|---|---|---|
| GSAP Core 3.15.1, artifact chưa tải được | NOT VERIFIED | Chưa đo | Chưa đo | Chưa đo |
| Adapter `home-motion.js`, chưa tạo | N/A | Chưa đo | Chưa đo | Chưa đo |
| Tổng JavaScript bổ sung dự kiến | N/A | Chưa đo | **Chưa chứng minh ≤35 KiB** | Chưa đo |

Ngưỡng nghiệm thu là **35 KiB = 35.840 byte gzip**, tính tổng kích thước nén riêng của core và adapter sẽ phục vụ. Dự kiến đo từ byte thực bằng gzip level 9 và Brotli quality 11, ghi cả phiên bản công cụ và SHA-256. Chưa chạy phép đo này. Không dùng kích thước GitHub hiển thị, package unpackedSize hoặc mức gzip của một phiên bản khác thay cho kết quả đo. JavaScript thêm vào source thực tế trong lượt này là 0 byte vì đã dừng trước tích hợp; con số đó không có nghĩa pilot đạt budget.

**Kết luận gate: BLOCKED / chưa đạt.** Có nguồn metadata và nội dung license công khai, thiếu artifact local, kiểm tra integrity/hash, đối chiếu notice/license trong artifact và phép đo core + adapter. Không cài plugin, package.json, bundler hoặc dependency khác.

## 3. CSP

Kiểm tra source không thấy CSP được khai báo trong Security_Config hoặc meta ở head chung. `/js/**` hiện được phép truy cập công khai. Những quan sát source này không chứng minh policy tại reverse proxy/production. [Security_Config](../../src/main/java/com/beautyshop/security/Security_Config.java) · [Head](../../src/main/resources/templates/fragments/head.html)

Đã thử GET `http://localhost:8080/` bằng curl với connect timeout 2 giây và tổng timeout 5 giây: **exit 7, không kết nối được**, không có response header để đánh giá. Log tại `target/gsap-c19/csp-http.log`. CSP thực tế và tương thích CSSOM/cleanup của artifact là **NOT VERIFIED**. Không khởi động ứng dụng với database thật và không nới CSP production.

## 4. Kiểm thử, ảnh và giới hạn

| Lớp kiểm chứng | Kết quả lượt pilot này |
|---|---|
| Source/inventory | Đã đọc cấu trúc hero, nơi tải script, Security_Config và trạng thái Git; lưu hash để đối chiếu source/config trước/sau. |
| Tải artifact | **2 lần lỗi kết nối**, curl exit 7; đây là lỗi công cụ tải, không phải ERROR của Maven test. |
| HTTP CSP | **1 lần lỗi kết nối**, curl exit 7; chưa nhận response header. |
| Maven JDK 21 `test` | **NOT RUN** do dừng tại gate trước tích hợp. Không có số PASS/FAIL/ERROR/SKIPPED mới. |
| Maven package JAR | **NOT RUN**; JAR có sẵn từ B1.4 không phải build của pilot. |
| Chrome fixture / E2E | **NOT RUN** cho pilot; chưa có animation để so sánh. |
| MariaDB integration | **NOT RUN**; không bật `P01_MARIADB_TEST`, không truy cập/ghi database. F01 giữ BLOCKED. |

Baseline lịch sử B1.4 là **58 PASS / 0 FAIL / 0 ERROR / 34 SKIPPED**, không phải kết quả chạy lại trong lượt này. Ma trận giữ **20/30 PASS (66,67%), core 10/12**. C19 PASS trước đó xác nhận hero tĩnh; không đồng nghĩa pilot GSAP PASS. Hai báo cáo P15 không thay đổi. [B1.4](byredo-inspired-ui-implementation.md#b14) · [Ma trận](byredo-component-gap-analysis.md)

Toàn bộ ca Chrome yêu cầu sau đây còn **NOT RUN**: năm viewport 360×800, 390×844, 768×1024, 1366×768, 1920×1080 × zoom 100%/200%; keyboard; JS disabled; chặn vendor request; reduced motion từ lúc tải và khi đổi preference; cleanup/revert; overflow/layout; chỉ Home tải script; Guest/Customer/Admin regression. Chưa có kết luận mới về CSP, CLS, lỗi JavaScript, accessibility hoặc performance.

Không tạo ảnh so sánh hoặc sao ảnh B1.4 thành bằng chứng pilot. `docs/ui/screenshots/byredo-inspired/GSAP-C19/` chưa có bộ ảnh mới vì chưa triển khai/chạy Chrome. Sau khi gate đạt mới chụp hero tĩnh và hero trong chuyển động trên cùng fixture, role, viewport và zoom; ảnh cần kèm log kiểm tra vì screenshot riêng không chứng minh reduced-motion/cleanup.

## 5. File thay đổi và bảo toàn kết quả trước

- Tạo `docs/reviews/gsap-c19-pilot.md`: ghi kết quả gate, nguồn đã thử, giới hạn và điều kiện tiếp tục.
- Cập nhật `docs/reviews/frontend-technology-enhancement.md`: ghi owner đã duyệt C19 và kết quả dừng tại gate, giữ lịch sử audit.
- Chỉ lưu inventory/hash/log kiểm tra trong `target/gsap-c19/`. Không thêm vendor hoặc adapter vào source; không sửa template, CSS, JavaScript hiện có, pom.xml, backend, Security, Flyway, database hoặc ma trận component.

Đối chiếu cuối lượt: **184/184 file trong src khớp hash**, không thêm/xóa file source; bốn file pom.xml, Design System và hai báo cáo P15 đều khớp hash trước/sau. `git diff --check` exit **0**; Git có cảnh báo chuyển LF sang CRLF cho vài file đã sửa sẵn, không có lỗi whitespace. Kết quả đối chiếu lưu tại `target/gsap-c19/preservation-check.json`.

## 6. Điều kiện tiếp tục và rollback

Cần tải được artifact version cố định từ nguồn chính thức hoặc có bản artifact chính thức được cung cấp cùng provenance đủ để đối chiếu integrity. Sự phê duyệt C19 đã có; không cần duyệt lại cùng phạm vi. Tuy nhiên gate vẫn phải đạt trước khi tải script vào Home.

Thứ tự tiếp tục trong phạm vi đã duyệt:

1. Xác nhận bản stable, pin version và nguồn phát hành; kiểm integrity tarball khi có metadata registry, ghi SHA-256 của core thực tế, đối chiếu license/notice.
2. Chuẩn bị adapter tối thiểu trong vùng kiểm tra, đo raw/gzip/Brotli riêng của core và adapter; chỉ chuyển sang Giai đoạn 2 khi tổng gzip ≤35.840 byte. Nếu vượt budget hoặc không xác minh được nguồn/license, dừng.
3. Chỉ sau gate: vendor local phiên bản cố định và `home-motion.js`, hai include defer riêng Home; ba phần trang trí dùng transform/opacity, reduced-motion và cleanup/revert, fallback tĩnh. Không animate H1/CTA/phần tử focus.
4. Chạy đủ Chrome/JDK 21 test/package, kiểm CSP nếu có endpoint, lưu ảnh/log đúng thư mục và báo số thực tế; dừng để nghiệm thu C19.

Hiện chưa cần rollback runtime vì chưa tích hợp. Nếu pilot tiếp tục rồi cần rollback, bỏ đúng hai include Home, adapter và file vendor/license/provenance mới theo diff pilot, giữ nguyên hero tĩnh và thay đổi U1–U2/P15 sẵn có; không dùng reset/clean. Sau rollback kiểm lại Home và đóng gói JAR. Không mở rộng sang component khác khi chưa có yêu cầu.

<a id="ket-qua-tiep-tuc"></a>

## 7. Tiếp tục với artifact owner cung cấp · 22–23/11/2026

### 7.1 Dependency gate: PASS trong phạm vi nguồn đã duyệt

Owner cung cấp `D:\Downloads\gsap.min.js`, xác nhận bản chính thức **GSAP Core 3.15.1** và SHA-256 kỳ vọng. `Get-FileHash -Algorithm SHA256` khớp chính xác:

```text
3D1BEA374AB1B703CC5C4105358EEC63C4465EFDC573931DC116E34A0A190C1B
```

Header thực tế khai báo GSAP 3.15.1, copyright 2026 GreenSock, đường dẫn license và tác giả. Đã đọc lại license chính thức; phạm vi website thương mại được phép và phải giữ notice. File vendor được sao nguyên byte, không minify lại hoặc bỏ header. Provenance ghi rõ nguồn nhận từ owner, đường dẫn upstream tagged và hash. Không khẳng định đã tải lại upstream/đối chiếu npm tarball integrity trong môi trường mạng hạn chế này. [Provenance](../../src/main/resources/static/js/vendor/gsap-3.15.1.provenance.md) · [License chính thức](https://gsap.com/community/standard-license/)

Chỉ dùng bản core browser chính thức, gồm CSSPlugin được upstream đóng gói sẵn trong core; BeautyShop không thêm file plugin hoặc gọi registerPlugin. Không CDN runtime, package.json, bundler hoặc dependency build mới.

### 7.2 Dung lượng cuối cùng đo từ byte thực

Node **v26.13.0**, zlib **1.3.2.1-motley-456ae73**, Brotli **1.2.0**. gzip level 9, Brotli quality 11; tổng là cộng kích thước nén riêng của hai file. [Số đo JSON](../ui/screenshots/byredo-inspired/GSAP-C19/sizes.json)

| File | Raw bytes | gzip-9 bytes | Brotli-11 bytes |
|---|---:|---:|---:|
| gsap-3.15.1.min.js | 73.357 | 28.511 | 25.842 |
| home-motion.js | 1.483 | 678 | 538 |
| **Tổng** | **74.840** | **29.189** | **26.380** |
| Ngưỡng gzip | — | **35.840** | — |
| Còn lại | — | **6.651** | — |

SHA-256 adapter cuối: `E804B020D6FCDA54A2617D7552A68A7FBEFA859EFFDD7A214B2E377D34FFE88C`. Bản adapter đầu đo 569 byte gzip, sau điều chỉnh lịch bắt đầu/cleanup là 678 byte; cả hai lần đều đo trước khi đưa bản tương ứng vào source. Các số cuối ở bảng là artifact đã đóng gói, không phải dự toán hoặc kích thước transfer HTTP đã thương lượng compression.

### 7.3 Triển khai và file thay đổi

| File trong lát cắt pilot | Nội dung |
|---|---|
| [home/index.html](../../src/main/resources/templates/home/index.html#L57) | Thêm đúng hai script local defer cuối body, vendor trước adapter; không sửa head/fragment chung. |
| [home-motion.js](../../src/main/resources/static/js/home-motion.js) | Guard khi thiếu vendor/sai version/thiếu vùng trang trí. Chỉ chọn ba hình trong vùng aria-hidden. |
| [gsap-3.15.1.min.js](../../src/main/resources/static/js/vendor/gsap-3.15.1.min.js) | Vendor pin, nguyên byte theo hash được duyệt. |
| [gsap-3.15.1.provenance.md](../../src/main/resources/static/js/vendor/gsap-3.15.1.provenance.md) | Nguồn, checksum, license và phép đo core. |
| [BootstrapPagesTest.java](../../src/test/java/com/beautyshop/controller/BootstrapPagesTest.java) | Ba ca MVC Guest/Customer/Admin kiểm Home có hai script local đúng thứ tự và đúng một lần; Login không có chúng. |
| Báo cáo này và [audit công nghệ](frontend-technology-enhancement.md) | Kết quả hiện tại, giới hạn và rollback, giữ lịch sử lần bị chặn. |
| [Bằng chứng GSAP-C19](../ui/screenshots/byredo-inspired/GSAP-C19/) | 51 ảnh pilot, 72 ảnh regression, JSON kích thước/layout/request/CSP/CLS và đối chiếu ảnh. |

Tween dùng y=6 px và opacity từ 0,86 về trạng thái CSS gốc, duration 260 ms, stagger 60 ms × hai khoảng: **toàn chuỗi 380 ms**. Chỉ transform/opacity thay đổi, không loop hoặc scroll animation. H1, CTA, tổ tiên chứa phần tử focus và nội dung mua sắm không được animate. Dùng hai requestAnimationFrame để bố cục header hiện có ổn định trước khi bắt đầu.

`gsap.matchMedia('(prefers-reduced-motion: no-preference)')` giữ bản tĩnh khi reduce từ đầu, tự revert khi preference đổi. Tween revert sau khi hoàn tất để không giữ inline transform/opacity. pagehide hủy frame đang chờ và revert media context; pageshow persisted lên lịch lại và không tạo context trùng. Trường hợp lifecycle persisted đã thử bằng event mô phỏng, chưa chứng minh mọi tình huống BFCache thực tế.

### 7.4 Maven JDK 21 và JAR

Maven 3.9.11, Eclipse Temurin **21.0.13**. `P01_MARIADB_TEST=false` trong toàn bộ lệnh Maven.

| Suite | Tổng | PASS | FAIL | ERROR | SKIPPED |
|---|---:|---:|---:|---:|---:|
| Baseline B1.4, lịch sử | 92 | 58 | 0 | 0 | 34 |
| Sau tích hợp đầu | 95 | 61 | 0 | 0 | 34 |
| Sau chỉnh thời điểm bắt đầu, lượt cuối | **95** | **61** | **0** | **0** | **34** |

Suite gồm unit/MVC/H2 cô lập; 34 SKIPPED là các ca MariaDB không được bật khi chưa có database test độc lập. Không coi H2 hoặc repository fixture là bằng chứng giao dịch MariaDB. Tổng được đối chiếu log và XML của đúng 20 lớp đã chạy, bỏ qua XML U21CaptureTest cũ còn trong target từ lượt trước. [Tổng kết Maven](../ui/screenshots/byredo-inspired/GSAP-C19/maven-summary.json)

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
$env:P01_MARIADB_TEST = 'false'
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' test
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' -DskipTests package
node --check src/main/resources/static/js/home-motion.js
git diff --check
```

Package cuối **BUILD SUCCESS**, JAR `target/beautyshop-0.0.1-SNAPSHOT.jar`. Đã mở ZIP và băm entry vendor/adapter trong JAR: khớp SHA-256 của file source. Home template cũng có trong JAR. [Bằng chứng entry JAR](../ui/screenshots/byredo-inspired/GSAP-C19/jar-assets.json). Log Maven tại `target/gsap-c19-resume/tests-final.log`, `package-final.log`.

### 7.5 Chrome Headless và responsive

Chrome **158.0.8127.36**, Windows, profile QA mới, điều khiển CDP trực tiếp theo chấp thuận bổ sung của owner. Kênh ChatGPT for Chrome trả “Browser is not available: chrome”, danh sách browser rỗng; không dùng Computer Use. Owner đã cho phép tiếp tục bằng Chrome Headless độc lập, không cần chờ tiện ích.

Fixture Tomcat dùng Spring MVC, Thymeleaf, Security, service và template/asset thật của working tree; repository trong bộ nhớ, loại DataSource/JPA/Flyway. Tên/giá sản phẩm từ hồ sơ nghiên cứu đã có, tồn chỉ là kịch bản QA. Mọi POST giỏ/login chỉ tới fixture loopback, không tới `beautyshop`.

**150 bản ghi kiểm tra** trong bộ pilot: 10 tổ hợp hero Guest (5 viewport × 2 zoom), cộng 140 lượt trang theo vai trò (Guest 60, Customer 50, Admin 30). 140 lượt gồm 30 Home và 110 trang khác; hai request GSAP/adapter chỉ xuất hiện ở Home. Catalog, PDP, Cart, Checkout, Account, Wishlist, Login, Admin và 404 công khai không tải chúng. Đây là số bản ghi kiểm tra browser, không cộng vào số test Maven. [Measurements](../ui/screenshots/byredo-inspired/GSAP-C19/measurements.json) · [Checks](../ui/screenshots/byredo-inspired/GSAP-C19/checks.json)

| Viewport vật lý | clientWidth 100% | clientWidth 200% | scroll ngang / fallback / reduced motion / keyboard |
|---|---:|---:|---|
| 360×800 | 345 | 172 | PASS cả hai zoom |
| 390×844 | 375 | 187 | PASS cả hai zoom |
| 768×1024 | 753 | 376 | PASS cả hai zoom |
| 1366×768 | 1.351 | 675 | PASS cả hai zoom |
| 1920×1080 | 1.905 | 952 | PASS cả hai zoom |

Zoom browser thật qua settingsPrivate.setDefaultZoom(1/2), viewport qua DevTools, xác nhận devicePixelRatio=1/2; không dùng CSS zoom/transform để giả zoom. Trên các lượt kiểm, scrollWidth bằng clientWidth. Rect của hero art, vùng copy, H1 và CTA không đổi giữa tĩnh/chuyển động/kết thúc. 10 cặp ảnh tĩnh/kết thúc có byte PNG giống hoàn toàn. [Đối chiếu ảnh](../ui/screenshots/byredo-inspired/GSAP-C19/static-settled-comparison.json)

Đã kiểm bằng assertion:

- Reduced motion từ đầu không có tween. Đổi no-preference → reduce giữa tween xóa style; bật lại cho phép chuỗi mới, hết 380 ms trở lại bản tĩnh và không còn tween. Không xuất hiện target ngoài ba phần trang trí.
- JS disabled và chặn riêng request vendor vẫn hiển thị đủ hero; CTA mở catalog bằng Enter. Adapter không ném lỗi khi window.gsap thiếu.
- Tab/Shift+Tab giữa CTA chính/phụ giữ thứ tự, focus outline 3 px; Enter điều hướng đúng. Native menu/nhóm disclosure, Escape, click/touch mô phỏng, resize và no-JS được kiểm thêm trong bộ regression.
- **0 Runtime.exceptionThrown** trong lượt pilot cuối và regression cuối. Lỗi tải vendor cố ý/ảnh SKU 404 theo fixture được phân biệt với JavaScript exception; C15 ảnh lỗi vẫn là vấn đề trước pilot.

### 7.6 Hồi quy các component đã PASS

Đã chạy lại bộ B1.4 trên code cuối bằng fixture mới và DevTools restart tắt: **100/100 tổ hợp**, 30 kiểm tra luồng bổ sung, không tràn ngang hoặc JS exception. Header vẫn 109 px mobile, 113 px Guest desktop, 116 px Customer/Admin desktop ở 100%, 226 px tại 360/390 ở 200%. [Measurements regression](../ui/screenshots/byredo-inspired/GSAP-C19/regression/measurements.json) · [Checks regression](../ui/screenshots/byredo-inspired/GSAP-C19/regression/checks.json)

| Component đã PASS | Bằng chứng hồi quy trong pilot |
|---|---|
| C01/C02/C04/C05/C06/C07/C08/C28 | Bộ 100 tổ hợp: menu, role, action, current, badge, focus, keyboard/no-JS và chiều rộng. Giỏ 0→1→5→12, login giữ hai SKU và 4.425.000 ₫. |
| C03/C09/C10/C11/C14/C16 | Search Enter 3→1, h1/count thật từ fixture server, GET loại da tiếng Việt, lưới cùng hàng có đáy bằng nhau, badge còn/hết đúng kịch bản; kiểm catalog trên năm viewport/hai zoom theo role. |
| C19 | Bộ 10 tổ hợp motion/fallback/reduced motion và ảnh so sánh nêu trên. |
| C22 | CTA dịch vụ mở đúng biểu mẫu /chat. |
| C23/C24/C25/C26 | PDP đúng brand/giá, Enter đổi SKU qua GET ?variant=11, thêm giỏ qua CTA native cả JS bật/tắt; Guest 1→2 đơn vị, server subtotal 410.000→820.000 ₫; login hợp nhất với giỏ Customer fixture thành 14 đơn vị, subtotal 5.245.000 ₫. Không đặt đơn hoặc gọi cổng thanh toán. |

Ma trận giữ **20/30 PASS, 6 PARTIAL, 4 FAIL, core 10/12**. Không sửa hai báo cáo P15/không nâng component khác nhờ thử nghiệm motion. F01 giữ BLOCKED; C15, C27, C21 và MariaDB còn mở như trước.

### 7.7 CLS và CSP: kết quả có giới hạn

Trong lượt hero nghiệm thu cuối, 10 tổ hợp ghi CLS=0 ở cả tải no-preference và đổi preference; không ghi nhận layout shift sau khi bắt đầu chuyển động. Không quy kết rằng toàn website luôn CLS=0.

Khi chẩn đoán cold load lặp ở 1920×1080, header `app.js` hiện có mở menu desktop làm đầu main chuyển y=109,390625→161,390625, có mẫu CLS **0,01564038499** ngay cả khi chặn cả hai script mới. Bản adapter đầu chạy cùng thời điểm có mẫu **0,01744504480**, vì vậy đã điều chỉnh chờ hai frame. Bộ đối chứng cuối 24 lượt (blocked/unblocked × reduce/no-preference × 6) chỉ ghi 0 hoặc 0,01564038499; không còn mẫu tăng này. Đây là dữ liệu lab nhỏ và phát hiện header có sẵn còn cần theo dõi, không phải cam kết field performance. [Trước điều chỉnh](../ui/screenshots/byredo-inspired/GSAP-C19/cls-before-frame-delay.json) · [Sau điều chỉnh](../ui/screenshots/byredo-inspired/GSAP-C19/cls-diagnostic.json)

HTTP thật của fixture trả 200 và không có Content-Security-Policy/Report-Only. Thử thêm CSP qua proxy QA cô lập, không sửa source Security: `default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; object-src 'self'; base-uri 'self'; form-action 'self'`. Animation, complete revert và đổi reduced-motion chạy với **0 securitypolicyviolation**. Không dùng unsafe-inline hoặc unsafe-eval để cho GSAP chạy. [Kết quả CSP](../ui/screenshots/byredo-inspired/GSAP-C19/checks.json)

GET localhost:8080 trong lượt tiếp tục vẫn curl exit 7, không có header production/demo. **CSP production NOT VERIFIED**; policy QA không thay thế policy triển khai. Không sửa Security_Config hoặc nới policy production. Chưa có Lighthouse/LCP/INP hoặc số đo RUM.

### 7.8 Ảnh tĩnh / chuyển động / kết thúc

Ảnh tĩnh dùng reduced motion trên cùng bản HTML/CSS; ảnh chuyển động chụp tween thật, ảnh kết thúc xác nhận khôi phục. Đây là đối chứng hero tĩnh/có motion, không phải ảnh Byredo hay dữ liệu thương phẩm MariaDB. Ảnh cận vùng hero đã cuộn để nhìn đủ phần trang trí trên mobile zoom 200%.

| Viewport / zoom | Tĩnh | Đang chuyển động | Kết thúc |
|---|---|---|---|
| 1366×768 / 100% | [Tĩnh](../ui/screenshots/byredo-inspired/GSAP-C19/hero-static-1366-100.png) | [Motion](../ui/screenshots/byredo-inspired/GSAP-C19/hero-motion-1366-100.png) | [Sau](../ui/screenshots/byredo-inspired/GSAP-C19/hero-settled-1366-100.png) |
| 390×844 / 100% | [Tĩnh](../ui/screenshots/byredo-inspired/GSAP-C19/hero-static-390-100.png) | [Motion](../ui/screenshots/byredo-inspired/GSAP-C19/hero-motion-390-100.png) | [Sau](../ui/screenshots/byredo-inspired/GSAP-C19/hero-settled-390-100.png) |
| 390×844 / 200% | [Tĩnh](../ui/screenshots/byredo-inspired/GSAP-C19/hero-static-390-200.png) | [Motion](../ui/screenshots/byredo-inspired/GSAP-C19/hero-motion-390-200.png) | [Sau](../ui/screenshots/byredo-inspired/GSAP-C19/hero-settled-390-200.png) |

[JS tắt 390/200%](../ui/screenshots/byredo-inspired/GSAP-C19/nojs-390-200.png) · [Vendor bị chặn 1366/100%](../ui/screenshots/byredo-inspired/GSAP-C19/vendor-blocked-1366-100.png) · [CSP cô lập](../ui/screenshots/byredo-inspired/GSAP-C19/isolated-csp-1366.png). Các trạng thái đầu trang ở cả năm viewport/hai zoom và ảnh regression ở cùng thư mục.

### 7.9 Giới hạn, kiểm soát thay đổi và rollback hiện tại

Lượt QA ban đầu phát hiện fixture DevTools tự restart khi ghi file kiểm thử; đã tắt bằng JVM property, chạy lại trên instance sạch. Một lượt harness bắt đầu trước khi port mới sẵn sàng và một đích 404 ban đầu nằm ngoài route công khai đã được sửa trong harness. Lỗi CLS của adapter đầu đã điều tra/điều chỉnh như mục 7.7. Kết quả công bố là các lượt cuối hoàn tất exit 0, không dùng các lượt dở dang làm bằng chứng PASS.

Source chỉ đổi Home template và BootstrapPagesTest, thêm adapter/vendor/provenance: 184 file trước →187 file sau, 182 file cũ giữ nguyên hash. CSS, app.js, head/header/footer, backend, Security, pom.xml, token, Flyway và hai báo cáo P15 được giữ nguyên theo snapshot/hash. Bốn file bảo vệ pom.xml/Design System/hai báo cáo P15 có 0 khác biệt. [Đối chiếu preservation](../ui/screenshots/byredo-inspired/GSAP-C19/preservation.json). Fixture đã được đóng và xóa credential Admin tạm sau QA. Không reset/clean, commit/push, seed hoặc ghi MariaDB.

Còn chưa xác minh: MariaDB thật và F01, CSP reverse proxy/production, RUM/performance dưới tải, screen reader và thiết bị cảm ứng vật lý, BFCache thực mọi browser, quyết định thẩm mỹ của owner. Chrome là headless với touch mô phỏng, không thay thế nghiệm thu thiết bị thật. Chưa tuyên bố production-ready.

Rollback chỉ bỏ hai include ở Home, xóa đúng adapter và hai file vendor/provenance thuộc pilot, gỡ ba ca test riêng pilot; kiểm lại hero tĩnh và Maven test/package. Không revert cả Home hoặc BootstrapPagesTest về HEAD vì chúng có thay đổi U1–U2 chưa commit. Không reset/clean working tree. **Dừng sau pilot C19 để owner nghiệm thu; không triển khai B2 hoặc mở rộng motion.**
