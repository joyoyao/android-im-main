package io.rong.imkit.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.rong.imlib.model.Message;

/**
 * Created by jenny_zhou on 15/1/24.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConversationProviderTag  {

    /**
     * 头像显示位置。1靠左显示，2代码靠右显示，3代表不显示。
     */
    int portraitPosition() default 1;

    /**
     * 是否横向居中显示
     */
    boolean centerInHorizontal() default false;

    String conversationType() default "private";
}

