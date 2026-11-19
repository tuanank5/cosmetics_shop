package com.beautyshop.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * An toàn khi khởi tạo: CHƯA cho phép đăng nhập.
 * Thay bằng UserDetailsService truy cập MariaDB khi thực hiện P04.
 */
@Service
public class BootstrapUserDetails_Service implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        throw new UsernameNotFoundException("Tài khoản chưa được triển khai trong scaffold");
    }
}
