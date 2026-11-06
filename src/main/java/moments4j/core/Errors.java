package moments4j.core;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Port of moments/core/errors.py.
 *
 * Note: @ModelAttribute advice does NOT run for @ExceptionHandler renders,
 * so the shared model attributes (currentUser, v, notificationCount, ...)
 * are added here explicitly. (@ControllerAdvice 类上不能挂 @RequestMapping,
 * Spring Boot 的 /error dispatch 由 BasicErrorController 兜底。)
 */
@ControllerAdvice
public class Errors {

    private final moments4j.core.security.CurrentUserService currentUserService;
    private final moments4j.repo.NotificationRepository notifications;
    private final ViewHelper viewHelper;

    public Errors(moments4j.core.security.CurrentUserService currentUserService,
                  moments4j.repo.NotificationRepository notifications, ViewHelper viewHelper) {
        this.currentUserService = currentUserService;
        this.notifications = notifications;
        this.viewHelper = viewHelper;
    }

    private ModelAndView errorPage(int status, String description) {
        ModelAndView mav = new ModelAndView("errors/" + status);
        mav.setStatus(HttpStatus.valueOf(status));
        mav.addObject("description", description);
        // shared model attributes (normally provided by TemplatingAdvice)
        var current = currentUserService.get();
        mav.addObject("currentUser", current);
        mav.addObject("v", viewHelper);
        mav.addObject("notificationCount", current == null ? null
                : notifications.countByReceiverIdAndReadFalse(current.getId()));
        return mav;
    }

    private String descriptionOf(HttpServletRequest request, String fallback) {
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (message instanceof String s && !s.isBlank()) {
            return s;
        }
        return fallback;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleResponseStatus(ResponseStatusException e, HttpServletRequest request) {
        int status = e.getStatusCode().value();
        if (status == 400 || status == 403 || status == 404 || status == 413 || status == 500) {
            return errorPage(status, descriptionOf(request, e.getReason() != null ? e.getReason() : fallback(status)));
        }
        ModelAndView mav = new ModelAndView("errors/500");
        mav.setStatus(e.getStatusCode());
        mav.addObject("description", e.getReason());
        return mav;
    }

    private String fallback(int status) {
        // werkzeug default descriptions, matching the original Flask error pages
        return switch (status) {
            case 400 -> "The browser (or proxy) sent a request that this server could not understand.";
            case 403 -> "You don't have the permission to access the requested resource. "
                    + "It is either read-protected or not readable by the server.";
            case 404 -> "The requested URL was not found on the server. "
                    + "If you entered the URL manually please check your spelling and try again.";
            case 413 -> "File Too Large";
            default -> "Internal Server Error";
        };
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleTooLarge(MaxUploadSizeExceededException e) {
        return errorPage(413, "File Too Large");
    }

    // mirrors the CSRFError handler (renders the 400 page, returns 500 — kept 1:1 with the original)
    @ExceptionHandler(CsrfException.class)
    public ModelAndView handleCsrf(CsrfException e) {
        ModelAndView mav = errorPage(400, "Session expired, return last page and try again.");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }

    // 404s raised by the framework (no matching route / static resource)
    @ExceptionHandler({NoResourceFoundException.class})
    public ModelAndView handleNoResource(NoResourceFoundException e) {
        return errorPage(404, "The requested URL was not found on the server. "
                + "If you entered the URL manually please check your spelling and try again.");
    }
}
