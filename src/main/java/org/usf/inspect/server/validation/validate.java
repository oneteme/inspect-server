package org.usf.inspect.server.validation;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.nimbusds.jose.Payload;
import jakarta.validation.Constraint;
@Target({PARAMETER, FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = Validator.class)
public @interface validate {
    Condition value();
    String message() default "invalid value";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
