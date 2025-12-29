export const STATUSES = {
  active: { 
    label: 'Активен', 
    color: 'success',
    value: 'active'
  },
  inactive: { 
    label: 'Неактивен', 
    color: 'default',
    value: 'inactive'
  }
};

export const STATUS_OPTIONS = Object.values(STATUSES);

export const normalizeStatus = (status) => {
  if (!status) return 'active';
  return Object.keys(STATUSES).includes(status) ? status : 'active';
};

export const getStatusData = (status) => {
  const normalizedStatus = normalizeStatus(status);
  return STATUSES[normalizedStatus] || STATUSES.active;
};