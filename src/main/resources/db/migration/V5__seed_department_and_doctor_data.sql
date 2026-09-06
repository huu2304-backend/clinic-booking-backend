-- Seed dữ liệu: 15 khoa khám thật + 50 tài khoản bác sĩ TEST.
--
-- LƯU Ý QUAN TRỌNG: 50 account bên dưới là TEST DATA (email @cbs.local giả,
-- dùng CHUNG 1 password test "Doctor@123" đã BCrypt-hash sẵn). Migration này
-- là forward-only nên sẽ chạy ở MỌI môi trường áp dụng chuỗi migration này
-- (kể cả staging/prod sau này nếu dùng chung) — KHÔNG dùng các tài khoản này
-- cho môi trường thật mà không đổi password / xóa trước khi go-live.
--
-- Dùng JOIN theo natural key (email/name) thay vì hardcode id, vì id hiện có
-- trên từng DB có thể khác nhau tùy dữ liệu đã tồn tại trước đó.

-- 1) 15 khoa khám thật của phòng khám tổng quát
INSERT INTO department (name, description) VALUES
    ('Nội tổng quát', 'Khám và điều trị các bệnh lý nội khoa tổng quát'),
    ('Ngoại tổng quát', 'Phẫu thuật và điều trị ngoại khoa tổng quát'),
    ('Sản phụ khoa', 'Chăm sóc sức khỏe sinh sản, thai sản và phụ khoa'),
    ('Nhi khoa', 'Khám và điều trị bệnh cho trẻ em'),
    ('Tai Mũi Họng', 'Khám và điều trị các bệnh lý tai, mũi, họng'),
    ('Mắt', 'Khám và điều trị các bệnh lý về mắt'),
    ('Răng Hàm Mặt', 'Khám và điều trị các bệnh lý răng, hàm, mặt'),
    ('Da liễu', 'Khám và điều trị các bệnh lý về da'),
    ('Tim mạch', 'Khám và điều trị các bệnh lý tim mạch'),
    ('Thần kinh', 'Khám và điều trị các bệnh lý thần kinh'),
    ('Cơ Xương Khớp', 'Khám và điều trị các bệnh lý xương khớp'),
    ('Tiêu hóa', 'Khám và điều trị các bệnh lý tiêu hóa'),
    ('Hô hấp', 'Khám và điều trị các bệnh lý hô hấp'),
    ('Nội tiết', 'Khám và điều trị các bệnh lý nội tiết, đái tháo đường'),
    ('Tâm thần', 'Khám và điều trị các bệnh lý tâm thần, tâm lý');

-- 2) 50 account role=DOCTOR, status=ACTIVE, dùng chung 1 password_hash test
--    (plaintext test password: "Doctor@123", KHÔNG lưu plaintext trong migration)
INSERT INTO account (email, password_hash, role, status) VALUES
    ('bacsi001@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi002@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi003@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi004@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi005@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi006@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi007@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi008@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi009@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi010@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi011@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi012@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi013@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi014@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi015@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi016@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi017@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi018@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi019@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi020@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi021@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi022@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi023@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi024@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi025@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi026@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi027@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi028@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi029@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi030@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi031@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi032@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi033@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi034@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi035@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi036@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi037@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi038@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi039@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi040@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi041@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi042@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi043@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi044@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi045@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi046@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi047@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi048@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi049@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE'),
    ('bacsi050@cbs.local', '$2a$10$CJvxtqjOSATpPvFHcKTh4e4SKGHGqLjDUNYsAVyiF4DPIC8F5EpeK', 'DOCTOR', 'ACTIVE');

-- 3) 50 doctor_profile — JOIN theo email/name để lấy đúng id, không hardcode số.
--    Phân bổ: 5 khoa đầu 4 bác sĩ/khoa (20), 10 khoa còn lại 3 bác sĩ/khoa (30) = 50.
INSERT INTO doctor_profile (account_id, full_name, department_id)
SELECT a.id, v.full_name, d.id
FROM (VALUES
    ('bacsi001@cbs.local', 'Nguyễn Văn An', 'Nội tổng quát'),
    ('bacsi002@cbs.local', 'Trần Thị Bích', 'Nội tổng quát'),
    ('bacsi003@cbs.local', 'Lê Văn Cường', 'Nội tổng quát'),
    ('bacsi004@cbs.local', 'Phạm Thị Dung', 'Nội tổng quát'),
    ('bacsi005@cbs.local', 'Hoàng Văn Đức', 'Ngoại tổng quát'),
    ('bacsi006@cbs.local', 'Huỳnh Thị Em', 'Ngoại tổng quát'),
    ('bacsi007@cbs.local', 'Phan Văn Phong', 'Ngoại tổng quát'),
    ('bacsi008@cbs.local', 'Vũ Thị Giang', 'Ngoại tổng quát'),
    ('bacsi009@cbs.local', 'Võ Văn Hùng', 'Sản phụ khoa'),
    ('bacsi010@cbs.local', 'Đặng Thị Hoa', 'Sản phụ khoa'),
    ('bacsi011@cbs.local', 'Bùi Văn Khoa', 'Sản phụ khoa'),
    ('bacsi012@cbs.local', 'Đỗ Thị Lan', 'Sản phụ khoa'),
    ('bacsi013@cbs.local', 'Hồ Văn Minh', 'Nhi khoa'),
    ('bacsi014@cbs.local', 'Ngô Thị Nga', 'Nhi khoa'),
    ('bacsi015@cbs.local', 'Dương Văn Phúc', 'Nhi khoa'),
    ('bacsi016@cbs.local', 'Nguyễn Thị Quỳnh', 'Nhi khoa'),
    ('bacsi017@cbs.local', 'Trần Văn Sơn', 'Tai Mũi Họng'),
    ('bacsi018@cbs.local', 'Lê Thị Thảo', 'Tai Mũi Họng'),
    ('bacsi019@cbs.local', 'Phạm Văn Tài', 'Tai Mũi Họng'),
    ('bacsi020@cbs.local', 'Hoàng Thị Uyên', 'Tai Mũi Họng'),
    ('bacsi021@cbs.local', 'Huỳnh Văn Vinh', 'Mắt'),
    ('bacsi022@cbs.local', 'Phan Thị Xuân', 'Mắt'),
    ('bacsi023@cbs.local', 'Vũ Văn Yên', 'Mắt'),
    ('bacsi024@cbs.local', 'Võ Thị Ánh', 'Răng Hàm Mặt'),
    ('bacsi025@cbs.local', 'Đặng Văn Bảo', 'Răng Hàm Mặt'),
    ('bacsi026@cbs.local', 'Bùi Thị Cẩm', 'Răng Hàm Mặt'),
    ('bacsi027@cbs.local', 'Đỗ Văn Duy', 'Da liễu'),
    ('bacsi028@cbs.local', 'Hồ Thị Điệp', 'Da liễu'),
    ('bacsi029@cbs.local', 'Ngô Văn Được', 'Da liễu'),
    ('bacsi030@cbs.local', 'Dương Thị Hằng', 'Tim mạch'),
    ('bacsi031@cbs.local', 'Nguyễn Văn Hải', 'Tim mạch'),
    ('bacsi032@cbs.local', 'Trần Thị Hiền', 'Tim mạch'),
    ('bacsi033@cbs.local', 'Lê Văn Hòa', 'Thần kinh'),
    ('bacsi034@cbs.local', 'Phạm Thị Huệ', 'Thần kinh'),
    ('bacsi035@cbs.local', 'Hoàng Văn Huy', 'Thần kinh'),
    ('bacsi036@cbs.local', 'Huỳnh Thị Kim', 'Cơ Xương Khớp'),
    ('bacsi037@cbs.local', 'Phan Văn Long', 'Cơ Xương Khớp'),
    ('bacsi038@cbs.local', 'Vũ Thị Mai', 'Cơ Xương Khớp'),
    ('bacsi039@cbs.local', 'Võ Văn Nam', 'Tiêu hóa'),
    ('bacsi040@cbs.local', 'Đặng Thị Ngọc', 'Tiêu hóa'),
    ('bacsi041@cbs.local', 'Bùi Văn Nhân', 'Tiêu hóa'),
    ('bacsi042@cbs.local', 'Đỗ Thị Oanh', 'Hô hấp'),
    ('bacsi043@cbs.local', 'Hồ Văn Phát', 'Hô hấp'),
    ('bacsi044@cbs.local', 'Ngô Thị Phương', 'Hô hấp'),
    ('bacsi045@cbs.local', 'Dương Văn Quang', 'Nội tiết'),
    ('bacsi046@cbs.local', 'Nguyễn Thị Quyên', 'Nội tiết'),
    ('bacsi047@cbs.local', 'Trần Văn Thắng', 'Nội tiết'),
    ('bacsi048@cbs.local', 'Lê Thị Thu', 'Tâm thần'),
    ('bacsi049@cbs.local', 'Phạm Văn Tuấn', 'Tâm thần'),
    ('bacsi050@cbs.local', 'Hoàng Thị Vân', 'Tâm thần')
) AS v(email, full_name, department_name)
JOIN account a ON a.email = v.email
JOIN department d ON d.name = v.department_name;
