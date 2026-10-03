/** Serializa una fecha local para los parámetros API sin convertirla a UTC. */
export function formatLocalDateForApi(value: Date | null | undefined): string | null {
  if (!value || Number.isNaN(value.getTime())) return null;

  const pad = (part: number): string => String(part).padStart(2, '0');
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`;
}

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

/**
 * Normaliza números de partida legacy sin perder ceros significativos.
 *
 * Sólo elimina el sufijo decimal compuesto exclusivamente por ceros cuando la
 * parte entera es numérica («4.0» → «4», «004.0» → «004»); conserva valores
 * como «4.5», «004» o «A4» tal cual.
 */
export function formatOperationPartida(value: number | string | null | undefined): string {
  if (value === null || value === undefined) return '—';
  if (typeof value === 'number') return Number.isFinite(value) ? String(value) : '—';

  const text = value.trim();
  if (!text) return '—';

  const entero = /^(\d+)\.0+$/.exec(text);
  return entero ? entero[1] : text;
}
