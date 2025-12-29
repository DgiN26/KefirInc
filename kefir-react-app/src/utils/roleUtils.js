// utils/roleUtils.js
export const ROLE_PATHS = {
  ADMIN: '/admin/dashboard',
  CLIENT: '/client/dashboard',
  COURIER: '/courier/dashboard',
  COLLECTOR: '/collector/dashboard',
  OFFICE: '/office/dashboard'
};

export const ROLE_NAMES = {
  ADMIN: 'Администратор',
  CLIENT: 'Клиент',
  COURIER: 'Курьер',
  COLLECTOR: 'Сборщик',
  OFFICE: 'Офис'
};

export const getDashboardPath = (role) => {
  const upperRole = role?.toUpperCase();
  return ROLE_PATHS[upperRole] || '/login';
};

export const getRoleName = (role) => {
  const upperRole = role?.toUpperCase();
  return ROLE_NAMES[upperRole] || 'Пользователь';
};