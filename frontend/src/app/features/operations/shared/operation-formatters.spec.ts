import { describe, expect, it } from 'vitest';
import { formatOperationPartida } from './operation-formatters';

describe('formatOperationPartida', () => {
  it('elimina el sufijo decimal de ceros conservando la parte entera', () => {
    expect(formatOperationPartida('4.0')).toBe('4');
    expect(formatOperationPartida('4.00')).toBe('4');
  });

  it('conserva ceros significativos de la parte entera', () => {
    expect(formatOperationPartida('004.0')).toBe('004');
    expect(formatOperationPartida('004')).toBe('004');
  });

  it('conserva decimales no nulos y valores no numéricos', () => {
    expect(formatOperationPartida('4.5')).toBe('4.5');
    expect(formatOperationPartida('A4')).toBe('A4');
  });

  it('normaliza números nativos', () => {
    expect(formatOperationPartida(4)).toBe('4');
    expect(formatOperationPartida(4.5)).toBe('4.5');
  });

  it('recorta espacios y presenta el marcador de ausencia en vacíos', () => {
    expect(formatOperationPartida(' 4.0 ')).toBe('4');
    expect(formatOperationPartida(null)).toBe('—');
    expect(formatOperationPartida(undefined)).toBe('—');
    expect(formatOperationPartida('')).toBe('—');
    expect(formatOperationPartida('   ')).toBe('—');
  });
});
