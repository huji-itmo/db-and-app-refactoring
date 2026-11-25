package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PricingCalculator pricingCalculator = new PricingCalculator();
    private final OrderValidator orderValidator;
    private final OrderEmailService orderEmailService;

    public OrderService(
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderValidator orderValidator,
            OrderEmailService orderEmailService) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderValidator = orderValidator;
        this.orderEmailService = orderEmailService;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        orderValidator.validate(request);

        List<Product> products = new ArrayList<>();
        List<PricingCalculator.LineItem> lineItems = new ArrayList<>();
        for (CreateOrderRequest.Item raw : request.items()) {
            Product product = productRepository.findById(raw.productId())
                    .orElseThrow(() -> new IllegalArgumentException("product " + raw.productId() + " not found"));
            int quantity = raw.quantity() == null ? 1 : raw.quantity();
            products.add(product);
            lineItems.add(new PricingCalculator.LineItem(product.getPrice(), quantity));
        }

        BigDecimal total = pricingCalculator.calculateOrderTotal(
                lineItems, request.customerType() == null ? "regular" : request.customerType(), request.couponCode());

        String[] name = request.customerFullName().split(" ", 2);
        String firstName = name[0];
        String lastName = name.length == 2 ? name[1] : null;
        customerRepository.insertIfAbsent(
                firstName, lastName, request.customerAddress(), request.customerPhone());
        Customer customer = customerRepository.findByFirstNameAndLastNameAndAddressAndPhone(
                firstName, lastName, request.customerAddress(), request.customerPhone());
        Order order = orderRepository.save(new Order(customer, "new"));

        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            int quantity = lineItems.get(i).quantity();
            orderItemRepository.save(new OrderItem(order.getId(), product, quantity));
        }

        orderEmailService.sendConfirmationEmail(request.customerFullName(), order.getId(), total);

        return order;
    }
}
