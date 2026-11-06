package moments4j.core;

import moments4j.core.security.CurrentUserService;
import moments4j.model.Notification;
import moments4j.repo.NotificationRepository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Port of moments/core/templating.py (context processor: notification_count) plus
 * flash message plumbing: moves interceptor/session flashes into the model.
 */
@ControllerAdvice
public class TemplatingAdvice {

    private final CurrentUserService currentUserService;
    private final NotificationRepository notifications;
    private final ViewHelper viewHelper;

    public TemplatingAdvice(CurrentUserService currentUserService, NotificationRepository notifications,
                            ViewHelper viewHelper) {
        this.currentUserService = currentUserService;
        this.notifications = notifications;
        this.viewHelper = viewHelper;
    }

    @ModelAttribute
    public void addAttributes(Model model, HttpServletRequest request) {
        var current = currentUserService.get();
        model.addAttribute("currentUser", current);
        model.addAttribute("v", viewHelper);

        // current path with query (request.full_path equivalent, used by follow forms' next=)
        String query = request.getQueryString();
        model.addAttribute("requestPath", request.getRequestURI() + (query != null ? "?" + query : ""));
        model.addAttribute("requestUrl", request.getRequestURL().toString());
        // scheme://host[:port]/context (url_for(_external=True) base)
        StringBuffer url = request.getRequestURL();
        String uri = request.getRequestURI();
        model.addAttribute("baseUrl", url.substring(0, url.length() - uri.length()) + request.getContextPath());

        if (current != null) {
            model.addAttribute("notificationCount", notifications.countByReceiverIdAndReadFalse(current.getId()));
        } else {
            model.addAttribute("notificationCount", null);
        }

        // move session-stashed interceptor flashes into the model (flask.flash equivalent)
        HttpSession session = request.getSession(false);
        if (session != null) {
            List<Object[]> stashed = (List<Object[]>) session.getAttribute("interceptorFlashes");
            if (stashed != null) {
                model.addAttribute("flashes", new ArrayList<>(stashed));
                session.removeAttribute("interceptorFlashes");
            }
        }
    }
}
