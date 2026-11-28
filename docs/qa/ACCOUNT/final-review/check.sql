SELECT version, success FROM flyway_schema_history WHERE version='10';
SELECT constraint_type, COUNT(*) AS constraints_found FROM information_schema.table_constraints
WHERE table_schema=DATABASE() AND table_name='thu_trang_thai_tai_khoan' GROUP BY constraint_type;
SELECT index_name, non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_order
FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='thu_trang_thai_tai_khoan'
GROUP BY index_name, non_unique ORDER BY index_name;
SELECT column_name, referenced_table_name, referenced_column_name FROM information_schema.key_column_usage
WHERE table_schema=DATABASE() AND table_name='thu_trang_thai_tai_khoan' AND referenced_table_name IS NOT NULL;
SELECT hanh_dong, ket_qua, ly_do, truoc, sau, COUNT(*) AS records_found
FROM nhat_ky_quan_tri WHERE hanh_dong IN ('ACCOUNT_LOCK','ACCOUNT_UNLOCK')
GROUP BY hanh_dong, ket_qua, ly_do, truoc, sau ORDER BY hanh_dong, ket_qua, ly_do;
SELECT trang_thai, so_lan_gui, COUNT(*) AS events_found FROM thu_trang_thai_tai_khoan
GROUP BY trang_thai, so_lan_gui ORDER BY trang_thai, so_lan_gui;
SELECT COUNT(*) AS success_without_event FROM nhat_ky_quan_tri a LEFT JOIN thu_trang_thai_tai_khoan t
ON a.correlation_id=t.ma_thao_tac WHERE a.hanh_dong IN ('ACCOUNT_LOCK','ACCOUNT_UNLOCK')
AND a.ket_qua='SUCCESS' AND t.ma_thu IS NULL;
SELECT COUNT(*) AS failed_with_event FROM nhat_ky_quan_tri a JOIN thu_trang_thai_tai_khoan t
ON a.correlation_id=t.ma_thao_tac WHERE a.hanh_dong IN ('ACCOUNT_LOCK','ACCOUNT_UNLOCK') AND a.ket_qua='FAILED';
