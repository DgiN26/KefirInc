import { 
  Person, 
  AdminPanelSettings, 
  LocalShipping, 
  Inventory 
} from '@mui/icons-material';

export const ROLES = {
  CLIENT: { 
    label: 'Клиент', 
    color: 'primary', 
    icon: <Person fontSize="small" />,
    value: 'CLIENT'
  },
  ADMIN: { 
    label: 'Админ', 
    color: 'error', 
    icon: <AdminPanelSettings fontSize="small" />,
    value: 'ADMIN'
  },
  COURIER: { 
    label: 'Курьер', 
    color: 'warning', 
    icon: <LocalShipping fontSize="small" />,
    value: 'COURIER'
  },
  COLLECTOR: { 
    label: 'Сборщик', 
    color: 'info', 
    icon: <Inventory fontSize="small" />,
    value: 'COLLECTOR'
  }
};

export const ROLE_OPTIONS = Object.values(ROLES);

// Функция для нормализации роли
export const normalizeRole = (role) => {
  if (!role) return 'CLIENT';
  const upperRole = role.toUpperCase();
  return Object.keys(ROLES).includes(upperRole) ? upperRole : 'CLIENT';
};

// Функция для получения данных роли
export const getRoleData = (role) => {
  const normalizedRole = normalizeRole(role);
  return ROLES[normalizedRole] || ROLES.CLIENT;
};

// Получить опции для Select
export const getRoleOptions = () => 
  ROLE_OPTIONS.map(role => ({
    value: role.value,
    label: role.label,
    icon: role.icon
  }));