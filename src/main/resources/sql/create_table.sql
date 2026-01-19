-- =========================================================
-- DESKIT & LIVE COMMERCE INTEGRATED DB SCHEMA
-- =========================================================

DROP DATABASE IF EXISTS livecommerce;
CREATE DATABASE livecommerce DEFAULT CHARACTER SET utf8mb4;
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
DROP TABLE IF EXISTS address;
DROP TABLE IF EXISTS member;

-- [Payment & CS]
DROP TABLE IF EXISTS toss_webhook_log;
DROP TABLE IF EXISTS toss_refund;
DROP TABLE IF EXISTS toss_payment;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS chat_handoff;
DROP TABLE IF EXISTS chat_info;
DROP TABLE IF EXISTS spring_ai_chat_memory;

-- =========================================================
-- 2. CREATE TABLES
-- =========================================================

-- ---------------------------------------------------------
-- [User Group] Member, Seller, Admin
-- ---------------------------------------------------------
CREATE TABLE member
(
    member_id    BIGINT UNSIGNED                                                                               NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
    `name`       VARCHAR(20)                                                                                   NOT NULL COMMENT '회원명',
    login_id     VARCHAR(100)                                                                                  NOT NULL COMMENT '로그인 아이디(이메일 등)',
    `profile`    TEXT                                                                                          NULL COMMENT '프로필',
    phone        VARCHAR(15)                                                                                   NOT NULL COMMENT '전화번호',
    is_agreed    TINYINT                                                                                       NOT NULL COMMENT '약관 동의 여부',
    `status`     ENUM ('ACTIVE', 'INACTIVE')                                                                   NOT NULL DEFAULT 'ACTIVE' COMMENT '회원 상태',
    created_at   DATETIME                                                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME                                                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    mbti         ENUM ('INTJ','INTP','ENTJ','ENTP','INFJ','INFP','ENFJ','ENFP','ISTJ','ISFJ','ESTJ','ESFJ','ISTP','ISFP','ESTP','ESFP','NONE')
                                                                                                               NULL     DEFAULT 'NONE',
    `role`       VARCHAR(20)                                                                                   NOT NULL DEFAULT 'ROLE_MEMBER',
    job_category ENUM ('ADMIN_PLAN_TYPE','CREATIVE_TYPE','EDU_RES_TYPE','MED_PRO_TYPE','FLEXIBLE_TYPE','NONE') NULL     DEFAULT 'NONE',
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_member_login (login_id), -- 이메일/아이디 중복 방지(실무 필수)
    KEY idx_member_phone (phone)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='회원';

CREATE TABLE address
(
    address_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '배송지 ID',
    member_id   BIGINT UNSIGNED NOT NULL COMMENT '회원 ID',
    receiver    VARCHAR(20)     NOT NULL COMMENT '수령인',
    postcode    VARCHAR(10)     NOT NULL COMMENT '우편번호',
    addr_detail VARCHAR(255)    NOT NULL COMMENT '주소',
    is_default  TINYINT         NOT NULL DEFAULT 0 COMMENT '기본 배송지 여부',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    PRIMARY KEY (address_id),
    KEY idx_address_member (member_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='배송지';

CREATE TABLE seller
(
    seller_id   BIGINT UNSIGNED                                   NOT NULL AUTO_INCREMENT COMMENT '판매자 ID',
    `status`    ENUM ('PENDING', 'ACTIVE', 'INACTIVE')            NOT NULL DEFAULT 'PENDING' COMMENT '승인 상태',
    created_at  DATETIME                                          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME                                          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `name`      VARCHAR(20)                                       NOT NULL COMMENT '판매자명(대표자명)',
    login_id    VARCHAR(100)                                      NOT NULL COMMENT '로그인 아이디',
    phone       VARCHAR(15)                                       NOT NULL COMMENT '전화번호',
    `profile`   TEXT                                              NULL COMMENT '판매자 프로필',
    `role`      ENUM ('ROLE_SELLER_OWNER', 'ROLE_SELLER_MANAGER') NOT NULL DEFAULT 'ROLE_SELLER_MANAGER',
    `is_agreed` TINYINT                                           NOT NULL COMMENT '약관 동의 여부',
    PRIMARY KEY (seller_id),
    UNIQUE KEY uk_seller_login (login_id),
    KEY idx_seller_phone (phone)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='판매자';

CREATE TABLE admin
(
    admin_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    login_id   VARCHAR(100)    NOT NULL,
    phone      VARCHAR(15)     NOT NULL,
    `name`     VARCHAR(20)     NOT NULL,
    `role`     VARCHAR(20)     NOT NULL DEFAULT 'ROLE_ADMIN',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (admin_id),
    UNIQUE KEY uk_admin_login (login_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='관리자';

-- ---------------------------------------------------------
-- [Meta Data] Tag, Category
-- ---------------------------------------------------------
CREATE TABLE tag_category
(
    tag_category_id   BIGINT UNSIGNED                          NOT NULL AUTO_INCREMENT COMMENT '태그 카테고리 ID',
    tag_code          ENUM ('SPACE','TONE','SITUATION','MOOD') NOT NULL COMMENT '태그 카테고리 코드',
    tag_category_name VARCHAR(30)                              NOT NULL COMMENT '태그 카테고리명',
    created_at        DATETIME                                 NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at        DATETIME                                 NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at        DATETIME                                 NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (tag_category_id),
    UNIQUE KEY uk_tag_category_code (tag_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='태그 카테고리';

CREATE TABLE tag
(
    tag_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '태그 ID',
    tag_category_id BIGINT UNSIGNED NOT NULL COMMENT '태그 카테고리 ID',
    tag_name        VARCHAR(50)     NOT NULL COMMENT '태그명',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at      DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (tag_id),
    KEY idx_tag_category (tag_category_id),
    UNIQUE KEY uk_tag_category_name (tag_category_id, tag_name) -- 같은 카테고리에서 태그명 중복 방지
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='태그';

CREATE TABLE forbidden_word
(
    word_id    INT UNSIGNED NOT NULL AUTO_INCREMENT,
    word       VARCHAR(50)  NOT NULL COMMENT '금지어',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (word_id),
    UNIQUE KEY uk_forbidden_word (word)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='금지어';

-- ---------------------------------------------------------
-- [Commerce Core] Product, Image, Setup
-- ---------------------------------------------------------
CREATE TABLE product
(
    product_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '상품 ID',
    seller_id    BIGINT UNSIGNED NOT NULL COMMENT '판매자 ID',
    product_name VARCHAR(100)    NOT NULL COMMENT '상품명',
    short_desc   VARCHAR(250)    NOT NULL COMMENT '한 줄 설명',
    detail_html  LONGTEXT        NOT NULL COMMENT '상세 HTML(웹 에디터 결과)',
    price        INT UNSIGNED    NOT NULL COMMENT '판매가(현재가)',
    cost_price   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '원가(없으면 0)',
    `status`     ENUM ('DRAFT','READY','ON_SALE','LIMITED_SALE','SOLD_OUT','PAUSED','HIDDEN','DELETED')
                                 NOT NULL DEFAULT 'DRAFT' COMMENT '상품 판매 상태',
    stock_qty    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '현재 재고 수량',
    safety_stock INT UNSIGNED    NOT NULL DEFAULT 5 COMMENT '안전 재고선',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at   DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (product_id),
    KEY idx_product_seller (seller_id),
    KEY idx_product_status (status),
    KEY idx_product_created (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='상품';

CREATE TABLE product_image
(
    product_image_id  BIGINT UNSIGNED              NOT NULL AUTO_INCREMENT COMMENT '상품 이미지 ID',
    product_id        BIGINT UNSIGNED              NOT NULL COMMENT '상품 ID',
    product_image_url VARCHAR(500)                 NOT NULL COMMENT '이미지 URL',
    stored_file_name  VARCHAR(500)                 NULL COMMENT '스토리지 키',
    image_type        ENUM ('THUMBNAIL','GALLERY') NOT NULL COMMENT '이미지 타입',
    slot_index        TINYINT UNSIGNED             NOT NULL DEFAULT 0 COMMENT '슬롯 인덱스',
    created_at        DATETIME                     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 시각',
    updated_at        DATETIME                     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at        DATETIME                     NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (product_image_id),
    UNIQUE KEY uk_product_image_slot (product_id, image_type, slot_index),
    KEY idx_product_image_product (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='상품 이미지';

CREATE TABLE product_tag
(
    product_id BIGINT UNSIGNED NOT NULL COMMENT '상품 ID',
    tag_id     BIGINT UNSIGNED NOT NULL COMMENT '태그 ID',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '매핑 생성 시각',
    deleted_at DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (product_id, tag_id),
    KEY idx_pt_tag (tag_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='상품-태그 매핑';

CREATE TABLE setup
(
    setup_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '셋업 ID',
    seller_id       BIGINT UNSIGNED NOT NULL COMMENT '판매자 ID',
    setup_name      VARCHAR(100)    NOT NULL COMMENT '셋업명',
    short_desc      VARCHAR(250)    NOT NULL COMMENT '한 줄 소개',
    tip_text        VARCHAR(500)    NULL COMMENT 'Tip 문구(옵션)',
    setup_image_url VARCHAR(500)    NOT NULL COMMENT '셋업 썸네일 URL',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at      DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (setup_id),
    KEY idx_setup_seller (seller_id),
    KEY idx_setup_created (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='셋업';

CREATE TABLE setup_tag
(
    setup_id   BIGINT UNSIGNED NOT NULL COMMENT '셋업 ID',
    tag_id     BIGINT UNSIGNED NOT NULL COMMENT '태그 ID',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '매핑 생성 시각',
    deleted_at DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (setup_id, tag_id),
    KEY idx_st_tag (tag_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='셋업-태그 매핑';

CREATE TABLE setup_product
(
    setup_id   BIGINT UNSIGNED NOT NULL COMMENT '셋업 ID',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '상품 ID',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '매핑 생성 시각',
    deleted_at DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (setup_id, product_id),
    KEY idx_sp_product (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='셋업-상품 매핑';

-- ---------------------------------------------------------
-- [Transactions] Cart, Order
-- ---------------------------------------------------------
CREATE TABLE cart
(
    cart_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '장바구니 ID',
    member_id  BIGINT UNSIGNED NOT NULL COMMENT '회원 ID(회원당 1개 장바구니)',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (cart_id),
    UNIQUE KEY uk_cart_member (member_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='장바구니(회원 1:1)';

CREATE TABLE cart_item
(
    cart_item_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '장바구니 아이템 ID',
    cart_id        BIGINT UNSIGNED NOT NULL COMMENT '장바구니 ID',
    product_id     BIGINT UNSIGNED NOT NULL COMMENT '상품 ID',
    quantity       INT UNSIGNED    NOT NULL COMMENT '수량',
    price_snapshot INT UNSIGNED    NOT NULL COMMENT '담을 당시 가격 스냅샷',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at     DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (cart_item_id),
    UNIQUE KEY uk_cart_item (cart_id, product_id),
    KEY idx_cart_item_cart (cart_id),
    KEY idx_cart_item_product (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='장바구니 아이템';

CREATE TABLE `order`
(
    order_id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '주문 ID',
    member_id            BIGINT UNSIGNED NOT NULL COMMENT '회원 ID',
    addr_detail          VARCHAR(255)    NULL COMMENT '배송지 주소(스냅샷)',
    order_number         VARCHAR(50)     NOT NULL COMMENT '구매자 노출 주문번호(유니크)',
    total_product_amount INT UNSIGNED    NOT NULL COMMENT '상품 총액',
    shipping_fee         INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '배송비',
    discount_fee         INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '할인 금액',
    order_amount         INT UNSIGNED    NOT NULL COMMENT '최종 금액',
    `status`             ENUM ('CREATED','PAID','CANCEL_REQUESTED','CANCELLED','COMPLETED','REFUND_REQUESTED','REFUND_REJECTED','REFUNDED')
                                         NOT NULL DEFAULT 'CREATED' COMMENT '주문 상태',
    cancel_reason        VARCHAR(500)    NULL COMMENT '주문 취소 사유',
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    paid_at              DATETIME        NULL COMMENT '결제 완료 시각',
    cancel_requested_at  DATETIME        NULL COMMENT '취소 요청 시각',
    refund_requested_at  DATETIME        NULL COMMENT '환불 요청 시각',
    cancelled_at         DATETIME        NULL COMMENT '취소 시각',
    updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_order_number (order_number), -- 주석상 유니크인데 제약이 없어서 필수 보완
    KEY idx_order_member (member_id),
    KEY idx_order_status (status),
    KEY idx_order_created (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='주문';

CREATE TABLE order_item
(
    order_item_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '주문 아이템 ID',
    order_id       BIGINT UNSIGNED NOT NULL COMMENT '주문 ID',
    product_id     BIGINT UNSIGNED NOT NULL COMMENT '상품 ID',
    seller_id      BIGINT UNSIGNED NOT NULL COMMENT '판매자 ID',
    product_name   VARCHAR(100)    NOT NULL COMMENT '상품명 스냅샷',
    unit_price     INT UNSIGNED    NOT NULL COMMENT '주문 시점 단가',
    quantity       INT UNSIGNED    NOT NULL COMMENT '수량',
    subtotal_price INT UNSIGNED    NOT NULL COMMENT '소계',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    deleted_at     DATETIME        NULL COMMENT '논리삭제 시각(NULL=활성)',
    PRIMARY KEY (order_item_id),
    KEY idx_order_item_order (order_id),
    KEY idx_order_item_product (product_id),
    KEY idx_order_item_seller (seller_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='주문 아이템';

-- ---------------------------------------------------------
-- [Live Commerce] Broadcast, VOD, Chat
-- ---------------------------------------------------------
CREATE TABLE broadcast
(
    broadcast_id             BIGINT UNSIGNED                                                                 NOT NULL AUTO_INCREMENT,
    seller_id                BIGINT UNSIGNED                                                                 NOT NULL COMMENT 'FK: seller',
    tag_category_id          BIGINT UNSIGNED                                                                 NULL COMMENT 'FK: tag_category (옵션)', -- 옵션이면 NULL 허용이 맞음
    broadcast_title          VARCHAR(30)                                                                     NOT NULL,
    broadcast_notice         VARCHAR(100)                                                                    NULL,
    `status`                 ENUM ('RESERVED','READY','ON_AIR','ENDED','VOD','DELETED','CANCELED','STOPPED') NOT NULL DEFAULT 'RESERVED',
    scheduled_at             DATETIME                                                                        NOT NULL COMMENT '예약 시간',
    started_at               DATETIME                                                                        NULL,
    ended_at                 DATETIME                                                                        NULL,
    broadcast_thumb_url      VARCHAR(255)                                                                    NOT NULL,
    broadcast_wait_url       VARCHAR(255)                                                                    NULL,
    stream_key               VARCHAR(100)                                                                    NULL,
    broadcast_layout         ENUM ('FULL', 'LAYOUT_2', 'LAYOUT_3', 'LAYOUT_4')                               NOT NULL DEFAULT 'LAYOUT_4',
    broadcast_stopped_reason VARCHAR(50)                                                                     NULL,
    created_at               DATETIME                                                                        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME                                                                        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (broadcast_id),
    KEY idx_broadcast_seller (seller_id),
    KEY idx_broadcast_status_time (status, scheduled_at),
    KEY idx_broadcast_tag_category (tag_category_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='라이브 방송';

CREATE TABLE broadcast_product
(
    bp_id         BIGINT UNSIGNED                      NOT NULL AUTO_INCREMENT,
    broadcast_id  BIGINT UNSIGNED                      NOT NULL COMMENT 'FK: broadcast',
    product_id    BIGINT UNSIGNED                      NOT NULL COMMENT 'FK: product',
    display_order INT                                  NOT NULL,
    bp_price      INT                                  NULL COMMENT '방송 특가',
    bp_quantity   INT                                  NOT NULL COMMENT '방송용 재고',
    is_pinned     CHAR(1)                              NOT NULL DEFAULT 'N' COMMENT 'N/Y',
    `status`      ENUM ('SELLING','SOLDOUT','DELETED') NOT NULL DEFAULT 'SELLING',
    created_at    DATETIME                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME                             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (bp_id),
    KEY idx_bp_broadcast (broadcast_id),
    KEY idx_bp_product (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='방송-상품 매핑';

CREATE TABLE vod
(
    vod_id           BIGINT UNSIGNED                     NOT NULL AUTO_INCREMENT,
    broadcast_id     BIGINT UNSIGNED                     NOT NULL COMMENT 'FK: broadcast',
    vod_url          VARCHAR(255)                        NULL,
    vod_size         BIGINT                              NOT NULL DEFAULT 0,
    `status`         ENUM ('PUBLIC','PRIVATE','DELETED') NOT NULL DEFAULT 'PUBLIC',
    vod_report_count INT                                 NOT NULL DEFAULT 0,
    vod_duration     INT                                 NULL COMMENT '초 단위',
    vod_admin_lock   CHAR(1)                             NOT NULL DEFAULT 'N' COMMENT 'Y/N',
    created_at       DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (vod_id),
    KEY idx_vod_broadcast (broadcast_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='다시보기(VOD)';

CREATE TABLE qcard
(
    qcard_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    broadcast_id   BIGINT UNSIGNED NOT NULL,
    qcard_question VARCHAR(50)     NOT NULL,
    sort_order     INT             NOT NULL DEFAULT 0,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (qcard_id),
    KEY idx_qcard_broadcast (broadcast_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='큐카드(질문)';

CREATE TABLE view_history
(
    history_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    broadcast_id BIGINT UNSIGNED NOT NULL,
    viewer_id    VARCHAR(100)    NOT NULL, -- 로그인/비로그인 포함한 Viewer 식별자(현 구조 유지)
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (history_id),
    KEY idx_vh_broadcast (broadcast_id),
    KEY idx_vh_viewer (viewer_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='시청 기록';

CREATE TABLE broadcast_result
(
    broadcast_id   BIGINT UNSIGNED NOT NULL COMMENT 'PK이자 FK',
    total_views    INT             NOT NULL DEFAULT 0,
    max_views      INT             NOT NULL DEFAULT 0,
    max_views_at   DATETIME        NULL COMMENT '최대 동시시청 시각(미집계 시 NULL 허용)', -- 실데이터 미존재 시 INSERT 실패 방지
    total_likes    INT             NOT NULL DEFAULT 0,
    total_chats    INT             NOT NULL DEFAULT 0,
    total_sales    DECIMAL(30, 0)  NOT NULL DEFAULT 0,
    avg_watch_time INT             NOT NULL DEFAULT 0,
    total_reports  INT             NOT NULL DEFAULT 0,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (broadcast_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='방송 결과 통계';

-- ★ 핵심 변경: live_chat은 이메일만 저장 (member_id 컬럼/ FK 제거)
CREATE TABLE live_chat
(
    message_id    BIGINT UNSIGNED                                  NOT NULL AUTO_INCREMENT,
    broadcast_id  BIGINT UNSIGNED                                  NOT NULL,
    member_email  VARCHAR(255)                                     NOT NULL,
    msg_type      ENUM ('TALK','ENTER','EXIT','PURCHASE','NOTICE') NOT NULL COMMENT '채팅 메시지 유형',
    content       VARCHAR(500)                                     NOT NULL,
    raw_content   VARCHAR(500)                                     NOT NULL,
    send_nick     VARCHAR(50)                                      NOT NULL,
    is_world      BOOLEAN                                          NOT NULL DEFAULT FALSE,
    send_lchat    DATETIME                                         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vod_play_time INT                                              NOT NULL DEFAULT 0 COMMENT '방송 시작 후 경과 시간(초)',
    PRIMARY KEY (message_id),
    KEY idx_live_chat_broadcast_time (broadcast_id, send_lchat),
    KEY idx_live_chat_member_email (member_email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='라이브 채팅';

CREATE TABLE sanction
(
    sanction_id     BIGINT UNSIGNED         NOT NULL AUTO_INCREMENT,
    broadcast_id    BIGINT UNSIGNED         NOT NULL,
    member_id       BIGINT UNSIGNED         NOT NULL,
    actor_type      ENUM ('ADMIN','SELLER') NOT NULL DEFAULT 'SELLER',
    seller_id       BIGINT UNSIGNED         NULL,
    admin_id        BIGINT UNSIGNED         NULL,
    `status`        ENUM ('MUTE','OUT')     NOT NULL DEFAULT 'MUTE',
    sanction_reason VARCHAR(50)             NULL,
    created_at      DATETIME                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sanction_id),
    KEY idx_sanction_broadcast (broadcast_id),
    KEY idx_sanction_member (member_id),
    KEY idx_sanction_seller (seller_id),
    KEY idx_sanction_admin (admin_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='제재(강퇴/채금)';

-- ---------------------------------------------------------
-- [Social & Notifications]
-- ---------------------------------------------------------
CREATE TABLE follow
(
    follow_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    follower_id  BIGINT UNSIGNED NOT NULL COMMENT '팔로우하는 멤버 ID',
    following_id BIGINT UNSIGNED NOT NULL COMMENT '팔로우 받는 판매자 ID',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follow_id),
    KEY idx_follow_follower (follower_id),
    KEY idx_follow_following (following_id),
    UNIQUE KEY uk_follow_pair (follower_id, following_id) -- 중복 팔로우 방지
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='팔로우';

CREATE TABLE notification
(
    noti_id      BIGINT UNSIGNED                                        NOT NULL AUTO_INCREMENT,
    member_id    BIGINT UNSIGNED                                        NOT NULL,
    noti_type    ENUM ('LIVE_START','FOLLOW','NOTICE','EVENT','SYSTEM') NOT NULL COMMENT '알림 유형',
    noti_content VARCHAR(255)                                           NOT NULL,
    created_at   DATETIME                                               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (noti_id),
    KEY idx_noti_member (member_id),
    KEY idx_noti_created (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='알림';

-- ---------------------------------------------------------
-- [Seller Onboarding & Evaluation (AI/Admin)]
-- ---------------------------------------------------------
CREATE TABLE seller_register
(
    register_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    plan_file    LONGBLOB        NOT NULL COMMENT '사업계획서 등',
    description  TEXT            NULL COMMENT '설명',
    seller_id    BIGINT UNSIGNED NOT NULL COMMENT 'FK: seller',
    company_name VARCHAR(100)    NOT NULL,
    PRIMARY KEY (register_id),
    KEY idx_sr_seller (seller_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='입점 신청서';

CREATE TABLE ai_evaluation
(
    ai_eval_id         BIGINT UNSIGNED               NOT NULL AUTO_INCREMENT,
    business_stability INT                           NOT NULL,
    product_competency INT                           NOT NULL,
    live_suitability   INT                           NOT NULL,
    operation_coop     INT                           NOT NULL,
    growth_potential   INT                           NOT NULL,
    total_score        INT                           NOT NULL,
    grade_recommended  ENUM ('A','B','C','REJECTED') NOT NULL DEFAULT 'C',
    summary            TEXT                          NOT NULL,
    created_at         DATETIME                      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seller_id          BIGINT UNSIGNED               NOT NULL,
    register_id        BIGINT UNSIGNED               NOT NULL COMMENT 'FK: seller_register',
    PRIMARY KEY (ai_eval_id),
    KEY idx_ai_seller (seller_id),
    KEY idx_ai_register (register_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI 입점 평가';

CREATE TABLE admin_evaluation
(
    admin_eval_id      BIGINT UNSIGNED               NOT NULL AUTO_INCREMENT,
    business_stability INT                           NOT NULL,
    product_competency INT                           NOT NULL,
    live_suitability   INT                           NOT NULL,
    operation_coop     INT                           NOT NULL,
    growth_potential   INT                           NOT NULL,
    total_score        INT                           NOT NULL,
    grade_recommended  ENUM ('A','B','C','REJECTED') NOT NULL DEFAULT 'C',
    admin_comment      VARCHAR(250)                  NULL,
    created_at         DATETIME                      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ai_eval_id         BIGINT UNSIGNED               NOT NULL COMMENT 'FK: ai_evaluation',
    PRIMARY KEY (admin_eval_id),
    KEY idx_ae_ai_eval (ai_eval_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='관리자 최종 평가';

CREATE TABLE company_registered
(
    company_id      BIGINT UNSIGNED            NOT NULL AUTO_INCREMENT,
    company_name    VARCHAR(100)               NOT NULL,
    business_number VARCHAR(15)                NOT NULL,
    seller_id       BIGINT UNSIGNED            NOT NULL COMMENT 'FK: seller',
    created_at      DATETIME                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status`        ENUM ('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (company_id),
    KEY idx_cr_seller (seller_id),
    UNIQUE KEY uk_cr_business_number (business_number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='등록된 기업 정보';

CREATE TABLE seller_grade
(
    grade_id   BIGINT UNSIGNED                   NOT NULL AUTO_INCREMENT,
    grade      ENUM ('A', 'B', 'C', 'REJECTED')  NOT NULL DEFAULT 'C',
    `status`   ENUM ('ACTIVE', 'TEMP', 'REVIEW') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME                          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME                          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expired_at DATETIME                          NULL,
    company_id BIGINT UNSIGNED                   NOT NULL COMMENT 'FK: company_registered',
    PRIMARY KEY (grade_id),
    KEY idx_sg_company (company_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='판매자 등급';

CREATE TABLE invitation
(
    invitation_id BIGINT UNSIGNED                       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(100)                          NOT NULL,
    created_at    DATETIME                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME                              NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expired_at    DATETIME                              NOT NULL,
    `status`      ENUM ('PENDING','ACCEPTED','EXPIRED') NOT NULL DEFAULT 'PENDING',
    token         VARCHAR(255)                          NOT NULL,
    seller_id     BIGINT UNSIGNED                       NOT NULL COMMENT '초대자',
    PRIMARY KEY (invitation_id),
    KEY idx_inv_seller (seller_id),
    UNIQUE KEY uk_inv_token (token)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='초대장';

-- ---------------------------------------------------------
-- [Payment (Toss)]
-- ---------------------------------------------------------
CREATE TABLE toss_payment
(
    payment_id          BIGINT UNSIGNED                                                                                             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    toss_payment_key    VARCHAR(64)                                                                                                 NOT NULL UNIQUE,
    toss_order_id       VARCHAR(255)                                                                                                NOT NULL,
    toss_payment_method ENUM ('계좌이체','신용/체크카드')                                                                                     NOT NULL,
    `status`            ENUM ('ABORTED','CANCELED','DONE','EXPIRED','IN_PROGRESS','PARTIAL_CANCELED','READY','WAITING_FOR_DEPOSIT') NOT NULL,
    request_date        DATETIME                                                                                                    NOT NULL,
    approved_date       DATETIME                                                                                                    NULL     DEFAULT NULL,
    total_amount        BIGINT                                                                                                      NOT NULL,
    created_at          DATETIME                                                                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME                                                                                                    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    order_id            VARCHAR(36)                                                                                                 NOT NULL, -- 앱에서 별도 UUID/문자열로 관리하는 값이라면 FK 강제하지 않는 게 안전
    KEY idx_tp_order_id (order_id),
    KEY idx_tp_status (status),
    KEY idx_tp_created (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='토스 결제 정보';

CREATE TABLE toss_refund
(
    refund_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    refund_key       VARCHAR(100)    NOT NULL,
    refund_amount    BIGINT          NOT NULL,
    refund_reason    VARCHAR(255)    NULL,
    refund_status    VARCHAR(255)    NOT NULL,
    requested_at     DATETIME        NOT NULL,
    approved_at      DATETIME        NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_id       BIGINT UNSIGNED NOT NULL COMMENT 'toss_payment의 PK와 타입 일치(참조용 컬럼)',
    toss_payment_key VARCHAR(64)     NOT NULL COMMENT 'FK: toss_payment.toss_payment_key',
    PRIMARY KEY (refund_id),
    KEY idx_tr_payment_key (toss_payment_key),
    KEY idx_tr_payment_id (payment_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='토스 환불 정보';

CREATE TABLE toss_webhook_log
(
    webhook_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    event_type       VARCHAR(50)     NOT NULL,
    raw_body         JSON            NOT NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    toss_payment_key VARCHAR(64)     NOT NULL COMMENT 'FK: toss_payment.toss_payment_key',
    order_id         VARCHAR(36)     NOT NULL,
    PRIMARY KEY (webhook_id),
    KEY idx_twl_payment_key (toss_payment_key),
    KEY idx_twl_created (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='토스 웹훅 로그';

-- ---------------------------------------------------------
-- [CS / Chatbot Support]
-- ---------------------------------------------------------
CREATE TABLE chat_info
(
    chat_id    BIGINT UNSIGNED                                            NOT NULL AUTO_INCREMENT,
    `status`   ENUM ('BOT_ACTIVE', 'ADMIN_ACTIVE', 'ESCALATED', 'CLOSED') NOT NULL DEFAULT 'BOT_ACTIVE',
    created_at DATETIME                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    member_id  BIGINT UNSIGNED                                            NOT NULL COMMENT 'FK: member',
    PRIMARY KEY (chat_id),
    KEY idx_chat_member (member_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='CS 채팅방';

CREATE TABLE chat_message
(
    message_id BIGINT UNSIGNED                      NOT NULL AUTO_INCREMENT,
    content    TEXT                                 NOT NULL,
    sender     ENUM ('ASSISTANT', 'SYSTEM', 'USER') NOT NULL,
    created_at DATETIME                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    chat_id    BIGINT UNSIGNED                      NOT NULL COMMENT 'FK: chat_info',
    PRIMARY KEY (message_id),
    KEY idx_msg_chat (chat_id),
    KEY idx_msg_created (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='CS 메시지';

CREATE TABLE chat_handoff
(
    handoff_id        BIGINT UNSIGNED                                           NOT NULL AUTO_INCREMENT,
    assigned_admin_id BIGINT UNSIGNED                                           NULL COMMENT 'FK: admin',
    `status`          ENUM ('ADMIN_WAITING', 'ADMIN_CHECKED', 'ADMIN_ANSWERED') NOT NULL DEFAULT 'ADMIN_WAITING',
    created_at        DATETIME                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    chat_id           BIGINT UNSIGNED                                           NOT NULL COMMENT 'FK: chat_info',
    PRIMARY KEY (handoff_id),
    KEY idx_handoff_chat (chat_id),
    KEY idx_handoff_admin (assigned_admin_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='상담원 연결 요청';

CREATE TABLE spring_ai_chat_memory
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(255)                                 NOT NULL,
    type            ENUM ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL') NOT NULL,
    content         TEXT                                         NOT NULL,
    timestamp       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sacm_conversation (conversation_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='채팅 메모리';

-- =========================================================
-- 3. ALTER TABLE (FOREIGN KEYS)
-- =========================================================

-- [Tag & Category]
ALTER TABLE tag
    ADD CONSTRAINT FK_tag_category
        FOREIGN KEY (tag_category_id) REFERENCES tag_category (tag_category_id);

-- [Product Relations]
ALTER TABLE product
    ADD CONSTRAINT FK_product_seller
        FOREIGN KEY (seller_id) REFERENCES seller (seller_id);

ALTER TABLE product_image
    ADD CONSTRAINT FK_product_image_product
        FOREIGN KEY (product_id) REFERENCES product (product_id);

ALTER TABLE product_tag
    ADD CONSTRAINT FK_pt_product
        FOREIGN KEY (product_id) REFERENCES product (product_id);
ALTER TABLE product_tag
    ADD CONSTRAINT FK_pt_tag
        FOREIGN KEY (tag_id) REFERENCES tag (tag_id);

-- [Setup Relations]
ALTER TABLE setup
    ADD CONSTRAINT FK_setup_seller
        FOREIGN KEY (seller_id) REFERENCES seller (seller_id);

ALTER TABLE setup_tag
    ADD CONSTRAINT FK_st_setup
        FOREIGN KEY (setup_id) REFERENCES setup (setup_id);
ALTER TABLE setup_tag
    ADD CONSTRAINT FK_st_tag
        FOREIGN KEY (tag_id) REFERENCES tag (tag_id);

ALTER TABLE setup_product
    ADD CONSTRAINT FK_sp_setup
        FOREIGN KEY (setup_id) REFERENCES setup (setup_id);
ALTER TABLE setup_product
    ADD CONSTRAINT FK_sp_product
        FOREIGN KEY (product_id) REFERENCES product (product_id);

-- [Cart Relations]
ALTER TABLE cart
    ADD CONSTRAINT FK_cart_member
        FOREIGN KEY (member_id) REFERENCES member (member_id);

ALTER TABLE cart_item
    ADD CONSTRAINT FK_cart_item_cart
        FOREIGN KEY (cart_id) REFERENCES cart (cart_id);
ALTER TABLE cart_item
    ADD CONSTRAINT FK_cart_item_product
        FOREIGN KEY (product_id) REFERENCES product (product_id);

-- [Address Relations]
ALTER TABLE address
    ADD CONSTRAINT FK_address_member
        FOREIGN KEY (member_id) REFERENCES member (member_id);

-- [Order Relations]
ALTER TABLE `order`
    ADD CONSTRAINT FK_order_member
        FOREIGN KEY (member_id) REFERENCES member (member_id);

ALTER TABLE order_item
    ADD CONSTRAINT FK_order_item_order
        FOREIGN KEY (order_id) REFERENCES `order` (order_id);
ALTER TABLE order_item
    ADD CONSTRAINT FK_order_item_product
        FOREIGN KEY (product_id) REFERENCES product (product_id);
ALTER TABLE order_item
    ADD CONSTRAINT FK_order_item_seller
        FOREIGN KEY (seller_id) REFERENCES seller (seller_id);

-- [Broadcast Relations]
ALTER TABLE broadcast
    ADD CONSTRAINT FK_broadcast_seller
        FOREIGN KEY (seller_id) REFERENCES seller (seller_id);

ALTER TABLE broadcast
    ADD CONSTRAINT FK_broadcast_tag_cat
        FOREIGN KEY (tag_category_id) REFERENCES tag_category (tag_category_id);

ALTER TABLE broadcast_product
    ADD CONSTRAINT FK_bp_broadcast
        FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);
ALTER TABLE broadcast_product
    ADD CONSTRAINT FK_bp_product
        FOREIGN KEY (product_id) REFERENCES product (product_id);

ALTER TABLE vod
    ADD CONSTRAINT FK_vod_broadcast
        FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);

ALTER TABLE qcard
    ADD CONSTRAINT FK_qcard_broadcast
        FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);

ALTER TABLE broadcast_result
    ADD CONSTRAINT FK_br_broadcast
        FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);

-- [Live Interaction Relations]
ALTER TABLE view_history
    ADD CONSTRAINT FK_vh_broadcast
        FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);

ALTER TABLE live_chat
    ADD CONSTRAINT FK_lc_broadcast
        FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);
-- NOTE: live_chat은 member_email만 저장하므로 member FK 없음

ALTER TABLE sanction
    ADD CONSTRAINT FK_sanction_broadcast
        FOREIGN KEY (broadcast_id) REFERENCES broadcast (broadcast_id);
ALTER TABLE sanction
    ADD CONSTRAINT FK_sanction_member
        FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE sanction
    ADD CONSTRAINT FK_sanction_seller
        FOREIGN KEY (seller_id) REFERENCES seller (seller_id);
ALTER TABLE sanction
    ADD CONSTRAINT FK_sanction_admin
        FOREIGN KEY (admin_id) REFERENCES admin (admin_id);

-- [Social Relations]
ALTER TABLE follow
    ADD CONSTRAINT FK_follow_follower
        FOREIGN KEY (follower_id) REFERENCES member (member_id);
ALTER TABLE follow
    ADD CONSTRAINT FK_follow_following
        FOREIGN KEY (following_id) REFERENCES seller (seller_id);

ALTER TABLE notification
    ADD CONSTRAINT FK_noti_member
        FOREIGN KEY (member_id) REFERENCES member (member_id);

-- [Seller Evaluation Relations]
ALTER TABLE seller_register
    ADD CONSTRAINT FK_sr_seller
        FOREIGN KEY (seller_id) REFERENCES seller (seller_id);

ALTER TABLE ai_evaluation
    ADD CONSTRAINT FK_ai_reg
        FOREIGN KEY (register_id) REFERENCES seller_register (register_id);
ALTER TABLE ai_evaluation
    ADD CONSTRAINT FK_ai_seller
        FOREIGN KEY (seller_id) REFERENCES seller (seller_id);

ALTER TABLE admin_evaluation
    ADD CONSTRAINT FK_ae_ai
        FOREIGN KEY (ai_eval_id) REFERENCES ai_evaluation (ai_eval_id);

ALTER TABLE company_registered
    ADD CONSTRAINT FK_cr_seller
        FOREIGN KEY (seller_id) REFERENCES seller (seller_id);

ALTER TABLE seller_grade
    ADD CONSTRAINT FK_sg_company
        FOREIGN KEY (company_id) REFERENCES company_registered (company_id);

ALTER TABLE invitation
    ADD CONSTRAINT FK_invitation_seller
        FOREIGN KEY (seller_id) REFERENCES seller (seller_id);

-- [Payment Relations]
ALTER TABLE toss_refund
    ADD CONSTRAINT FK_refund_payment_key
        FOREIGN KEY (toss_payment_key) REFERENCES toss_payment (toss_payment_key);

ALTER TABLE toss_webhook_log
    ADD CONSTRAINT FK_webhook_payment_key
        FOREIGN KEY (toss_payment_key) REFERENCES toss_payment (toss_payment_key);

-- [CS Relations]
ALTER TABLE chat_info
    ADD CONSTRAINT FK_chat_member
        FOREIGN KEY (member_id) REFERENCES member (member_id);

ALTER TABLE chat_message
    ADD CONSTRAINT FK_msg_chat
        FOREIGN KEY (chat_id) REFERENCES chat_info (chat_id);

ALTER TABLE chat_handoff
    ADD CONSTRAINT FK_handoff_chat
        FOREIGN KEY (chat_id) REFERENCES chat_info (chat_id);

ALTER TABLE chat_handoff
    ADD CONSTRAINT FK_handoff_admin
        FOREIGN KEY (assigned_admin_id) REFERENCES admin (admin_id);
ALTER TABLE forbidden_word
    ADD CONSTRAINT uq_forbidden_word UNIQUE (word);

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE forbidden_word
    MODIFY word VARCHAR(50)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_bin
        NOT NULL;

ALTER TABLE forbidden_word
    ADD CONSTRAINT chk_word_not_empty
        CHECK (CHAR_LENGTH(word) > 0);