export const UNITS = {
  KG: { label: 'кг', value: 'KG', short: 'кг' },
  G: { label: 'г', value: 'G', short: 'г', factor: 1000 },
  L: { label: 'л', value: 'L', short: 'л' },
  ML: { label: 'мл', value: 'ML', short: 'мл', factor: 1000 },
  PCS: { label: 'шт', value: 'PCS', short: 'шт' },
  PACK: { label: 'упак', value: 'PACK', short: 'уп.' }
};

export const UNIT_OPTIONS = Object.values(UNITS);

export const getUnitData = (unit) => {
  return UNITS[unit] || UNITS.PCS;
};

// Конвертация единиц измерения
export const convertUnits = (value, fromUnit, toUnit) => {
  if (fromUnit === toUnit) return value;
  
  const fromData = UNITS[fromUnit];
  const toData = UNITS[toUnit];
  
  if (fromData?.factor && toData?.factor) {
    return (value * fromData.factor) / toData.factor;
  }
  
  return value; // Если нет коэффициента конвертации
};