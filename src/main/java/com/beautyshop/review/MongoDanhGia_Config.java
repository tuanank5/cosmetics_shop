package com.beautyshop.review;

import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * P23: MongoDB tắt thì lỗi phải đến nhanh (vài giây) để trang vẫn render bản thiếu đánh giá thay vì treo.
 * Customizer chạy sau khi Spring Boot áp {@code spring.data.mongodb.uri}, nên các timeout dưới đây là giá trị cuối.
 */
@Configuration(proxyBeanMethods = false)
public class MongoDanhGia_Config {
    static final int CHON_SERVER_MS = 2_000;
    static final int KET_NOI_MS = 2_000;
    static final int DOC_MS = 5_000;

    @Bean
    MongoClientSettingsBuilderCustomizer thoiGianChoMongo() {
        return builder -> builder
            .applyToClusterSettings(c -> c.serverSelectionTimeout(CHON_SERVER_MS, TimeUnit.MILLISECONDS))
            .applyToSocketSettings(s -> s.connectTimeout(KET_NOI_MS, TimeUnit.MILLISECONDS)
                .readTimeout(DOC_MS, TimeUnit.MILLISECONDS));
    }
}
