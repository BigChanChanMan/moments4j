package moments4j.core.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moments4j.core.decorators.ConfirmRequired;
import moments4j.core.decorators.FreshLoginRequired;
import moments4j.core.decorators.LoginRequired;
import moments4j.core.decorators.PermissionRequired;
import moments4j.model.User;
import moments4j.service.UserService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of moments/decorators.py plus flask-login's login_required / fresh_login_required
 * runtime. One interceptor evaluates the decorator annotations on each handler.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String FRESH_LOGIN_KEY = "moments.freshLogin";
    /** flask-login's login_manager.login_message. */
    public static final String LOGIN_MESSAGE = "Please log in to access this page.";
    /** flask-login's login_manager.needs_refresh_message. */
    public static final String REFRESH_MESSAGE = "Please reauthenticate to access this page.";

    private final UserService userService;
    private final moments4j.core.security.CurrentUserService currentUserService;

    public AuthInterceptor(UserService userService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        User user = currentUserService.get();

        if (handlerMethod.hasMethodAnnotation(LoginRequired.class)
                || handlerMethod.hasMethodAnnotation(ConfirmRequired.class)
                || handlerMethod.hasMethodAnnotation(FreshLoginRequired.class)
                || handlerMethod.hasMethodAnnotation(PermissionRequired.class)) {
            if (user == null) {
                // login_required: redirect to login page with next
                String target = request.getRequestURI() + queryString(request);
                flash(request, "warning", LOGIN_MESSAGE);
                response.sendRedirect("/auth/login?next=" + java.net.URLEncoder.encode(target, java.nio.charset.StandardCharsets.UTF_8));
                return false;
            }
        }

        if (handlerMethod.hasMethodAnnotation(FreshLoginRequired.class)) {
            if (!isFresh(request)) {
                flash(request, "warning", REFRESH_MESSAGE);
                String target = request.getRequestURI() + queryString(request);
                response.sendRedirect("/auth/re-authenticate?next=" + java.net.URLEncoder.encode(target, java.nio.charset.StandardCharsets.UTF_8));
                return false;
            }
        }

        if (handlerMethod.hasMethodAnnotation(ConfirmRequired.class)) {
            if (!user.isConfirmed()) {
                String resendUrl = "/auth/resend-confirmation-email";
                String message = "Please confirm your account first. Didn't receive the email? "
                        + "<a class=\"alert-link\" href=\"" + resendUrl + "\">Resend Confirmation Email</a>";
                flash(request, "warning", message);
                response.sendRedirect("/");
                return false;
            }
        }

        PermissionRequired permission = handlerMethod.getMethodAnnotation(PermissionRequired.class);
        if (permission != null && !userService.can(user, permission.value())) {
            // mirrors abort(403) from permission_required
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }

    private boolean isFresh(HttpServletRequest request) {
        Object fresh = request.getSession().getAttribute(FRESH_LOGIN_KEY);
        return Boolean.TRUE.equals(fresh);
    }

    private String queryString(HttpServletRequest request) {
        return request.getQueryString() == null ? "" : "?" + request.getQueryString();
    }

    /**
     * Flash before a direct sendRedirect (no RedirectAttributes available in an interceptor):
     * stashes the message in the session; a @ControllerAdvice moves it into the model
     * for the next render. Mirrors flask.flash().
     */
    @SuppressWarnings("unchecked")
    public static void flash(HttpServletRequest request, String category, String message) {
        List<Object[]> flashes = (List<Object[]>) request.getSession()
                .getAttribute("interceptorFlashes");
        if (flashes == null) {
            flashes = new ArrayList<>();
            request.getSession().setAttribute("interceptorFlashes", flashes);
        }
        flashes.add(new Object[] {category, message});
    }
}
