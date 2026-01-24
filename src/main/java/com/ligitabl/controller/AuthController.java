package com.ligitabl.controller;

import com.ligitabl.domain.model.user.UserId;
import com.ligitabl.infrastructure.auth.DemoAuthFilter;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo authentication controller.
 *
 * <p>This is a simple demo implementation that:
 * <ul>
 *   <li>Generates a UserId on registration and stores it in session</li>
 *   <li>Looks up existing users by email on login</li>
 *   <li>Works with DemoAuthFilter to provide Principal to controllers</li>
 * </ul>
 *
 * <p>NOT FOR PRODUCTION USE - no real password hashing or security.</p>
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    // Simple in-memory user store for demo (email -> userId)
    // In a real app, this would be a database
    private static final Map<String, String> DEMO_USERS = new ConcurrentHashMap<>();
    private static final Map<String, String> DEMO_DISPLAY_NAMES = new ConcurrentHashMap<>();

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("pageTitle", "Register");
        model.addAttribute("registerForm", new RegisterForm());
        model.addAttribute("isDemo", true);
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute RegisterForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpSession session) {

        model.addAttribute("isDemo", true);

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Register");
            return "auth/register";
        }

        // Check if email already registered
        if (DEMO_USERS.containsKey(form.getEmail().toLowerCase())) {
            result.rejectValue("email", "duplicate", "This email is already registered");
            model.addAttribute("pageTitle", "Register");
            return "auth/register";
        }

        // Generate new user ID and store in demo registry
        UserId userId = UserId.generate();
        String email = form.getEmail().toLowerCase();
        DEMO_USERS.put(email, userId.value());
        DEMO_DISPLAY_NAMES.put(userId.value(), form.getDisplayName());

        // Store in session for DemoAuthFilter
        session.setAttribute(DemoAuthFilter.SESSION_USER_ID_KEY, userId.value());
        session.setAttribute(DemoAuthFilter.SESSION_DISPLAY_NAME_KEY, form.getDisplayName());

        log.info("Demo registration: email={}, userId={}, displayName={}",
            email, userId.value(), form.getDisplayName());

        redirectAttributes.addFlashAttribute("message",
            "Welcome, " + form.getDisplayName() + "! You're now logged in.");
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/predictions/user/me";
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("pageTitle", "Login");
        model.addAttribute("loginForm", new LoginForm());
        model.addAttribute("isDemo", true);
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute LoginForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpSession session) {

        model.addAttribute("isDemo", true);

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Login");
            return "auth/login";
        }

        String email = form.getEmail().toLowerCase();
        String userId = DEMO_USERS.get(email);

        if (userId == null) {
            // For demo, auto-create user on first login
            UserId newUserId = UserId.generate();
            userId = newUserId.value();
            DEMO_USERS.put(email, userId);
            // Extract display name from email
            String displayName = email.split("@")[0];
            DEMO_DISPLAY_NAMES.put(userId, displayName);

            log.info("Demo auto-registration on login: email={}, userId={}", email, userId);
        }

        String displayName = DEMO_DISPLAY_NAMES.getOrDefault(userId, "User");

        // Store in session for DemoAuthFilter
        session.setAttribute(DemoAuthFilter.SESSION_USER_ID_KEY, userId);
        session.setAttribute(DemoAuthFilter.SESSION_DISPLAY_NAME_KEY, displayName);

        log.info("Demo login: email={}, userId={}", email, userId);

        redirectAttributes.addFlashAttribute("message",
            "Welcome back, " + displayName + "!");
        redirectAttributes.addFlashAttribute("messageType", "success");

        return "redirect:/predictions/user/me";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        String displayName = (String) session.getAttribute(DemoAuthFilter.SESSION_DISPLAY_NAME_KEY);

        // Clear session
        session.removeAttribute(DemoAuthFilter.SESSION_USER_ID_KEY);
        session.removeAttribute(DemoAuthFilter.SESSION_DISPLAY_NAME_KEY);

        log.info("Demo logout: displayName={}", displayName);

        redirectAttributes.addFlashAttribute("message",
            "You've been logged out. See you next time!");
        redirectAttributes.addFlashAttribute("messageType", "info");

        return "redirect:/";
    }

    @Data
    public static class RegisterForm {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Display name is required")
        @Size(min = 2, max = 100, message = "Display name must be between 2 and 100 characters")
        private String displayName;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank(message = "Confirm password is required")
        private String confirmPassword;
    }

    @Data
    public static class LoginForm {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }
}
