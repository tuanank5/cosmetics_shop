ALTER TABLE nguoi_dung
    ADD COLUMN nha_cung_cap_oauth VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN ma_danh_tinh_oauth VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD CONSTRAINT uq_nguoi_dung_danh_tinh_oauth UNIQUE (nha_cung_cap_oauth, ma_danh_tinh_oauth),
    ADD CONSTRAINT ck_nguoi_dung_danh_tinh_oauth CHECK (
        (nha_cung_cap_oauth IS NULL AND ma_danh_tinh_oauth IS NULL)
        OR (nha_cung_cap_oauth IS NOT NULL AND ma_danh_tinh_oauth IS NOT NULL
            AND nha_cung_cap_oauth IN ('google', 'github') AND ma_danh_tinh_oauth <> '')
    );
