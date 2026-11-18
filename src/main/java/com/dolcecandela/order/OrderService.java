package com.dolcecandela.order;

import com.dolcecandela.catalog.CustomizationOption;
import com.dolcecandela.catalog.CustomizationOptionRepository;
import com.dolcecandela.catalog.OptionType;
import com.dolcecandela.catalog.Product;
import com.dolcecandela.catalog.ProductRepository;
import com.dolcecandela.common.ApiException;
import com.dolcecandela.common.Money;
import com.dolcecandela.config.AppProperties;
import com.dolcecandela.delivery.Delivery;
import com.dolcecandela.delivery.DeliveryMethod;
import com.dolcecandela.delivery.DeliveryStatus;
import com.dolcecandela.order.dto.OrderDtos.CreateOrderRequest;
import com.dolcecandela.order.dto.OrderDtos.CustomOrderRequest;
import com.dolcecandela.order.dto.OrderDtos.DeliveryRequest;
import com.dolcecandela.order.dto.OrderDtos.DeliveryUpdateRequest;
import com.dolcecandela.order.dto.OrderDtos.OrderItemRequest;
import com.dolcecandela.order.dto.OrderDtos.OrderResponse;
import com.dolcecandela.order.dto.OrderDtos.OrderTrackingResponse;
import com.dolcecandela.order.dto.OrderDtos.PaymentRequest;
import com.dolcecandela.payment.Payment;
import com.dolcecandela.payment.PaymentEnums.PaymentStatus;
import com.dolcecandela.security.AuthenticatedUser;
import com.dolcecandela.storage.FileStorageService;
import com.dolcecandela.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class OrderService {

    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");
    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    /** Alphabet sans caractères ambigus (0/O, 1/I) pour des références faciles à dicter au téléphone. */
    private static final char[] REF_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int MAX_COLORS_PER_ITEM = 3;
    private static final String CUSTOM_ITEM_NAME = "Création personnalisée";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomizationOptionRepository optionRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final FileStorageService storageService;
    private final String pickupLocation;
    private final SecureRandom random = new SecureRandom();

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        CustomizationOptionRepository optionRepository, UserRepository userRepository,
                        PricingService pricingService, FileStorageService storageService,
                        AppProperties properties) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.optionRepository = optionRepository;
        this.userRepository = userRepository;
        this.pricingService = pricingService;
        this.storageService = storageService;
        this.pickupLocation = properties.business().pickupLocation();
    }

    // =====================================================================
    //  Côté client
    // =====================================================================

    /** Commande depuis le panier (client connecté ou invité). */
    public OrderResponse createOrder(CreateOrderRequest request, AuthenticatedUser currentUser) {
        Order order = newOrder(OrderType.STANDARD, request.customerName(), request.customerPhone(),
                request.customerEmail(), currentUser);
        order.setNotes(request.notes());

        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> ApiException.badRequest("Produit indisponible (id " + itemRequest.productId() + ")."));

            List<CustomizationOption> options = resolveOptions(product, itemRequest.optionIds());
            if (itemRequest.customText() != null && !itemRequest.customText().isBlank() && !product.isCustomizable()) {
                throw ApiException.badRequest("Le produit « " + product.getNameFr() + " » n'accepte pas de texte personnalisé.");
            }

            product.decreaseStock(itemRequest.quantity()); // 409 si stock insuffisant

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setProductName(product.getNameFr());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(pricingService.unitPrice(product.getPrice(), options));
            item.setCustomText(blankToNull(itemRequest.customText()));
            options.forEach(option -> item.addCustomization(OrderItemCustomization.from(option)));
            order.addItem(item);
        }

        order.attachDelivery(buildDelivery(request.delivery()));
        pricingService.recalculate(order);
        return OrderMapper.toResponse(orderRepository.save(order));
    }

    /** Demande de création sur mesure : le prix est fixé ensuite par l'admin (devis). */
    public OrderResponse createCustomOrder(CustomOrderRequest request, AuthenticatedUser currentUser) {
        if (request.referenceImageUrl() != null
                && !storageService.isStoredUrl(request.referenceImageUrl(), FileStorageService.Folder.REFERENCES)) {
            throw ApiException.badRequest("Photo de référence invalide : utilisez d'abord /api/uploads/reference-image.");
        }

        Order order = newOrder(OrderType.CUSTOM, request.customerName(), request.customerPhone(),
                request.customerEmail(), currentUser);
        order.setOccasion(request.occasion());
        order.setBudget(request.budget() == null ? null : Money.of(request.budget()));
        order.setNotes(request.notes());
        order.setReferenceImageUrl(request.referenceImageUrl());
        order.setQuoted(false);

        OrderItem item = new OrderItem();
        item.setProductName(CUSTOM_ITEM_NAME);
        item.setQuantity(request.quantity());
        item.setUnitPrice(Money.zero());
        item.setCustomText(blankToNull(request.customText()));

        List<String> colorCodes = request.colorCodes() == null ? List.of() : request.colorCodes();
        if (!colorCodes.isEmpty()) {
            List<CustomizationOption> colors = optionRepository.findAllByTypeAndCodeInAndActiveTrue(OptionType.COLOR,
                    new HashSet<>(colorCodes));
            if (colors.size() != new HashSet<>(colorCodes).size()) {
                throw ApiException.badRequest("Une ou plusieurs couleurs sont inconnues.");
            }
            colors.forEach(color -> item.addCustomization(OrderItemCustomization.from(color)));
        }
        if (request.scentCode() != null && !request.scentCode().isBlank()) {
            CustomizationOption scent = optionRepository.findByTypeAndCodeAndActiveTrue(OptionType.SCENT, request.scentCode())
                    .orElseThrow(() -> ApiException.badRequest("Parfum inconnu."));
            item.addCustomization(OrderItemCustomization.from(scent));
        }
        order.addItem(item);

        order.attachDelivery(buildDelivery(request.delivery()));
        pricingService.recalculate(order);
        return OrderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> myOrders(AuthenticatedUser user, Pageable pageable) {
        return orderRepository.findAllByUser_Id(user.id(), pageable).map(OrderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse myOrder(AuthenticatedUser user, String reference) {
        return OrderMapper.toResponse(findOwnedOrder(user, reference));
    }

    /** Le client peut annuler sa commande tant qu'elle n'est pas confirmée. */
    public OrderResponse cancelMyOrder(AuthenticatedUser user, String reference) {
        Order order = findOwnedOrder(user, reference);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw ApiException.conflict("Seule une commande en attente peut être annulée. Contactez-nous pour toute modification.");
        }
        cancel(order);
        return OrderMapper.toResponse(order);
    }

    /** Suivi public : référence + téléphone (évite qu'on devine les commandes des autres). */
    @Transactional(readOnly = true)
    public OrderTrackingResponse track(String reference, String phone) {
        Order order = orderRepository.findByReference(reference == null ? "" : reference.trim().toUpperCase())
                .filter(o -> normalizePhone(o.getCustomerPhone()).equals(normalizePhone(phone)))
                .orElseThrow(() -> ApiException.notFound("Aucune commande ne correspond à cette référence et ce numéro."));
        return OrderMapper.toTracking(order);
    }

    // =====================================================================
    //  Côté administrateur
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<OrderResponse> listForAdmin(OrderStatus status, Pageable pageable) {
        Page<Order> page = status == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findAllByStatus(status, pageable);
        return page.map(OrderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getForAdmin(Long id) {
        return OrderMapper.toResponse(findOrder(id));
    }

    /** Fixe le prix unitaire d'une commande sur mesure (devis) et calcule l'acompte. */
    public OrderResponse quoteCustomOrder(Long id, BigDecimal unitPrice) {
        Order order = findOrder(id);
        if (order.getType() != OrderType.CUSTOM) {
            throw ApiException.badRequest("Seules les commandes sur mesure ont un devis.");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw ApiException.conflict("Le devis ne peut être modifié qu'avant la confirmation.");
        }
        order.getItems().forEach(item -> item.setUnitPrice(Money.of(unitPrice)));
        order.setQuoted(true);
        pricingService.recalculate(order);
        return OrderMapper.toResponse(order);
    }

    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = findOrder(id);
        OrderStatus current = order.getStatus();
        if (!current.canTransitionTo(newStatus)) {
            throw ApiException.conflict("Transition interdite : " + current + " → " + newStatus + ".");
        }
        Delivery delivery = order.getDelivery();
        DeliveryMethod method = delivery.getMethod();

        switch (newStatus) {
            case CONFIRMED -> {
                if (!order.isQuoted()) {
                    throw ApiException.conflict("Fixez d'abord le prix (devis) de cette commande sur mesure.");
                }
                if (order.amountPaid().compareTo(order.getDepositAmount()) < 0) {
                    throw ApiException.conflict("Acompte non reçu : " + order.getDepositAmount() + " DT requis, "
                            + order.amountPaid() + " DT payés.");
                }
            }
            case SHIPPED -> {
                if (method != DeliveryMethod.DELIVERY) {
                    throw ApiException.conflict("Commande en retrait : utilisez READY_FOR_PICKUP.");
                }
                delivery.setStatus(DeliveryStatus.IN_TRANSIT);
            }
            case READY_FOR_PICKUP -> {
                if (method != DeliveryMethod.PICKUP) {
                    throw ApiException.conflict("Commande en livraison : utilisez SHIPPED.");
                }
                delivery.setStatus(DeliveryStatus.READY_FOR_PICKUP);
            }
            case DELIVERED -> {
                delivery.setStatus(method == DeliveryMethod.DELIVERY ? DeliveryStatus.DELIVERED : DeliveryStatus.PICKED_UP);
                delivery.setDeliveredAt(Instant.now());
            }
            case CANCELLED -> {
                cancel(order);
                return OrderMapper.toResponse(order);
            }
            default -> {
                // IN_PREPARATION : rien de plus
            }
        }
        order.setStatus(newStatus);
        return OrderMapper.toResponse(order);
    }

    public OrderResponse updateDelivery(Long id, DeliveryUpdateRequest request) {
        Order order = findOrder(id);
        Delivery delivery = order.getDelivery();
        if (request.scheduledDate() != null) {
            if (request.scheduledDate().isBefore(LocalDate.now(TUNIS))) {
                throw ApiException.badRequest("La date prévue ne peut pas être dans le passé.");
            }
            delivery.setScheduledDate(request.scheduledDate());
            if (delivery.getStatus() == DeliveryStatus.PENDING) {
                delivery.setStatus(DeliveryStatus.SCHEDULED);
            }
        }
        if (request.trackingNumber() != null) {
            delivery.setTrackingNumber(blankToNull(request.trackingNumber()));
        }
        return OrderMapper.toResponse(order);
    }

    /** Enregistre un paiement reçu (espèces, virement, D17...). */
    public OrderResponse addPayment(Long id, PaymentRequest request) {
        Order order = findOrder(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw ApiException.conflict("Commande annulée : paiement impossible.");
        }
        if (!order.isQuoted()) {
            throw ApiException.conflict("Fixez d'abord le prix (devis) de cette commande sur mesure.");
        }
        BigDecimal amount = Money.of(request.amount());
        if (amount.compareTo(order.balanceDue()) > 0) {
            throw ApiException.badRequest("Montant supérieur au reste à payer (" + order.balanceDue() + " DT).");
        }
        Payment payment = new Payment();
        payment.setType(request.type());
        payment.setMethod(request.method());
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setTransactionRef(blankToNull(request.transactionRef()));
        payment.setPaidAt(Instant.now());
        order.addPayment(payment);
        orderRepository.flush();
        return OrderMapper.toResponse(order);
    }

    public Map<OrderStatus, Long> countByStatus() {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, orderRepository.countByStatus(status));
        }
        return counts;
    }

    // =====================================================================
    //  Outils internes
    // =====================================================================

    private Order newOrder(OrderType type, String name, String phone, String email, AuthenticatedUser currentUser) {
        Order order = new Order();
        order.setType(type);
        order.setReference(generateReference());
        order.setCustomerName(name.trim());
        order.setCustomerPhone(phone.trim());
        order.setCustomerEmail(blankToNull(email));
        if (currentUser != null) {
            order.setUser(userRepository.getReferenceById(currentUser.id()));
        }
        return order;
    }

    private List<CustomizationOption> resolveOptions(Product product, List<Long> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) {
            return List.of();
        }
        if (!product.isCustomizable()) {
            throw ApiException.badRequest("Le produit « " + product.getNameFr() + " » n'est pas personnalisable.");
        }
        List<CustomizationOption> options = optionRepository.findAllById(new HashSet<>(optionIds));
        if (options.size() != new HashSet<>(optionIds).size()) {
            throw ApiException.badRequest("Option de personnalisation inconnue.");
        }
        Map<OptionType, Integer> perType = new EnumMap<>(OptionType.class);
        List<CustomizationOption> result = new ArrayList<>();
        for (CustomizationOption option : options) {
            if (!option.isActive() || !product.allowsOption(option)) {
                throw ApiException.badRequest("L'option « " + option.getLabelFr() + " » n'est pas disponible pour ce produit.");
            }
            int count = perType.merge(option.getType(), 1, Integer::sum);
            int max = option.getType() == OptionType.COLOR ? MAX_COLORS_PER_ITEM : 1;
            if (count > max) {
                throw ApiException.badRequest("Trop d'options de type " + option.getType() + " pour un même article.");
            }
            result.add(option);
        }
        return result;
    }

    private Delivery buildDelivery(DeliveryRequest request) {
        Delivery delivery = new Delivery();
        delivery.setMethod(request.method());
        if (request.method() == DeliveryMethod.DELIVERY) {
            if (isBlank(request.addressLine()) || isBlank(request.city())) {
                throw ApiException.badRequest("Adresse et ville obligatoires pour une livraison.");
            }
            delivery.setAddressLine(request.addressLine().trim());
            delivery.setCity(request.city().trim());
            delivery.setGovernorate(blankToNull(request.governorate()));
            delivery.setPostalCode(blankToNull(request.postalCode()));
        } else {
            delivery.setPickupLocation(pickupLocation);
        }
        return delivery;
    }

    /** Annule et remet en stock les articles concernés. */
    private void cancel(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                item.getProduct().increaseStock(item.getQuantity());
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.getDelivery().setStatus(DeliveryStatus.FAILED);
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> ApiException.notFound("Commande introuvable."));
    }

    private Order findOwnedOrder(AuthenticatedUser user, String reference) {
        return orderRepository.findByReference(reference)
                .filter(o -> o.getUser() != null && o.getUser().getId().equals(user.id()))
                .orElseThrow(() -> ApiException.notFound("Commande introuvable."));
    }

    private String generateReference() {
        String datePart = LocalDate.now(TUNIS).format(REF_DATE);
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder("DC-").append(datePart).append('-');
            for (int i = 0; i < 6; i++) {
                sb.append(REF_ALPHABET[random.nextInt(REF_ALPHABET.length)]);
            }
            String reference = sb.toString();
            if (!orderRepository.existsByReference(reference)) {
                return reference;
            }
        }
        throw new IllegalStateException("Impossible de générer une référence de commande unique");
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("[^0-9]", "");
        // +216 XX XXX XXX et XX XXX XXX désignent le même numéro tunisien
        return digits.startsWith("216") && digits.length() == 11 ? digits.substring(3) : digits;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
