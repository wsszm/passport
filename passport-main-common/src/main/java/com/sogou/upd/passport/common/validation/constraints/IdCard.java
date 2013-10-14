package com.sogou.upd.passport.common.validation.constraints;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created with IntelliJ IDEA.
 * User: liuling
 * Date: 13-9-4
 * Time: 上午10:50
 * To change this template use File | Settings | File Templates.
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = IdCardValidator.class)
@Documented
public @interface IdCard {
    String message() default "身份证格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
