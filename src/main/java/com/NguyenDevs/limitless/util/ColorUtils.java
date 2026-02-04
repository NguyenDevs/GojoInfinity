package com.NguyenDevs.limitless.util;

import net.md_5.bungee.api.ChatColor;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern
            .compile("<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.*?)</gradient>");

    public static String colorize(String message) {
        if (message == null)
            return "";

        Matcher gradientMatcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (gradientMatcher.find()) {
            String startHex = gradientMatcher.group(1);
            String endHex = gradientMatcher.group(2);
            String text = gradientMatcher.group(3);
            gradientMatcher.appendReplacement(sb, applyGradient(text, startHex, endHex));
        }
        gradientMatcher.appendTail(sb);
        message = sb.toString();

        Matcher hexMatcher = HEX_PATTERN.matcher(message);
        sb = new StringBuffer();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            hexMatcher.appendReplacement(sb, ChatColor.of("#" + hex).toString());
        }
        hexMatcher.appendTail(sb);
        message = sb.toString();

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String applyGradient(String text, String startHex, String endHex) {
        StringBuilder result = new StringBuilder();
        Color start = Color.decode(startHex);
        Color end = Color.decode(endHex);
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) (length - 1);
            if (length == 1)
                ratio = 0;

            int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));

            Color stepColor = new Color(red, green, blue);
            String hex = String.format("#%02x%02x%02x", stepColor.getRed(), stepColor.getGreen(), stepColor.getBlue());

            result.append(ChatColor.of(hex)).append(text.charAt(i));
        }
        return result.toString();
    }
}
