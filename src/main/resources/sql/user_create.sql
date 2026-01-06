########## MEMBER ##########
DROP TABLE member;
INSERT INTO member(name, login_id, profile, phone, is_agreed, mbti, role, job_category)
VALUES
    ('홍길동', 'gildong2@member.com', '', '010-1111-2222', 1, 'ENFP', 'ROLE_MEMBER', 'CREATIVE_TYPE'),
    ('김민수', 'minsu1@member.com', '', '010-2222-3333', 1, 'ISTJ', 'ROLE_MEMBER', 'ADMIN_PLAN_TYPE'),
    ('이서연', 'seoyeon1@member.com', '', '010-3333-4444', 1, 'INFJ', 'ROLE_MEMBER', 'CREATIVE_TYPE'),
    ('박지훈', 'jihoon1@member.com', '', '010-4444-5555', 1, 'ENTP', 'ROLE_MEMBER', 'FLEXIBLE_TYPE'),
    ('최유진', 'yujin1@member.com', '', '010-5555-6666', 1, 'ISFP', 'ROLE_MEMBER', 'EDU_RES_TYPE'),
    ('정현우', 'hyunwoo1@member.com', '', '010-6666-7777', 1, 'INTJ', 'ROLE_MEMBER', 'MED_PRO_TYPE'),
    ('한소희', 'sohee1@member.com', '', '010-7777-8888', 1, 'ESFJ', 'ROLE_MEMBER', 'CREATIVE_TYPE'),
    ('오준혁', 'junhyuk1@member.com', '', '010-8888-9999', 1, 'ENTJ', 'ROLE_MEMBER', 'ADMIN_PLAN_TYPE'),
    ('윤하늘', 'haneul1@member.com', '', '010-9999-0000', 1, 'INFP', 'ROLE_MEMBER', 'NONE'),
    ('장도윤', 'doyoon1@member.com', '', '010-1212-3434', 1, 'ESTP', 'ROLE_MEMBER', 'FLEXIBLE_TYPE'),
    ('백지민', 'jimin1@member.com', '', '010-3434-5656', 1, 'ISFJ', 'ROLE_MEMBER', 'EDU_RES_TYPE');

########## SELLER ##########
DROP TABLE seller;
INSERT INTO seller(name, login_id, profile, phone, is_agreed, role)
VALUES
    ('이진수', 'binarylee@seller.com', '', '010-1234-5678', 1, 'ROLE_SELLER_OWNER'),
    ('박서준', 'seojun@seller.com', '', '010-2001-3002', 1, 'ROLE_SELLER_MANAGER'),
    ('이수민', 'sumin@seller.com', '', '010-2001-3003', 1, 'ROLE_SELLER_MANAGER'),
    ('최현석', 'hyunsuk@seller.com', '', '010-2001-3004', 1, 'ROLE_SELLER_OWNER'),
    ('정다은', 'daeun@seller.com', '', '010-2001-3005', 1, 'ROLE_SELLER_MANAGER'),

    ('윤태호', 'taeho@seller.com', '', '010-2001-3006', 1, 'ROLE_SELLER_OWNER'),
    ('한지민', 'jimin@seller.com', '', '010-2001-3007', 1, 'ROLE_SELLER_MANAGER'),
    ('오세훈', 'sehun@seller.com', '', '010-2001-3008', 1, 'ROLE_SELLER_MANAGER'),
    ('백승우', 'seungwoo@seller.com', '', '010-2001-3009', 1, 'ROLE_SELLER_OWNER'),
    ('임하린', 'harin@seller.com', '', '010-2001-3010', 1, 'ROLE_SELLER_MANAGER'),

    ('조민재', 'minjae@seller.com', '', '010-2001-3011', 1, 'ROLE_SELLER_MANAGER'),
    ('강유라', 'yura@seller.com', '', '010-2001-3012', 1, 'ROLE_SELLER_OWNER'),
    ('신동혁', 'donghyuk@seller.com', '', '010-2001-3013', 1, 'ROLE_SELLER_MANAGER'),
    ('문채원', 'chaewon@seller.com', '', '010-2001-3014', 1, 'ROLE_SELLER_MANAGER'),
    ('서강준', 'kangjun@seller.com', '', '010-2001-3015', 1, 'ROLE_SELLER_OWNER');

