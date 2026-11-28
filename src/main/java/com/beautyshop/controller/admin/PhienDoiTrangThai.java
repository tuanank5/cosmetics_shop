package com.beautyshop.controller.admin;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
@SessionScope
class PhienDoiTrangThai implements Serializable {
    record Lenh(long admin, long dich, long version, boolean moKhoa) implements Serializable {}
    private final LinkedHashMap<UUID, Lenh> lenh = new LinkedHashMap<>();

    synchronized UUID cap(long admin, long dich, long version, boolean moKhoa) {
        var id = UUID.randomUUID();
        lenh.put(id, new Lenh(admin, dich, version, moKhoa));
        if (lenh.size() > 256) lenh.pollFirstEntry();
        return id;
    }

    synchronized Lenh doc(UUID id, long admin, long dich) {
        var form = lenh.get(id);
        if (form == null || form.admin() != admin || form.dich() != dich) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hãy tải lại danh sách tài khoản.");
        }
        return form;
    }
}
