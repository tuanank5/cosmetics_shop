package com.beautyshop.security;

import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.service.NguoiDung_Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class NguoiDungDetails_Service implements UserDetailsService {
    private final NguoiDung_Repository repository;

    public NguoiDungDetails_Service(NguoiDung_Repository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return repository.findByEmail(NguoiDung_Service.chuanHoaEmail(username))
            .map(PhienNguoiDung::new)
            .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại."));
    }
}
