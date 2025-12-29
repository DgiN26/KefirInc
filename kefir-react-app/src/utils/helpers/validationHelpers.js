/**
 * Валидация данных клиента
 */
export const validateClientData = (data, isCreate = false) => {
  const errors = {};
  
  if (!data.username?.trim()) {
    errors.username = 'Имя пользователя обязательно';
  }
  
  if (!data.email?.trim()) {
    errors.email = 'Email обязателен';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email)) {
    errors.email = 'Некорректный email';
  }
  
  if (isCreate && !data.password?.trim()) {
    errors.password = 'Пароль обязателен для нового клиента';
  } else if (data.password && data.password.length < 6) {
    errors.password = 'Пароль должен быть не менее 6 символов';
  }
  
  return errors;
};

/**
 * Подготовка данных клиента для отправки
 */
export const prepareClientData = (formData, isCreate = false) => {
  const data = {
    username: formData.username.trim(),
    email: formData.email.trim(),
    name: formData.firstname?.trim() || formData.username.trim(),
    firstname: formData.firstname?.trim() || formData.username.trim(),
    city: formData.city?.trim() || '',
    role: formData.role || 'CLIENT',
    status: formData.status || 'active'
  };
  
  // Пароль только если указан
  if (formData.password?.trim()) {
    data.password = formData.password.trim();
  }
  
  return data;
};