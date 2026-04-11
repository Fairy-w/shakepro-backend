package com.shakepro.dto.response;

public record AuthConfigResponse(
        LoginConfig login,
        RegisterConfig register,
        ValidationConfig validation,
        BehaviorConfig behavior
) {

    public static AuthConfigResponse defaultConfig() {
        return new AuthConfigResponse(
                new LoginConfig(
                        "登录界面",
                        "输入账号密码，继续你的调酒灵感与个人收藏。",
                        "用户名",
                        "密码",
                        "登录",
                        "正在为你准备专属酒单...",
                        "注册账号",
                        "还没有账号？",
                        "登录失败"
                ),
                new RegisterConfig(
                        "创建账号",
                        "注册后即可登录并同步你的个人酒单",
                        "请输入用户名",
                        "请输入密码",
                        "请输入昵称",
                        "立即注册",
                        "正在提交注册信息...",
                        "返回登录",
                        "注册成功，请前往登录",
                        "注册失败"
                ),
                new ValidationConfig(
                        3,
                        50,
                        6,
                        50,
                        50,
                        "请输入用户名、密码和昵称",
                        "用户名长度需在 3 到 50 个字符之间",
                        "密码长度需在 6 到 50 个字符之间",
                        "昵称长度不能超过 50 个字符"
                ),
                new BehaviorConfig("back_to_login")
        );
    }

    public record LoginConfig(
            String title,
            String subtitle,
            String usernameLabel,
            String passwordLabel,
            String submitText,
            String loadingText,
            String registerEntryText,
            String registerHintText,
            String defaultErrorText
    ) {
    }

    public record RegisterConfig(
            String title,
            String subtitle,
            String usernamePlaceholder,
            String passwordPlaceholder,
            String nicknamePlaceholder,
            String submitText,
            String loadingText,
            String backToLoginText,
            String successText,
            String defaultErrorText
    ) {
    }

    public record ValidationConfig(
            Integer usernameMinLength,
            Integer usernameMaxLength,
            Integer passwordMinLength,
            Integer passwordMaxLength,
            Integer nicknameMaxLength,
            String emptyFormText,
            String usernameLengthText,
            String passwordLengthText,
            String nicknameLengthText
    ) {
    }

    public record BehaviorConfig(String registerSuccessAction) {
    }
}
