package com.deskit.deskit.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.deskit.deskit.account.entity.Member;
import com.deskit.deskit.account.entity.Seller;
import com.deskit.deskit.account.enums.JobCategory;
import com.deskit.deskit.account.enums.MBTI;
import com.deskit.deskit.account.enums.MemberStatus;
import com.deskit.deskit.account.enums.SellerRole;
import com.deskit.deskit.account.enums.SellerStatus;
import com.deskit.deskit.order.dto.CreateOrderItemRequest;
import com.deskit.deskit.order.dto.CreateOrderRequest;
import com.deskit.deskit.order.dto.CreateOrderResponse;
import com.deskit.deskit.order.entity.Order;
import com.deskit.deskit.order.enums.OrderStatus;
import com.deskit.deskit.order.repository.OrderItemRepository;
import com.deskit.deskit.order.repository.OrderRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(OrderService.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderServiceTest {

  @Autowired
  private OrderService orderService;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private OrderItemRepository orderItemRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  void createOrderPersistsOrderAndItems() {
    Member member = persistMember();
    Seller seller = persistSeller();
    insertProduct(10L, seller.getSellerId(), "Test Product A", 10);
    insertProduct(11L, seller.getSellerId(), "Test Product B", 10);

    CreateOrderRequest request = new CreateOrderRequest(List.of(
      new CreateOrderItemRequest(10L, 2),
      new CreateOrderItemRequest(11L, 1)
    ));

    CreateOrderResponse response = orderService.createOrder(member.getMemberId(), request);
    assertNotNull(response.orderId());
    assertEquals(OrderStatus.CREATED, response.status());

    Order order = orderRepository.findById(response.orderId()).orElse(null);
    assertNotNull(order);
    assertEquals(2, orderItemRepository.findByOrder_Id(response.orderId()).size());

    int stockA = ((Number) entityManager.getEntityManager()
      .createNativeQuery("SELECT stock_qty FROM product WHERE product_id = :productId")
      .setParameter("productId", 10L)
      .getSingleResult()).intValue();
    int stockB = ((Number) entityManager.getEntityManager()
      .createNativeQuery("SELECT stock_qty FROM product WHERE product_id = :productId")
      .setParameter("productId", 11L)
      .getSingleResult()).intValue();
    assertEquals(8, stockA);
    assertEquals(9, stockB);
  }

  @Test
  void createOrderRejectsInvalidQuantity() {
    Member member = persistMember();
    Seller seller = persistSeller();
    insertProduct(20L, seller.getSellerId(), "Test Product C", 10);

    CreateOrderRequest request = new CreateOrderRequest(List.of(
      new CreateOrderItemRequest(20L, 0)
    ));

    ResponseStatusException ex = assertThrows(
      ResponseStatusException.class,
      () -> orderService.createOrder(member.getMemberId(), request)
    );
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void createOrderRejectsInsufficientStock() {
    Member member = persistMember();
    Seller seller = persistSeller();
    insertProduct(30L, seller.getSellerId(), "Test Product D", 1);

    CreateOrderRequest request = new CreateOrderRequest(List.of(
      new CreateOrderItemRequest(30L, 2)
    ));

    ResponseStatusException ex = assertThrows(
      ResponseStatusException.class,
      () -> orderService.createOrder(member.getMemberId(), request)
    );
    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

    int stock = ((Number) entityManager.getEntityManager()
      .createNativeQuery("SELECT stock_qty FROM product WHERE product_id = :productId")
      .setParameter("productId", 30L)
      .getSingleResult()).intValue();
    assertEquals(1, stock);
  }

  private Member persistMember() {
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
    entityManager.flush();
    return member;
  }

  private Seller persistSeller() {
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
    return seller;
  }

  private void insertProduct(Long productId, Long sellerId, String productName, int stockQty) {
    entityManager.getEntityManager().createNativeQuery(
      "INSERT INTO product (product_id, seller_id, product_name, short_desc, detail_html, " +
      "price, cost_price, status, stock_qty, safety_stock, created_at, updated_at) " +
      "VALUES (:productId, :sellerId, :productName, 'Short', '<p>Detail</p>', " +
      "10000, 12000, 'ON_SALE', :stockQty, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    ).setParameter("productId", productId)
      .setParameter("sellerId", sellerId)
      .setParameter("productName", productName)
      .setParameter("stockQty", stockQty)
      .executeUpdate();
  }
}
