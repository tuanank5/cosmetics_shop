package com.beautyshop.service;

public enum LyDoTaiKhoan {
    KHACH_YEU_CAU_KHOA(false, "Chủ tài khoản yêu cầu khóa"),
    BAO_VE_TAI_KHOAN(false, "Bảo vệ tài khoản trong lúc xác minh"),
    KHACH_XAC_NHAN_MO(true, "Đã xác minh yêu cầu mở của chủ tài khoản"),
    DA_XU_LY_RUI_RO(true, "Đã xử lý lý do bảo mật");

    private final boolean moKhoa;
    private final String nhan;

    LyDoTaiKhoan(boolean moKhoa, String nhan) {
        this.moKhoa = moKhoa;
        this.nhan = nhan;
    }

    public boolean isMoKhoa() { return moKhoa; }
    public String getNhan() { return nhan; }
}
