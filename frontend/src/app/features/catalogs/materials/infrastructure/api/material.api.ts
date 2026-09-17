import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MaterialDto {
  materialkey: number;
  clave: string;
  descripcion: string;
  fraccion: string;
  unidad: string;
  unidadt: string;
  tipomaterial: string;
}

export interface Pagina<T> {
  items: T[];
  total: number;
  pagina: number;
  tamano: number;
}

/**
 * Consulta del catálogo de materiales (RF-010).
 */
@Injectable({ providedIn: 'root' })
export class MaterialService {
  private readonly http = inject(HttpClient);

  listar(filtro: string | null, pagina: number, tamano: number): Observable<Pagina<MaterialDto>> {
    const params: Record<string, string> = { pagina: String(pagina), tamano: String(tamano) };
    if (filtro && filtro.trim()) {
      params['filtro'] = filtro.trim();
    }
    return this.http.get<Pagina<MaterialDto>>('/api/v1/catalogos/materiales', { params });
  }
}