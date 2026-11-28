BeautyShop bổ sung kiểm tra mật khẩu mới dưới khóa tài khoản, cho phép nhập lại khi link reset còn hiệu lực và yêu cầu bộ chọn tài khoản khi bấm đăng nhập Google. Quyền đặt lại mật khẩu được gắn vào ID phiên mới. OAuth state được lấy và xóa dưới cùng khóa phiên.

Bản kiểm dựa trên main `f390c3a`. Maven mặc định đạt 234 ca, bỏ qua 88 ca theo điều kiện môi trường. Suite MariaDB/WebKit riêng đạt 40/40, gồm hai phiên gỡ phương thức đồng thời và logout đa tab. Package đạt. Schema tiếp tục dùng migration hiện có. Google/GitHub trong suite là fixture. Owner sẽ nghiệm thu sau khi đối chiếu CI với commit của PR.

[Báo cáo AUTH](docs/reviews/AUTH-ket-qua.md) · [Bằng chứng regression](docs/qa/AUTH/final-review/)
