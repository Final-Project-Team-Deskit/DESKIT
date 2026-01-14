-- =========================================================
-- LIVE COMMERCE DUMMY DATA
-- =========================================================

USE livecommerce;

-- -------------------------
-- 0) forbidden_word (샘플)
-- -------------------------
INSERT INTO forbidden_word (word)
VALUES ('욕설1'),
       ('비방1'),
       ('광고1'),
       ('도배1');

-- -------------------------
-- 1) tag_category (4)
-- -------------------------
INSERT INTO tag_category (tag_code, tag_category_name)
VALUES ('SPACE', '공간'),
       ('TONE', '톤'),
       ('SITUATION', '상황'),
       ('MOOD', '무드');

-- -------------------------
-- 2) tag (카테고리당 5개 = 총 20)
-- tag_category_id: 1=SPACE, 2=TONE, 3=SITUATION, 4=MOOD
-- -------------------------
INSERT INTO tag (tag_category_id, tag_name)
VALUES (1, '데스크셋업'),
       (1, '홈스튜디오'),
       (1, '미니멀데스크'),
       (1, '게이밍데스크'),
       (1, '오피스셋업');

INSERT INTO tag (tag_category_id, tag_name)
VALUES (2, '미니멀'),
       (2, '모던'),
       (2, '빈티지'),
       (2, '하이테크'),
       (2, '웜톤');

INSERT INTO tag (tag_category_id, tag_name)
VALUES (3, '재택근무'),
       (3, '라이브방송'),
       (3, '편집작업'),
       (3, '스터디'),
       (3, '게임플레이');

INSERT INTO tag (tag_category_id, tag_name)
VALUES (4, '집중'),
       (4, '차분'),
       (4, '에너지'),
       (4, '감성'),
       (4, '프로페셔널');

-- -------------------------
-- 3) member (4명)
-- member_id: 1..4
-- -------------------------
INSERT INTO member (`name`, login_id, `profile`, phone, is_agreed, `status`, mbti, `role`, job_category)
VALUES ('박용헌', 'yheon.park@member.test', '더미 회원 프로필', '010-1000-0001', 1, 'ACTIVE', 'INTJ', 'ROLE_MEMBER',
        'CREATIVE_TYPE'),
       ('김도윤', 'doyun.kim@member.test', '더미 회원 프로필', '010-1000-0002', 1, 'ACTIVE', 'ISTJ', 'ROLE_MEMBER',
        'EDU_RES_TYPE'),
       ('주장우', 'jangwoo.joo@member.test', '더미 회원 프로필', '010-1000-0003', 1, 'ACTIVE', 'ENTJ', 'ROLE_MEMBER',
        'ADMIN_PLAN_TYPE'),
       ('고하원', 'hawon.go@member.test', '더미 회원 프로필', '010-1000-0004', 1, 'ACTIVE', 'INTP', 'ROLE_MEMBER',
        'FLEXIBLE_TYPE');

-- -------------------------
-- 4) address (회원당 1개)
-- address_id: 1..4
-- -------------------------
INSERT INTO address (member_id, receiver, postcode, addr_detail, is_default)
VALUES (1, '박용헌', '06236', '서울 강남구 테헤란로 1', 1),
       (2, '김도윤', '04158', '서울 마포구 양화로 2', 1),
       (3, '주장우', '03027', '서울 종로구 세종대로 3', 1),
       (4, '고하원', '06611', '서울 서초구 서초대로 4', 1);

-- -------------------------
-- 5) seller (4명)
-- seller_id: 1..4
-- -------------------------
INSERT INTO seller (`status`, `name`, login_id, phone, `profile`, `role`, is_agreed)
VALUES ('ACTIVE', '박용헌', 'yheon.park@seller.test', '010-2000-0001', '더미 판매자 프로필', 'ROLE_SELLER_OWNER', 1),
       ('ACTIVE', '김도윤', 'doyun.kim@seller.test', '010-2000-0002', '더미 판매자 프로필', 'ROLE_SELLER_OWNER', 1),
       ('ACTIVE', '주장우', 'jangwoo.joo@seller.test', '010-2000-0003', '더미 판매자 프로필', 'ROLE_SELLER_OWNER', 1),
       ('ACTIVE', '고하원', 'hawon.go@seller.test', '010-2000-0004', '더미 판매자 프로필', 'ROLE_SELLER_OWNER', 1);

-- -------------------------
-- 6) admin (4명)
-- admin_id: 1..4
-- -------------------------
INSERT INTO admin (login_id, phone, `name`, `role`)
VALUES ('00parkyh@naver.com', '010-8318-8176', '박용헌', 'ROLE_ADMIN'),
       ('dyniiyeyo@naver.com', '010-4255-1027', '김도윤', 'ROLE_ADMIN'),
       ('jwscape@naver.com', '010-9258-2658', '주장우', 'ROLE_ADMIN'),
       ('hawon2k2k@naver.com', '010-2775-9804', '고하원', 'ROLE_ADMIN');

-- =========================================================
-- 7) product (23개) - 4개씩 배치(마지막 3개)
-- seller_id: 1=박용헌, 2=김도윤, 3=주장우, 4=고하원
-- =========================================================

-- Batch 1
INSERT INTO product (seller_id, product_name, short_desc, detail_html, price, cost_price, `status`, stock_qty,
                     safety_stock, deleted_at)
VALUES (1, '로지텍 G502x LightSpeed', '무선 마우스. 데스크 셋업/게임/업무에 모두 어울리는 구성.', '<h1>로지텍 G502x LightSpeed</h1><p>더미 상세입니다.</p>',
        83000, 95450, 'ON_SALE', 15, 5, NULL),
       (1, '다얼유 A87Pro', '텐키리스 키보드. 컴팩트 셋업에 적합한 기본 선택.', '<h1>다얼유 A87Pro</h1><p>더미 상세입니다.</p>', 100000, 115000,
        'ON_SALE', 12, 5, NULL),
       (1, '로지텍 G913', '로우프로파일 무선 키보드. 깔끔한 타건감과 디자인.', '<h1>로지텍 G913</h1><p>더미 상세입니다.</p>', 269000, 301280, 'ON_SALE',
        8, 5, NULL),
       (1, '로지텍 G733 LIGHTSPEED', '무선 헤드셋. 장시간 사용 환경을 고려한 제품.', '<h1>로지텍 G733</h1><p>더미 상세입니다.</p>', 137780, 158450,
        'ON_SALE', 6, 5, NULL);

-- Batch 2
INSERT INTO product (seller_id, product_name, short_desc, detail_html, price, cost_price, `status`, stock_qty,
                     safety_stock, deleted_at)
VALUES (1, '가죽 마우스장패드 대형', '대형 데스크 패드. 책상 보호와 정리감에 도움.', '<h1>가죽 마우스장패드 대형</h1><p>더미 상세입니다.</p>', 15000, 18000,
        'ON_SALE', 40, 5, NULL),
       (1, 'OS퍼니처 실리콘 게이밍의자', '게이밍 의자. 장시간 착석 환경을 위한 구성.', '<h1>OS퍼니처 실리콘 게이밍의자</h1><p>더미 상세입니다.</p>', 372000, 416640,
        'LIMITED_SALE', 3, 5, NULL),
       (1, '다이소 나무 모니터 받침대', '모니터 높이 보정용 받침대. 가성비 아이템.', '<h1>다이소 나무 모니터 받침대</h1><p>더미 상세입니다.</p>', 5000, 0, 'ON_SALE',
        25, 5, NULL),
       (2, '삼성전자 24T452 24인치 모니터', '24인치 모니터. 업무/학습용 기본 구성에 적합.', '<h1>삼성전자 24T452</h1><p>더미 상세입니다.</p>', 92700, 106610,
        'ON_SALE', 10, 5, NULL);

-- Batch 3
INSERT INTO product (seller_id, product_name, short_desc, detail_html, price, cost_price, `status`, stock_qty,
                     safety_stock, deleted_at)
VALUES (2, '레이니75 키보드 WOB 블랙 바이올렛축', '75배열 키보드. 컬러/스위치 조합이 특징.', '<h1>레이니75</h1><p>더미 상세입니다.</p>', 94900, 109140,
        'ON_SALE', 14, 5, NULL),
       (2, '큐데스크 키슬롯 수납형 손목받침대', '수납형 손목받침대. 키보드 사용 피로도 완화.', '<h1>큐데스크 손목받침대</h1><p>더미 상세입니다.</p>', 59000, 67850,
        'ON_SALE', 20, 5, NULL),
       (3, '엔커 맥고 3in1 접이식 무선충전 스테이션', '3in1 무선충전. 데스크 케이블 정리 최적화.', '<h1>엔커 3in1</h1><p>더미 상세입니다.</p>', 89000, 102350,
        'ON_SALE', 18, 5, NULL),
       (3, '시디즈 T20 탭플러스 블랙 메쉬의자 TNA200HF', '메쉬 의자. 통기성과 장시간 착석을 고려한 구성.', '<h1>시디즈 T20</h1><p>더미 상세입니다.</p>', 199000,
        228850, 'ON_SALE', 7, 5, NULL);

-- Batch 4
INSERT INTO product (seller_id, product_name, short_desc, detail_html, price, cost_price, `status`, stock_qty,
                     safety_stock, deleted_at)
VALUES (3, '삼나무 원목 피톤치드향 모니터 받침대', '원목 받침대. 수납/정리형 데스크 구성에 적합.', '<h1>삼나무 원목 받침대</h1><p>더미 상세입니다.</p>', 6290, 7860,
        'ON_SALE', 22, 5, NULL),
       (3, 'Wavebone Grand Speaker Stand(Wood, Pair)', '우드 스피커 스탠드(1조). 오디오 정렬/각도 최적화.',
        '<h1>Wavebone Stand</h1><p>더미 상세입니다.</p>', 330000, 369600, 'READY', 4, 5, NULL),
       (3, 'Neuman NDH20 Black', '모니터링 헤드셋. 스튜디오/편집 환경용.', '<h1>Neuman NDH20</h1><p>더미 상세입니다.</p>', 961000, 1057100,
        'READY', 2, 5, NULL),
       (3, '현대일렉트릭 블랙 알루미늄 멀티탭 8구 HDSVAL-825', '8구 멀티탭. 전원 구성/정리에 최적화.', '<h1>현대일렉트릭 8구</h1><p>더미 상세입니다.</p>', 32000,
        38400, 'ON_SALE', 30, 5, NULL);

-- Batch 5
INSERT INTO product (seller_id, product_name, short_desc, detail_html, price, cost_price, `status`, stock_qty,
                     safety_stock, deleted_at)
VALUES (3, '로지텍 버티컬 LIFT 마우스 왼손용 910-007321', '인체공학 버티컬 마우스(왼손). 장시간 작업용.', '<h1>로지텍 LIFT(왼손)</h1><p>더미 상세입니다.</p>',
        89900, 103390, 'ON_SALE', 12, 5, NULL),
       (3, 'Ekeepment 키보드 손목 받침대 호두나무 월넛 팜레스트 텐키리스 배열', '월넛 팜레스트. 텐키리스 키보드용.',
        '<h1>Ekeepment 월넛 팜레스트</h1><p>더미 상세입니다.</p>', 15290, 18350, 'ON_SALE', 35, 5, NULL),
       (3, 'WOB CRUSH80 REBOOT 크러쉬 80 리부트', '80배열 기반 커스텀 키보드 더미 상품.', '<h1>CRUSH80 REBOOT</h1><p>더미 상세입니다.</p>', 139000,
        159850, 'ON_SALE', 9, 5, NULL),
       (3, 'PRESONUS Eris E3.5 Gen2 스튜디오 모니터 스피커 1조 3.5인치', '3.5인치 모니터 스피커(1조). 데스크 오디오 입문용.',
        '<h1>PRESONUS E3.5</h1><p>더미 상세입니다.</p>', 155000, 178250, 'ON_SALE', 6, 5, NULL);

-- Batch 6 (3)
INSERT INTO product (seller_id, product_name, short_desc, detail_html, price, cost_price, `status`, stock_qty,
                     safety_stock, deleted_at)
VALUES (4, 'Tago Studio T3-01', '레퍼런스 성향 헤드폰. 모니터링/감상 환경용.', '<h1>Tago Studio T3-01</h1><p>더미 상세입니다.</p>', 900000,
        990000, 'READY', 2, 5, NULL),
       (4, 'Resident Audio M5 레지던트오디오 엠파이브 5인치 액티브 모니터 스피커 (2통/1조 국내정식수입품)', '5인치 액티브 모니터 스피커(1조). 홈스튜디오용.',
        '<h1>Resident Audio M5</h1><p>더미 상세입니다.</p>', 560000, 627200, 'ON_SALE', 3, 5, NULL),
       (4, 'STEINBERG UR44 USB 오디오인터페이스', '오디오 인터페이스. 홈레코딩/스트리밍 구성용.', '<h1>STEINBERG UR44</h1><p>더미 상세입니다.</p>',
        552000, 618240, 'ON_SALE', 3, 5, NULL);

-- -------------------------
-- 8) product_image (상품당 1개 THUMBNAIL)
-- product_id: 1..23
-- -------------------------
INSERT INTO product_image (product_id, product_image_url, stored_file_name, image_type, slot_index)
VALUES (1, 'https://img.test/p/1-thumb.jpg', 'p/1/thumb.jpg', 'THUMBNAIL', 0),
       (2, 'https://img.test/p/2-thumb.jpg', 'p/2/thumb.jpg', 'THUMBNAIL', 0),
       (3, 'https://img.test/p/3-thumb.jpg', 'p/3/thumb.jpg', 'THUMBNAIL', 0),
       (4, 'https://img.test/p/4-thumb.jpg', 'p/4/thumb.jpg', 'THUMBNAIL', 0);

INSERT INTO product_image (product_id, product_image_url, stored_file_name, image_type, slot_index)
VALUES (5, 'https://img.test/p/5-thumb.jpg', 'p/5/thumb.jpg', 'THUMBNAIL', 0),
       (6, 'https://img.test/p/6-thumb.jpg', 'p/6/thumb.jpg', 'THUMBNAIL', 0),
       (7, 'https://img.test/p/7-thumb.jpg', 'p/7/thumb.jpg', 'THUMBNAIL', 0),
       (8, 'https://img.test/p/8-thumb.jpg', 'p/8/thumb.jpg', 'THUMBNAIL', 0);

INSERT INTO product_image (product_id, product_image_url, stored_file_name, image_type, slot_index)
VALUES (9, 'https://img.test/p/9-thumb.jpg', 'p/9/thumb.jpg', 'THUMBNAIL', 0),
       (10, 'https://img.test/p/10-thumb.jpg', 'p/10/thumb.jpg', 'THUMBNAIL', 0),
       (11, 'https://img.test/p/11-thumb.jpg', 'p/11/thumb.jpg', 'THUMBNAIL', 0),
       (12, 'https://img.test/p/12-thumb.jpg', 'p/12/thumb.jpg', 'THUMBNAIL', 0);

INSERT INTO product_image (product_id, product_image_url, stored_file_name, image_type, slot_index)
VALUES (13, 'https://img.test/p/13-thumb.jpg', 'p/13/thumb.jpg', 'THUMBNAIL', 0),
       (14, 'https://img.test/p/14-thumb.jpg', 'p/14/thumb.jpg', 'THUMBNAIL', 0),
       (15, 'https://img.test/p/15-thumb.jpg', 'p/15/thumb.jpg', 'THUMBNAIL', 0),
       (16, 'https://img.test/p/16-thumb.jpg', 'p/16/thumb.jpg', 'THUMBNAIL', 0);

INSERT INTO product_image (product_id, product_image_url, stored_file_name, image_type, slot_index)
VALUES (17, 'https://img.test/p/17-thumb.jpg', 'p/17/thumb.jpg', 'THUMBNAIL', 0),
       (18, 'https://img.test/p/18-thumb.jpg', 'p/18/thumb.jpg', 'THUMBNAIL', 0),
       (19, 'https://img.test/p/19-thumb.jpg', 'p/19/thumb.jpg', 'THUMBNAIL', 0),
       (20, 'https://img.test/p/20-thumb.jpg', 'p/20/thumb.jpg', 'THUMBNAIL', 0);

INSERT INTO product_image (product_id, product_image_url, stored_file_name, image_type, slot_index)
VALUES (21, 'https://img.test/p/21-thumb.jpg', 'p/21/thumb.jpg', 'THUMBNAIL', 0),
       (22, 'https://img.test/p/22-thumb.jpg', 'p/22/thumb.jpg', 'THUMBNAIL', 0),
       (23, 'https://img.test/p/23-thumb.jpg', 'p/23/thumb.jpg', 'THUMBNAIL', 0);

-- -------------------------
-- 9) product_tag (상품-태그 매핑: 샘플로 다양하게)
-- tag_id: 1..20
-- -------------------------
INSERT INTO product_tag (product_id, tag_id)
VALUES (1, 1),
       (1, 6),
       (1, 11),
       (1, 16);

INSERT INTO product_tag (product_id, tag_id)
VALUES (2, 1),
       (2, 7),
       (2, 14),
       (2, 17);

INSERT INTO product_tag (product_id, tag_id)
VALUES (3, 2),
       (3, 9),
       (3, 12),
       (3, 20);

INSERT INTO product_tag (product_id, tag_id)
VALUES (4, 4),
       (4, 8),
       (4, 15),
       (4, 18);

INSERT INTO product_tag (product_id, tag_id)
VALUES (5, 3),
       (6, 5),
       (7, 1),
       (8, 2);

INSERT INTO product_tag (product_id, tag_id)
VALUES (9, 4),
       (10, 6),
       (11, 7),
       (12, 8);

INSERT INTO product_tag (product_id, tag_id)
VALUES (13, 9),
       (14, 10),
       (15, 11),
       (16, 12);

INSERT INTO product_tag (product_id, tag_id)
VALUES (17, 13),
       (18, 14),
       (19, 15),
       (20, 16);

INSERT INTO product_tag (product_id, tag_id)
VALUES (21, 17),
       (22, 18),
       (23, 19);

-- -------------------------
-- 10) setup (판매자당 2개 = 총 8)
-- setup_id: 1..8
-- -------------------------
INSERT INTO setup (seller_id, setup_name, short_desc, tip_text, setup_image_url)
VALUES (1, 'YH 미니멀 데스크', '업무 집중을 위한 미니멀 셋업', '케이블 정리로 시야를 확보하세요.', 'https://img.test/s/1.jpg'),
       (1, 'YH 게이밍 데스크', '게임/업무 겸용 셋업', '손목 받침대로 피로도를 줄이세요.', 'https://img.test/s/2.jpg'),
       (2, 'DY 듀얼모니터 셋업', '멀티태스킹 최적화 셋업', '모니터 높이를 눈높이에 맞추세요.', 'https://img.test/s/3.jpg'),
       (2, 'DY 스터디 셋업', '학습용 집중 셋업', '조명 톤을 일정하게 유지하세요.', 'https://img.test/s/4.jpg');

INSERT INTO setup (seller_id, setup_name, short_desc, tip_text, setup_image_url)
VALUES (3, 'JW 홈스튜디오', '오디오 기반 홈스튜디오 셋업', '스피커 위치를 좌우 대칭으로 배치하세요.', 'https://img.test/s/5.jpg'),
       (3, 'JW 라이브 방송 셋업', '라이브 커머스 방송용 셋업', '멀티탭은 테이블 아래로 숨기세요.', 'https://img.test/s/6.jpg'),
       (4, 'HW 레코딩 셋업', '레코딩/편집을 위한 셋업', '입출력 장비는 라벨링해두세요.', 'https://img.test/s/7.jpg'),
       (4, 'HW 감상 셋업', '음악 감상을 위한 셋업', '청취 위치를 고정해 비교 청취하세요.', 'https://img.test/s/8.jpg');

-- -------------------------
-- 11) setup_tag (셋업-태그 매핑: 셋업당 3개)
-- -------------------------
INSERT INTO setup_tag (setup_id, tag_id)
VALUES (1, 1),
       (1, 6),
       (1, 16),
       (2, 4);

INSERT INTO setup_tag (setup_id, tag_id)
VALUES (2, 8),
       (2, 15),
       (2, 18),
       (3, 2);

INSERT INTO setup_tag (setup_id, tag_id)
VALUES (3, 11),
       (3, 17),
       (3, 20),
       (4, 3);

INSERT INTO setup_tag (setup_id, tag_id)
VALUES (4, 14),
       (4, 16),
       (4, 19),
       (5, 2);

INSERT INTO setup_tag (setup_id, tag_id)
VALUES (5, 12),
       (5, 13),
       (5, 20),
       (6, 4);

INSERT INTO setup_tag (setup_id, tag_id)
VALUES (6, 11),
       (6, 18),
       (6, 15),
       (7, 2);

INSERT INTO setup_tag (setup_id, tag_id)
VALUES (7, 12),
       (7, 14),
       (7, 16),
       (8, 3);

INSERT INTO setup_tag (setup_id, tag_id)
VALUES (8, 9),
       (8, 10),
       (8, 20);

-- -------------------------
-- 12) setup_product (셋업-상품 매핑: 셋업당 3개)
-- -------------------------
INSERT INTO setup_product (setup_id, product_id)
VALUES (1, 1),
       (1, 5),
       (1, 7),
       (2, 2);

INSERT INTO setup_product (setup_id, product_id)
VALUES (2, 3),
       (2, 4),
       (2, 6),
       (3, 8);

INSERT INTO setup_product (setup_id, product_id)
VALUES (3, 9),
       (3, 10),
       (3, 11),
       (4, 8);

INSERT INTO setup_product (setup_id, product_id)
VALUES (4, 7),
       (4, 5),
       (4, 11),
       (5, 12);

INSERT INTO setup_product (setup_id, product_id)
VALUES (5, 13),
       (5, 14),
       (5, 15),
       (6, 16);

INSERT INTO setup_product (setup_id, product_id)
VALUES (6, 17),
       (6, 18),
       (6, 12),
       (7, 21);

INSERT INTO setup_product (setup_id, product_id)
VALUES (7, 22),
       (7, 23),
       (7, 19),
       (8, 21);

INSERT INTO setup_product (setup_id, product_id)
VALUES (8, 20),
       (8, 19),
       (8, 18);

-- =========================================================
-- 13) order (회원당 10개 = 총 40개)
-- - 단순화를 위해: 주문당 order_item 1개, quantity=1
-- - total_product_amount = order_amount = 상품 가격
-- - shipping_fee=0, discount_fee=0
-- status는 혼합(대부분 PAID, 일부 COMPLETED/CANCELLED)
-- =========================================================

-- member_id=1 (order_id 1~10)
INSERT INTO `order` (member_id, addr_detail, order_number, total_product_amount, shipping_fee, discount_fee,
                     order_amount, `status`, cancel_reason, paid_at, cancelled_at)
VALUES (1, '서울 강남구 테헤란로 1', 'ORD-000001', 83000, 0, 0, 83000, 'PAID', NULL, NOW(), NULL),
       (1, '서울 강남구 테헤란로 1', 'ORD-000002', 100000, 0, 0, 100000, 'PAID', NULL, NOW(), NULL),
       (1, '서울 강남구 테헤란로 1', 'ORD-000003', 269000, 0, 0, 269000, 'COMPLETED', NULL, NOW(), NULL),
       (1, '서울 강남구 테헤란로 1', 'ORD-000004', 137780, 0, 0, 137780, 'PAID', NULL, NOW(), NULL);

INSERT INTO `order` (member_id, addr_detail, order_number, total_product_amount, shipping_fee, discount_fee,
                     order_amount, `status`, cancel_reason, paid_at, cancelled_at)
VALUES (1, '서울 강남구 테헤란로 1', 'ORD-000005', 15000, 0, 0, 15000, 'PAID', NULL, NOW(), NULL),
       (1, '서울 강남구 테헤란로 1', 'ORD-000006', 372000, 0, 0, 372000, 'PAID', NULL, NOW(), NULL),
       (1, '서울 강남구 테헤란로 1', 'ORD-000007', 5000, 0, 0, 5000, 'CANCELLED', '단순 변심', NULL, NOW()),
       (1, '서울 강남구 테헤란로 1', 'ORD-000008', 92700, 0, 0, 92700, 'PAID', NULL, NOW(), NULL);

INSERT INTO `order` (member_id, addr_detail, order_number, total_product_amount, shipping_fee, discount_fee,
                     order_amount, `status`, cancel_reason, paid_at, cancelled_at)
VALUES (1, '서울 강남구 테헤란로 1', 'ORD-000009', 94900, 0, 0, 94900, 'PAID', NULL, NOW(), NULL),
       (1, '서울 강남구 테헤란로 1', 'ORD-000010', 59000, 0, 0, 59000, 'COMPLETED', NULL, NOW(), NULL);

-- member_id=2 (order_id 11~20)
INSERT INTO `order` (member_id, addr_detail, order_number, total_product_amount, shipping_fee, discount_fee,
                     order_amount, `status`, cancel_reason, paid_at, cancelled_at)
VALUES (2, '서울 마포구 양화로 2', 'ORD-000011', 89000, 0, 0, 89000, 'PAID', NULL, NOW(), NULL),
       (2, '서울 마포구 양화로 2', 'ORD-000012', 199000, 0, 0, 199000, 'PAID', NULL, NOW(), NULL),
       (2, '서울 마포구 양화로 2', 'ORD-000013', 6290, 0, 0, 6290, 'COMPLETED', NULL, NOW(), NULL),
       (2, '서울 마포구 양화로 2', 'ORD-000014', 330000, 0, 0, 330000, 'PAID', NULL, NOW(), NULL);

INSERT INTO `order` (member_id, addr_detail, order_number, total_product_amount, shipping_fee, discount_fee,
                     order_amount, `status`, cancel_reason, paid_at, cancelled_at)
VALUES (2, '서울 마포구 양화로 2', 'ORD-000015', 961000, 0, 0, 961000, 'PAID', NULL, NOW(), NULL),
       (2, '서울 마포구 양화로 2', 'ORD-000016', 32000, 0, 0, 32000, 'CANCELLED', '배송지 변경', NULL, NOW()),
       (2, '서울 마포구 양화로 2', 'ORD-000017', 89900, 0, 0, 89900, 'PAID', NULL, NOW(), NULL),
       (2, '서울 마포구 양화로 2', 'ORD-000018', 15290, 0, 0, 15290, 'PAID', NULL, NOW(), NULL);

INSERT INTO `order` (member_id, addr_detail, order_number, total_product_amount, shipping_fee, discount_fee,
                     order_amount, `status`, cancel_reason, paid_at, cancelled_at)
VALUES (2, '서울 마포구 양화로 2', 'ORD-000019', 139000, 0, 0, 139000, 'COMPLETED', NULL, NOW(), NULL),
       (2, '서울 마포구 양화로 2', 'ORD-000020', 155000, 0, 0, 155000, 'PAID', NULL, NOW(), NULL);

-- member_id=3 (order_id 21~30)
INSERT INTO `order` (member_id, addr_detail, order_number, total_product_amount, shipping_fee, discount_fee,
                     order_amount, `status`, cancel_reason, paid_at, cancelled_at)
VALUES (3, '서울 종로구 세종대로 3', 'ORD-000021', 900000, 0, 0, 900000, 'PAID', NULL, NOW(), NULL),
       (3, '서울 종로구 세종대로 3', 'ORD-000022', 560000, 0, 0, 560000, 'PAID', NULL, NOW(), NULL),
       (3, '서울 종로구 세종대로 3', 'ORD-000023', 552000, 0, 0, 552000, 'COMPLETED', NULL, NOW(), NULL),
       (3, '서울 종로구 세종대로 3', 'ORD-000024', 83000, 0, 0, 83000, 'PAID', NULL, NOW(), NULL);

INSERT INTO `order` (member_id, addr_detail, order_number, total_product_amount, shipping_fee, discount_fee,
                     order_amount, `status`, cancel_reason, paid_at, cancelled_at)
VALUES (3, '서울 종로구 세종대로 3', 'ORD-000025', 100000, 0, 0, 100000, 'PAID', NULL, NOW(), NULL),
       (3, '서울 종로구 세종대로 3', 'ORD-000026', 269000, 0, 0, 269000, 'CANCELLED', '단순 변심', NULL, NOW()),
       (3, '서울 종로구 세종대로 3', 'ORD-000027', 137780, 0, 0, 137780, 'PAID', NULL, NOW(), NULL),
       (3, '서울 종로구 세종대로 3', 'ORD-000028', 15000, 0, 0, 15000, 'PAID', NULL, NOW(), NULL);

INSERT INTO `order` (member_id, addr_detail, order_number, total_product_amount, shipping_fee, discount_fee,
                     order_amount, `status`, cancel_reason, paid_at, cancelled_at)
VALUES (3, '서울 종로구 세종대로 3', 'ORD-000029', 372000, 0, 0, 372000, 'COMPLETED', NULL, NOW(), NULL),
       (3, '서울 종로구 세종대로 3', 'ORD-000030', 5000, 0, 0, 5000, 'PAID', NULL, NOW(), NULL);

-- member_id=4 (order_id 31~40)
INSERT INTO `order` (member_id, addr_detail, order_number, total_product_amount, shipping_fee, discount_fee,
                     order_amount, `status`, cancel_reason, paid_at, cancelled_at)
VALUES (4, '서울 서초구 서초대로 4', 'ORD-000031', 92700, 0, 0, 92700, 'PAID', NULL, NOW(), NULL),
       (4, '서울 서초구 서초대로 4', 'ORD-000032', 94900, 0, 0, 94900, 'PAID', NULL, NOW(), NULL),
       (4, '서울 서초구 서초대로 4', 'ORD-000033', 59000, 0, 0, 59000, 'COMPLETED', NULL, NOW(), NULL),
       (4, '서울 서초구 서초대로 4', 'ORD-000034', 89000, 0, 0, 89000, 'PAID', NULL, NOW(), NULL);

INSERT INTO `order` (member_id, addr_detail, order_number, total_product_amount, shipping_fee, discount_fee,
                     order_amount, `status`, cancel_reason, paid_at, cancelled_at)
VALUES (4, '서울 서초구 서초대로 4', 'ORD-000035', 199000, 0, 0, 199000, 'PAID', NULL, NOW(), NULL),
       (4, '서울 서초구 서초대로 4', 'ORD-000036', 6290, 0, 0, 6290, 'CANCELLED', '재고 부족', NULL, NOW()),
       (4, '서울 서초구 서초대로 4', 'ORD-000037', 330000, 0, 0, 330000, 'PAID', NULL, NOW(), NULL),
       (4, '서울 서초구 서초대로 4', 'ORD-000038', 961000, 0, 0, 961000, 'PAID', NULL, NOW(), NULL);

INSERT INTO `order` (member_id, addr_detail, order_number, total_product_amount, shipping_fee, discount_fee,
                     order_amount, `status`, cancel_reason, paid_at, cancelled_at)
VALUES (4, '서울 서초구 서초대로 4', 'ORD-000039', 32000, 0, 0, 32000, 'COMPLETED', NULL, NOW(), NULL),
       (4, '서울 서초구 서초대로 4', 'ORD-000040', 89900, 0, 0, 89900, 'PAID', NULL, NOW(), NULL);

-- -------------------------
-- 14) order_item (주문당 1개, 총 40개)
-- order_id 1..40 / product_id를 순환 배치
-- seller_id는 해당 product의 seller를 맞춰 스냅샷 구성
-- -------------------------

-- order_id 1~4
INSERT INTO order_item (order_id, product_id, seller_id, product_name, unit_price, quantity, subtotal_price)
VALUES (1, 1, 1, '로지텍 G502x LightSpeed', 83000, 1, 83000),
       (2, 2, 1, '다얼유 A87Pro', 100000, 1, 100000),
       (3, 3, 1, '로지텍 G913', 269000, 1, 269000),
       (4, 4, 1, '로지텍 G733 LIGHTSPEED', 137780, 1, 137780);

-- order_id 5~8
INSERT INTO order_item (order_id, product_id, seller_id, product_name, unit_price, quantity, subtotal_price)
VALUES (5, 5, 1, '가죽 마우스장패드 대형', 15000, 1, 15000),
       (6, 6, 1, 'OS퍼니처 실리콘 게이밍의자', 372000, 1, 372000),
       (7, 7, 1, '다이소 나무 모니터 받침대', 5000, 1, 5000),
       (8, 8, 2, '삼성전자 24T452 24인치 모니터', 92700, 1, 92700);

-- order_id 9~12
INSERT INTO order_item (order_id, product_id, seller_id, product_name, unit_price, quantity, subtotal_price)
VALUES (9, 9, 2, '레이니75 키보드 WOB 블랙 바이올렛축', 94900, 1, 94900),
       (10, 10, 2, '큐데스크 키슬롯 수납형 손목받침대', 59000, 1, 59000),
       (11, 11, 3, '엔커 맥고 3in1 접이식 무선충전 스테이션', 89000, 1, 89000),
       (12, 12, 3, '시디즈 T20 탭플러스 블랙 메쉬의자 TNA200HF', 199000, 1, 199000);

-- order_id 13~16
INSERT INTO order_item (order_id, product_id, seller_id, product_name, unit_price, quantity, subtotal_price)
VALUES (13, 13, 3, '삼나무 원목 피톤치드향 모니터 받침대', 6290, 1, 6290),
       (14, 14, 3, 'Wavebone Grand Speaker Stand(Wood, Pair)', 330000, 1, 330000),
       (15, 15, 3, 'Neuman NDH20 Black', 961000, 1, 961000),
       (16, 16, 3, '현대일렉트릭 블랙 알루미늄 멀티탭 8구 HDSVAL-825', 32000, 1, 32000);

-- order_id 17~20
INSERT INTO order_item (order_id, product_id, seller_id, product_name, unit_price, quantity, subtotal_price)
VALUES (17, 17, 3, '로지텍 버티컬 LIFT 마우스 왼손용 910-007321', 89900, 1, 89900),
       (18, 18, 3, 'Ekeepment 키보드 손목 받침대 호두나무 월넛 팜레스트 텐키리스 배열', 15290, 1, 15290),
       (19, 19, 3, 'WOB CRUSH80 REBOOT 크러쉬 80 리부트', 139000, 1, 139000),
       (20, 20, 3, 'PRESONUS Eris E3.5 Gen2 스튜디오 모니터 스피커 1조 3.5인치', 155000, 1, 155000);

-- order_id 21~24
INSERT INTO order_item (order_id, product_id, seller_id, product_name, unit_price, quantity, subtotal_price)
VALUES (21, 21, 4, 'Tago Studio T3-01', 900000, 1, 900000),
       (22, 22, 4, 'Resident Audio M5 레지던트오디오 엠파이브 5인치 액티브 모니터 스피커 (2통/1조 국내정식수입품)', 560000, 1, 560000),
       (23, 23, 4, 'STEINBERG UR44 USB 오디오인터페이스', 552000, 1, 552000),
       (24, 1, 1, '로지텍 G502x LightSpeed', 83000, 1, 83000);

-- order_id 25~28
INSERT INTO order_item (order_id, product_id, seller_id, product_name, unit_price, quantity, subtotal_price)
VALUES (25, 2, 1, '다얼유 A87Pro', 100000, 1, 100000),
       (26, 3, 1, '로지텍 G913', 269000, 1, 269000),
       (27, 4, 1, '로지텍 G733 LIGHTSPEED', 137780, 1, 137780),
       (28, 5, 1, '가죽 마우스장패드 대형', 15000, 1, 15000);

-- order_id 29~32
INSERT INTO order_item (order_id, product_id, seller_id, product_name, unit_price, quantity, subtotal_price)
VALUES (29, 6, 1, 'OS퍼니처 실리콘 게이밍의자', 372000, 1, 372000),
       (30, 7, 1, '다이소 나무 모니터 받침대', 5000, 1, 5000),
       (31, 8, 2, '삼성전자 24T452 24인치 모니터', 92700, 1, 92700),
       (32, 9, 2, '레이니75 키보드 WOB 블랙 바이올렛축', 94900, 1, 94900);

-- order_id 33~36
INSERT INTO order_item (order_id, product_id, seller_id, product_name, unit_price, quantity, subtotal_price)
VALUES (33, 10, 2, '큐데스크 키슬롯 수납형 손목받침대', 59000, 1, 59000),
       (34, 11, 3, '엔커 맥고 3in1 접이식 무선충전 스테이션', 89000, 1, 89000),
       (35, 12, 3, '시디즈 T20 탭플러스 블랙 메쉬의자 TNA200HF', 199000, 1, 199000),
       (36, 13, 3, '삼나무 원목 피톤치드향 모니터 받침대', 6290, 1, 6290);

-- order_id 37~40
INSERT INTO order_item (order_id, product_id, seller_id, product_name, unit_price, quantity, subtotal_price)
VALUES (37, 14, 3, 'Wavebone Grand Speaker Stand(Wood, Pair)', 330000, 1, 330000),
       (38, 15, 3, 'Neuman NDH20 Black', 961000, 1, 961000),
       (39, 16, 3, '현대일렉트릭 블랙 알루미늄 멀티탭 8구 HDSVAL-825', 32000, 1, 32000),
       (40, 17, 3, '로지텍 버티컬 LIFT 마우스 왼손용 910-007321', 89900, 1, 89900);

-- =========================================================
-- 15) broadcast (판매자당 2개 = 총 8)
-- broadcast_id: 1..8
-- tag_category_id는 옵션이므로 일부 NULL
-- scheduled_at은 NOW() + interval로 적당히 분산
-- =========================================================
INSERT INTO broadcast (seller_id, tag_category_id, broadcast_title, broadcast_notice, `status`, scheduled_at,
                       started_at, ended_at, broadcast_thumb_url, broadcast_wait_url, stream_key, broadcast_layout,
                       broadcast_stopped_reason)
VALUES (1, 1, 'YH 라이브 #1', '오늘의 데스크 셋업 특가', 'ENDED', DATE_ADD(NOW(), INTERVAL -3 DAY), DATE_ADD(NOW(), INTERVAL -3 DAY),
        DATE_ADD(NOW(), INTERVAL -3 DAY), 'https://img.test/b/1.jpg', NULL, 'sk_yh_1', 'LAYOUT_4', NULL),
       (1, 2, 'YH 라이브 #2', '키보드/마우스 추천', 'VOD', DATE_ADD(NOW(), INTERVAL -1 DAY), DATE_ADD(NOW(), INTERVAL -1 DAY),
        DATE_ADD(NOW(), INTERVAL -1 DAY), 'https://img.test/b/2.jpg', NULL, 'sk_yh_2', 'LAYOUT_4', NULL),
       (2, 1, 'DY 라이브 #1', '모니터 셋업 특집', 'ENDED', DATE_ADD(NOW(), INTERVAL -2 DAY), DATE_ADD(NOW(), INTERVAL -2 DAY),
        DATE_ADD(NOW(), INTERVAL -2 DAY), 'https://img.test/b/3.jpg', NULL, 'sk_dy_1', 'LAYOUT_3', NULL),
       (2, NULL, 'DY 라이브 #2', '스터디 셋업', 'RESERVED', DATE_ADD(NOW(), INTERVAL 1 DAY), NULL, NULL,
        'https://img.test/b/4.jpg', 'https://wait.test/b/4', NULL, 'LAYOUT_4', NULL);

INSERT INTO broadcast (seller_id, tag_category_id, broadcast_title, broadcast_notice, `status`, scheduled_at,
                       started_at, ended_at, broadcast_thumb_url, broadcast_wait_url, stream_key, broadcast_layout,
                       broadcast_stopped_reason)
VALUES (3, 3, 'JW 라이브 #1', '홈스튜디오 장비 특가', 'ENDED', DATE_ADD(NOW(), INTERVAL -5 DAY), DATE_ADD(NOW(), INTERVAL -5 DAY),
        DATE_ADD(NOW(), INTERVAL -5 DAY), 'https://img.test/b/5.jpg', NULL, 'sk_jw_1', 'FULL', NULL),
       (3, 4, 'JW 라이브 #2', '방송 셋업 추천', 'ON_AIR', DATE_ADD(NOW(), INTERVAL 0 DAY), DATE_ADD(NOW(), INTERVAL 0 DAY), NULL,
        'https://img.test/b/6.jpg', NULL, 'sk_jw_2', 'LAYOUT_2', NULL),
       (4, 3, 'HW 라이브 #1', '레코딩 장비 가이드', 'ENDED', DATE_ADD(NOW(), INTERVAL -7 DAY), DATE_ADD(NOW(), INTERVAL -7 DAY),
        DATE_ADD(NOW(), INTERVAL -7 DAY), 'https://img.test/b/7.jpg', NULL, 'sk_hw_1', 'LAYOUT_4', NULL),
       (4, NULL, 'HW 라이브 #2', '헤드폰/스피커 비교', 'READY', DATE_ADD(NOW(), INTERVAL 0 DAY), NULL, NULL,
        'https://img.test/b/8.jpg', 'https://wait.test/b/8', 'sk_hw_2', 'LAYOUT_4', NULL);

-- -------------------------
-- 16) broadcast_product (방송당 3개 정도)
-- bp_id: 1..
-- bp_quantity는 방송용 재고
-- -------------------------
INSERT INTO broadcast_product (broadcast_id, product_id, display_order, bp_price, bp_quantity, is_pinned, `status`)
VALUES (1, 1, 1, 79000, 10, 'Y', 'SELLING'),
       (1, 2, 2, 95000, 10, 'N', 'SELLING'),
       (1, 5, 3, 13000, 30, 'N', 'SELLING'),
       (2, 3, 1, 249000, 5, 'Y', 'SELLING');

INSERT INTO broadcast_product (broadcast_id, product_id, display_order, bp_price, bp_quantity, is_pinned, `status`)
VALUES (2, 4, 2, 129000, 5, 'N', 'SELLING'),
       (2, 6, 3, 349000, 2, 'N', 'SELLING'),
       (3, 8, 1, 89900, 8, 'Y', 'SELLING'),
       (3, 9, 2, 89900, 10, 'N', 'SELLING');

INSERT INTO broadcast_product (broadcast_id, product_id, display_order, bp_price, bp_quantity, is_pinned, `status`)
VALUES (3, 10, 3, 54000, 10, 'N', 'SELLING'),
       (4, 8, 1, 92700, 5, 'N', 'SELLING'),
       (4, 11, 2, 85000, 10, 'N', 'SELLING'),
       (5, 14, 1, 310000, 3, 'Y', 'SELLING');

INSERT INTO broadcast_product (broadcast_id, product_id, display_order, bp_price, bp_quantity, is_pinned, `status`)
VALUES (5, 15, 2, 930000, 2, 'N', 'SELLING'),
       (5, 20, 3, 149000, 5, 'N', 'SELLING'),
       (6, 16, 1, 29000, 20, 'Y', 'SELLING'),
       (6, 12, 2, 189000, 5, 'N', 'SELLING');

INSERT INTO broadcast_product (broadcast_id, product_id, display_order, bp_price, bp_quantity, is_pinned, `status`)
VALUES (7, 23, 1, 529000, 2, 'Y', 'SELLING'),
       (7, 22, 2, 540000, 2, 'N', 'SELLING'),
       (8, 21, 1, 880000, 2, 'Y', 'SELLING'),
       (8, 15, 2, 961000, 1, 'N', 'SELLING');

-- -------------------------
-- 17) vod (ENDED/VOD 방송 위주로 생성)
-- vod_id: 1..4
-- -------------------------
INSERT INTO vod (broadcast_id, vod_url, vod_size, `status`, vod_report_count, vod_duration, vod_admin_lock)
VALUES (1, 'https://vod.test/v/1.mp4', 102400000, 'PUBLIC', 0, 1800, 'N'),
       (2, 'https://vod.test/v/2.mp4', 204800000, 'PUBLIC', 1, 2400, 'N'),
       (3, 'https://vod.test/v/3.mp4', 153600000, 'PUBLIC', 0, 2100, 'N'),
       (7, 'https://vod.test/v/7.mp4', 307200000, 'PRIVATE', 0, 2700, 'N');

-- -------------------------
-- 18) qcard (방송당 2개 정도 = 8개)
-- -------------------------
INSERT INTO qcard (broadcast_id, qcard_question, sort_order)
VALUES (1, '오늘 특가 품목은?', 1),
       (1, '배송은 언제되나요?', 2),
       (2, 'AS 정책이 궁금해요', 1),
       (2, '추천 조합 있나요?', 2);

INSERT INTO qcard (broadcast_id, qcard_question, sort_order)
VALUES (5, '스탠드 차이가 뭔가요?', 1),
       (5, '입문용 추천?', 2),
       (6, '멀티탭 안전 기준?', 1),
       (7, '인터페이스 연결 팁?', 1);

-- -------------------------
-- 19) view_history (샘플)
-- viewer_id는 로그인/비로그인 식별자
-- -------------------------
INSERT INTO view_history (broadcast_id, viewer_id)
VALUES (1, 'anon-001'),
       (1, 'anon-002'),
       (2, 'member-1'),
       (5, 'member-3');

INSERT INTO view_history (broadcast_id, viewer_id)
VALUES (6, 'member-4'),
       (6, 'anon-010'),
       (7, 'anon-011'),
       (8, 'member-2');

-- -------------------------
-- 20) broadcast_result (방송 8개 전부 생성)
-- -------------------------
INSERT INTO broadcast_result (broadcast_id, total_views, max_views, max_views_at, total_likes, total_chats, total_sales,
                              avg_watch_time, total_reports)
VALUES (1, 1200, 180, DATE_ADD(NOW(), INTERVAL -3 DAY), 240, 80, 1500000, 420, 0),
       (2, 900, 160, DATE_ADD(NOW(), INTERVAL -1 DAY), 180, 60, 1200000, 380, 1),
       (3, 700, 140, DATE_ADD(NOW(), INTERVAL -2 DAY), 150, 40, 900000, 350, 0),
       (4, 0, 0, NULL, 0, 0, 0, 0, 0);

INSERT INTO broadcast_result (broadcast_id, total_views, max_views, max_views_at, total_likes, total_chats, total_sales,
                              avg_watch_time, total_reports)
VALUES (5, 1500, 220, DATE_ADD(NOW(), INTERVAL -5 DAY), 300, 120, 2400000, 460, 0),
       (6, 300, 90, DATE_ADD(NOW(), INTERVAL 0 DAY), 80, 50, 300000, 260, 0),
       (7, 400, 100, DATE_ADD(NOW(), INTERVAL -7 DAY), 90, 30, 600000, 310, 0),
       (8, 50, 20, DATE_ADD(NOW(), INTERVAL 0 DAY), 10, 5, 50000, 180, 0);

-- -------------------------
-- 21) live_chat (샘플: 8개)
-- member_email만 저장
-- -------------------------
INSERT INTO live_chat (broadcast_id, member_email, msg_type, content, raw_content, send_nick, is_world, is_hidden,
                       vod_play_time)
VALUES (6, 'yheon.park@member.test', 'ENTER', '입장했습니다', '입장했습니다', '박용헌', FALSE, FALSE, 5),
       (6, 'doyun.kim@member.test', 'TALK', '오늘 특가 뭐예요?', '오늘 특가 뭐예요?', '김도윤', FALSE, FALSE, 18),
       (6, 'jangwoo.joo@member.test', 'TALK', '세팅 팁 감사합니다', '세팅 팁 감사합니다', '주장우', FALSE, FALSE, 35),
       (6, 'hawon.go@member.test', 'TALK', '재고 얼마나 남았나요?', '재고 얼마나 남았나요?', '고하원', FALSE, FALSE, 50);

INSERT INTO live_chat (broadcast_id, member_email, msg_type, content, raw_content, send_nick, is_world, is_hidden,
                       vod_play_time)
VALUES (2, 'doyun.kim@member.test', 'TALK', '키보드 추천 조합 있나요?', '키보드 추천 조합 있나요?', '김도윤', FALSE, FALSE, 120),
       (1, 'yheon.park@member.test', 'NOTICE', '공지 확인 부탁드립니다', '공지 확인 부탁드립니다', '박용헌', TRUE, FALSE, 60),
       (5, 'jangwoo.joo@member.test', 'TALK', '스피커 스탠드 꼭 필요해요?', '스피커 스탠드 꼭 필요해요?', '주장우', FALSE, FALSE, 240),
       (7, 'hawon.go@member.test', 'TALK', '오디오인터페이스 연결이 궁금해요', '오디오인터페이스 연결이 궁금해요', '고하원', FALSE, FALSE, 180);

-- -------------------------
-- 22) sanction (샘플 2개)
-- -------------------------
INSERT INTO sanction (broadcast_id, member_id, actor_type, seller_id, admin_id, `status`, sanction_reason)
VALUES (6, 2, 'SELLER', 3, NULL, 'MUTE', '도배'),
       (6, 4, 'ADMIN', NULL, 1, 'OUT', '비방');

-- -------------------------
-- 23) follow (샘플 4개)
-- member 4명이 각각 seller 1명을 팔로우
-- -------------------------
INSERT INTO follow (follower_id, following_id)
VALUES (1, 3),
       (2, 1),
       (3, 4),
       (4, 2);

-- -------------------------
-- 24) notification (샘플 8개)
-- -------------------------
INSERT INTO notification (member_id, noti_type, noti_content)
VALUES (1, 'LIVE_START', 'JW 라이브 #2 시작 알림'),
       (2, 'NOTICE', '배송 정책이 업데이트되었습니다'),
       (3, 'EVENT', '신규 가입 이벤트 참여 가능'),
       (4, 'SYSTEM', '비밀번호 변경 권장 안내');

INSERT INTO notification (member_id, noti_type, noti_content)
VALUES (1, 'FOLLOW', '새로운 판매자를 팔로우했습니다'),
       (2, 'LIVE_START', 'HW 라이브 #2 방송 대기'),
       (3, 'NOTICE', 'VOD 업로드 완료'),
       (4, 'EVENT', '특가 쿠폰이 발급되었습니다');

-- =========================================================
-- 25) Seller Onboarding & Evaluation
-- seller_register(4) -> ai_evaluation(4) -> admin_evaluation(4)
-- company_registered(4) -> seller_grade(4)
-- invitation(4)
-- =========================================================

-- seller_register (plan_file은 최소 더미 BLOB)
INSERT INTO seller_register (plan_file, description, seller_id, company_name)
VALUES (UNHEX('DEADBEEF'), '사업계획서 더미', 1, 'YH 컴퍼니'),
       (UNHEX('DEADBEEF'), '사업계획서 더미', 2, 'DY 컴퍼니'),
       (UNHEX('DEADBEEF'), '사업계획서 더미', 3, 'JW 컴퍼니'),
       (UNHEX('DEADBEEF'), '사업계획서 더미', 4, 'HW 컴퍼니');

-- ai_evaluation (register_id=1..4)
INSERT INTO ai_evaluation (business_stability, product_competency, live_suitability, operation_coop, growth_potential,
                           total_score, grade_recommended, summary, seller_id, register_id)
VALUES (18, 19, 17, 18, 19, 91, 'A', '전반적으로 안정적이며 라이브 적합도가 높습니다.', 1, 1),
       (16, 17, 16, 17, 16, 82, 'B', '운영 협업 및 상품 경쟁력이 양호합니다.', 2, 2),
       (15, 18, 19, 16, 18, 86, 'A', '라이브 적합도가 매우 높습니다.', 3, 3),
       (14, 15, 16, 15, 15, 75, 'C', '기본 요건 충족. 운영 프로세스 보완 필요.', 4, 4);

-- admin_evaluation (ai_eval_id=1..4)
INSERT INTO admin_evaluation (business_stability, product_competency, live_suitability, operation_coop,
                              growth_potential, total_score, grade_recommended, admin_comment, ai_eval_id)
VALUES (18, 19, 18, 18, 19, 92, 'A', '우수. 즉시 승인 권장', 1),
       (16, 17, 16, 17, 16, 82, 'B', '보완사항 제출 후 승인', 2),
       (15, 18, 19, 16, 18, 86, 'A', '라이브 운영 경험 확인됨', 3),
       (14, 15, 16, 15, 15, 75, 'C', '운영/CS 체계 보완 필요', 4);

-- company_registered (seller_id 1..4)
INSERT INTO company_registered (company_name, business_number, seller_id, `status`)
VALUES ('YH 컴퍼니', '111-11-11111', 1, 'ACTIVE'),
       ('DY 컴퍼니', '222-22-22222', 2, 'ACTIVE'),
       ('JW 컴퍼니', '333-33-33333', 3, 'ACTIVE'),
       ('HW 컴퍼니', '444-44-44444', 4, 'ACTIVE');

-- seller_grade (company_id 1..4)
INSERT INTO seller_grade (grade, `status`, expired_at, company_id)
VALUES ('A', 'ACTIVE', NULL, 1),
       ('B', 'ACTIVE', NULL, 2),
       ('A', 'ACTIVE', NULL, 3),
       ('C', 'REVIEW', NULL, 4);

-- invitation (seller_id 1..4)
INSERT INTO invitation (email, expired_at, `status`, token, seller_id)
VALUES ('invitee1@test.com', DATE_ADD(NOW(), INTERVAL 7 DAY), 'PENDING', 'tok-111', 1),
       ('invitee2@test.com', DATE_ADD(NOW(), INTERVAL 7 DAY), 'PENDING', 'tok-222', 2),
       ('invitee3@test.com', DATE_ADD(NOW(), INTERVAL 7 DAY), 'PENDING', 'ACCEPTED', 'tok-333', 3),
       ('invitee4@test.com', DATE_ADD(NOW(), INTERVAL 7 DAY), 'PENDING', 'tok-444', 4);

-- =========================================================
-- 26) Payment (Toss)
-- - toss_payment 4개
-- - toss_refund 2개 (toss_payment_key FK)
-- - toss_webhook_log 4개 (toss_payment_key FK)
-- =========================================================

INSERT INTO toss_payment (toss_payment_key, toss_order_id, toss_payment_method, `status`, request_date, approved_date,
                          total_amount, order_id)
VALUES ('paykey-001', 'toss-ord-001', '신용/체크카드', 'DONE', NOW(), NOW(), 83000, 'app-uuid-ord-001'),
       ('paykey-002', 'toss-ord-002', '신용/체크카드', 'DONE', NOW(), NOW(), 100000, 'app-uuid-ord-002'),
       ('paykey-003', 'toss-ord-003', '계좌이체', 'DONE', NOW(), NOW(), 269000, 'app-uuid-ord-003'),
       ('paykey-004', 'toss-ord-004', '신용/체크카드', 'DONE', NOW(), NOW(), 137780, 'app-uuid-ord-004');

INSERT INTO toss_refund (refund_key, refund_amount, refund_reason, refund_status, requested_at, approved_at, payment_id,
                         toss_payment_key)
VALUES ('refund-001', 5000, '단순 변심', 'DONE', NOW(), NOW(), 1, 'paykey-001'),
       ('refund-002', 32000, '배송지 변경', 'DONE', NOW(), NOW(), 4, 'paykey-004');

INSERT INTO toss_webhook_log (event_type, raw_body, toss_payment_key, order_id)
VALUES ('PAYMENT_DONE', JSON_OBJECT('k', 'v', 'paymentKey', 'paykey-001'), 'paykey-001', 'app-uuid-ord-001'),
       ('PAYMENT_DONE', JSON_OBJECT('k', 'v', 'paymentKey', 'paykey-002'), 'paykey-002', 'app-uuid-ord-002'),
       ('PAYMENT_DONE', JSON_OBJECT('k', 'v', 'paymentKey', 'paykey-003'), 'paykey-003', 'app-uuid-ord-003'),
       ('PAYMENT_DONE', JSON_OBJECT('k', 'v', 'paymentKey', 'paykey-004'), 'paykey-004', 'app-uuid-ord-004');

-- =========================================================
-- 27) CS / Chatbot Support
-- chat_info(회원당 1개) -> chat_message(샘플) -> chat_handoff(샘플)
-- spring_ai_chat_memory(샘플)
-- =========================================================

INSERT INTO chat_info (member_id, `status`)
VALUES (1, 'BOT_ACTIVE'),
       (2, 'ESCALATED'),
       (3, 'BOT_ACTIVE'),
       (4, 'ADMIN_ACTIVE');

INSERT INTO chat_message (content, sender, chat_id)
VALUES ('배송 언제 되나요?', 'USER', 1),
       ('안녕하세요. 배송 일정 안내드립니다.', 'ASSISTANT', 1),
       ('환불 요청하고 싶어요', 'USER', 2),
       ('상담원 연결을 진행하겠습니다.', 'SYSTEM', 2);

INSERT INTO chat_message (content, sender, chat_id)
VALUES ('라이브 쿠폰 적용이 안돼요', 'USER', 3),
       ('쿠폰 적용 조건을 확인해 주세요.', 'ASSISTANT', 3),
       ('제품 불량 문의드립니다', 'USER', 4),
       ('관리자가 확인 후 답변드리겠습니다.', 'ASSISTANT', 4);

INSERT INTO chat_handoff (assigned_admin_id, `status`, chat_id)
VALUES (1, 'ADMIN_WAITING', 2),
       (2, 'ADMIN_CHECKED', 4);

INSERT INTO spring_ai_chat_memory (conversation_id, type, content)
VALUES ('conv-001', 'USER', '배송 문의'),
       ('conv-001', 'ASSISTANT', '배송 일정 안내'),
       ('conv-002', 'USER', '환불 요청'),
       ('conv-002', 'TOOL', 'handoff_created');