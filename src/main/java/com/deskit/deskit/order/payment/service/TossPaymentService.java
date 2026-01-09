package com.deskit.deskit.order.payment.service;

import com.deskit.deskit.order.entity.Order;
import com.deskit.deskit.order.enums.OrderStatus;
import com.deskit.deskit.order.payment.dto.TossPaymentConfirmRequest;
import com.deskit.deskit.order.payment.dto.TossPaymentConfirmResult;
import com.deskit.deskit.order.payment.entity.TossPayment;
import com.deskit.deskit.order.payment.repository.TossPaymentRepository;
import com.deskit.deskit.order.repository.OrderRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TossPaymentService {

  private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

  private final OrderRepository orderRepository;
  private final TossPaymentRepository tossPaymentRepository;
  private final ObjectMapper objectMapper;

  @Value("${toss.payments.secret-key}")
  private String tossSecretKey;

  public TossPaymentService(
    OrderRepository orderRepository,
    TossPaymentRepository tossPaymentRepository,
    ObjectMapper objectMapper
  ) {
    this.orderRepository = orderRepository;
    this.tossPaymentRepository = tossPaymentRepository;
    this.objectMapper = objectMapper;
  }

  public TossPaymentConfirmResult confirmPayment(TossPaymentConfirmRequest request) {
    String paymentKey = normalizeText(request.paymentKey());
    String orderIdText = normalizeText(request.orderId());
    Long amount = request.amount();

    if (tossSecretKey == null || tossSecretKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "missing toss secret key");
    }
    if (paymentKey.isEmpty() || orderIdText.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid payment request");
    }
    if (amount == null || amount < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid amount");
    }

    Order order = findOrderForUpdate(orderIdText);

    Integer orderAmount = order.getOrderAmount();
    long expectedAmount = orderAmount == null ? 0L : orderAmount;
    if (expectedAmount != amount) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount mismatch");
    }

    Optional<TossPayment> existing = tossPaymentRepository.findByTossPaymentKey(paymentKey);
    if (existing.isPresent()) {
      updateOrderPaid(order);
      return new TossPaymentConfirmResult(
        HttpStatus.OK.value(),
        buildResponseFrom(existing.get())
      );
    }

    Map<String, Object> body = new HashMap<>();
    body.put("paymentKey", paymentKey);
    body.put("orderId", orderIdText);
    body.put("amount", amount);

    String idempotencyKey = generateIdempotencyKey(paymentKey, orderIdText, amount);

    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(CONFIRM_URL).openConnection();
      connection.setRequestProperty("Authorization", basicAuthHeader(tossSecretKey));
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Idempotency-Key", idempotencyKey);
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);

      try (OutputStream outputStream = connection.getOutputStream()) {
        objectMapper.writeValue(outputStream, body);
      }

      int statusCode = connection.getResponseCode();
      boolean isSuccess = statusCode == HttpURLConnection.HTTP_OK;
      try (InputStream responseStream = isSuccess
        ? connection.getInputStream()
        : connection.getErrorStream()) {
        Map<String, Object> responseBody = objectMapper.readValue(
          responseStream,
          new TypeReference<>() {}
        );

        if (isSuccess) {
          TossPayment payment = toEntity(responseBody, orderIdText);
          tossPaymentRepository.save(payment);
          updateOrderPaid(order);
        }

        return new TossPaymentConfirmResult(statusCode, responseBody);
      }
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "toss confirm failed", ex);
    }
  }

  private void updateOrderPaid(Order order) {
    if (order.getStatus() == OrderStatus.PAID) {
      return;
    }
    if (order.getStatus() != OrderStatus.CREATED) {
      return;
    }
    order.markPaid();
  }

  private Order findOrderForUpdate(String orderIdText) {
    if (orderIdText.matches("^[0-9]+$")) {
      try {
        Long id = Long.parseLong(orderIdText);
        Optional<Order> byId = orderRepository.findByIdForUpdate(id);
        if (byId.isPresent()) {
          return byId.get();
        }
      } catch (NumberFormatException ignored) {
      }
    }

    return orderRepository.findByOrderNumberForUpdate(orderIdText)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
  }

  private TossPayment toEntity(Map<String, Object> responseBody, String orderIdText) {
    String paymentKey = asText(responseBody.get("paymentKey"));
    String tossOrderId = asText(responseBody.get("orderId"));
    String method = mapMethod(asText(responseBody.get("method")));
    String status = asText(responseBody.get("status"));
    Long totalAmount = asLong(responseBody.get("totalAmount"));
    LocalDateTime requestedAt = parseDate(asText(responseBody.get("requestedAt")));
    LocalDateTime approvedAt = parseDate(asText(responseBody.get("approvedAt")));

    if (paymentKey == null || paymentKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "missing payment key");
    }

    return TossPayment.create(
      paymentKey,
      tossOrderId == null || tossOrderId.isBlank() ? orderIdText : tossOrderId,
      method,
      status,
      requestedAt != null ? requestedAt : LocalDateTime.now(),
      approvedAt,
      totalAmount == null ? 0L : totalAmount,
      orderIdText
    );
  }

  private String mapMethod(String method) {
    if (method == null) {
      return "신용/체크카드";
    }
    if (method.contains("계좌")) {
      return "계좌이체";
    }
    if (method.contains("카드")) {
      return "신용/체크카드";
    }
    return "신용/체크카드";
  }

  private LocalDateTime parseDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return OffsetDateTime.parse(value).toLocalDateTime();
  }

  private Map<String, Object> buildResponseFrom(TossPayment payment) {
    Map<String, Object> response = new HashMap<>();
    response.put("paymentKey", payment.getTossPaymentKey());
    response.put("orderId", payment.getTossOrderId());
    response.put("status", payment.getStatus());
    response.put("totalAmount", payment.getTotalAmount());
    response.put("method", payment.getTossPaymentMethod());
    response.put("requestedAt", payment.getRequestDate());
    response.put("approvedAt", payment.getApprovedDate());
    return response;
  }

  private String basicAuthHeader(String secretKey) {
    String raw = secretKey + ":";
    String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    return "Basic " + encoded;
  }

  private String generateIdempotencyKey(String paymentKey, String orderId, Long amount) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String source = paymentKey + ":" + orderId + ":" + amount;
      byte[] hashed = digest.digest(source.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(hashed);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "idempotency failed", ex);
    }
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private String normalizeText(String value) {
    return value == null ? "" : value.trim();
  }

  private String asText(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private Long asLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
