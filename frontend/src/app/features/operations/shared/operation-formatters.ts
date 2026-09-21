/** Formatea LocalDateTime recibido sin aplicar conversiones de zona horaria. */
export function formatOperationDate(value: string | null | undefined): string {
  if (!value) return '—';

  const date = value.split('T')[0];
  const [year, month, day] = date.split('-');
  return year && month && day ? `${day}/${month}/${year}` : value;
}

/** Presenta cantidades sin truncar la representación recibida. */
export function formatOperationQuantity(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') return '—';
  if (typeof value === 'string') return value;

  return Number.isFinite(value)
    ? new Intl.NumberFormat('es-MX', { maximumFractionDigits: 20 }).format(value)
    : String(value);
}

/** Sustituye textos nulos o vacíos por el marcador visual de dato ausente. */
export function formatOperationText(value: string | null | undefined): string {
  return value?.trim() || '—';
}
