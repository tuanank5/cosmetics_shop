package com.beautyshop.controller;

import com.beautyshop.cart.ThongBaoGopGio;
import com.beautyshop.security.PhienNguoiDung;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class GopGioNotice_Config implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HienThongBao());
    }

    private static class HienThongBao implements HandlerInterceptor {
        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView model) {
            if (!(handler instanceof HandlerMethod) || model == null || request.getDispatcherType() != DispatcherType.REQUEST
                || !"GET".equals(request.getMethod()) || response.getStatus() != 200) return;
            String view = model.getViewName();
            if (view == null || view.startsWith("redirect:") || view.startsWith("forward:") || !yeuCauTrangHtml(request)) return;
            var xacThuc = SecurityContextHolder.getContext().getAuthentication();
            if (xacThuc == null || !(xacThuc.getPrincipal() instanceof PhienNguoiDung phien)
                || !request.isUserInRole("CUSTOMER")) return;
            var notice = ThongBaoGopGio.nhan(request, phien);
            if (notice != null) {
                model.addObject("gopGioNotice", notice);
                response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, private");
            }
        }

        private static boolean yeuCauTrangHtml(HttpServletRequest request) {
            String cheDo = request.getHeader("Sec-Fetch-Mode");
            String dich = request.getHeader("Sec-Fetch-Dest");
            if ((cheDo != null && !cheDo.equals("navigate")) || (dich != null && !dich.equals("document"))
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || request.getHeader("Purpose") != null || request.getHeader("Sec-Purpose") != null) return false;
            String accept = request.getHeader(HttpHeaders.ACCEPT);
            if (accept == null || accept.isBlank()) return true;
            try {
                return MediaType.parseMediaTypes(accept).stream().anyMatch(loai -> loai.getQualityValue() > 0
                    && !loai.isWildcardType() && !loai.isWildcardSubtype()
                    && (loai.isCompatibleWith(MediaType.TEXT_HTML) || loai.toString().startsWith("application/xhtml+xml")));
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
    }
}
