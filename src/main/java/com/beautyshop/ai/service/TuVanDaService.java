package com.beautyshop.ai.service;


import com.beautyshop.ai.RagProvider;
import com.beautyshop.ai.dto.TuVanDaRequest;
import com.beautyshop.entity.BienTheSanPham;
import com.beautyshop.repository.BienTheSanPham_Repository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TuVanDaService {

    private final BienTheSanPham_Repository bienTheRepository;
    private final ChatModel chatModel;

    public TuVanDaService(
            BienTheSanPham_Repository bienTheRepository,
            RagProvider ragProvider) {

        this.bienTheRepository = bienTheRepository;
        this.chatModel = ragProvider.chat();
    }

    public String tuVan(TuVanDaRequest request) {

        // 1. Lấy sản phẩm đang bán và còn hàng từ database
        List<BienTheSanPham> danhSach = bienTheRepository.findAllDangBanConHang();

        // 2. Chuẩn bị thông tin sản phẩm cho AI
        String thongTinSanPham = danhSach.stream()
                .map(this::chuyenThanhThongTin)
                .collect(Collectors.joining("\n\n"));

        if (thongTinSanPham.isBlank()) {
            return "Hiện tại cửa hàng chưa có sản phẩm phù hợp để tư vấn.";
        }

        // 3. Tạo nội dung yêu cầu AI
        String prompt = """
                Bạn là chuyên viên tư vấn chăm sóc da của cửa hàng mỹ phẩm.

                Hãy tư vấn dựa trên thông tin khách hàng và danh sách sản phẩm
                thực tế của cửa hàng bên dưới.

                QUY TẮC:
                - Chỉ được đề xuất sản phẩm có trong danh sách.
                - Không tự bịa tên sản phẩm, thành phần hoặc giá bán.
                - Giải thích vì sao sản phẩm phù hợp với khách hàng.
                - Đề xuất cách sử dụng dựa trên hướng dẫn có sẵn.
                - Nếu không đủ thông tin, hãy nói rõ.
                - Không chẩn đoán bệnh hoặc cam kết chữa khỏi bệnh.
                - Nếu khách có dấu hiệu kích ứng nghiêm trọng, hãy khuyên gặp bác sĩ da liễu.

                THÔNG TIN KHÁCH HÀNG:
                Loại da: %s
                Vấn đề da: %s
                Độ nhạy cảm: %s
                Nhu cầu: %s

                DANH SÁCH SẢN PHẨM:
                %s

                Hãy trả lời bằng tiếng Việt, dễ hiểu, chia thành từng mục.
                """.formatted(
                request.loaiDa(),
                request.vanDeDa(),
                request.daNhayCam(),
                request.nhuCau(),
                thongTinSanPham
        );

        // 4. Gửi yêu cầu đến Ollama / Qwen
        return chatModel.call(new Prompt(prompt))
                .getResult()
                .getOutput()
                .getText();
    }

    private String chuyenThanhThongTin(BienTheSanPham bienThe) {

        var sanPham = bienThe.getSanPham();

        return """
                Tên sản phẩm: %s
                Biến thể: %s
                Giá bán: %s VNĐ
                Số lượng khả dụng: %d
                Thành phần: %s
                Loại da phù hợp: %s
                Mô tả: %s
                Hướng dẫn sử dụng: %s
                """.formatted(
                sanPham.getTenSanPham(),
                bienThe.getTenBienThe(),
                bienThe.getGiaBan(),
                bienThe.getSoLuongKhaDung(),
                sanPham.getThanhPhan(),
                sanPham.getLoaiDaPhuHop(),
                sanPham.getMoTa(),
                sanPham.getHuongDanSuDung()
        );
    }
}
