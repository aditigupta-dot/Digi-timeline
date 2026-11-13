package com.digitaltimeline.util;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/** Minimal RFC 4180 CSV helper that also neutralises spreadsheet formula injection (cells starting with = + - @). */
public final class CsvWriter {
  private CsvWriter() {}
  public static String cell(Object value) {
    if (value == null) return "";
    String s = value.toString();
    if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;
    return (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
  }
  public static String row(Object... cells) { return Stream.of(cells).map(CsvWriter::cell).collect(Collectors.joining(",")) + "\r\n"; }
}
