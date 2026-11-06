package moments4j.core.decorators;

import java.lang.annotation.*;

/** Port of moments/decorators.py @login_required. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LoginRequired {
}
