-- =========================================================
-- DESKIT & LIVE COMMERCE INTEGRATED DB SCHEMA
-- 최근작성일: 2026-01-06
-- 수정사항:
-- chat_info, chat_handoff 테이블 updated_at 컬럼 추가 (26.01.06)
-- broadcast_result, view_history 테이블 컬럼 수정 (26.01.05)
-- seller_grade 테이블 컬럼(grade) 수정 : enum 요소 추가 (26.01.04)
-- spring_ai_chat_memory 테이블 추가 (25.12.31)
-- member, seller 테이블 컬럼 추가 및 일부 타입 변경 / seller_register 테이블 컬럼 누락 수정 (25.12.30)
-- 사용자 커스텀 반영 및 toss_payment 테이블 에러(VARCHAR AUTO_INCREMENT) 수정 (25.12.25)
-- =========================================================

DROP DATABASE livecommerce;
CREATE DATABASE livecommerce;
USE livecommerce;

SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 1. DROP TABLES
-- =========================================================

-- [Commerce Core]
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS `order`; -- 예약어 이슈로 백틱 유지
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS setup_product;
DROP TABLE IF EXISTS setup_tag;
DROP TABLE IF EXISTS setup;
DROP TABLE IF EXISTS product_image;
DROP TABLE IF EXISTS product_tag;
DROP TABLE IF EXISTS product;

-- [Meta Data]
DROP TABLE IF EXISTS tag;
DROP TABLE IF EXISTS tag_category;
DROP TABLE IF EXISTS forbidden_word;

-- [Live Streaming]
DROP TABLE IF EXISTS broadcast_product;
DROP TABLE IF EXISTS view_history;
DROP TABLE IF EXISTS qcard;
DROP TABLE IF EXISTS vod;
DROP TABLE IF EXISTS broadcast_result;
DROP TABLE IF EXISTS sanction;
DROP TABLE IF EXISTS live_chat;
DROP TABLE IF EXISTS broadcast;

-- [Social & Notification]
DROP TABLE IF EXISTS follow;
DROP TABLE IF EXISTS notification;

-- [Users & Auth]
DROP TABLE IF EXISTS admin_evaluation;
DROP TABLE IF EXISTS ai_evaluation;
DROP TABLE IF EXISTS invitation;
DROP TABLE IF EXISTS seller_grade;
DROP TABLE IF EXISTS company_registered;
DROP TABLE IF EXISTS seller_register;
DROP TABLE IF EXISTS seller;
DROP TABLE IF EXISTS admin;
DROP TABLE IF EXISTS member;

-- [Payment & CS]
DROP TABLE IF EXISTS toss_webhook_log;
DROP TABLE IF EXISTS toss_refund;
DROP TABLE IF EXISTS toss_payment;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS chat_handoff;
DROP TABLE IF EXISTS chat_info;


-- =========================================================
-- 2. CREATE TABLES (PK included)
-- =========================================================

-- ---------------------------------------------------------
-- [User Group] Member, Seller, Admin
-- ---------------------------------------------------------
CREATE TABLE member (
    member_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
    `name`        VARCHAR(20)     NOT NULL COMMENT '회원명',
    login_id      VARCHAR(100)    NOT NULL COMMENT '로그인 아이디(이메일 등)',
    `profile`     TEXT    NULL COMMENT '프로필',
    phone         VARCHAR(15)     NOT NULL COMMENT '전화번호',
    is_agreed     TINYINT         NOT NULL COMMENT '약관 동의 여부',
    `status`      ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT '회원 상태',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    mbti          ENUM('INTJ', 'INTP', 'ENTJ', 'ENTP',
                       'INFJ', 'INFP', 'ENFJ', 'ENFP',
                       'ISTJ', 'ISFJ', 'ESTJ', 'ESFJ',
                       'ISTP', 'ISFP', 'ESTP', 'ESFP', 'NONE') NULL DEFAULT 'NONE',
    `role`        VARCHAR(20)     NOT NULL DEFAULT 'ROLE_MEMBER',
    job_category  ENUM('ADMIN_PLAN_TYPE', 'CREATIVE_TYPE', 'EDU_RES_TYPE', 'MED_PRO_TYPE', 'FLEXIBLE_TYPE', 'NONE') NULL DEFAULT 'NONE',
    PRIMARY KEY (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원';

CREATE TABLE seller (
    seller_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '판매자 ID',
    `status`      ENUM('PENDING', 'ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'PENDING' COMMENT '승인 상태',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `name`        VARCHAR(20)     NOT NULL COMMENT '판매자명(대표자명)',
    login_id      VARCHAR(100)    NOT NULL COMMENT '로그인 아이디',
    phone         VARCHAR(15)     NOT NULL COMMENT '전화번호',
    `profile`     TEXT    NULL COMMENT '판매자 프로필',
    `role`        ENUM('ROLE_SELLER_OWNER', 'ROLE_SELLER_MANAGER')     NOT NULL DEFAULT 'ROLE_SELLER_MANAGER',
    `is_agreed`   TINYINT   NOT NULL COMMENT '약관 동의 여부',
    PRIMARY KEY (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='판매자';

CREATE TABLE admin (
    admin_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    login_id      VARCHAR(100)    NOT NULL,
    phone         VARCHAR(15)     NOT NULL,
    `name`        VARCHAR(20)     NOT NULL,
    `role`        VARCHAR(20)     NOT NULL DEFAULT 'ROLE_ADMIN',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (admin_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='관리자';

-- ---------------------------------------------------------
-- [Meta Data] Tag, Category
-- ---------------------------------------------------------
CREATE TABLE tag_category(
    tag_category_id   BIGINT UNSIGNED                          NOT NULL AUTO_INCREMENT COMMENT '태그 카테고리 ID',
    tag_code          ENUM ('SPACE','TONE','SITUATION','MOOD') NOT NULL COMMENT '태그 카테고리 코드(공간/톤/상황/무드)',
    tag_category_name VARCHAR(30)                              NOT NULL COMMENT '태그 카테고리명(예: 공간, 톤, 상황, 무드)',
    created_at        DATETIME                                 NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at        DATETIME                                 NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at        DATETIME                                 NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (tag_category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='태그 카테고리';

CREATE TABLE tag(
    tag_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '태그 ID',
    tag_category_id BIGINT UNSIGNED NOT NULL COMMENT '태그 카테고리 ID(논리 FK: tag_category.tag_category_id)',
    tag_name        VARCHAR(50)     NOT NULL COMMENT '태그명(예: 미니멀, 화이트 등)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at      DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (tag_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='태그';

CREATE TABLE forbidden_word (
    word_id       INT UNSIGNED NOT NULL AUTO_INCREMENT,
    word          VARCHAR(50)  NOT NULL COMMENT '금지어',
    replacement   VARCHAR(50)  NOT NULL COMMENT '대체어',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='금지어';

-- ---------------------------------------------------------
-- [Commerce Core] Product, Image, Setup
-- ---------------------------------------------------------
CREATE TABLE product(
    product_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '상품 ID',
    seller_id    BIGINT UNSIGNED NOT NULL COMMENT '판매자 ID(논리 FK: seller/member)',
    product_name VARCHAR(100)    NOT NULL COMMENT '상품명',
    short_desc   VARCHAR(250)    NOT NULL COMMENT '한 줄 설명',
    detail_html  LONGTEXT        NOT NULL COMMENT '상세 HTML(웹 에디터 결과)',
    price        INT UNSIGNED    NOT NULL COMMENT '판매가(현재가)',
    cost_price   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '원가(할인율 표기용, 없으면 0)',
    `status`     ENUM (
        'DRAFT',        -- 임시저장(작성 중)
        'READY',        -- 판매 준비 완료
        'ON_SALE',      -- 판매중
        'LIMITED_SALE', -- 한정 판매(재고 임박)
        'SOLD_OUT',     -- 품절
        'PAUSED',       -- 일시중지
        'HIDDEN',       -- 숨김
        'DELETED'       -- 논리삭제 상태(표시용 상태값)
        ) NOT NULL DEFAULT 'DRAFT' COMMENT '상품 판매 상태',
    stock_qty    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '현재 재고 수량',
    safety_stock INT UNSIGNED    NOT NULL DEFAULT 5 COMMENT '안전 재고선(이하로 내려가면 알림 대상)',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at   DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='상품';

CREATE TABLE product_image (
    product_image_id  BIGINT UNSIGNED              NOT NULL AUTO_INCREMENT COMMENT '상품 이미지 ID',
    product_id        BIGINT UNSIGNED              NOT NULL COMMENT '상품 ID(논리 FK: product.product_id)',
    product_image_url VARCHAR(500)                 NOT NULL COMMENT '이미지 URL(NCP Object Storage 등)',
    image_type        ENUM ('THUMBNAIL','GALLERY') NOT NULL COMMENT '이미지 타입(THUMBNAIL=대표 1칸, GALLERY=갤러리)',
    slot_index        TINYINT UNSIGNED             NOT NULL DEFAULT 0 COMMENT '슬롯 인덱스(정렬/고정용: THUMBNAIL=0, GALLERY=1~4 권장)',
    created_at        DATETIME                     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 시각',
    updated_at        DATETIME                     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at        DATETIME                     NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (product_image_id),
    UNIQUE KEY uk_product_image_slot (product_id, image_type, slot_index)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='상품 이미지';

CREATE TABLE product_tag (
    product_id BIGINT UNSIGNED NOT NULL COMMENT '상품 ID(논리 FK: product.product_id)',
    tag_id     BIGINT UNSIGNED NOT NULL COMMENT '태그 ID(논리 FK: tag.tag_id)',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '매핑 생성 시각',
    deleted_at DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (product_id, tag_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='상품-태그 매핑';

CREATE TABLE setup (
    setup_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '셋업 ID',
    seller_id       BIGINT UNSIGNED NOT NULL COMMENT '판매자 ID(논리 FK: seller/member)',
    setup_name      VARCHAR(100)    NOT NULL COMMENT '셋업명',
    short_desc      VARCHAR(250)    NOT NULL COMMENT '한 줄 소개',
    tip_text        VARCHAR(500)    NULL COMMENT 'Tip 문구(옵션)',
    setup_image_url VARCHAR(500)    NOT NULL COMMENT '셋업 썸네일 URL(대표 1장)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at      DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (setup_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='셋업';

CREATE TABLE setup_tag (
    setup_id   BIGINT UNSIGNED NOT NULL COMMENT '셋업 ID(논리 FK: setup.setup_id)',
    tag_id     BIGINT UNSIGNED NOT NULL COMMENT '태그 ID(논리 FK: tag.tag_id)',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '매핑 생성 시각',
    deleted_at DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (setup_id, tag_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='셋업-태그 매핑';

CREATE TABLE setup_product (
    setup_id   BIGINT UNSIGNED NOT NULL COMMENT '셋업 ID(논리 FK: setup.setup_id)',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '상품 ID(논리 FK: product.product_id)',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '매핑 생성 시각',
    deleted_at DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (setup_id, product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='셋업-상품 매핑';

-- ---------------------------------------------------------
-- [Transactions] Cart, Order
-- ---------------------------------------------------------
CREATE TABLE cart (
    cart_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '장바구니 ID',
    member_id  BIGINT UNSIGNED NOT NULL COMMENT '회원 ID(회원당 1개 장바구니)',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (cart_id),
    UNIQUE KEY uk_cart_member (member_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='장바구니(회원 1:1)';

CREATE TABLE cart_item (
    cart_item_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '장바구니 아이템 ID',
    cart_id        BIGINT UNSIGNED NOT NULL COMMENT '장바구니 ID(논리 FK: cart.cart_id)',
    product_id     BIGINT UNSIGNED NOT NULL COMMENT '상품 ID(논리 FK: product.product_id)',
    quantity       INT UNSIGNED    NOT NULL COMMENT '수량',
    price_snapshot INT UNSIGNED    NOT NULL COMMENT '담을 당시 가격 스냅샷(표시/검증용)',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at     DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (cart_item_id),
    UNIQUE KEY uk_cart_item (cart_id, product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='장바구니 아이템';

-- [주의] order는 예약어이므로 백틱(`) 필수 사용
CREATE TABLE `order` (
    order_id             BIGINT UNSIGNED                                 NOT NULL AUTO_INCREMENT COMMENT '주문 ID',
    member_id            BIGINT UNSIGNED                                 NOT NULL COMMENT '회원 ID(논리 FK: member)',
    order_number         VARCHAR(50)                                     NOT NULL COMMENT '구매자 노출 주문번호(유니크)',
    total_product_amount INT UNSIGNED                                    NOT NULL COMMENT '상품 총액(상품가 합)',
    shipping_fee         INT UNSIGNED                                    NOT NULL DEFAULT 0 COMMENT '배송비(추후 확장)',
    discount_fee         INT UNSIGNED                                    NOT NULL DEFAULT 0 COMMENT '할인 금액(쿠폰/프로모션 등)',
    order_amount         INT UNSIGNED                                    NOT NULL COMMENT '최종 금액(=상품총액-할인+배송비)',
    `status`             ENUM ('CREATED','PAID','CANCELLED','COMPLETED') NOT NULL DEFAULT 'CREATED' COMMENT '주문 상태',
    cancel_reason        VARCHAR(500)                                    NULL COMMENT '주문 취소 사유',
    created_at           DATETIME                                        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    paid_at              DATETIME                                        NULL COMMENT '결제 완료 시각(결제 담당이 채움)',
    cancelled_at         DATETIME                                        NULL COMMENT '취소 시각',
    updated_at           DATETIME                                        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at           DATETIME                                        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='주문';

CREATE TABLE order_item (
    order_item_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '주문 아이템 ID',
    order_id       BIGINT UNSIGNED NOT NULL COMMENT '주문 ID(논리 FK: `order`.order_id)',
    product_id     BIGINT UNSIGNED NOT NULL COMMENT '상품 ID(논리 FK: product.product_id)',
    seller_id      BIGINT UNSIGNED NOT NULL COMMENT '판매자 ID(논리 FK: seller/member)',
    product_name   VARCHAR(100)    NOT NULL COMMENT '상품명 스냅샷(주문 시점)',
    unit_price     INT UNSIGNED    NOT NULL COMMENT '주문 시점 단가',
    quantity       INT UNSIGNED    NOT NULL COMMENT '수량',
    subtotal_price INT UNSIGNED    NOT NULL COMMENT '소계(=unit_price * quantity)',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at     DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (order_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='주문 아이템';

-- ---------------------------------------------------------
-- [Live Commerce] Broadcast, VOD, Chat
-- ---------------------------------------------------------
CREATE TABLE broadcast (
    broadcast_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    seller_id        BIGINT UNSIGNED NOT NULL COMMENT 'FK: seller',
    tag_category_id  BIGINT UNSIGNED NOT NULL COMMENT 'FK: tag_category (옵션)',
    broadcast_title  VARCHAR(30)     NOT NULL,
    broadcast_notice VARCHAR(100)    NULL,
    `status`         ENUM('RESERVED','READY','ON_AIR','ENDED','VOD','DELETED','CANCELED','STOPPED') NOT NULL DEFAULT 'RESERVED',
    scheduled_at     DATETIME        NOT NULL COMMENT '예약 시간',
    started_at       DATETIME        NULL,
    ended_at         DATETIME        NULL,
    broadcast_thumb_url VARCHAR(255) NOT NULL,
    broadcast_wait_url  VARCHAR(255) NULL,
    stream_key       VARCHAR(100)    NULL,
    broadcast_layout ENUM('FULL', 'LAYOUT_2', 'LAYOUT_3', 'LAYOUT_4') NOT NULL DEFAULT 'LAYOUT_4',
    broadcast_stopped_reason VARCHAR(50) NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (broadcast_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='라이브 방송';

CREATE TABLE broadcast_product (
    bp_id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    broadcast_id  BIGINT UNSIGNED NOT NULL COMMENT 'FK: broadcast',
    product_id    BIGINT UNSIGNED NOT NULL COMMENT 'FK: product',
    display_order INT             NOT NULL,
    bp_price      INT             NULL COMMENT '방송 특가',
    bp_quantity   INT             NOT NULL COMMENT '방송용 재고',
    is_pinned     CHAR(1)         NOT NULL DEFAULT 'N' COMMENT 'N/Y',
    `status`      ENUM('SELLING','SOLDOUT','DELETED') NOT NULL DEFAULT 'SELLING',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (bp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='방송-상품 매핑';

CREATE TABLE vod (
    vod_id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    broadcast_id   BIGINT UNSIGNED NOT NULL COMMENT 'FK: broadcast',
    vod_url        VARCHAR(255)    NULL,
    vod_size       BIGINT          NOT NULL DEFAULT 0,
    `status`       ENUM('PUBLIC','PRIVATE','DELETED') NOT NULL DEFAULT 'PUBLIC',
    vod_report_count INT           NOT NULL DEFAULT 0,
    vod_duration   INT             NULL COMMENT '초 단위',
    vod_admin_lock CHAR(1)         NOT NULL DEFAULT 'N' COMMENT 'Y/N',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (vod_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='다시보기(VOD)';

CREATE TABLE qcard (
    qcard_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    broadcast_id   BIGINT UNSIGNED NOT NULL,
    qcard_question VARCHAR(50)     NOT NULL,
    sort_order     INT             NOT NULL DEFAULT 0,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (qcard_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='큐카드(질문)';

CREATE TABLE view_history (
    history_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    broadcast_id   BIGINT UNSIGNED NOT NULL,
    viewer_id      VARCHAR(100)    NOT NULL,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='시청 기록';

CREATE TABLE broadcast_result (
    broadcast_id   BIGINT UNSIGNED NOT NULL COMMENT 'PK이자 FK',
    total_views    INT             NOT NULL DEFAULT 0,
    max_views      INT             NOT NULL DEFAULT 0,
    max_views_at   DATETIME        NOT NULL,
    total_likes    INT             NOT NULL DEFAULT 0,
    total_chats    INT             NOT NULL DEFAULT 0,
    total_sales    DECIMAL(30, 0)  NOT NULL DEFAULT 0,
    avg_watch_time INT             NOT NULL DEFAULT 0,
    total_reports  INT             NOT NULL DEFAULT 0,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (broadcast_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='방송 결과 통계';

CREATE TABLE live_chat (
    message_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    broadcast_id   BIGINT UNSIGNED NOT NULL,
    member_id      BIGINT UNSIGNED NOT NULL,
    msg_type       ENUM('TALK','ENTER','EXIT','PURCHASE','NOTICE') NOT NULL COMMENT '채팅 메시지 유형',
    content        VARCHAR(500)    NOT NULL,
    send_nick      VARCHAR(50)     NOT NULL,
    is_world       BOOLEAN         NOT NULL DEFAULT FALSE,
    is_hidden      BOOLEAN         NOT NULL DEFAULT FALSE COMMENT '숨김 처리 여부',
    send_lchat     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vod_play_time  INT             NOT NULL DEFAULT 0 COMMENT '방송 시작 후 경과 시간(초)',
    PRIMARY KEY (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='라이브 채팅';

CREATE TABLE sanction (
    sanction_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    broadcast_id   BIGINT UNSIGNED NOT NULL,
    member_id      BIGINT UNSIGNED NOT NULL,
    actor_type     ENUM('ADMIN','SELLER') NOT NULL DEFAULT 'SELLER',
    seller_id      BIGINT UNSIGNED NULL,
    admin_id       BIGINT UNSIGNED NULL,
    `status`       ENUM('MUTE','OUT') NOT NULL DEFAULT 'MUTE',
    sanction_reason VARCHAR(50)    NULL,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sanction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='제재(강퇴/채금)';

-- ---------------------------------------------------------
-- [Social & Notifications]
-- ---------------------------------------------------------
CREATE TABLE follow (
    follow_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    follower_id    BIGINT UNSIGNED NOT NULL COMMENT '팔로우하는 멤버 ID',
    following_id   BIGINT UNSIGNED NOT NULL COMMENT '팔로우 받는 판매자 ID',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='팔로우';

CREATE TABLE notification (
    noti_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id      BIGINT UNSIGNED NOT NULL,
    noti_type      ENUM('LIVE_START','FOLLOW','NOTICE','EVENT','SYSTEM') NOT NULL COMMENT '알림 유형',
    noti_content   VARCHAR(255)    NOT NULL,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (noti_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림';

-- ---------------------------------------------------------
-- [Seller Onboarding & Evaluation (AI/Admin)]
-- ---------------------------------------------------------
CREATE TABLE seller_register (
    register_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    plan_file      LONGBLOB        NOT NULL COMMENT '사업계획서 등',
    description    TEXT            NULL COMMENT '설명',
    seller_id      BIGINT UNSIGNED NOT NULL COMMENT 'FK: seller',
    company_name   VARCHAR(100)    NOT NULL,
    PRIMARY KEY (register_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='입점 신청서';

CREATE TABLE ai_evaluation (
    ai_eval_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_stability INT         NOT NULL,
    product_competency INT         NOT NULL,
    live_suitability   INT         NOT NULL,
    operation_coop     INT         NOT NULL,
    growth_potential   INT         NOT NULL,
    total_score        INT         NOT NULL,
    grade_recommended ENUM('A','B','C','REJECTED') NOT NULL DEFAULT 'C',
    summary            TEXT        NOT NULL,
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seller_id          BIGINT UNSIGNED NOT NULL,
    register_id        BIGINT UNSIGNED NOT NULL COMMENT 'FK: seller_register',
    PRIMARY KEY (ai_eval_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 입점 평가';

CREATE TABLE admin_evaluation (
    admin_eval_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_stability INT         NOT NULL,
    product_competency INT         NOT NULL,
    live_suitability   INT         NOT NULL,
    operation_coop     INT         NOT NULL,
    growth_potential   INT         NOT NULL,
    total_score        INT         NOT NULL,
    grade_recommended  ENUM('A','B','C','REJECTED') NOT NULL DEFAULT 'C',
    admin_comment      VARCHAR(250) NULL,
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ai_eval_id         BIGINT UNSIGNED NOT NULL COMMENT 'FK: ai_evaluation',
    PRIMARY KEY (admin_eval_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='관리자 최종 평가';

CREATE TABLE company_registered (
    company_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    company_name   VARCHAR(100)    NOT NULL,
    business_number VARCHAR(15)    NOT NULL,
    seller_id      BIGINT UNSIGNED NOT NULL COMMENT 'FK: seller',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status`       ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='등록된 기업 정보';

CREATE TABLE seller_grade (
    grade_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    grade          ENUM('A', 'B', 'C', 'REJECTED') NOT NULL DEFAULT 'C',
    `status`       ENUM('ACTIVE', 'TEMP', 'REVIEW') NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expired_at     DATETIME        NULL,
    company_id     BIGINT UNSIGNED NOT NULL COMMENT 'FK: company_registered',
    PRIMARY KEY (grade_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='판매자 등급';

CREATE TABLE invitation (
    invitation_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    email          VARCHAR(100)    NOT NULL,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expired_at     DATETIME        NOT NULL,
    `status`       ENUM('PENDING','ACCEPTED','EXPIRED') NOT NULL DEFAULT 'PENDING',
    token          VARCHAR(255)    NOT NULL,
    seller_id      BIGINT UNSIGNED NOT NULL COMMENT '초대자',
    PRIMARY KEY (invitation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='초대장';

-- ---------------------------------------------------------
-- [Payment (Toss)]
-- ---------------------------------------------------------
-- [수정완료] AUTO_INCREMENT를 사용하기 위해 payment_id를 BIGINT UNSIGNED로 변경
CREATE TABLE toss_payment (
    payment_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    toss_payment_key    VARCHAR(64)     NOT NULL UNIQUE,
    toss_order_id       VARCHAR(255)    NOT NULL,
    toss_payment_method ENUM('계좌이체','신용/체크카드') NOT NULL,
    `status`            ENUM('ABORTED','CANCELED','DONE','EXPIRED','IN_PROGRESS','PARTIAL_CANCELED','READY','WAITING_FOR_DEPOSIT') NOT NULL,
    request_date        DATETIME        NOT NULL,
    approved_date       DATETIME        NULL DEFAULT NULL,
    total_amount        BIGINT          NOT NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    order_id            VARCHAR(36)     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='토스 결제 정보';

CREATE TABLE toss_refund (
    refund_id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    refund_key          VARCHAR(100)    NOT NULL,
    refund_amount       BIGINT          NOT NULL,
    refund_reason       VARCHAR(255)    NULL,
    refund_status       VARCHAR(255)    NOT NULL,
    requested_at        DATETIME        NOT NULL,
    approved_at         DATETIME        NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_id          BIGINT UNSIGNED NOT NULL COMMENT 'toss_payment의 PK와 타입 일치시킴',
    toss_payment_key    VARCHAR(64)     NOT NULL UNIQUE COMMENT 'FK: toss_payment (UNIQUE Key 참조)',
    PRIMARY KEY (refund_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='토스 환불 정보';

CREATE TABLE toss_webhook_log (
    webhook_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    event_type          VARCHAR(50)     NOT NULL,
    raw_body            JSON            NOT NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    toss_payment_key    VARCHAR(64)     NOT NULL UNIQUE,
    order_id            VARCHAR(36)     NOT NULL,
    PRIMARY KEY (webhook_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='토스 웹훅 로그';

-- ---------------------------------------------------------
-- [CS / Chatbot Support]
-- ---------------------------------------------------------
CREATE TABLE chat_info (
    chat_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `status`      ENUM('BOT_ACTIVE', 'ADMIN_ACTIVE', 'ESCALATED', 'CLOSED') NOT NULL DEFAULT 'BOT_ACTIVE',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    member_id     BIGINT UNSIGNED NOT NULL COMMENT 'FK: member',
    PRIMARY KEY (chat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CS 채팅방';

CREATE TABLE chat_message (
    message_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    content       TEXT            NOT NULL,
    sender        ENUM('ASSISTANT', 'SYSTEM', 'USER') NOT NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    chat_id       BIGINT UNSIGNED NOT NULL COMMENT 'FK: chat_info',
    PRIMARY KEY (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CS 메시지';

CREATE TABLE chat_handoff (
    handoff_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    assigned_admin_id BIGINT UNSIGNED NULL COMMENT 'FK: admin',
    `status`      ENUM('ADMIN_WAITING', 'ADMIN_CHECKED', 'ADMIN_ANSWERED') NOT NULL DEFAULT 'ADMIN_WAITING',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    chat_id       BIGINT UNSIGNED NOT NULL COMMENT 'FK: chat_info',
    PRIMARY KEY (handoff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상담원 연결 요청';

CREATE TABLE spring_ai_chat_memory (
   id BIGINT AUTO_INCREMENT PRIMARY KEY,
   conversation_id VARCHAR(255) NOT NULL,
   type ENUM('USER', 'ASSISTANT', 'SYSTEM', 'TOOL') NOT NULL,
   content TEXT NOT NULL,
   timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='채팅 메모리';

-- =========================================================
-- 3. ALTER TABLE (FOREIGN KEYS)
-- =========================================================

-- [Tag & Category]
ALTER TABLE tag ADD CONSTRAINT FK_tag_category FOREIGN KEY (tag_category_id) REFERENCES tag_category (tag_category_id);

-- [Product Relations]
ALTER TABLE product ADD CONSTRAINT FK_product_seller FOREIGN KEY (seller_id) REFERENCES seller (seller_id);
ALTER TABLE product_image ADD CONSTRAINT FK_product_image_product FOREIGN KEY (product_id) REFERENCES product (product_id);
ALTER TABLE product_tag ADD CONSTRAINT FK_pt_product FOREIGN KEY (product_id) REFERENCES product (product_id);
ALTER TABLE product_tag ADD CONSTRAINT FK_pt_tag FOREIGN KEY (tag_id) REFERENCES tag (tag_id);

-- [Setup Relations]
ALTER TABLE setup ADD CONSTRAINT FK_setup_seller FOREIGN KEY (seller_id) REFERENCES seller (seller_id);
ALTER TABLE setup_tag ADD CONSTRAINT FK_st_setup FOREIGN KEY (setup_id) REFERENCES setup (setup_id);
ALTER TABLE setup_tag ADD CONSTRAINT FK_st_tag FOREIGN KEY (tag_id) REFERENCES tag (tag_id);
ALTER TABLE setup_product ADD CONSTRAINT FK_sp_setup FOREIGN KEY (setup_id) REFERENCES setup (setup_id);
ALTER TABLE setup_product ADD CONSTRAINT FK_sp_product FOREIGN KEY (product_id) REFERENCES product (product_id);

-- [Cart Relations]
ALTER TABLE cart ADD CONSTRAINT FK_cart_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE cart_item ADD CONSTRAINT FK_cart_item_cart FOREIGN KEY (cart_id) REFERENCES cart (cart_id);
ALTER TABLE cart_item ADD CONSTRAINT FK_cart_item_product FOREIGN KEY (product_id) REFERENCES product (product_id);

-- [Order Relations] (테이블명 `order`에 백틱 사용)
ALTER TABLE `order` ADD CONSTRAINT FK_order_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE order_item ADD CONSTRAINT FK_order_item_order FOREIGN KEY (order_id) REFERENCES `order` (order_id);
ALTER TABLE order_item ADD CONSTRAINT FK_order_item_product FOREIGN KEY (product_id) REFERENCES product (product_id);
ALTER TABLE order_item ADD CONSTRAINT FK_order_item_seller FOREIGN KEY (seller_id) REFERENCES seller (seller_id);

-- [Broadcast Relations]
ALTER TABLE broadcast ADD CONSTRAINT FK_broadcast_seller FOREIGN KEY (seller_id) REFERENCES seller (seller_id);
-- ALTER TABLE broadcast ADD CONSTRAINT FK_broadcast_tag_cat FOREIGN KEY (tag_category_id) REFERENCES tag_category (tag_category_id); 

ALTER TABLE broadcast_product ADD CONSTRAINT FK_bp_broadcast FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);
ALTER TABLE broadcast_product ADD CONSTRAINT FK_bp_product FOREIGN KEY (product_id) REFERENCES product (product_id);

ALTER TABLE vod ADD CONSTRAINT FK_vod_broadcast FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);
ALTER TABLE qcard ADD CONSTRAINT FK_qcard_broadcast FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);
ALTER TABLE broadcast_result ADD CONSTRAINT FK_br_broadcast FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);

-- [Live Interaction Relations]
ALTER TABLE view_history ADD CONSTRAINT FK_vh_broadcast FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);
# ALTER TABLE view_history ADD CONSTRAINT FK_vh_member FOREIGN KEY (member_id) REFERENCES member (member_id);

ALTER TABLE live_chat ADD CONSTRAINT FK_lc_broadcast FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);
ALTER TABLE live_chat ADD CONSTRAINT FK_lc_member FOREIGN KEY (member_id) REFERENCES member (member_id);

ALTER TABLE sanction ADD CONSTRAINT FK_sanction_broadcast FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);
ALTER TABLE sanction ADD CONSTRAINT FK_sanction_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE sanction ADD CONSTRAINT FK_sanction_seller FOREIGN KEY (seller_id) REFERENCES seller (seller_id);
ALTER TABLE sanction ADD CONSTRAINT FK_sanction_admin FOREIGN KEY (admin_id) REFERENCES admin (admin_id);

-- [Social Relations]
ALTER TABLE follow ADD CONSTRAINT FK_follow_follower FOREIGN KEY (follower_id) REFERENCES member (member_id);
ALTER TABLE follow ADD CONSTRAINT FK_follow_following FOREIGN KEY (following_id) REFERENCES seller (seller_id);
ALTER TABLE notification ADD CONSTRAINT FK_noti_member FOREIGN KEY (member_id) REFERENCES member (member_id);

-- [Seller Evaluation Relations]
ALTER TABLE seller_register ADD CONSTRAINT FK_sr_seller FOREIGN KEY (seller_id) REFERENCES seller (seller_id);

ALTER TABLE ai_evaluation ADD CONSTRAINT FK_ai_reg FOREIGN KEY (register_id) REFERENCES seller_register (register_id);
ALTER TABLE ai_evaluation ADD CONSTRAINT FK_ai_seller FOREIGN KEY (seller_id) REFERENCES seller (seller_id);

ALTER TABLE admin_evaluation ADD CONSTRAINT FK_ae_ai FOREIGN KEY (ai_eval_id) REFERENCES ai_evaluation (ai_eval_id);

ALTER TABLE company_registered ADD CONSTRAINT FK_cr_seller FOREIGN KEY (seller_id) REFERENCES seller (seller_id);
ALTER TABLE seller_grade ADD CONSTRAINT FK_sg_company FOREIGN KEY (company_id) REFERENCES company_registered (company_id);

ALTER TABLE invitation ADD CONSTRAINT FK_invitation_seller FOREIGN KEY (seller_id) REFERENCES seller (seller_id);

-- [Payment Relations]
-- toss_refund의 FK를 toss_payment_key(UNIQUE)에 연결
ALTER TABLE toss_refund ADD CONSTRAINT FK_refund_payment_key FOREIGN KEY (toss_payment_key) REFERENCES toss_payment (toss_payment_key);
-- toss_webhook_log의 FK를 toss_payment_key(UNIQUE)에 연결
ALTER TABLE toss_webhook_log ADD CONSTRAINT FK_webhook_payment_key FOREIGN KEY (toss_payment_key) REFERENCES toss_payment (toss_payment_key);

-- [CS Relations]
ALTER TABLE chat_info ADD CONSTRAINT FK_chat_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE chat_message ADD CONSTRAINT FK_msg_chat FOREIGN KEY (chat_id) REFERENCES chat_info (chat_id);
ALTER TABLE chat_handoff ADD CONSTRAINT FK_handoff_chat FOREIGN KEY (chat_id) REFERENCES chat_info (chat_id);
ALTER TABLE chat_handoff ADD CONSTRAINT FK_handoff_admin FOREIGN KEY (assigned_admin_id) REFERENCES admin (admin_id);

SET FOREIGN_KEY_CHECKS = 1;
