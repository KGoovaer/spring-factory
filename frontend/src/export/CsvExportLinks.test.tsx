import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { CsvExportLinks } from "./CsvExportLinks";

describe("CsvExportLinks", () => {
  it("offers everything planned and logged as CSV downloads", () => {
    render(<CsvExportLinks />);

    const planned = screen.getByRole("link", { name: "Download planned Programs" });
    const logged = screen.getByRole("link", { name: "Download Logged Sessions" });
    expect(planned).toHaveAttribute("href", "/api/export/planned.csv");
    expect(planned).toHaveAttribute("download");
    expect(logged).toHaveAttribute("href", "/api/export/logged.csv");
    expect(logged).toHaveAttribute("download");
  });
});
