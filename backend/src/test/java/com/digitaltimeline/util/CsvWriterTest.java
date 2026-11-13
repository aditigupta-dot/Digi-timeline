package com.digitaltimeline.util;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
class CsvWriterTest {
  @Test void plainCellUnchanged() { assertEquals("hello", CsvWriter.cell("hello")); }
  @Test void commasAndQuotesAreEscaped() { assertEquals("\"a,\"\"b\"\"\"", CsvWriter.cell("a,\"b\"")); }
  @Test void formulaPrefixIsNeutralised() { assertEquals("'=SUM(A1)", CsvWriter.cell("=SUM(A1)")); }
  @Test void nullBecomesEmpty() { assertEquals("a,,b\r\n", CsvWriter.row("a", null, "b")); }
}
