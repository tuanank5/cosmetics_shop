# NAV-01 · Nguồn điều hướng và fallback

| Vai trò | Nguồn → đích | Quay lại |
|---|---|---|
| Guest/Customer/Admin | Logo hoặc Trang chủ → home | GET `/?home=opening` mở chương 1, không mượn trạng thái của history entry khác |
| Guest/Customer/Admin | Home featured, popular, all → PDP | `from` theo đúng vùng. GET home kèm sort hợp lệ và anchor `{region}-product-{id}`. Nếu mất thẻ, JS đưa focus về heading vùng hoặc heading mở đầu |
| Guest/Customer/Admin | Catalog hoặc tìm kiếm → PDP | GET catalog giữ q, brand, category, skinType, sort, page và size đã lọc; chọn SKU hoặc chuyển trang review giữ nguồn |
| Guest/Customer/Admin | PDP trực tiếp, nguồn không hợp lệ | Catalog mặc định. Nguồn wishlist yêu cầu Customer |
| Customer | Wishlist → PDP | GET wishlist; direct link không có nguồn về catalog |
| Guest/Customer | Giỏ → checkout | Checkout yêu cầu Customer, lưu GET trong Spring Security. Giỏ hiện không có link từ dòng giỏ vào PDP |
| Customer | Checkout → địa chỉ → thêm hoặc sửa | `from=checkout`; hủy/quay lại hoặc lưu thành công về GET checkout. Checkout đọc lại giỏ và địa chỉ theo chủ. Form lỗi giữ hiển thị tại chỗ. Form mở từ hồ sơ quay về danh sách địa chỉ |
| Customer | Đơn → review hoặc form đổi trả | Link quay về đơn theo ID quan hệ đã đọc từ backend. Danh sách đổi trả và chi tiết quay về cùng scope Customer |
| Admin | Tổng quan → chi tiết đơn, đổi trả, sửa voucher | `nav=dashboard` kèm kỳ rõ ràng. Quay về tổng quan cùng kỳ. Form giữ nguồn qua validate và PRG |
| Admin | Danh sách đơn có trạng thái → chi tiết | `listStatus` là enum danh sách, độc lập với trường trạng thái dùng để cập nhật đơn |
| Admin | Catalog → SKU → form SKU; catalog → form sửa; voucher → form | Quay về danh sách theo quan hệ hiện có. Danh sách này chưa có q/page cần giữ |
| Admin | Tài khoản → chi tiết → yêu cầu reset | Giữ q/page theo contract AUTH-02. Không đưa token reset vào nguồn |
| Admin | Review → duyệt/ẩn | PRG hiện có giữ bộ lọc trạng thái, sao, sản phẩm và page |

Các route protected vẫn kiểm role và quyền sở hữu trên backend. Bản ghi history của home chỉ giữ số chương và ID DOM của thẻ. Không lưu draft, token, state OAuth, callback hay địa chỉ. No-JS dùng link GET và anchor native.
