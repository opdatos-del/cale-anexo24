export type CsvValue = string | number | null | undefined;

export const CSV_EXPORT_PAGE_SIZE = 100;
export const CSV_EXPORT_MAX_ROWS = 10_000;
export const CSV_EXPORT_LIMIT_MESSAGE = "La exportación supera el máximo de 10,000 registros.";

export function serializeCsv(headers: string[], rows: CsvValue[][]): string {
  const escape = (value: CsvValue): string => {
    let text = value === null || value === undefined ? "" : String(value);
    if (typeof value === "string" && /^[=+\-@]/.test(text)) text = "'" + text;
    return /[",\r\n]/.test(text) ? '"' + text.replace(/"/g, '""') + '"' : text;
  };
  return "\uFEFF" + [headers, ...rows].map((row) => row.map(escape).join(",")).join("\r\n");
}

export function downloadCsv(filename: string, content: string): void {
  const blob = new Blob([content], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

export function normalizeCsvDate(value: string | null): string {
  return value ? value.split("T")[0] : "";
}

export interface CsvPage<T> {
  items: T[];
  total: number;
}

export async function loadAllCsvPages<T>(fetchPage: (page: number, pageSize: number) => Promise<CsvPage<T>>): Promise<T[]> {
  const first = await fetchPage(1, CSV_EXPORT_PAGE_SIZE);
  if (first.total > CSV_EXPORT_MAX_ROWS) throw new Error(CSV_EXPORT_LIMIT_MESSAGE);
  if (first.total === 0) return [];
  const items = [...first.items];
  const pageCount = Math.ceil(first.total / CSV_EXPORT_PAGE_SIZE);
  for (let page = 2; page <= pageCount; page++) {
    items.push(...(await fetchPage(page, CSV_EXPORT_PAGE_SIZE)).items);
  }
  return items;
}
