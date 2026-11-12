package be.ae.springfactory.export;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A CSV document as RFC 4180 writes it: comma-separated cells, as many on every line as there are
 * headings (a short line is filled with empty cells), CRLF line ends, and a cell quoted
 * when it holds a comma, quote or line break, its quotes doubled. Numbers use a dot for decimals,
 * whatever the locale. A cell a spreadsheet would take for a formula (it starts with =, +, -, @, a
 * tab or a carriage return) is written with a leading apostrophe, so a name such as "=1+1" stays
 * text.
 */
final class Csv {

  private final StringBuilder text = new StringBuilder();
  private final int columns;

  /** A CSV document whose first line holds these column headings. */
  Csv(List<String> headings) {
    columns = headings.size();
    line(headings);
  }

  /**
   * Appends one line of cells, filled with empty cells up to the number of headings. A {@code
   * null} cell is written empty.
   */
  void line(List<?> cells) {
    if (cells.size() > columns) {
      throw new IllegalArgumentException(
          "A line has at most " + columns + " cells, was " + cells.size());
    }
    List<String> written = new ArrayList<>(columns);
    for (Object cell : cells) {
      written.add(cell(cell));
    }
    while (written.size() < columns) {
      written.add("");
    }
    text.append(String.join(",", written)).append("\r\n");
  }

  @Override
  public String toString() {
    return text.toString();
  }

  private static String cell(Object value) {
    if (value == null) {
      return "";
    }
    String text = value instanceof BigDecimal number ? number.toPlainString() : value.toString();
    if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
      text = "'" + text;
    }
    if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
      return '"' + text.replace("\"", "\"\"") + '"';
    }
    return text;
  }
}
