package com.game.network.util;

public class TerminalColor {
    // 重置所有样式（默认）
    public static final String RESET = "\033[0m";

    // 前景色（文字颜色）
    public static final String BLACK = "\033[30m";
    public static final String RED = "\033[31m";
    public static final String GREEN = "\033[32m";
    public static final String YELLOW = "\033[33m";
    public static final String BLUE = "\033[34m";
    public static final String PURPLE = "\033[35m";
    public static final String CYAN = "\033[36m";
    public static final String WHITE = "\033[37m";

    // 背景色
    public static final String BLACK_BG = "\033[40m";
    public static final String RED_BG = "\033[41m";
    public static final String GREEN_BG = "\033[42m";
    public static final String YELLOW_BG = "\033[43m";
    public static final String BLUE_BG = "\033[44m";
    public static final String PURPLE_BG = "\033[45m";
    public static final String CYAN_BG = "\033[46m";
    public static final String WHITE_BG = "\033[47m";

    // 样式
    public static final String BOLD = "\033[1m";       // 加粗
    public static final String UNDERLINE = "\033[4m";  // 下划线
    public static final String BLINK = "\033[5m";      // 闪烁（部分终端不支持）
    public static final String REVERSE = "\033[7m";    // 反色（前景色与背景色互换）

    // 包装文本为指定样式
    public static String colorize(String text, String... styles) {
        StringBuilder sb = new StringBuilder();
        // 添加所有样式
        for (String style : styles) {
            sb.append(style);
        }
        // 添加文本内容
        sb.append(text);
        // 重置样式（避免影响后续输出）
        sb.append(RESET);
        return sb.toString();
    }
}
