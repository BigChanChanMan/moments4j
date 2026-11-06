package moments4j.core.decorators;

import java.lang.annotation.*;

/** Port of moments/decorators.py @permission_required(name). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PermissionRequired {
    String value();
}
