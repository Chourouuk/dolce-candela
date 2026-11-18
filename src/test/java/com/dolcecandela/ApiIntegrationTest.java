package com.dolcecandela;

import com.dolcecandela.catalog.CustomizationOptionRepository;
import com.dolcecandela.catalog.OptionType;
import com.dolcecandela.catalog.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de bout en bout de l'API (H2 en mode PostgreSQL + vraies migrations Flyway).
 * Chaque test est annulé (rollback) à la fin : ils sont indépendants.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@test.dolcecandela.tn";
    private static final String ADMIN_PASSWORD = "Admin-Test-Password-123";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CustomizationOptionRepository optionRepository;

    // ------------------------------------------------------------------ catalogue

    @Test
    void catalogIsPublic() throws Exception {
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].nameAr").value("باقات الشموع"));

        mvc.perform(get("/api/products").param("category", "bouquets-bougies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));

        mvc.perform(get("/api/products/bouquet-roses-mariee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(89.0))
                .andExpect(jsonPath("$.options.length()").value(20));
    }

    @Test
    void unknownProductReturns404() throws Exception {
        mvc.perform(get("/api/products/n-existe-pas")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ sécurité

    @Test
    void adminRoutesAreProtected() throws Exception {
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());

        String customerToken = registerCustomer();
        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersByStatus.PENDING").exists());
    }

    @Test
    void forgedTokenIsRejected() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.forged"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordThenLockout() throws Exception {
        String email = "lock-" + UUID.randomUUID() + "@test.tn";
        register(email);
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("email", email, "password", "mauvais1"))))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", "Bougie2026"))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void invalidPayloadReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("fullName", "", "email", "pas-un-email", "password", "court"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    // ------------------------------------------------------------------ commandes

    @Test
    void guestOrderWithDeliveryAddsEightDinarsAndDecrementsStock() throws Exception {
        Long sageId = productId("bougie-sauge-cedre");
        int stockBefore = productRepository.findById(sageId).orElseThrow().getStockQuantity();

        JsonNode order = createOrder(Map.of(
                "customerName", "Amira Ben Salah",
                "customerPhone", "+216 22 333 444",
                "items", List.of(Map.of("productId", sageId, "quantity", 2)),
                "delivery", Map.of("method", "DELIVERY", "addressLine", "Rue de la Corniche", "city", "Monastir")));

        assertThat(order.get("subtotal").decimalValue()).isEqualByComparingTo("84");
        assertThat(order.get("deliveryFee").decimalValue()).isEqualByComparingTo("8");
        assertThat(order.get("total").decimalValue()).isEqualByComparingTo("92");
        assertThat(order.get("depositAmount").decimalValue()).isEqualByComparingTo("0");
        assertThat(order.get("status").asText()).isEqualTo("PENDING");
        assertThat(productRepository.findById(sageId).orElseThrow().getStockQuantity()).isEqualTo(stockBefore - 2);

        // Suivi public : le format du numéro n'a pas d'importance, mais il doit correspondre
        String reference = order.get("reference").asText();
        mvc.perform(get("/api/orders/track").param("reference", reference).param("phone", "22333444"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customerName").doesNotExist());
        mvc.perform(get("/api/orders/track").param("reference", reference).param("phone", "99999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deliveryWithoutAddressIsRejected() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                        "customerName", "Test", "customerPhone", "22333444",
                        "items", List.of(Map.of("productId", productId("bougie-sauge-cedre"), "quantity", 1)),
                        "delivery", Map.of("method", "DELIVERY")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void insufficientStockReturns409() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                        "customerName", "Test", "customerPhone", "22333444",
                        "items", List.of(Map.of("productId", productId("bougie-sauge-cedre"), "quantity", 99)),
                        "delivery", Map.of("method", "PICKUP")))))
                .andExpect(status().isConflict());
    }

    @Test
    void optionNotAllowedForProductIsRejected() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                        "customerName", "Test", "customerPhone", "22333444",
                        "items", List.of(Map.of("productId", productId("bougie-sauge-cedre"), "quantity", 1,
                                "optionIds", List.of(optionId(OptionType.COLOR, "creme")))),
                        "delivery", Map.of("method", "PICKUP")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void personalizedOrderNeedsDepositBeforeConfirmation() throws Exception {
        JsonNode order = createOrder(Map.of(
                "customerName", "Sarra", "customerPhone", "55111222",
                "items", List.of(Map.of(
                        "productId", productId("bouquet-roses-mariee"), "quantity", 1,
                        "optionIds", List.of(optionId(OptionType.COLOR, "creme"), optionId(OptionType.SCENT, "vanille")),
                        "customText", "Emma & Tom")),
                "delivery", Map.of("method", "PICKUP")));

        assertThat(order.get("deliveryFee").decimalValue()).isEqualByComparingTo("0");
        assertThat(order.get("total").decimalValue()).isEqualByComparingTo("89");
        assertThat(order.get("depositAmount").decimalValue()).isEqualByComparingTo("26.7");

        long id = order.get("id").asLong();
        String admin = "Bearer " + adminToken();

        // Pas d'acompte → confirmation refusée
        mvc.perform(patch("/api/admin/orders/" + id + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isConflict());

        // Acompte reçu
        mvc.perform(post("/api/admin/orders/" + id + "/payments").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("type", "DEPOSIT", "method", "D17", "amount", 26.7))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceDue").value(62.3));

        mvc.perform(patch("/api/admin/orders/" + id + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Une commande en retrait ne peut pas être "expédiée"
        mvc.perform(patch("/api/admin/orders/" + id + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status", "IN_PREPARATION"))))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/admin/orders/" + id + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("status", "SHIPPED"))))
                .andExpect(status().isConflict());
    }

    @Test
    void customOrderIsQuotedByAdmin() throws Exception {
        MvcResult result = mvc.perform(post("/api/orders/custom").contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                        "customerName", "Yasmine", "customerPhone", "98765432", "occasion", "WEDDING",
                        "colorCodes", List.of("creme", "or"), "scentCode", "jasmin", "quantity", 20,
                        "budget", 600, "customText", "Yasmine & Karim — 12.06.2027",
                        "delivery", Map.of("method", "DELIVERY", "addressLine", "Av. Habib Bourguiba", "city", "Monastir")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CUSTOM"))
                .andExpect(jsonPath("$.quoted").value(false))
                .andReturn();
        long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/admin/orders/" + id + "/quote").header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("unitPrice", 28))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoted").value(true))
                .andExpect(jsonPath("$.subtotal").value(560.0))
                .andExpect(jsonPath("$.total").value(568.0))
                .andExpect(jsonPath("$.depositAmount").value(170.4));
    }

    @Test
    void customerSeesOnlyOwnOrders() throws Exception {
        String token = registerCustomer();
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of(
                                "customerName", "Client", "customerPhone", "22333444",
                                "items", List.of(Map.of("productId", productId("diffuseur-botanique"), "quantity", 1)),
                                "delivery", Map.of("method", "PICKUP")))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/orders/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        String otherToken = registerCustomer();
        mvc.perform(get("/api/orders/me").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void reviewRequiresDeliveredPurchase() throws Exception {
        String token = registerCustomer();
        mvc.perform(post("/api/products/bougie-sauge-cedre/reviews").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(Map.of("rating", 5, "comment", "Magnifique"))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ outils

    private JsonNode createOrder(Map<String, Object> request) throws Exception {
        MvcResult result = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private String registerCustomer() throws Exception {
        return register("client-" + UUID.randomUUID() + "@test.tn");
    }

    private String register(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("fullName", "Client Test", "email", email, "password", "Bougie2026"))))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String adminToken() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private Long productId(String slug) {
        return productRepository.findBySlugAndActiveTrue(slug).orElseThrow().getId();
    }

    private Long optionId(OptionType type, String code) {
        return optionRepository.findByTypeAndCodeAndActiveTrue(type, code).orElseThrow().getId();
    }

    private String body(Object value) throws Exception {
        return json.writeValueAsString(value);
    }
}
