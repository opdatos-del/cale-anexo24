import { describe, expect, it, vi } from 'vitest';
import { CSV_EXPORT_LIMIT_MESSAGE, CSV_EXPORT_PAGE_SIZE, downloadCsv, loadAllCsvPages, normalizeCsvDate, serializeCsv } from './csv-export';

describe('csv export', () => {
  it('serializes BOM, CRLF, quotes, nulls and formula-like strings safely', () => {
    const csv = serializeCsv(['A', 'B'], [['a,b', 'say "hi"'], ['line\nbreak', null], [undefined, '=1'], ['+x', '-x'], ['@x', -125.5]]);
    expect(csv).toBe('\uFEFFA,B\r\n"a,b","say ""hi"""\r\n"line\nbreak",\r\n,\'=1\r\n\'+x,\'-x\r\n\'@x,-125.5');
  });

  it('downloads CSV and revokes the object URL', () => {
    const createObjectURL = vi.fn(() => 'blob:csv');
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL });
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    downloadCsv('export.csv', 'content');
    expect(createObjectURL).toHaveBeenCalledOnce();
    expect(click).toHaveBeenCalledOnce();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:csv');
    vi.unstubAllGlobals();
  });

  it('carga páginas completas y rechaza límite usando total de primera respuesta', async () => {
    const fetchPage = vi.fn(async (page: number, pageSize: number) => ({ items: [page], total: 201, page, pageSize }));
    await expect(loadAllCsvPages(fetchPage)).resolves.toEqual([1, 2, 3]);
    expect(fetchPage.mock.calls.map(([page, size]) => [page, size])).toEqual([[1, CSV_EXPORT_PAGE_SIZE], [2, CSV_EXPORT_PAGE_SIZE], [3, CSV_EXPORT_PAGE_SIZE]]);
    const overLimit = vi.fn(async () => ({ items: [], total: 10_001 }));
    await expect(loadAllCsvPages(overLimit)).rejects.toThrow(CSV_EXPORT_LIMIT_MESSAGE);
    expect(overLimit).toHaveBeenCalledOnce();
    const empty = vi.fn(async () => ({ items: [], total: 0 }));
    await expect(loadAllCsvPages(empty)).resolves.toEqual([]);
    expect(empty).toHaveBeenCalledOnce();
  });

  it('normalizes ISO dates without locale conversion', () => {
    expect(normalizeCsvDate('2026-05-04T18:30:00Z')).toBe('2026-05-04');
    expect(normalizeCsvDate(null)).toBe('');
  });
});
