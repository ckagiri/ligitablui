package com.ligitabl.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

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
            Model model) {
        
        model.addAttribute("isDemo", true);
        
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Register");
            return "auth/register";
        }
        
        // In demo mode, just redirect to predictions
        redirectAttributes.addFlashAttribute("message", 
            "Demo Mode: Registration successful! You're automatically logged in as " + form.getDisplayName());
        redirectAttributes.addFlashAttribute("messageType", "success");
        
        return "redirect:/predictions/me";
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
            Model model) {
        
        model.addAttribute("isDemo", true);
        
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Login");
            return "auth/login";
        }
        
        // In demo mode, just redirect to predictions
        redirectAttributes.addFlashAttribute("message", 
            "Demo Mode: Login successful! Welcome back!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        
        return "redirect:/predictions/me";
    }

    @GetMapping("/logout")
    public String logout(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", 
            "Demo Mode: You've been logged out.");
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
