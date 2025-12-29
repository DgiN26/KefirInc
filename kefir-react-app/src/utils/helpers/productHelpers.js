/**
 * Валидация данных товара
 */
export const validateProductData = (data) => {
  const errors = {};
  
  if (!data.name?.trim()) {
    errors.name = 'Название товара обязательно';
  }
  
  if (!data.price || data.price <= 0) {
    errors.price = 'Цена должна быть больше 0';
  }
  
  if (!data.category) {
    errors.category = 'Категория обязательна';
  }
  
  if (!data.unit) {
    errors.unit = 'Единица измерения обязательна';
  }
  
  if (data.stock !== undefined && data.stock < 0) {
    errors.stock = 'Количество не может быть отрицательным';
  }
  
  return errors;
};

/**
 * Подготовка данных товара для отправки
 */
export const prepareProductData = (formData) => {
  return {
    name: formData.name.trim(),
    description: formData.description?.trim() || '',
    price: parseFloat(formData.price) || 0,
    category: formData.category,
    unit: formData.unit,
    stock: parseInt(formData.stock) || 0,
    minStock: parseInt(formData.minStock) || 0,
    barcode: formData.barcode?.trim() || '',
    supplier: formData.supplier?.trim() || '',
    isActive: formData.isActive !== false
  };
};

/**
 * Форматирование цены
 */
export const formatPrice = (price) => {
  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    minimumFractionDigits: 2
  }).format(price);
};

/**
 * Фильтрация товаров
 */
export const filterProducts = (products, searchTerm, selectedCategory = '') => {
  let filtered = products;
  
  if (searchTerm.trim()) {
    const term = searchTerm.toLowerCase();
    filtered = filtered.filter(product =>
      product.name?.toLowerCase().includes(term) ||
      product.description?.toLowerCase().includes(term) ||
      product.barcode?.toLowerCase().includes(term) ||
      product.supplier?.toLowerCase().includes(term)
    );
  }
  
  if (selectedCategory) {
    filtered = filtered.filter(product => product.category === selectedCategory);
  }
  
  return filtered;
};

/**
 * Сортировка товаров
 */
export const sortProducts = (products, orderBy, order) => {
  return [...products].sort((a, b) => {
    let aValue = a[orderBy];
    let bValue = b[orderBy];
    
    // Для числовых полей
    if (orderBy === 'price' || orderBy === 'stock') {
      aValue = parseFloat(aValue) || 0;
      bValue = parseFloat(bValue) || 0;
    }
    
    // Для строк приводим к нижнему регистру
    if (typeof aValue === 'string' && typeof bValue === 'string') {
      aValue = aValue.toLowerCase();
      bValue = bValue.toLowerCase();
    }
    
    // Обрабатываем null/undefined
    if (aValue == null && bValue == null) return 0;
    if (aValue == null) return 1;
    if (bValue == null) return -1;
    
    if (aValue < bValue) return order === 'asc' ? -1 : 1;
    if (aValue > bValue) return order === 'asc' ? 1 : -1;
    return 0;
  });
};

/**
 * Получить цвет для статуса товара
 */
export const getStockStatusColor = (stock, minStock) => {
  if (stock === 0) return 'error';
  if (stock <= minStock) return 'warning';
  return 'success';
};

/**
 * Получить текст для статуса товара
 */
export const getStockStatusText = (stock, minStock) => {
  if (stock === 0) return 'Нет в наличии';
  if (stock <= minStock) return 'Мало';
  return 'В наличии';
};