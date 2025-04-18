package sh.fyz.fiber.annotations.security;

import sh.fyz.fiber.core.authentication.AuthScheme;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuthType {
    AuthScheme[] value();
} 