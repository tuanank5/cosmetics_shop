# PERF-FINAL · Review ứng viên qa/perf

**IN_PROGRESS**, trên parent `e33e4e3e72e23852b8dcc60623da822acd32cae7`. Bốn file Java trong [manifest đã benchmark](../candidate/source.json) khớp byte với bản chép vào clone QA. Phần bổ sung FINAL là kiểm thử fixture và tài liệu. Review PR, CI GitHub và nghiệm thu đúng commit còn chờ.

## Phạm vi và semantics

GiaoDien_Advice vẫn tính huy hiệu Guest theo số lượng trong session. Với Customer ở /cart, /checkout và /checkout/quote, controller cộng các số lượng kiểu long từ danh sách thực sự đưa vào model. Cả giỏ rỗng và dòng thiếu hàng đều có nghĩa rõ ràng: tổng bằng 0 khi rỗng, hàng thiếu tồn vẫn đóng góp số lượng đang nằm trong giỏ. Checkout có thể từ chối báo giá độc lập với con số đó. Admin không nhận huy hiệu Customer trong controller giỏ. Các trang Customer khác vẫn dùng service, gồm cơ chế bỏ qua lỗi đọc huy hiệu phụ.

Huy hiệu và nội dung của một response Customer nay cùng một lần đọc. Hai tab có thể thấy hai thời điểm khác nhau cho đến lần tải lại. Có thể có khác biệt với bản cũ ngay trong một response khi có writer chen giữa hai lần đọc. Đây là thay đổi về tính nhất quán hiển thị. Không chia sẻ danh sách giữa request. Các endpoint ghi, kiểm session/CSRF, kiểm chủ giỏ, khóa giỏ/SKU, quote server-side, đặt/hủy đơn, voucher và transaction không có hunk sửa. CheckoutPagesTest dùng số lượng 2 trên một SKU và verify đúng một lần gọi service. Suite WebKit cũ chỉ kiểm các giỏ có mỗi SKU một đơn vị, vì vậy đã bổ sung phép thử mutation.

## Đọc kết quả benchmark

Cả hai build.json có cùng parent và SHA-256 seed SQL `898ce606da5eb83264973060560a9f2bc2c9166c8bd743e1150442a33f2441c2`. Diagnostic xác nhận kích thước, phân bố active và số dòng giỏ. Dữ liệu nghiệp vụ cùng seed, mỗi lượt có datadir riêng và credential fixture ngẫu nhiên. JAR khác hash như dự kiến. Runner dùng cùng cấu hình JDK 21.0.13, G1 256–512 MiB, MariaDB 12.2.2 với buffer pool 128 MiB, template cache bật và Mongo riêng. Baseline chạy trước, candidate chạy sau, route theo thứ tự cố định. Login, migration và thao tác chuẩn bị đã làm ấm một phần JVM/DB. Mỗi route warm-up ít nhất hai giây, sau đó hai client tự gửi liên tiếp 50 request/client. Cold browser dùng context mới, cold HTTP trong bảng gốc chỉ là lần đầu của route.

Đã tính lại median, p95/p99 nearest-rank từ từng bộ 100 latency, đối chiếu cờ thành công và danh sách câu SQL trên 12 route mỗi bản. Error rate 0/1.200 mỗi bản theo status, redirect và marker đã định. SQL là trace của một request bổ sung, không phải đếm trên cả 2.400 request. Cả bảy route liên quan giảm một SELECT. Không có bằng chứng N+1 theo chiều dài giỏ. Một batch mỗi route/build chưa đủ cho khoảng tin cậy hay kết luận mức tăng latency/RPS ổn định. [Mẫu trước](../baseline/latency.json) · [Mẫu sau](../candidate/latency.json) · [Bảng đủ p50/p95/p99, RPS, bytes và query](../../../reviews/PERF-ket-qua.md)

| Route có RPS giảm | RPS trước → sau | Thời gian hoàn tất batch trước → sau, giây |
|---|---:|---:|
| Catalog trang 5 | 50,19 → 45,30 | 1,992 → 2,208 |
| PDP | 112,84 → 112,33 | 0,886 → 0,890 |
| Giỏ 1 | 204,50 → 199,4 | 0,489 → 0,501 |
| Checkout 1 | 158,95 → 153,47 | 0,629 → 0,652 |
| Giỏ 10 | 156,65 → 140,36 | 0,638 → 0,712 |
| Quote 100 | 105,51 → 101,63 | 0,948 → 0,984 |

RPS lấy 100 chia thời gian tới khi **cả hai** client xong. Thời gian batch trong bảng suy ngược từ RPS đã làm tròn. Với giỏ 10, latency trung bình tăng 12,00 → 14,14 ms dù p95 gần nhau. Với quote 100, p95 giảm nhưng client chậm hơn mất 0,983 giây ở candidate so với 0,947 giây ở baseline. Vì vậy giảm một SELECT mỗi request vẫn có thể đi cùng giảm RPS trong một batch. Thiếu nhiều lượt đảo thứ tự và trace scheduler/GC gắn từng request để quy nguyên nhân sâu hơn. Không gán các hồi quy này cho DB, GC hay code chỉ từ số tổng.

CPU JVM trong cửa sổ HTTP/trace là 56,31 → 49,95 CPU-giây. Peak RSS JVM tăng 546 → 555 MiB, MariaDB xấp xỉ 134 MiB hai lượt. GC từ startup tới cuối tải ghi 70 → 68 pause, tổng 315 → 287 ms. Các cửa sổ đo khác nhau; không dùng peak RSS hay tổng GC để giải thích riêng một route. Lợi ích định lượng có thể tái kiểm là bỏ một SELECT danh sách, với 100 dòng khoảng 10.657 byte TSV được tránh. Giỏ lớn vẫn có khoảng 409 KB HTML.

## Regression trên checkout QA

| Tình huống đã chạy | Quan sát thực tế |
|---|---|
| Guest | Giỏ trống có badge 0, thêm một SKU thấy badge 1. GET checkout chuyển tới login. |
| Customer có 1/10/100 dòng | GET cart và GET checkout ở cả ba kích thước trả 200, badge và số dòng bằng nhau. |
| Hai tab cùng session thêm SKU trùng | Hai POST được phát đồng thời lên giỏ đang có một đơn vị. Sau reload, cả hai tab còn một dòng, số lượng 3 và badge 3. |
| Đổi số lượng | Ghi 4 ở tab A. Tab B đang mở vẫn hiển thị snapshot 3; reload thấy 4. |
| Checkout với voucher | POST quote với voucher FIXED giảm 10.000, quote valid và badge 4. |
| Xóa dòng cuối | POST quantity=0. GET giỏ có 0 dòng, badge 0. GET checkout badge 0, quote invalid. |
| SKU hết tồn trong giỏ 10 dòng | Giỏ vẫn đếm 10, có cảnh báo tại SKU hết hàng; checkout badge 10 và quote invalid. |

Hai POST song song kiểm kết quả cuối trong một lịch chạy thật; chưa ép lịch GET chen đúng giữa hai transaction ghi, chưa stress nhiều session hoặc đo ở tải production. Không có push cập nhật tab cũ. Các phép thử bổ sung dùng WebKit và các POST cùng origin có CSRF, không đo latency. [Kết quả mutation](badge.json) · [Kết quả kích thước giỏ](cart-webkit.json)

Từ clone QA mới tại đúng parent, với bốn file nguồn khớp hash: Maven 237 PASS/88 SKIP, package PASS, suite MariaDB 94/94 trên datadir riêng (gồm 12 unit test chạy kèm). Benchmark tải đã được kiểm lại từ log gốc, không chạy lại để thay số đo. Để tái chạy các tình huống huy hiệu, dùng runner trong protocol với `-DiagnosticsOnly -BadgeReview`, SourceManifest của candidate và thư mục output mới. [Maven](test.txt) · [Package](package.txt) · [MariaDB](integration.txt)
