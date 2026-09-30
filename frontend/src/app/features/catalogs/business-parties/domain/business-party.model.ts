export type BusinessPartyKind = 'clients' | 'providers' | 'agents';

export interface BusinessPartyRow {
  key: string;
  name: string;
  fiscalId: string;
  country?: string;
  email?: string;
  patent?: string;
  agency?: string;
}

export interface BusinessPartyPage { items: BusinessPartyRow[]; total: number; pagina: number; tamano: number; }
export interface ApiPage<T> { items: T[]; total: number; pagina: number; tamano: number; }
export interface ClientDto { clientekey?: number; clave: string; nombre: string; idfiscal: string; pais?: string; correo?: string; }
export interface ProviderDto { proveedorkey?: number; clave: string; nombre: string; idfiscal: string; pais?: string; correo?: string; }
export interface AgentDto { clave: string; nombre: string; rfc: string; patente?: string; agenciaAduanal?: string; }
