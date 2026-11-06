package moments4j.core.security;

import moments4j.model.User;
import moments4j.repo.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Provides current_user (flask-login) to controllers and templates.
 * A null user plays the role of flask-login's Guest (can() is always false).
 * Like flask-login's user_loader, the user is re-loaded from the database
 * each request (the session-cached principal is detached).
 */
@Service("currentUserService")
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (auth.getPrincipal() instanceof moments4j.service.AppUserDetailsService.AuthUser authUser) {
            // re-load so the entity is managed (session-cached principal is detached)
            return users.findById(authUser.user().getId()).orElse(null);
        }
        return null;
    }

    public boolean isAuthenticated() {
        return get() != null;
    }
}
