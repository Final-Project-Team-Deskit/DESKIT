package com.deskit.deskit.order.service;

import com.deskit.deskit.account.repository.MemberRepository;
import com.deskit.deskit.order.dto.CreateOrderItemRequest;
import com.deskit.deskit.order.dto.CreateOrderRequest;
import com.deskit.deskit.order.dto.CreateOrderResponse;
import com.deskit.deskit.order.dto.OrderDetailResponse;
import com.deskit.deskit.order.dto.OrderItemResponse;
import com.deskit.deskit.order.dto.OrderStatusUpdateRequest;
import com.deskit.deskit.order.dto.OrderStatusUpdateResponse;
import com.deskit.deskit.order.dto.OrderSummaryResponse;
import com.deskit.deskit.order.entity.Order;
import com.deskit.deskit.order.entity.OrderItem;
import com.deskit.deskit.order.enums.OrderStatus;
import com.deskit.deskit.order.repository.OrderItemRepository;
import com.deskit.deskit.order.repository.OrderRepository;
import com.deskit.deskit.product.entity.Product;
import com.deskit.deskit.product.repository.ProductRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final ProductRepository productRepository;
  private final MemberRepository memberRepository;

  public OrderService(OrderRepository orderRepository,
                      OrderItemRepository orderItemRepository,
                      ProductRepository productRepository,
                      MemberRepository memberRepository) {
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
    this.productRepository = productRepository;
    this.memberRepository = memberRepository;
  }

  public CreateOrderResponse createOrder(Long memberId, CreateOrderRequest request) {
    if (memberId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "member_id required");
    }
    if (!memberRepository.existsById(memberId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "member not found");
    }
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request required");
    }

    List<CreateOrderItemRequest> items = request.items();
    if (items == null || items.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items required");
    }

    Map<Long, Integer> quantityByProductId = new HashMap<>();
    for (CreateOrderItemRequest item : items) {
      if (item == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item required");
      }
      Long productId = item.productId();
      if (productId == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "product_id required");
      }
      int quantity = safeQuantity(item.quantity());
      quantityByProductId.merge(productId, quantity, Integer::sum);
    }

    List<Long> productIds = new ArrayList<>(quantityByProductId.keySet());
    Collections.sort(productIds);

    Map<Long, Product> productsById = new HashMap<>();
    for (Long productId : productIds) {
      Product product = productRepository.findByIdForUpdate(productId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "product not found"));
      int requestedQty = quantityByProductId.get(productId);
      Integer stockQty = product.getStockQty();
      int currentStock = stockQty == null ? 0 : stockQty;
      if (currentStock < requestedQty) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "insufficient stock: product_id=" + productId);
      }
      product.decreaseStock(requestedQty);
      productsById.put(productId, product);
    }

    int totalProductAmount = 0;
    for (CreateOrderItemRequest item : items) {
      int quantity = safeQuantity(item.quantity());
      Product product = productsById.get(item.productId());
      totalProductAmount += product.getPrice() * quantity;
    }

    int shippingFee = 0;
    int discountFee = 0;
    int orderAmount = totalProductAmount - discountFee + shippingFee;
    String orderNumber = generateOrderNumber();

    Order order = Order.create(
      memberId,
      orderNumber,
      totalProductAmount,
      shippingFee,
      discountFee,
      orderAmount,
      OrderStatus.CREATED
    );
    Order savedOrder = orderRepository.save(order);

    for (CreateOrderItemRequest item : items) {
      int quantity = safeQuantity(item.quantity());
      Product product = productsById.get(item.productId());
      int unitPrice = product.getPrice();
      int subtotal = unitPrice * quantity;
      OrderItem orderItem = OrderItem.create(
        savedOrder,
        product.getId(),
        product.getSellerId(),
        product.getProductName(),
        unitPrice,
        quantity,
        subtotal
      );
      orderItemRepository.save(orderItem);
    }

    return new CreateOrderResponse(
      savedOrder.getId(),
      savedOrder.getOrderNumber(),
      savedOrder.getStatus(),
      savedOrder.getOrderAmount()
    );
  }

  @Transactional(readOnly = true)
  public List<OrderSummaryResponse> getMyOrders(Long memberId) {
    if (memberId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "member_id required");
    }
    if (!memberRepository.existsById(memberId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "member not found");
    }

    return orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
      .stream()
      .map(OrderSummaryResponse::from)
      .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public OrderDetailResponse getMyOrderDetail(Long memberId, Long orderId) {
    if (memberId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "member_id required");
    }
    if (!memberRepository.existsById(memberId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "member not found");
    }
    if (orderId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "order_id required");
    }

    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    if (!order.getMemberId().equals(memberId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
    }

    List<OrderItemResponse> items = orderItemRepository.findByOrder_Id(orderId)
      .stream()
      .map(OrderItemResponse::from)
      .collect(Collectors.toList());

    return OrderDetailResponse.from(order, items);
  }

  public OrderStatusUpdateResponse updateOrderStatus(Long memberId, Long orderId, OrderStatusUpdateRequest request) {
    if (memberId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "member_id required");
    }
    if (!memberRepository.existsById(memberId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "member not found");
    }
    if (orderId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "order_id required");
    }
    if (request == null || request.status() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status required");
    }

    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    if (!order.getMemberId().equals(memberId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
    }

    OrderStatus newStatus = request.status();
    if (newStatus == order.getStatus()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status unchanged");
    }

    order.changeStatus(newStatus);
    orderRepository.save(order);
    return new OrderStatusUpdateResponse(order.getId(), order.getStatus());
  }

  private int safeQuantity(Integer quantity) {
    if (quantity == null || quantity < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be >= 1");
    }
    return quantity;
  }

  private String generateOrderNumber() {
    long now = System.currentTimeMillis();
    int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
    return "ORD-" + now + "-" + suffix;
  }
}
