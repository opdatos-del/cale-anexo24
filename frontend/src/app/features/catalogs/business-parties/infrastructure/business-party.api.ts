import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { AgentDto, ApiPage, BusinessPartyKind, BusinessPartyPage, BusinessPartyRow, ClientDto, ProviderDto } from '@features/catalogs/business-parties/domain/business-party.model';

/** Cliente HTTP para maestros read-only de socios comerciales. */
@Injectable({ providedIn: 'root' })
export class BusinessPartyApi {
  private readonly http = inject(HttpClient);

  search(kind: BusinessPartyKind, filter: string, page: number, pageSize: number): Observable<BusinessPartyPage> {
    const endpoint = kind === 'clients' ? 'clientes' : kind === 'providers' ? 'proveedores' : 'agentes-aduanales';
    let params = new HttpParams().set('pagina', page).set('tamano', pageSize);
    if (filter.trim()) params = params.set('filtro', filter.trim());
    return this.http.get<ApiPage<ClientDto | ProviderDto | AgentDto>>(`/api/v1/catalogos/${endpoint}`, { params }).pipe(
      map((response) => ({ ...response, items: response.items.map((item) => this.toRow(kind, item)) })),
    );
  }

  private toRow(kind: BusinessPartyKind, item: ClientDto | ProviderDto | AgentDto): BusinessPartyRow {
    if (kind === 'agents') {
      const agent = item as AgentDto;
      return { key: agent.clave, name: agent.nombre, fiscalId: agent.rfc, patent: agent.patente, agency: agent.agenciaAduanal };
    }
    const party = item as ClientDto | ProviderDto;
    return { key: party.clave, name: party.nombre, fiscalId: party.idfiscal, country: party.pais, email: party.correo };
  }
}
