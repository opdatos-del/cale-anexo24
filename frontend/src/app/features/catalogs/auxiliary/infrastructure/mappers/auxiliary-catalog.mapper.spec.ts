import { describe, expect, it } from 'vitest';
import { AuxiliaryCatalogMapper } from './auxiliary-catalog.mapper';

describe('AuxiliaryCatalogMapper', () => {
  it('mapea unidades y conserva paginación', () => {
    expect(AuxiliaryCatalogMapper.units({ items: [{ clave: 'KG', nombre: 'KILOGRAMO', alias: 'KG' }], total: 1, pagina: 2, tamano: 20 })).toEqual({
      items: [{ code: 'KG', name: 'KILOGRAMO', alias: 'KG' }], total: 1, page: 2, pageSize: 20,
    });
  });

  it('mapea tipos de material', () => {
    expect(AuxiliaryCatalogMapper.materialTypes({ items: [{ nombre: 'INSUMOS' }], total: 1, pagina: 1, tamano: 20 }).items)
      .toEqual([{ name: 'INSUMOS' }]);
  });

  it('mapea almacenes', () => {
    expect(AuxiliaryCatalogMapper.warehouses({ items: [{ almacenKey: 2, clave: 'ALM02', descripcion: 'Materia prima' }], total: 1, pagina: 1, tamano: 20 }).items)
      .toEqual([{ id: 2, code: 'ALM02', description: 'Materia prima' }]);
  });

  it('mapea categorías con vigencia', () => {
    expect(AuxiliaryCatalogMapper.categories({ items: [{ clave: 'A', descripcion: 'Maquila', diasValidos: 365, meses: 12 }], total: 1, pagina: 1, tamano: 20 }).items)
      .toEqual([{ code: 'A', description: 'Maquila', validDays: 365, months: 12 }]);
  });
});
