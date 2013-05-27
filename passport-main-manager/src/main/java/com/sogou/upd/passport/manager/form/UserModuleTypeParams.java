package com.sogou.upd.passport.manager.form;

import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Min;

/**
 * Created with IntelliJ IDEA. User: hujunfei Date: 13-5-24 Time: 下午12:03 To change this template
 * use File | Settings | File Templates.
 */
public class UserModuleTypeParams extends BaseUserParams {
    @NotBlank
    protected String module;
    @NotBlank
    protected String mode;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}