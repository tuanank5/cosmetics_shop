package com.beautyshop.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnhBienTheSanPham_Service {
    public static final String URL_PREFIX = "/uploads/sku/";
    private static final Pattern TEN_ANH = Pattern.compile("[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}\\.(jpg|png)");
    private static final long GIOI_HAN_BYTE = 2L * 1024 * 1024;
    private static final int GIOI_HAN_CHIEU = 2048;
    private static final Logger logger = LoggerFactory.getLogger(AnhBienTheSanPham_Service.class);
    private final Path thuMuc;

    public AnhBienTheSanPham_Service(@Value("${app.uploads.sku-dir:uploads/sku}") Path thuMuc) {
        this.thuMuc = thuMuc.toAbsolutePath().normalize();
    }

    String luu(MultipartFile tapTin) {
        String tenGoc = Objects.toString(tapTin.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        String dinhDang = dinhDang(tenGoc, tapTin.getContentType());
        if (tapTin.getSize() > GIOI_HAN_BYTE || dinhDang == null
            || tenGoc.contains("/") || tenGoc.contains("\\")) {
            throw new IllegalArgumentException("Chỉ nhận ảnh PNG hoặc JPEG tối đa 2 MB.");
        }

        BufferedImage anh = docAnh(tapTin, dinhDang);
        String ten = UUID.randomUUID() + "." + dinhDang;
        Path dich = thuMuc.resolve(ten);
        boolean daTao = false;
        try {
            Files.createDirectories(thuMuc);
            try (var output = Files.newOutputStream(dich, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                daTao = true;
                if (!ImageIO.write(anh, dinhDang, output)) {
                    throw new IOException("Thiếu bộ ghi ảnh.");
                }
            }
        } catch (IOException exception) {
            if (daTao) {
                xoa(URL_PREFIX + ten);
            }
            throw new UncheckedIOException("Không thể lưu ảnh biến thể sản phẩm.", exception);
        }
        return URL_PREFIX + ten;
    }

    private BufferedImage docAnh(MultipartFile tapTin, String dinhDang) {
        try (var input = new MemoryCacheImageInputStream(tapTin.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Nội dung tập tin không phải ảnh hợp lệ.");
            }
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                boolean dungDinhDang = reader.getFormatName().equalsIgnoreCase(dinhDang)
                    || ("jpg".equals(dinhDang) && reader.getFormatName().equalsIgnoreCase("jpeg"));
                if (!dungDinhDang || reader.getWidth(0) > GIOI_HAN_CHIEU || reader.getHeight(0) > GIOI_HAN_CHIEU) {
                    throw new IllegalArgumentException("Ảnh sai định dạng hoặc vượt quá 2048 × 2048 px.");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc ảnh đã chọn.", exception);
        }
    }

    private String dinhDang(String ten, String mime) {
        if ("image/png".equals(mime) && ten.endsWith(".png")) {
            return "png";
        }
        if ("image/jpeg".equals(mime) && (ten.endsWith(".jpg") || ten.endsWith(".jpeg"))) {
            return "jpg";
        }
        return null;
    }

    public Optional<Path> tim(String ten) {
        if (!TEN_ANH.matcher(ten).matches()) {
            return Optional.empty();
        }
        Path path = thuMuc.resolve(ten);
        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) ? Optional.of(path) : Optional.empty();
    }

    void xoa(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        tim(url.substring(URL_PREFIX.length())).ifPresent(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                logger.warn("Không thể xóa ảnh SKU {}", path.getFileName());
            }
        });
    }
}
