package moments4j.blueprints;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moments4j.core.Utils;
import moments4j.core.decorators.LoginRequired;
import moments4j.core.security.AuthInterceptor;
import moments4j.core.security.SecurityConfig;
import moments4j.forms.ForgetPasswordForm;
import moments4j.forms.LoginForm;
import moments4j.forms.RegisterForm;
import moments4j.forms.ResetPasswordForm;
import moments4j.model.User;
import moments4j.repo.UserRepository;
import moments4j.service.AppUserDetailsService;
import moments4j.service.MailService;
import moments4j.service.UserService;
import moments4j.settings.MomentsSettings;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Port of moments/blueprints/auth.py.
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository users;
    private final UserService userService;
    private final MailService mail;
    private final Utils utils;
    private final SecurityContextRepository contextRepository;
    private final TokenBasedRememberMeServices rememberMeServices;
    private final moments4j.core.security.CurrentUserService currentUserService;
    private final MomentsSettings settings;

    public AuthController(UserRepository users, UserService userService, MailService mail, Utils utils,
                          SecurityContextRepository contextRepository,
                          TokenBasedRememberMeServices rememberMeServices,
                          moments4j.core.security.CurrentUserService currentUserService, MomentsSettings settings) {
        this.users = users;
        this.userService = userService;
        this.mail = mail;
        this.utils = utils;
        this.contextRepository = contextRepository;
        this.rememberMeServices = rememberMeServices;
        this.currentUserService = currentUserService;
        this.settings = settings;
    }

    @GetMapping("/login")
    public Object loginPage(Model model) {
        if (currentUserService.get() != null) {
            return new RedirectView("/");
        }
        model.addAttribute("form", new LoginForm());
        return "auth/login";
    }

    @PostMapping("/login")
    public RedirectView login(@ModelAttribute("form") LoginForm form, BindingResult result,
                              HttpServletRequest request, HttpServletResponse response,
                              RedirectAttributes ra) {
        if (currentUserService.get() != null) {
            return new RedirectView("/");
        }
        if (result.hasErrors()) {
            Utils.flashErrors(result, ra);
            return new RedirectView("/auth/login");
        }
        User user = users.findByEmailIgnoreCase(form.getEmail()).orElse(null);
        if (user != null && userService.validatePassword(user, form.getPassword())) {
            if (!user.isActive()) {  // login_user returns False for inactive users
                Utils.flash(ra, "warning", "Your account is blocked.");
                return new RedirectView("/");
            }
            signIn(request, response, user, form.isRememberMe());
            Utils.flash(ra, "info", "Login success.");
            return utils.redirectBack(request, "/");
        }
        Utils.flash(ra, "warning", "Invalid email or password.");
        return new RedirectView("/auth/login");
    }

    /** Programmatic login, mirrors flask-login's login_user(). */
    private void signIn(HttpServletRequest request, HttpServletResponse response, User user, boolean remember) {
        var principal = new AppUserDetailsService.AuthUser(user);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        if (remember) {
            rememberMeServices.loginSuccess(request, response, auth);
        }
        // fresh-login bookkeeping (flask-login's login_fresh)
        request.getSession().setAttribute(AuthInterceptor.FRESH_LOGIN_KEY, true);
    }

    @GetMapping("/re-authenticate")
    @LoginRequired
    public String reAuthenticatePage(Model model) {
        return "auth/login";
    }

    @PostMapping("/re-authenticate")
    @LoginRequired
    public RedirectView reAuthenticate(@ModelAttribute("form") LoginForm form, BindingResult result,
                                       HttpServletRequest request, HttpServletResponse response,
                                       RedirectAttributes ra) {
        User user = currentUserService.get();
        if (isFresh(request)) {
            return new RedirectView("/");
        }
        if (!result.hasErrors() && user != null && userService.validatePassword(user, form.getPassword())) {
            request.getSession().setAttribute(AuthInterceptor.FRESH_LOGIN_KEY, true);
            return utils.redirectBack(request, "/");
        }
        return new RedirectView("/auth/re-authenticate");
    }

    private boolean isFresh(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getSession().getAttribute(AuthInterceptor.FRESH_LOGIN_KEY));
    }

    @GetMapping("/logout")
    @LoginRequired
    public RedirectView logout(HttpServletRequest request, HttpServletResponse response, RedirectAttributes ra) {
        // mirrors logout_user(): clear context, session and remember-me cookie
        var logoutHandler = new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler();
        logoutHandler.logout(request, response, null);
        new org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler(
                TokenBasedRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY
        ).logout(request, response, null);
        Utils.flash(ra, "info", "Logout success.");
        return new RedirectView("/");
    }

    @GetMapping("/register")
    public Object registerPage(Model model) {
        if (currentUserService.get() != null) {
            return new RedirectView("/");
        }
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public RedirectView register(@ModelAttribute("form") RegisterForm form, BindingResult result,
                                 HttpServletRequest request, HttpServletResponse response,
                                 RedirectAttributes ra) {
        if (currentUserService.get() != null) {
            return new RedirectView("/");
        }
        if (!result.hasErrors()) {
            if (!form.getPassword().equals(form.getPassword2())) {
                result.rejectValue("password2", "", "Field must be equal to password.");
            }
            if (users.findByEmailIgnoreCase(form.getEmail()).isPresent()) {
                result.rejectValue("email", "", "The email is already in use.");
            }
            if (users.findByUsername(form.getUsername()).isPresent()) {
                result.rejectValue("username", "", "The username is already in use.");
            }
        }
        if (result.hasErrors()) {
            Utils.flashErrors(result, ra);
            return new RedirectView("/auth/register");
        }
        User user = userService.createUser(form.getName(), form.getEmail().toLowerCase(),
                form.getUsername(), form.getPassword(), false);
        signIn(request, response, user, false);
        String token = utils.generateToken(user, MomentsSettings.Operation.CONFIRM, Duration.ofHours(1));
        String confirmUrl = baseUrl(request) + "/auth/confirm/" + token;
        mail.sendConfirmationEmail(user, token, confirmUrl);
        Utils.flash(ra, "info", "Confirmation email sent, please check your inbox.");
        return new RedirectView("/");
    }

    @GetMapping("/confirm/{token}")
    @LoginRequired
    public RedirectView confirm(@PathVariable String token, RedirectAttributes ra) {
        User user = currentUserService.get();
        if (user.isConfirmed()) {
            return new RedirectView("/");
        }
        if (!utils.parseToken(user, token, MomentsSettings.Operation.CONFIRM).isEmpty()) {
            user.setConfirmed(true);
            users.save(user);
            Utils.flash(ra, "success", "Account confirmed.");
            return new RedirectView("/");
        }
        Utils.flash(ra, "danger", "Invalid or expired token.");
        return new RedirectView("/auth/resend-confirmation-email");
    }

    @GetMapping("/resend-confirmation-email")
    @LoginRequired
    public RedirectView resendConfirmationEmail(HttpServletRequest request, RedirectAttributes ra) {
        User user = currentUserService.get();
        if (user.isConfirmed()) {
            return new RedirectView("/");
        }
        String token = utils.generateToken(user, MomentsSettings.Operation.CONFIRM, Duration.ofHours(1));
        String confirmUrl = baseUrl(request) + "/auth/confirm/" + token;
        mail.sendConfirmationEmail(user, token, confirmUrl);
        Utils.flash(ra, "info", "New email sent, please check your inbox.");
        return new RedirectView("/");
    }

    @GetMapping("/forget-password")
    public Object forgetPasswordPage() {
        if (currentUserService.get() != null) {
            return new RedirectView("/");
        }
        return "auth/reset_password";
    }

    @PostMapping("/forget-password")
    public RedirectView forgetPassword(@ModelAttribute("form") ForgetPasswordForm form, BindingResult result,
                                       HttpServletRequest request, RedirectAttributes ra) {
        if (currentUserService.get() != null) {
            return new RedirectView("/");
        }
        if (result.hasErrors()) {
            Utils.flashErrors(result, ra);
            return new RedirectView("/auth/forget-password");
        }
        User user = users.findByEmailIgnoreCase(form.getEmail()).orElse(null);
        if (user != null) {
            String token = utils.generateToken(user, MomentsSettings.Operation.RESET_PASSWORD, Duration.ofHours(1));
            String resetUrl = baseUrl(request) + "/auth/reset-password/" + token;
            mail.sendResetPasswordEmail(user, token, resetUrl);
            Utils.flash(ra, "info", "Password reset email sent, please check your inbox.");
            return new RedirectView("/auth/login");
        }
        Utils.flash(ra, "warning", "Invalid email.");
        return new RedirectView("/auth/forget-password");
    }

    @GetMapping("/reset-password/{token}")
    public Object resetPasswordPage(@PathVariable String token, Model model) {
        if (currentUserService.get() != null) {
            return new RedirectView("/");
        }
        model.addAttribute("token", token);
        model.addAttribute("form", new ResetPasswordForm());
        return "auth/reset_password";
    }

    @PostMapping("/reset-password/{token}")
    public RedirectView resetPassword(@PathVariable String token,
                                      @ModelAttribute("form") ResetPasswordForm form, BindingResult result,
                                      RedirectAttributes ra) {
        if (currentUserService.get() != null) {
            return new RedirectView("/");
        }
        if (result.hasErrors()) {
            Utils.flashErrors(result, ra);
            return new RedirectView("/auth/reset-password/" + token);
        }
        User user = users.findByEmailIgnoreCase(form.getEmail()).orElse(null);
        if (user == null) {
            return new RedirectView("/");
        }
        if (!utils.parseToken(user, token, MomentsSettings.Operation.RESET_PASSWORD).isEmpty()) {
            userService.setPassword(user, form.getPassword());
            users.save(user);
            Utils.flash(ra, "success", "Password updated.");
            return new RedirectView("/auth/login");
        }
        Utils.flash(ra, "danger", "Invalid or expired link.");
        return new RedirectView("/auth/forget-password");
    }

    private String baseUrl(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        String context = request.getContextPath();
        int idx = url.indexOf(context.isEmpty() ? "" : context);
        return idx >= 0 ? url.substring(0, idx + context.length()) : url;
    }
}
