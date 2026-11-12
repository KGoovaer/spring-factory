package be.ae.springfactory.export;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Downloads everything planned or logged as a CSV file in UTF-8. The file starts with a byte order
 * mark, so that a spreadsheet such as Excel reads names with accents as UTF-8 too.
 */
@RestController
@RequestMapping("/api/export")
class ExportController {

  private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

  private static final String BYTE_ORDER_MARK = "\uFEFF";

  private final CsvExport export;

  ExportController(CsvExport export) {
    this.export = export;
  }

  @GetMapping("/planned.csv")
  ResponseEntity<String> planned() {
    return download("kettlebell-planned.csv", export.planned());
  }

  @GetMapping("/logged.csv")
  ResponseEntity<String> logged() {
    return download("kettlebell-logged.csv", export.logged());
  }

  private static ResponseEntity<String> download(String filename, String csv) {
    return ResponseEntity.ok()
        .contentType(CSV)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(filename).build().toString())
        .body(BYTE_ORDER_MARK + csv);
  }
}
