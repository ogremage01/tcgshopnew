-- order_infos 수령인 주소·전화·이메일 암호문 저장용 컬럼 길이 확장
-- MariaDB / MySQL 8+
-- recipient_name 은 평문(VARCHAR(255)) 유지. 신규 주문부터 3필드만 암호화 저장.

ALTER TABLE order_infos
    MODIFY COLUMN recipient_address VARCHAR(1024) NULL,
    MODIFY COLUMN recipient_phone VARCHAR(512) NULL,
    MODIFY COLUMN recipient_email VARCHAR(512) NULL;
