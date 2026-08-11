package org.usf.inspect.server.validation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates values against the {@link Condition} configured by the {@link Validate} annotation.
 */
public class Validator implements ConstraintValidator<Validate, Object> {
    private Condition condition;

    /**
     * Initializes this validator with the condition declared on the annotation.
     *
     * @param annotation the validation annotation that provides the condition to apply
     */
    @Override
    public void initialize(Validate annotation) {
        this.condition = annotation.value();
    }

    /**
     * Checks whether the provided value satisfies the configured validation condition.
     *
     * @param value the value to validate
     * @param context the validation context for this constraint evaluation
     * @return {@code true} when the value is {@code null} or matches the configured condition, otherwise {@code false}
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        return value == null || condition.test(value);
    }
}
