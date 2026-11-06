package moments4j.core.decorators;

import java.lang.annotation.*;

/** Port of flask-login's @fresh_login_required. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FreshLoginRequired {
}
