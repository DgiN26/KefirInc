// utils/helpers/safeValues.js
export const safeString = (value, defaultValue = '') => {
  if (value === null || value === undefined) {
    return defaultValue;
  }
  return String(value);
};

export const safeNumber = (value, defaultValue = 0) => {
  if (value === null || value === undefined) {
    return defaultValue;
  }
  const num = Number(value);
  return !isNaN(num) ? num : defaultValue;
};

export const safeBoolean = (value, defaultValue = true) => {
  if (value === null || value === undefined) {
    return defaultValue;
  }
  return Boolean(value);
};

export const safeUnit = (value) => {
  const units = ['шт', 'кг', 'г', 'л', 'мл', 'уп', 'пак'];
  const safeValue = safeString(value, 'шт');
  return units.includes(safeValue) ? safeValue : 'шт';
};