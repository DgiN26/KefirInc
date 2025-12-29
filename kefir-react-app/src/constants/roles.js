export const ROLES = {
  ADMIN: 'admin',
  CLIENT: 'client',
  COURIER: 'courier',
  COLLECTOR: 'collector'
};

export const ROLE_LABELS = {
  [ROLES.ADMIN]: 'Администратор',
  [ROLES.CLIENT]: 'Клиент',
  [ROLES.COURIER]: 'Курьер',
  [ROLES.COLLECTOR]: 'Сборщик'
};

export const ROLE_PERMISSIONS = {
  [ROLES.ADMIN]: [
    'view_dashboard',
    'manage_clients',
    'manage_products',
    'manage_orders',
    'manage_deliveries',
    'manage_couriers',
    'manage_collectors',
    'view_reports',
    'manage_warehouse'
  ],
  [ROLES.CLIENT]: [
    'view_products',
    'manage_cart',
    'place_orders',
    'view_orders',
    'update_profile'
  ],
  [ROLES.COURIER]: [
    'view_assignments',
    'update_delivery_status',
    'view_delivery_details'
  ],
  [ROLES.COLLECTOR]: [
    'view_tasks',
    'update_order_status',
    'view_order_details'
  ]
};

// Проверка разрешений
export const hasPermission = (role, permission) => {
  return ROLE_PERMISSIONS[role]?.includes(permission) || false;
};

// Проверка роли
export const isRole = (userRole, requiredRole) => {
  return userRole === requiredRole;
};

export const isAdmin = (userRole) => isRole(userRole, ROLES.ADMIN);
export const isClient = (userRole) => isRole(userRole, ROLES.CLIENT);
export const isCourier = (userRole) => isRole(userRole, ROLES.COURIER);
export const isCollector = (userRole) => isRole(userRole, ROLES.COLLECTOR);