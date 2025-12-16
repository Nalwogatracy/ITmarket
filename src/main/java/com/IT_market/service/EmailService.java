package com.IT_market.service;

import com.IT_market.model.Order;
import com.IT_market.model.OrderItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.admin.email}")
    private String adminEmail;
    
    private final DateTimeFormatter dateFormatter = 
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    
    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }
    
    @Async
    public void sendOrderConfirmation(Order order) {
        try {
            Context context = new Context(Locale.getDefault());
            context.setVariable("order", order);
            context.setVariable("orderDate", order.getCreatedAt().format(dateFormatter));
            context.setVariable("items", order.getItems());
            context.setVariable("totalItems", order.getItems().size());
            context.setVariable("subtotal", formatCurrency(order.getSubtotal()));
            context.setVariable("tax", formatCurrency(order.getTaxAmount()));
            context.setVariable("shipping", formatCurrency(order.getShippingAmount()));
            context.setVariable("discount", formatCurrency(order.getDiscountAmount()));
            context.setVariable("total", formatCurrency(order.getTotalAmount()));
            context.setVariable("isGuest", order.isGuestOrder());
            
            String htmlContent = templateEngine.process("email/order-confirmation", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, "IT Market");
            helper.setTo(order.getCustomerEmail());
            helper.setSubject("Order Confirmation - #" + order.getId());
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            System.out.println("Order confirmation email sent to: " + order.getCustomerEmail());
            
        } catch (Exception e) {
            System.err.println("Failed to send order confirmation email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Async
    public void sendOrderNotificationToAdmin(Order order) {
        try {
            Context context = new Context(Locale.getDefault());
            context.setVariable("order", order);
            context.setVariable("customerEmail", order.getCustomerEmail());
            context.setVariable("customerPhone", order.getCustomerPhone() != null ? order.getCustomerPhone() : "Not provided");
            context.setVariable("customerName", order.getCustomerName() != null ? order.getCustomerName() : order.getCustomerEmail());
            context.setVariable("totalAmount", formatCurrency(order.getTotalAmount()));
            context.setVariable("itemCount", order.getItems().size());
            context.setVariable("isGuest", order.isGuestOrder());
            context.setVariable("orderDate", order.getCreatedAt().format(dateFormatter));
            
            // Build items table for email
            StringBuilder itemsHtml = new StringBuilder();
            for (OrderItem item : order.getItems()) {
                itemsHtml.append("<tr>")
                        .append("<td>").append(item.getProductName()).append("</td>")
                        .append("<td>").append(item.getQuantity()).append("</td>")
                        .append("<td>").append(formatCurrency(item.getUnitPrice())).append("</td>")
                        .append("<td>").append(formatCurrency(item.getTotalPrice())).append("</td>")
                        .append("</tr>");
            }
            context.setVariable("itemsHtml", itemsHtml.toString());
            
            String htmlContent = templateEngine.process("email/admin-order-notification", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, "IT Market");
            helper.setTo(adminEmail);
            helper.setSubject("🚨 New Order Alert - #" + order.getId());
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            System.out.println("Admin notification email sent for order: " + order.getId());
            
        } catch (Exception e) {
            System.err.println("Failed to send admin notification email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            Context context = new Context(Locale.getDefault());
            context.setVariable("userName", userName);
            context.setVariable("year", java.time.Year.now().getValue());
            
            String htmlContent = templateEngine.process("email/welcome", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, "IT Market");
            helper.setTo(toEmail);
            helper.setSubject("Welcome to IT Market!");
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            System.out.println("Welcome email sent to: " + toEmail);
            
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
    }
    
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            Context context = new Context(Locale.getDefault());
            context.setVariable("resetToken", resetToken);
            context.setVariable("resetUrl", "http://localhost:8080/reset-password?token=" + resetToken);
            context.setVariable("year", java.time.Year.now().getValue());
            
            String htmlContent = templateEngine.process("email/password-reset", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, "IT Market");
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request");
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            System.out.println("Password reset email sent to: " + toEmail);
            
        } catch (Exception e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
        }
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return String.format("$%.2f", amount);
    }
}