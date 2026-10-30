package com.erp.common.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 轻量 CSV 导出（UTF-8 BOM，兼容 Excel）。
 */
public final class CsvUtils {

    private CsvUtils() {
    }

    public static byte[] toCsv(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append(headers.stream().map(CsvUtils::escape).reduce((a, b) -> a + "," + b).orElse("")).append("\r\n");
        for (List<String> row : rows) {
            sb.append(row.stream().map(CsvUtils::escape).reduce((a, b) -> a + "," + b).orElse("")).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
