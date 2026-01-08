INSERT INTO admin(login_id, phone, name, role)
VALUES ('dyniiyeyo@naver.com', '010-4255-1027', '김도윤', 'ROLE_ADMIN');
INSERT INTO admin(login_id, phone, name, role)
VALUES ('hawon2k2k@naver.com', '010-2775-9804', '고하원', 'ROLE_ADMIN');
INSERT INTO admin(login_id, phone, name, role)
VALUES ('00parkyh@naver.com', '010-8318-8176', '박용헌', 'ROLE_ADMIN');
INSERT INTO admin(login_id, phone, name, role)
VALUES ('jwscape@naver.com', '010-9258-2658', '주장우', 'ROLE_ADMIN');

-- 상품/셋업 더미 데이터 (한국어 버전)
-- 실행 방법 (MySQL):
--   SOURCE src/main/resources/sql/livecommerce_create_table.sql;
--   SOURCE src/main/resources/sql/seed-products-setups.sql;

-- FK를 위한 최소 seller 더미
USE livecommerce;

INSERT INTO seller (seller_id, status, name, login_id, phone, profile, role, is_agreed)
VALUES
    (1, 'ACTIVE', '시드 셀러 A', 'seller-a@example.com', '010-1000-1000', NULL, 'ROLE_SELLER_OWNER', 1),
    (2, 'ACTIVE', '시드 셀러 B', 'seller-b@example.com', '010-2000-2000', NULL, 'ROLE_SELLER_OWNER', 1);

-- Products (30)
INSERT INTO product (
    product_id, seller_id, product_name, short_desc, detail_html,
    price, cost_price, status, stock_qty, safety_stock
)
VALUES
    (1, 1, '오크 데스크 120', '원목 감성의 120cm 컴팩트 책상', '<p>따뜻한 오크 톤 원목 책상(가로 120cm)입니다.</p>', 198000, 240000, 'ON_SALE', 20, 5),
    (2, 1, '메쉬 태스크 체어', '통풍 좋은 메쉬 의자', '<p>메쉬 등판, 높이 조절 가능.</p>', 89000, 110000, 'ON_SALE', 40, 5),
    (3, 1, 'LED 데스크 램프 네오', '미니멀 LED 스탠드', '<p>터치 디밍, 은은한 웜화이트.</p>', 39000, 45000, 'ON_SALE', 60, 5),
    (4, 1, '모니터 스탠드 프로', '시야를 올려주는 모니터 받침대', '<p>알루미늄 프레임 + 수납 공간.</p>', 49000, 59000, 'ON_SALE', 35, 5),
    (5, 1, '슬림 케이블 트레이', '책상 아래 케이블 정리 트레이', '<p>스틸 트레이, 간편 설치.</p>', 19000, 25000, 'ON_SALE', 80, 5),
    (6, 1, '펠트 데스크 매트', '부드러운 펠트 매트 80x40', '<p>그레이 펠트, 스티치 마감.</p>', 24000, 30000, 'ON_SALE', 70, 5),
    (7, 1, '미니 월 선반', '작은 공간용 벽 선반', '<p>내추럴 우드 마감.</p>', 32000, 39000, 'ON_SALE', 25, 5),
    (8, 1, '키보드 트레이', '슬라이드 키보드 서랍', '<p>부드러운 레일, 공간 절약.</p>', 45000, 52000, 'ON_SALE', 15, 5),
    (9, 1, '데스크 오거나이저 세트', '트레이 구성 정리 세트', '<p>문구/소품을 깔끔하게.</p>', 28000, 35000, 'ON_SALE', 55, 5),
    (10, 1, '에어 풋레스트', '각도/높이 조절 발받침', '<p>장시간 작업 시 다리 부담 완화.</p>', 36000, 42000, 'ON_SALE', 30, 5),

    (11, 2, '월넛 데스크 140', '넓은 140cm 월넛 책상', '<p>가로 140cm, 케이블 홀 포함.</p>', 248000, 290000, 'ON_SALE', 18, 5),
    (12, 2, '에르고 체어 플러스', '허리 지지 강화 인체공학 의자', '<p>요추 지지 + 헤드레스트.</p>', 159000, 199000, 'ON_SALE', 22, 5),
    (13, 2, '모니터 라이트 바', '눈부심 줄이는 라이트 바', '<p>모니터 위 설치, 간접 조명.</p>', 52000, 65000, 'ON_SALE', 45, 5),
    (14, 2, '클램프 데스크 서랍', '책상에 고정하는 숨은 서랍', '<p>하부 수납으로 책상 위를 깔끔하게.</p>', 42000, 52000, 'ON_SALE', 33, 5),
    (15, 2, '자석 케이블 클립', '자석식 케이블 클립 세트', '<p>자석 베이스, 6개 구성.</p>', 12000, 15000, 'ON_SALE', 120, 5),
    (16, 2, '스탠딩 매트', '장시간 서서 작업용 매트', '<p>피로 완화 쿠션 매트.</p>', 59000, 72000, 'ON_SALE', 28, 5),
    (17, 2, '모니터 암 라이트', '싱글 모니터 암', '<p>클램프 방식, VESA 100 호환.</p>', 68000, 82000, 'ON_SALE', 26, 5),
    (18, 2, '노트북 스탠드', '자세를 올려주는 노트북 거치대', '<p>알루미늄 바디, 통풍 설계.</p>', 33000, 40000, 'ON_SALE', 48, 5),
    (19, 2, '미니 데스크 시계', '심플 디지털 시계', '<p>은은한 LED 표시.</p>', 27000, 32000, 'ON_SALE', 52, 5),
    (20, 2, '세라믹 화분 2P', '화이트 톤 화분 세트', '<p>무광 화이트 마감, 2개 구성.</p>', 26000, 32000, 'ON_SALE', 44, 5),

    (21, 1, '클램프 램프 듀오', '듀얼 헤드 클램프 조명', '<p>유연한 암, 주광 LED.</p>', 78000, 92000, 'ON_SALE', 16, 5),
    (22, 1, '롱 마우스패드 XL', '키보드까지 커버하는 롱 패드', '<p>900mm 길이, 논슬립 베이스.</p>', 18000, 22000, 'ON_SALE', 65, 5),
    (23, 1, '데스크 흡음 패널', '작업 공간 소음/울림 완화', '<p>에코 감소, 집중력 향상.</p>', 69000, 85000, 'ON_SALE', 12, 5),
    (24, 1, '와이드 데스크 선반', '모니터/책 수납용 선반', '<p>상단 수납으로 공간 확보.</p>', 52000, 60000, 'ON_SALE', 19, 5),
    (25, 1, '알루미늄 펜 스탠드', '콤팩트 펜꽂이', '<p>작지만 안정감 있는 무게감.</p>', 14000, 18000, 'ON_SALE', 90, 5),
    (26, 2, 'USB 미니 데스크 팬', '조용한 미니 선풍기', '<p>3단 풍량, USB 전원.</p>', 25000, 30000, 'ON_SALE', 70, 5),
    (27, 2, 'USB 허브 7포트', '7포트 확장 허브', '<p>전원형, USB 3.0.</p>', 46000, 55000, 'ON_SALE', 34, 5),
    (28, 2, '대나무 모니터 스탠드', '내추럴 대나무 받침대', '<p>대나무 마감, 수납 공간.</p>', 39000, 47000, 'ON_SALE', 27, 5),
    (29, 2, '데스크 라이트 스트립', '분위기용 LED 스트립', '<p>USB 전원, 웜 라이트.</p>', 21000, 28000, 'ON_SALE', 58, 5),
    (30, 2, '투명 체어 매트', '바닥 보호용 의자 매트', '<p>하드 플로어용, 투명 타입.</p>', 43000, 52000, 'ON_SALE', 21, 5);

-- Setups (10)
INSERT INTO setup (
    setup_id, seller_id, setup_name, short_desc, tip_text, setup_image_url
)
VALUES
    (1, 1, '따뜻한 미니멀 데스크', '우드 톤으로 시작하는 기본 셋업', '웜톤 조명 하나면 분위기 완성.', 'https://cdn.example.com/setups/setup-1.jpg'),
    (2, 1, '콤팩트 집중 코너', '작은 공간을 집중 공간으로', '시야에 꼭 필요한 것만 남겨요.', 'https://cdn.example.com/setups/setup-2.jpg'),
    (3, 1, '나이트 워커 스테이션', '야근/새벽 작업을 위한 조명 셋업', '눈부심 줄이려면 웜화이트 추천.', 'https://cdn.example.com/setups/setup-3.jpg'),
    (4, 1, '크리에이티브 워크벤치', '도구와 소품이 많은 작업형 셋업', '케이블은 책상 아래로 숨기기.', 'https://cdn.example.com/setups/setup-4.jpg'),
    (5, 1, '케이블 제로 룩', '정리된 선 없는 책상', '트레이+클립으로 마감이 달라져요.', 'https://cdn.example.com/setups/setup-5.jpg'),
    (6, 2, '에르고 프로덕티비티', '자세부터 챙기는 생산성 셋업', '모니터는 눈높이에 맞추기.', 'https://cdn.example.com/setups/setup-6.jpg'),
    (7, 2, '브라이트 홈오피스', '밝고 가벼운 홈오피스', '뉴트럴 컬러로 통일하면 안정감.', 'https://cdn.example.com/setups/setup-7.jpg'),
    (8, 2, '스탠딩 루틴', '앉았다 서는 루틴 셋업', '매트 하나로 피로도가 달라져요.', 'https://cdn.example.com/setups/setup-8.jpg'),
    (9, 2, '테크 에센셜', '효율을 올리는 액세서리 셋업', '허브/클립은 손 닿는 곳에.', 'https://cdn.example.com/setups/setup-9.jpg'),
    (10, 2, '콰이어트 코너', '차분하게 정돈된 코너', '식물 하나로 온도가 올라가요.', 'https://cdn.example.com/setups/setup-10.jpg');

-- Setup-Product mapping (3 products per setup)
INSERT INTO setup_product (setup_id, product_id)
VALUES
    (1, 1), (1, 2), (1, 3),
    (2, 4), (2, 5), (2, 6),
    (3, 7), (3, 8), (3, 9),
    (4, 10), (4, 11), (4, 12),
    (5, 13), (5, 14), (5, 15),
    (6, 16), (6, 17), (6, 18),
    (7, 19), (7, 20), (7, 21),
    (8, 22), (8, 23), (8, 24),
    (9, 25), (9, 26), (9, 27),
    (10, 28), (10, 29), (10, 30);

-- Verification queries
SELECT COUNT(*) AS product_count FROM product;
SELECT COUNT(*) AS setup_count FROM setup;
SELECT COUNT(*) AS setup_product_count FROM setup_product;

SELECT sp.setup_id, s.setup_name, p.product_name
FROM setup_product sp
         JOIN setup s ON s.setup_id = sp.setup_id
         JOIN product p ON p.product_id = sp.product_id
ORDER BY sp.setup_id, sp.product_id
LIMIT 10;

SELECT * FROM seller WHERE seller_id = 3;

UPDATE seller SET status = 'ACTIVE' WHERE seller_id=3;