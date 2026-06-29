package org.usf.inspect.server.validation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
public class Validator implements ConstraintValidator<Validate, Object> {
    private Condition condition;
    @Override
    public void initialize(Validate annotation) {
        this.condition = annotation.value();
    }
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        return value == null || condition.test(value);
    }
}
