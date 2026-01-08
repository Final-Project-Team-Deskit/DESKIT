package com.deskit.deskit.order.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.deskit.deskit.account.entity.Member;
import com.deskit.deskit.account.entity.Seller;
import com.deskit.deskit.account.enums.JobCategory;
import com.deskit.deskit.account.enums.MBTI;
import com.deskit.deskit.account.enums.MemberStatus;
import com.deskit.deskit.account.enums.SellerRole;
import com.deskit.deskit.account.enums.SellerStatus;
import com.deskit.deskit.order.entity.Order;
import com.deskit.deskit.order.entity.OrderItem;
import com.deskit.deskit.order.enums.OrderStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private OrderItemRepository orderItemRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  void saveAndLoadOrderWithItems() {
    Member member = Member.builder()
      .name("Test Member")
      .loginId("member@test.com")
      .phone("010-0000-0000")
      .isAgreed(true)
      .status(MemberStatus.ACTIVE)
      .role("ROLE_MEMBER")
      .mbti(MBTI.NONE)
      .jobCategory(JobCategory.NONE)
      .build();
    entityManager.persist(member);

    Seller seller = Seller.builder()
      .status(SellerStatus.ACTIVE)
      .name("Test Seller")
      .loginId("seller@test.com")
      .phone("010-1000-1000")
      .role(SellerRole.ROLE_SELLER_OWNER)
      .isAgreed(true)
      .build();
    entityManager.persist(seller);
    entityManager.flush();

    Long memberId = member.getMemberId();
    Long sellerId = seller.getSellerId();
    assertNotNull(memberId);
    assertNotNull(sellerId);

    entityManager.getEntityManager().createNativeQuery(
      "INSERT INTO product (product_id, seller_id, product_name, short_desc, detail_html, " +
      "price, cost_price, status, stock_qty, safety_stock, created_at, updated_at) " +
      "VALUES (:productId, :sellerId, 'Test Product A', 'Short A', '<p>Detail A</p>', " +
      "10000, 12000, 'ON_SALE', 10, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    ).setParameter("productId", 10L)
      .setParameter("sellerId", sellerId)
      .executeUpdate();

    entityManager.getEntityManager().createNativeQuery(
      "INSERT INTO product (product_id, seller_id, product_name, short_desc, detail_html, " +
      "price, cost_price, status, stock_qty, safety_stock, created_at, updated_at) " +
      "VALUES (:productId, :sellerId, 'Test Product B', 'Short B', '<p>Detail B</p>', " +
      "10000, 12000, 'ON_SALE', 10, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    ).setParameter("productId", 11L)
      .setParameter("sellerId", sellerId)
      .executeUpdate();

    Order order = Order.create(
      memberId,
      "ORD-TEST-001",
      30000,
      3000,
      0,
      33000,
      OrderStatus.CREATED
    );
    Order savedOrder = orderRepository.save(order);
    assertNotNull(savedOrder.getId());

    OrderItem item1 = OrderItem.create(
      savedOrder,
      10L,
      sellerId,
      "Test Product A",
      10000,
      2,
      20000
    );
    OrderItem item2 = OrderItem.create(
      savedOrder,
      11L,
      sellerId,
      "Test Product B",
      10000,
      1,
      10000
    );
    orderItemRepository.save(item1);
    orderItemRepository.save(item2);

    List<Order> orders = orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    assertEquals(1, orders.size());
    assertEquals(savedOrder.getId(), orders.get(0).getId());

    List<OrderItem> items = orderItemRepository.findByOrder_Id(savedOrder.getId());
    assertEquals(2, items.size());
  }
}
