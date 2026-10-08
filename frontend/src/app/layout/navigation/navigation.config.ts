export interface NavigationItem {
  label: string;
  icon: string;
  route: string;
  permission?: string;
  anyOfPermissions?: string[];
}

export interface NavigationGroup {
  label: string;
  icon: string;
  items: NavigationItem[];
}

/** Metadata única de navegación; sólo contiene rutas funcionales existentes. */
export const NAVIGATION_GROUPS: NavigationGroup[] = [
  { label: 'Catálogos', icon: 'inventory_2', items: [
    { label: 'Materiales', icon: 'inventory_2', route: '/materiales', permission: 'MATERIALES_CONSULTAR' },
    { label: 'Productos', icon: 'category', route: '/productos', permission: 'PRODUCTOS_CONSULTAR' },
    { label: 'Estructuras', icon: 'account_tree', route: '/estructuras', permission: 'ESTRUCTURAS_CONSULTAR' },
    { label: 'Catálogos auxiliares', icon: 'list_alt', route: '/catalogos', permission: 'CATALOGOS_AUX_CONSULTAR' },
    { label: 'Socios comerciales', icon: 'handshake', route: '/catalogos/socios-comerciales', permission: 'CATALOGOS_AUX_CONSULTAR' },
  ]},
  { label: 'Operaciones', icon: 'swap_horiz', items: [
    { label: 'Entradas', icon: 'move_to_inbox', route: '/operaciones/entradas', permission: 'OPERACIONES_CONSULTAR' },
    { label: 'Salidas', icon: 'outbox', route: '/operaciones/salidas', permission: 'OPERACIONES_CONSULTAR' },
    { label: 'Materiales utilizados', icon: 'layers', route: '/operaciones/materiales-utilizados', permission: 'OPERACIONES_CONSULTAR' },
    { label: 'Activos fijos', icon: 'precision_manufacturing', route: '/operaciones/activos-fijos', permission: 'OPERACIONES_CONSULTAR' },
    { label: 'Carga de pedimentos', icon: 'upload_file', route: '/operaciones/pedimentos', permission: 'PEDIMENTOS_CARGAR' },
    { label: 'Actas', icon: 'assignment', route: '/operaciones/actas', permission: 'ACTAS_CARGAR' },
    { label: 'Constancias', icon: 'description', route: '/operaciones/constancias', permission: 'CONSTANCIAS_CARGAR' },
  ]},
  { label: 'Interfaces', icon: 'sync_alt', items: [
    { label: 'Importaciones de catálogos', icon: 'upload_file', route: '/catalogos/importaciones', anyOfPermissions: ['MATERIALES_CARGAR', 'PRODUCTOS_CARGAR', 'CLIENTES_CARGAR', 'PROVEEDORES_CARGAR', 'AGENTES_CARGAR'] },
    { label: 'Carga de facturación', icon: 'receipt_long', route: '/facturacion', permission: 'FACTURACION_CARGAR' },
  ]},
  { label: 'Reportes', icon: 'assessment', items: [
    { label: 'Reportes', icon: 'assessment', route: '/reportes', permission: 'REPORTES_GENERAR' },
  ]},
  { label: 'Administración', icon: 'admin_panel_settings', items: [
    { label: 'Usuarios', icon: 'group', route: '/usuarios', permission: 'USUARIOS_ADMINISTRAR' },
    { label: 'Perfiles', icon: 'admin_panel_settings', route: '/perfiles', permission: 'PERFILES_ADMINISTRAR' },
    { label: 'Bitácora', icon: 'manage_search', route: '/bitacora', permission: 'BITACORA_CONSULTAR' },
  ]},
];
