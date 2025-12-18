// File: src/main/java/com/IT_market/controller/AdminController.java
package com.IT_market.controller;

import com.IT_market.model.SellRequest;
import com.IT_market.model.Order;
import com.IT_market.model.Product;
import com.IT_market.model.User;
import com.IT_market.security.SessionManager;
import com.IT_market.service.OrderService;
import com.IT_market.service.ProductService;
import com.IT_market.service.SellRequestService;
import com.IT_market.service.UserService;
import com.twilio.rest.api.v2010.account.call.Payment;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    private final OrderService orderService;
    private final ProductService productService;
    private final SellRequestService sellRequestService;
    private final UserService userService;
    private final SessionManager sessionManager;  // Add this
   // private final PaymentService paymentService;
    
    public AdminController(OrderService orderService, ProductService productService,SessionManager sessionManager,
                          SellRequestService sellRequestService, UserService userService
                          ) {
        this.sessionManager = sessionManager;
        this.orderService = orderService;
        this.productService = productService;
        this.sellRequestService = sellRequestService;
        this.userService = userService;
        //this.paymentService = paymentService;
    }
    
    // ============== DASHBOARD ==============
    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        try {
            // === AUTHENTICATION & AUTHORIZATION CHECK ===
            String token = extractTokenFromRequest(request);
            
            if (token == null || !sessionManager.isValidSession(token)) {
                return "redirect:/";
            }
            
            User user = sessionManager.getUserFromSession(token);
            boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName().name()));
            
            if (!isAdmin) {
                return "redirect:/";
            }
            
            // Add user to model
            model.addAttribute("user", user);
            
            // === DASHBOARD STATISTICS ===
            long totalOrders = orderService.countAllOrders();
            long pendingOrders = orderService.countOrdersByStatus(Order.OrderStatus.PENDING);
            long pendingSellRequests = sellRequestService.countByStatus(SellRequest.SellRequestStatus.PENDING);
            
            System.out.println("Testing ProductService...");
            long lowStockProducts = productService.countLowStockProducts(5);
            System.out.println("Low stock products count: " + lowStockProducts);
            
            long totalUsers = userService.countAllUsers();
            
            // Recent activities
            List<Order> recentOrders = orderService.getRecentOrders(10);
            List<SellRequest> recentSellRequests = sellRequestService.getRecentSellRequests(5);
            
            // Sales summary (last 30 days)
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            Map<String, BigDecimal> salesData = orderService.getSalesSummary(startDate, endDate);
            
            // Get sell request statistics
            Map<String, Object> sellRequestStats = sellRequestService.getSellRequestStatistics();
            
            // Add all attributes to model
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("pendingOrders", pendingOrders);
            model.addAttribute("pendingSellRequests", pendingSellRequests);
            model.addAttribute("lowStockProducts", lowStockProducts);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("recentOrders", recentOrders);
            model.addAttribute("recentSellRequests", recentSellRequests);
            model.addAttribute("salesData", salesData);
            model.addAttribute("sellRequestStats", sellRequestStats);
            
            // Get today's date for display
            model.addAttribute("today", LocalDate.now());
            model.addAttribute("thirtyDaysAgo", startDate);
            
            return "admin/dashboard";
            
        } catch (Exception e) {
            // Log the error
            System.err.println("Error loading admin dashboard: " + e.getMessage());
            e.printStackTrace();
            
            // Add error message to model
            model.addAttribute("error", "Unable to load dashboard statistics. Please try again.");
            
            // Set default values
            model.addAttribute("totalOrders", 0);
            model.addAttribute("pendingOrders", 0);
            model.addAttribute("pendingSellRequests", 0);
            model.addAttribute("lowStockProducts", 0);
            model.addAttribute("totalUsers", 0);
            model.addAttribute("recentOrders", new ArrayList<>());
            model.addAttribute("recentSellRequests", new ArrayList<>());
            model.addAttribute("salesData", new HashMap<>());
            model.addAttribute("sellRequestStats", new HashMap<>());
            
            return "admin/dashboard";
        }
    }
    
    
    // ============== ORDERS MANAGEMENT ==============
    @GetMapping("/orders")
    public String viewOrders(@RequestParam(required = false) String status, Model model,HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token == null || !sessionManager.isValidSession(token)) {
                return "redirect:/";
            }
            
            User user = sessionManager.getUserFromSession(token);
            boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName().name()));
            
            if (!isAdmin) {
                return "redirect:/";
            }
            
            model.addAttribute("user", user);
            List<Order> orders;
            if (status != null && !status.isEmpty()) {
                orders = orderService.getOrdersByStatus(Order.OrderStatus.valueOf(status));
            } else {
                orders = orderService.getAllOrders();
            }
            
            model.addAttribute("orders", orders);
            model.addAttribute("statuses", Order.OrderStatus.values());
            model.addAttribute("selectedStatus", status);
            
            // Order statistics
            model.addAttribute("totalOrdersCount", orders.size());
            model.addAttribute("pendingCount", orderService.countOrdersByStatus(Order.OrderStatus.PENDING));
            model.addAttribute("completedCount", orderService.countOrdersByStatus(Order.OrderStatus.DELIVERED));
            
            return "admin/orders";
            
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load orders: " + e.getMessage());
            model.addAttribute("orders", new ArrayList<>());
            return "admin/orders";
        }
    }
    
    @PostMapping("/orders/{id}/approve")
    public String approveOrder(HttpServletRequest request,@PathVariable String id) {
        try {
            String token = extractTokenFromRequest(request);
            if (token == null || !sessionManager.isValidSession(token)) {
                return "redirect:/";
            }
            
            User user = sessionManager.getUserFromSession(token);
            boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName().name()));
            
            if (!isAdmin) {
                return "redirect:/";
            }
            orderService.approveOrder(id);
            return "redirect:/admin/orders?success=approved";
        } catch (Exception e) {
            return "redirect:/admin/orders?error=" + e.getMessage();
        }
    }
    
    @PostMapping("/orders/{id}/reject")
    public String rejectOrder(@PathVariable Long id, @RequestParam String reason) {
        try {
            orderService.rejectOrder(id, reason);
            return "redirect:/admin/orders?success=rejected";
        } catch (Exception e) {
            return "redirect:/admin/orders?error=" + e.getMessage();
        }
    }
    
    @GetMapping("/orders/{id}")
    public String viewOrderDetails(@PathVariable String id, Model model) {
        try {
            Order order = orderService.getOrderById(id);
            model.addAttribute("order", order);
            return "admin/order-details";
        } catch (Exception e) {
            return "redirect:/admin/orders?error=Order not found";
        }
    }
    
    // ============== PRODUCTS MANAGEMENT ==============
    @GetMapping("/products")
    public String manageProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            Model model) {
        
        try {
            List<Product> products;
            if (status != null && status.equals("pending")) {
                products = productService.getPendingProducts();
            } else if (category != null && !category.isEmpty()) {
                products = productService.getProductsByCategory(category);
            } else {
                products = productService.getAllProducts();
            }
            
            model.addAttribute("products", products);
            model.addAttribute("categories", Product.ProductCategory.values());
            model.addAttribute("conditions", Product.ProductCondition.values());
            
            // Product statistics
            model.addAttribute("totalProducts", productService.countAllProducts());
            model.addAttribute("activeProducts", productService.countActiveProducts());
            model.addAttribute("pendingApproval", productService.countPendingProducts());
            
            return "admin/products";
            
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load products: " + e.getMessage());
            model.addAttribute("products", new ArrayList<>());
            return "admin/products";
        }
    }
    
    @GetMapping("/products/new")
    public String showAddProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", Product.ProductCategory.values());
        model.addAttribute("conditions", Product.ProductCondition.values());
        return "admin/product-form";
    }
    
    @PostMapping("/products")
    public String addProduct(@ModelAttribute Product product) {
        try {
            product.setApproved(true);
            product.setAvailable(true);
            productService.saveProduct(product);
            return "redirect:/admin/products?success=created";
        } catch (Exception e) {
            return "redirect:/admin/products/new?error=" + e.getMessage();
        }
    }
    
    @GetMapping("/products/{id}/edit")
    public String showEditProductForm(@PathVariable String id, Model model) {
        try {
            Product product = productService.getProductById(id);
            model.addAttribute("product", product);
            model.addAttribute("categories", Product.ProductCategory.values());
            model.addAttribute("conditions", Product.ProductCondition.values());
            return "admin/product-form";
        } catch (Exception e) {
            return "redirect:/admin/products?error=Product not found";
        }
    }
    
    @PostMapping("/products/{id}")
    public String updateProduct(@PathVariable String id, @ModelAttribute Product product) {
        try {
            productService.updateProduct(id, product);
            return "redirect:/admin/products?success=updated";
        } catch (Exception e) {
            return "redirect:/admin/products/" + id + "/edit?error=" + e.getMessage();
        }
    }
    
    @PostMapping("/products/{id}/toggle-active")
    public String toggleProductActive(@PathVariable String id) {
        try {
            productService.toggleActive(id);
            return "redirect:/admin/products?success=toggled";
        } catch (Exception e) {
            return "redirect:/admin/products?error=" + e.getMessage();
        }
    }
    
    @PostMapping("/products/{id}/toggle-featured")
    public String toggleProductFeatured(@PathVariable String id) {
        try {
            productService.toggleFeatured(id);
            return "redirect:/admin/products?success=featured_toggled";
        } catch (Exception e) {
            return "redirect:/admin/products?error=" + e.getMessage();
        }
    }
    
    @PostMapping("/products/{id}/approve")
    public String approveProduct(@PathVariable String id) {
        try {
            Product product = productService.getProductById(id);
            product.setApproved(true);
            productService.saveProduct(product);
            return "redirect:/admin/products?success=approved";
        } catch (Exception e) {
            return "redirect:/admin/products?error=" + e.getMessage();
        }
    }
    
    // ============== SELL REQUESTS MANAGEMENT ==============
    @GetMapping("/sell-requests")
    public String viewSellRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Model model) {
        
        try {
            List<SellRequest> sellRequests;
            if (status != null && !status.isEmpty()) {
                sellRequests = sellRequestService.getSellRequestsByStatus(
                    SellRequest.SellRequestStatus.valueOf(status.toUpperCase())
                );
            } else {
                sellRequests = sellRequestService.getAllSellRequests();
            }
            
            model.addAttribute("sellRequests", sellRequests);
            model.addAttribute("statuses", SellRequest.SellRequestStatus.values());
            model.addAttribute("selectedStatus", status);
            
            // Statistics
            model.addAttribute("pendingCount", sellRequestService.countByStatus(SellRequest.SellRequestStatus.PENDING));
            model.addAttribute("approvedCount", sellRequestService.countByStatus(SellRequest.SellRequestStatus.APPROVED));
            model.addAttribute("rejectedCount", sellRequestService.countByStatus(SellRequest.SellRequestStatus.REJECTED));
            
            return "admin/sell-requests";
            
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load sell requests: " + e.getMessage());
            model.addAttribute("sellRequests", new ArrayList<>());
            return "admin/sell-requests";
        }
    }
    
    @GetMapping("/sell-requests/{id}")
    public String viewSellRequestDetails(@PathVariable Long id, Model model) {
        try {
            SellRequest sellRequest = sellRequestService.getSellRequestById(id);
            model.addAttribute("sellRequest", sellRequest);
            return "admin/sell-request-details";
        } catch (Exception e) {
            return "redirect:/admin/sell-requests?error=Request not found";
        }
    }
    
    @PostMapping("/sell-requests/{id}/approve")
    public String approveSellRequest(@PathVariable Long id, @RequestParam BigDecimal finalPrice) {
        try {
            // In a real app, you'd get the admin user from security context
            User adminUser = userService.getAllUsers().stream()
                    .filter(User::isAdmin)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No admin user found"));
            
            sellRequestService.approveSellRequest(id, finalPrice, adminUser);
            return "redirect:/admin/sell-requests?success=approved";
        } catch (Exception e) {
            return "redirect:/admin/sell-requests?error=" + e.getMessage();
        }
    }
    
    @PostMapping("/sell-requests/{id}/reject")
    public String rejectSellRequest(@PathVariable Long id, @RequestParam String reason) {
        try {
            // In a real app, you'd get the admin user from security context
            User adminUser = userService.getAllUsers().stream()
                    .filter(User::isAdmin)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No admin user found"));
            
            sellRequestService.rejectSellRequest(id, reason, adminUser);
            return "redirect:/admin/sell-requests?success=rejected";
        } catch (Exception e) {
            return "redirect:/admin/sell-requests?error=" + e.getMessage();
        }
    }
    
    @PostMapping("/sell-requests/{id}/negotiate")
    public String negotiateSellRequest(@PathVariable Long id, 
                                      @RequestParam BigDecimal offeredPrice,
                                      @RequestParam String negotiationNotes) {
        try {
            // In a real app, you'd get the admin user from security context
            User adminUser = userService.getAllUsers().stream()
                    .filter(User::isAdmin)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No admin user found"));
            
            sellRequestService.startNegotiation(id, offeredPrice, adminUser);
            return "redirect:/admin/sell-requests?success=negotiation_started";
        } catch (Exception e) {
            return "redirect:/admin/sell-requests?error=" + e.getMessage();
        }
    }
    
    @PostMapping("/sell-requests/{id}/complete")
    public String completeSellRequest(@PathVariable Long id) {
        try {
            // In a real app, you'd get the admin user from security context
            User adminUser = userService.getAllUsers().stream()
                    .filter(User::isAdmin)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No admin user found"));
            
            sellRequestService.completeSellRequest(id, adminUser);
            return "redirect:/admin/sell-requests?success=completed";
        } catch (Exception e) {
            return "redirect:/admin/sell-requests?error=" + e.getMessage();
        }
    }
    
    // ============== USERS MANAGEMENT ==============
    @GetMapping("/users")
    public String manageUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            Model model) {
        
        try {
            List<User> users = userService.getAllUsers();
            
            // Filter by role if specified
            if (role != null && !role.isEmpty()) {
                List<User> filteredUsers = new ArrayList<>();
                for (User user : users) {
                    if (role.equals("admin") && user.isAdmin()) {
                        filteredUsers.add(user);
                    } else if (role.equals("seller") && user.isSeller()) {
                        filteredUsers.add(user);
                    } else if (role.equals("customer") && user.isUser()) {
                        filteredUsers.add(user);
                    }
                }
                users = filteredUsers;
            }
            
            model.addAttribute("users", users);
            model.addAttribute("totalUsers", users.size());
            
            // Count by role
            long adminCount = users.stream().filter(User::isAdmin).count();
            long sellerCount = users.stream().filter(User::isSeller).count();
            long customerCount = users.stream().filter(User::isUser).count();
            
            model.addAttribute("adminCount", adminCount);
            model.addAttribute("sellerCount", sellerCount);
            model.addAttribute("customerCount", customerCount);
            
            return "admin/users";
            
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load users: " + e.getMessage());
            model.addAttribute("users", new ArrayList<>());
            return "admin/users";
        }
    }
    
    @GetMapping("/users/{id}")
    public String viewUserDetails(@PathVariable Long id, Model model) {
        try {
            User user = userService.getUserById(id);
            model.addAttribute("user", user);
            
            // Get user's orders
            List<Order> userOrders = orderService.getOrdersByUser(id);
            model.addAttribute("userOrders", userOrders);
            
            // Get user's sell requests
            List<SellRequest> userSellRequests = sellRequestService.getUserSellRequests(id);
            model.addAttribute("userSellRequests", userSellRequests);
            
            return "admin/user-details";
        } catch (Exception e) {
            return "redirect:/admin/users?error=User not found";
        }
    }
    
    @PostMapping("/users/{id}/toggle")
    public String toggleUserStatus(@PathVariable Long id) {
        try {
            userService.toggleUserStatus(id);
            return "redirect:/admin/users?success=status_toggled";
        } catch (Exception e) {
            return "redirect:/admin/users?error=" + e.getMessage();
        }
    }
    
    @PostMapping("/users/{id}/verify")
    public String verifyUser(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            user.setVerified(true);
            userService.saveUser(user);  // Changed from registerUser to saveUser
            return "redirect:/admin/users?success=verified";
        } catch (Exception e) {
            return "redirect:/admin/users?error=" + e.getMessage();
        }
    }
    
    // ============== PAYMENTS MANAGEMENT ==============
    /*@GetMapping("/payments")
    public String viewPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Model model) {
        
        try {
            List<Payment> payments;
            if (status != null && !status.isEmpty()) {
                payments = paymentService.getPaymentsByStatus(Payment.PaymentStatus.valueOf(status.toUpperCase()));
            } else {
                payments = paymentService.getAllPayments();
            }
            
            model.addAttribute("payments", payments);
            model.addAttribute("statuses", Payment.PaymentStatus.values());
            model.addAttribute("selectedStatus", status);
            
            // Payment statistics
            model.addAttribute("totalPayments", payments.size());
            model.addAttribute("totalRevenue", paymentService.getTotalRevenue());
            
            return "admin/payments";
            
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load payments: " + e.getMessage());
            model.addAttribute("payments", new ArrayList<>());
            return "admin/payments";
        }
    }
    
     @GetMapping("/payments/{id}")
    public String viewPaymentDetails(@PathVariable Long id, Model model) {
        try {
            Payment payment = paymentService.getPaymentById(id);
            model.addAttribute("payment", payment);
            return "admin/payment-details";
        } catch (Exception e) {
            return "redirect:/admin/payments?error=Payment not found";
        }
    }
    
   @PostMapping("/payments/{id}/process")
    public String processPayment(@PathVariable Long id, @RequestParam String transactionId) {
        try {
            paymentService.processPayment(id, transactionId);
            return "redirect:/admin/payments?success=processed";
        } catch (Exception e) {
            return "redirect:/admin/payments?error=" + e.getMessage();
        }
    } */
    
    /*@PostMapping("/payments/{id}/refund")
    public String refundPayment(@PathVariable Long id, @RequestParam String refundReason) {
        try {
            paymentService.refundPayment(id, refundReason);
            return "redirect:/admin/payments?success=refunded";
        } catch (Exception e) {
            return "redirect:/admin/payments?error=" + e.getMessage();
        }
    } */
    
    // ============== REPORTS ==============
    @GetMapping("/reports")
    public String viewReports(
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
        
        try {
            LocalDate start = (startDate != null && !startDate.isEmpty()) ? 
                    LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
            LocalDate end = (endDate != null && !endDate.isEmpty()) ? 
                    LocalDate.parse(endDate) : LocalDate.now();
            
            // Sales report
            Map<String, Object> salesReport = orderService.getSalesReport(start, end);
            Map<String, Long> categorySales = productService.getCategorySales();
            List<Product> topProducts = productService.getTopSellingProducts(10);
            
            // Payment report
            //BigDecimal revenue = paymentService.getRevenueBetweenDates(start, end);
            
            // Sell request report
            Map<String, Object> sellRequestStats = sellRequestService.getSellRequestStatistics();
            
            model.addAttribute("salesReport", salesReport);
            model.addAttribute("categorySales", categorySales);
            model.addAttribute("topProducts", topProducts);
            //model.addAttribute("revenue", revenue);
            model.addAttribute("sellRequestStats", sellRequestStats);
            model.addAttribute("startDate", start);
            model.addAttribute("endDate", end);
            model.addAttribute("reportType", reportType);
            
            return "admin/reports";
            
        } catch (Exception e) {
            model.addAttribute("error", "Failed to generate reports: " + e.getMessage());
            model.addAttribute("salesReport", new HashMap<>());
            model.addAttribute("categorySales", new HashMap<>());
            model.addAttribute("topProducts", new ArrayList<>());
            return "admin/reports";
        }
    }
    
    // ============== INVENTORY MANAGEMENT ==============
    @GetMapping("/inventory")
    public String viewInventory(Model model) {
        try {
            List<Product> lowStockProducts = productService.getLowStockProducts(5);
            List<Product> outOfStockProducts = productService.getOutOfStockProducts();
            
            model.addAttribute("lowStockProducts", lowStockProducts);
            model.addAttribute("outOfStockProducts", outOfStockProducts);
            model.addAttribute("totalProducts", productService.countAllProducts());
            model.addAttribute("totalValue", productService.calculateInventoryValue());
            model.addAttribute("activeProducts", productService.countActiveProducts());
            
            // Category distribution
            Map<String, Long> categoryDistribution = new HashMap<>();
            List<Product> allProducts = productService.getAllProducts();
            for (Product product : allProducts) {
                String category = product.getCategory().name();
                categoryDistribution.put(category, 
                    categoryDistribution.getOrDefault(category, 0L) + 1);
            }
            model.addAttribute("categoryDistribution", categoryDistribution);
            
            return "admin/inventory";
            
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load inventory: " + e.getMessage());
            model.addAttribute("lowStockProducts", new ArrayList<>());
            model.addAttribute("outOfStockProducts", new ArrayList<>());
            return "admin/inventory";
        }
    }
    
    // ============== SETTINGS ==============
    @GetMapping("/settings")
    public String viewSettings(Model model) {
        // This would typically load system settings
        model.addAttribute("pageTitle", "System Settings");
        return "admin/settings";
    }
    
    // ============== ERROR HANDLING ==============
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        model.addAttribute("error", "An error occurred: " + e.getMessage());
        return "admin/error";
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Check Authorization header first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Check cookie
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("auth_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        // Check session attribute
        Object tokenAttr = request.getSession().getAttribute("auth_token");
        if (tokenAttr != null) {
            return tokenAttr.toString();
        }
        
        return null;
    }
}