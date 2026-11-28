package com.beautyshop.integration;

import java.lang.annotation.*;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.test.context.jdbc.Sql;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Sql(statements = """
    INSERT INTO nguoi_dung (ma_nguoi_dung,email,mat_khau_bam,ho_ten,vai_tro)
    VALUES (9100001,'audit-fixture@example.invalid','disabled','Fixture','ADMIN')
    ON DUPLICATE KEY UPDATE ma_nguoi_dung=ma_nguoi_dung;
    """)
@WithUserDetails(value = "audit-fixture@example.invalid", setupBefore = TestExecutionEvent.TEST_EXECUTION)
@interface QuanTriFixture {}
