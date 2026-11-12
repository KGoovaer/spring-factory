/** Links that download everything planned and everything logged, each as its own CSV file. */
export function CsvExportLinks() {
  return (
    <section aria-labelledby="export-heading" className="export">
      <h2 id="export-heading">Export to CSV</h2>
      <p>Every Program, for a spreadsheet. Planned and logged data come in separate files.</p>
      <ul>
        <li>
          <a href="/api/export/planned.csv" download>
            Download planned Programs
          </a>
        </li>
        <li>
          <a href="/api/export/logged.csv" download>
            Download Logged Sessions
          </a>
        </li>
      </ul>
    </section>
  );
}
