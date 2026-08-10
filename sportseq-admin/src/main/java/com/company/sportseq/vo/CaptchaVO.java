package com.company.sportseq.vo;

/**
 * 图形验证码出参：uuid 用于登录回传，img 为 base64 图片（data URL）。
 */
public record CaptchaVO(String uuid, String img) {
}
