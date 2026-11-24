package com.beautyshop.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.assertThat;

class MomoSignatureTest {
    @Test
    void hmacSha256MatchesRfc4231Case2() {
        String expected = "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843";
        assertThat(MomoSignature.sign("Jefe", "what do ya want for nothing?")).isEqualTo(expected);
        assertThat(MomoSignature.verify("Jefe", "what do ya want for nothing?", expected)).isTrue();
        assertThat(MomoSignature.verify("Jefe", "wrong", expected)).isFalse();
        assertThat(MomoSignature.verify("Jefe", "wrong", "fg")).isFalse();
    }

    @Test
    void ipnCanonicalFieldOrderingMatchesCaptureWallet() {
        var ipn = new MomoSignature.Ipn("partner", "order", "attempt", 12345L, "BeautyShop", "momo_wallet",
            77L, 0, "Thành công", "webApp", 1200L, "", null);
        assertThat(ipn.raw("access")).isEqualTo("accessKey=access&amount=12345&extraData=&message=Thành công"
            + "&orderId=order&orderInfo=BeautyShop&orderType=momo_wallet&partnerCode=partner&payType=webApp"
            + "&requestId=attempt&responseTime=1200&resultCode=0&transId=77");
    }

    @Test
    void mockOnlyEnabledInDevOrTestWithoutProd() {
        var env = new MockEnvironment().withProperty("app.payment.momo.enabled", "true")
            .withProperty("app.payment.momo.mode", "mock");
        env.setActiveProfiles("dev");
        assertThat(new Momo_Settings(env, new ObjectMapper()).mock()).isTrue();
        env.setActiveProfiles("test");
        assertThat(new Momo_Settings(env, new ObjectMapper()).mock()).isTrue();
        env.setActiveProfiles("dev", "prod");
        assertThat(new Momo_Settings(env, new ObjectMapper()).enabled()).isFalse();
        env.setActiveProfiles("prod");
        assertThat(new Momo_Settings(env, new ObjectMapper()).enabled()).isFalse();
    }
}
