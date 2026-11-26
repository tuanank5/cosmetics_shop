package com.beautyshop.controller;

import com.beautyshop.dto.MatKhauMoi_Form;
import com.beautyshop.repository.DanhTinhOAuth_Repository;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.OAuth2ThaoTac;
import com.beautyshop.security.OAuth2ThaoTac.MucDich;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.DanhTinhOAuth_Service;
import com.beautyshop.service.EmailVerification_Service;
import com.beautyshop.service.MatKhau_Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.WebUtils;

@Controller
@RequestMapping("/account/security")
class BaoMatTaiKhoan_Controller {
    private static final String TRANG = "redirect:/account/security";
    private static final String GRANT = BaoMatTaiKhoan_Controller.class.getName() + ".grant";
    private static final String CHALLENGE = BaoMatTaiKhoan_Controller.class.getName() + ".challenge";
    private record XacThucLai(long user, long version, String method, Instant expires) implements java.io.Serializable {
        boolean hopLe(PhienNguoiDung principal) {
            return user == principal.getMaNguoiDung() && version == principal.getPhienBanXacThuc()
                && Instant.now().isBefore(expires);
        }
    }
    private record ThuXacMinh(long user, long version, String challenge,
                             OAuth2ThaoTac.BangChungPhien proof) implements java.io.Serializable {}

    private final NguoiDung_Repository users;
    private final DanhTinhOAuth_Repository identityRepository;
    private final DanhTinhOAuth_Service identities;
    private final MatKhau_Service passwords;
    private final EmailVerification_Service otp;
    private final PasswordEncoder encoder;
    private final ObjectProvider<ClientRegistrationRepository> clients;

    BaoMatTaiKhoan_Controller(NguoiDung_Repository users, DanhTinhOAuth_Repository identityRepository,
                             DanhTinhOAuth_Service identities, MatKhau_Service passwords,
                             EmailVerification_Service otp, PasswordEncoder encoder,
                             ObjectProvider<ClientRegistrationRepository> clients) {
        this.users = users;
        this.identityRepository = identityRepository;
        this.identities = identities;
        this.passwords = passwords;
        this.otp = otp;
        this.encoder = encoder;
        this.clients = clients;
    }

    @GetMapping
    String xem(@AuthenticationPrincipal PhienNguoiDung principal, HttpSession session, Model model) {
        var user = users.docBaoMat(principal.getMaNguoiDung()).orElseThrow();
        var proof = OAuth2ThaoTac.bangChung(session, principal);
        model.addAttribute("email", user.getEmail());
        model.addAttribute("daXacMinhEmail", user.getDaXacMinhEmail());
        model.addAttribute("coMatKhau", user.getCoMatKhau());
        model.addAttribute("providers", providers());
        model.addAttribute("linked", linked(principal));
        model.addAttribute("grant", grant(session, principal) != null);
        model.addAttribute("lienKetCho", proof != null && proof.purpose() == MucDich.LIEN_KET ? proof.proof().email() : null);
        model.addAttribute("xacThucOAuthCho", proof != null && proof.purpose() == MucDich.XAC_THUC_LAI);
        model.addAttribute("otpCho", session.getAttribute(CHALLENGE) != null);
        return "auth/security";
    }

    @PostMapping("/reauth/password")
    String bangMatKhau(@AuthenticationPrincipal PhienNguoiDung principal, HttpSession session,
                       @RequestParam String matKhau, RedirectAttributes flash) {
        synchronized (WebUtils.getSessionMutex(session)) {
            session.removeAttribute(GRANT);
            var user = users.findById(principal.getMaNguoiDung()).orElseThrow();
            if (user.getMatKhauBam() == null || !passwords.choPhepThuMatKhau(user.getMaNguoiDung())
                || !encoder.matches(matKhau, user.getMatKhauBam())) {
                flash.addFlashAttribute("error", "Chưa xác thực được mật khẩu hiện tại. Vui lòng thử lại sau.");
                return TRANG;
            }
            luuGrant(session, principal, "password");
        }
        return TRANG;
    }

    @PostMapping("/oauth/{provider}")
    String batDauOAuth(@PathVariable String provider, @RequestParam MucDich purpose,
                       @AuthenticationPrincipal PhienNguoiDung principal, HttpSession session,
                       RedirectAttributes flash) {
        synchronized (WebUtils.getSessionMutex(session)) {
            boolean linked = linked(principal).contains(provider);
            if (!providers().contains(provider) || (purpose == MucDich.LIEN_KET
                ? linked || grant(session, principal) == null : !linked)) {
                flash.addFlashAttribute("error", "Hãy xác thực lại bằng phương thức đăng nhập hiện có.");
                return TRANG;
            }
            flash.addAttribute("operation", OAuth2ThaoTac.batDau(session, principal, provider, purpose));
            return "redirect:/oauth2/authorization/" + provider;
        }
    }

    @PostMapping("/email/send")
    String guiMa(@AuthenticationPrincipal PhienNguoiDung principal, HttpSession session, RedirectAttributes flash) {
        synchronized (WebUtils.getSessionMutex(session)) {
            var user = users.findById(principal.getMaNguoiDung()).orElseThrow();
            var proof = OAuth2ThaoTac.bangChung(session, principal);
            if (proof != null && proof.purpose() != MucDich.XAC_THUC_LAI) {
                proof = null;
            }
            if (proof == null && (user.getNgayXacMinhEmail() != null || user.getMatKhauBam() == null
                || grant(session, principal) == null || !"password".equals(grant(session, principal).method()))) {
                flash.addFlashAttribute("error", "Hãy xác thực lại bằng mật khẩu hoặc Google hoặc GitHub đã liên kết.");
                return TRANG;
            }
            try {
                var challenge = otp.guiOTP(user.getEmail(), "security");
                var old = (ThuXacMinh) session.getAttribute(CHALLENGE);
                if (old != null) {
                    otp.xoaOTP(old.challenge());
                }
                session.setAttribute(CHALLENGE, new ThuXacMinh(principal.getMaNguoiDung(),
                    principal.getPhienBanXacThuc(), challenge, proof));
                flash.addFlashAttribute("message", "Mã xác minh đã được gửi đến email BeautyShop của bạn.");
            } catch (RuntimeException failure) {
                flash.addFlashAttribute("error", "Chưa gửi được mã. Vui lòng thử lại sau.");
            }
        }
        return TRANG;
    }

    @PostMapping("/email/verify")
    String xacMinh(@AuthenticationPrincipal PhienNguoiDung principal, HttpSession session,
                  @RequestParam String ma, RedirectAttributes flash) {
        synchronized (WebUtils.getSessionMutex(session)) {
            var challenge = (ThuXacMinh) session.getAttribute(CHALLENGE);
            var proof = OAuth2ThaoTac.bangChung(session, principal);
            boolean oauth = challenge != null && challenge.proof() != null && challenge.proof().equals(proof);
            boolean password = grant(session, principal) != null && "password".equals(grant(session, principal).method());
            if (challenge == null || challenge.user() != principal.getMaNguoiDung()
                || challenge.version() != principal.getPhienBanXacThuc()
                || (!oauth && (!password || challenge.proof() != null))) {
                flash.addFlashAttribute("error", "Yêu cầu xác minh đã hết hạn. Hãy bắt đầu lại.");
                return TRANG;
            }
            try {
                if (!otp.xacThucOTP(challenge.challenge(), ma)) {
                    flash.addFlashAttribute("error", "Mã xác minh không đúng hoặc đã hết hạn.");
                    return TRANG;
                }
                session.removeAttribute(CHALLENGE);
                identities.xacNhanEmail(principal.getMaNguoiDung(), principal.getPhienBanXacThuc());
                if (oauth) {
                    luuGrant(session, principal, proof.proof().provider());
                    OAuth2ThaoTac.xoaBangChung(session);
                }
                flash.addFlashAttribute("message", "Đã xác minh email BeautyShop.");
            } catch (RuntimeException failure) {
                flash.addFlashAttribute("error", "Chưa xác minh được email. Vui lòng thử lại.");
            }
        }
        return TRANG;
    }

    @PostMapping("/oauth/confirm")
    String lienKet(@AuthenticationPrincipal PhienNguoiDung principal, HttpServletRequest request,
                   HttpServletResponse response, Authentication auth, RedirectAttributes flash) {
        var session = request.getSession();
        synchronized (WebUtils.getSessionMutex(session)) {
            var proof = OAuth2ThaoTac.bangChung(session, principal);
            if (grant(session, principal) == null || proof == null || proof.purpose() != MucDich.LIEN_KET
                || !providers().contains(proof.proof().provider())) {
                flash.addFlashAttribute("error", "Yêu cầu liên kết đã hết hạn. Hãy bắt đầu liên kết lại.");
                return TRANG;
            }
            try {
                identities.lienKet(principal.getMaNguoiDung(), principal.getPhienBanXacThuc(), proof.proof());
                return thoat(request, response, auth);
            } catch (AccessDeniedException | DataIntegrityViolationException failure) {
                OAuth2ThaoTac.xoaBangChung(session);
                flash.addFlashAttribute("error", "Không thể liên kết phương thức này với tài khoản.");
                return TRANG;
            }
        }
    }

    @PostMapping("/oauth/{provider}/unlink")
    String go(@PathVariable String provider, @AuthenticationPrincipal PhienNguoiDung principal,
              HttpServletRequest request, HttpServletResponse response, Authentication auth,
              RedirectAttributes flash) {
        var session = request.getSession();
        synchronized (WebUtils.getSessionMutex(session)) {
            var grant = grant(session, principal);
            if (grant != null) {
                try {
                    identities.goLienKet(principal.getMaNguoiDung(), principal.getPhienBanXacThuc(),
                        provider, grant.method(), providers());
                    return thoat(request, response, auth);
                } catch (AccessDeniedException denied) {
                    // The response is shared with expired or missing reauthentication.
                }
            }
            flash.addFlashAttribute("error", "Hãy xác thực lại bằng phương thức sẽ giữ sau khi gỡ.");
            return TRANG;
        }
    }

    @PostMapping("/password")
    String doiMatKhau(@Valid @ModelAttribute("form") MatKhauMoi_Form form, BindingResult errors,
                     @RequestParam(defaultValue = "") String matKhauHienTai,
                     @AuthenticationPrincipal PhienNguoiDung principal, HttpServletRequest request,
                     HttpServletResponse response, Authentication auth, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("error", "Mật khẩu cần ít nhất 12 ký tự và phải khớp với phần nhập lại. Nếu dùng nhiều chữ có dấu hoặc emoji, hãy thử rút ngắn mật khẩu.");
            return TRANG;
        }
        var session = request.getSession();
        synchronized (WebUtils.getSessionMutex(session)) {
            var grant = grant(session, principal);
            try {
                passwords.doi(principal.getMaNguoiDung(), principal.getPhienBanXacThuc(), matKhauHienTai,
                    form.getMatKhauMoi(), grant != null && providers().contains(grant.method()));
                return thoat(request, response, auth);
            } catch (AccessDeniedException denied) {
                flash.addFlashAttribute("error", "Hãy kiểm tra mật khẩu hiện tại hoặc xác minh lại bằng Google hay GitHub đã liên kết, rồi xác minh email.");
                return TRANG;
            }
        }
    }

    private Set<String> linked(PhienNguoiDung principal) {
        return Set.copyOf(identityRepository.docNhaCungCap(principal.getMaNguoiDung()));
    }

    private Set<String> providers() {
        var registrations = clients.getIfAvailable();
        return registrations == null ? Set.of() : Stream.of("google", "github")
            .filter(provider -> registrations.findByRegistrationId(provider) != null).collect(Collectors.toSet());
    }

    private static XacThucLai grant(HttpSession session, PhienNguoiDung principal) {
        var grant = (XacThucLai) session.getAttribute(GRANT);
        return grant != null && grant.hopLe(principal) ? grant : null;
    }

    private static void luuGrant(HttpSession session, PhienNguoiDung principal, String method) {
        session.setAttribute(GRANT, new XacThucLai(principal.getMaNguoiDung(),
            principal.getPhienBanXacThuc(), method, Instant.now().plusSeconds(300)));
    }

    private static String thoat(HttpServletRequest request, HttpServletResponse response, Authentication auth) {
        new SecurityContextLogoutHandler().logout(request, response, auth);
        return "redirect:/login?securityUpdated";
    }
}
