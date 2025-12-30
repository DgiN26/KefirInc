import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import './Login.css';

const Login = ({ onLogin, loading }) => {
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [isDemoLogin, setIsDemoLogin] = useState(false);
  const [demoCredentials, setDemoCredentials] = useState(null);
  const [isValidating, setIsValidating] = useState(false);
  const navigate = useNavigate();

  // Функция для автоматической отправки формы
  const handleAutoSubmit = useCallback(async () => {
    if (!formData.username.trim() || !formData.password.trim()) {
      setError('Пожалуйста, заполните все поля');
      return;
    }
    
    try {
      await onLogin(formData);
    } catch (error) {
      if (error.isBanned) {
        setError('Ваш аккаунт заблокирован. Доступ запрещен.');
      } else {
        setError(error.message || 'Ошибка при входе');
      }
    }
  }, [formData, onLogin]);

  // Эффект для автоматической отправки формы после обновления данных
  useEffect(() => {
    if (isDemoLogin && formData.username && formData.password) {
      handleAutoSubmit();
      setIsDemoLogin(false);
    }
  }, [formData, isDemoLogin, handleAutoSubmit]);

  // Эффект для обработки демо-входа с предустановленными данными
  useEffect(() => {
    if (demoCredentials) {
      const { username, password } = demoCredentials;
      setFormData({ username, password });
      setError('');
      setIsDemoLogin(true);
      setDemoCredentials(null);
    }
  }, [demoCredentials]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    
    // Обновляем форму
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Очищаем ошибку только если пользователь начал вводить данные
    // и это не связано с проверкой валидации
    if (!isValidating) {
      setError('');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsValidating(true);
    
    // Сразу показываем ошибку валидации
    if (!formData.username.trim() || !formData.password.trim()) {
      setError('Пожалуйста, заполните все поля');
      // Не сбрасываем isValidating сразу, даем пользователю увидеть сообщение
      setTimeout(() => setIsValidating(false), 100);
      return;
    }
    
    setError('');
    
    try {
      await onLogin(formData);
      setIsValidating(false);
    } catch (error) {
      setIsValidating(false);
      if (error.isBanned) {
        setError('Ваш аккаунт заблокирован. Доступ запрещен.');
      } else {
        setError(error.message || 'Ошибка при входе');
      }
    }
  };

  // Функция для демо-входа
  const handleDemoLogin = (username, password) => {
    setDemoCredentials({ username, password });
    setIsValidating(false);
  };

  const handleRegisterClick = () => {
    navigate('/register');
  };

  // Обработчик клика по полям формы
  const handleFieldClick = () => {
    setIsValidating(false);
    if (error === 'Пожалуйста, заполните все поля') {
      setError('');
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <div style={{ textAlign: 'center', marginBottom: '20px' }}>
          <h2>🥛 KEFIR Logistics</h2>
          <p style={{ color: '#666', fontSize: '14px' }}>Система управления доставками</p>
        </div>
        
        <h3 style={{ marginBottom: '20px', textAlign: 'center' }}>Вход в систему</h3>
        
        {error && (
          <div className={`error-message ${error.includes('заблокирован') ? 'status-banned' : ''}`}>
            {error}
          </div>
        )}
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">Имя пользователя</label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              onClick={handleFieldClick}
              placeholder="Введите имя пользователя"
              required
              disabled={loading}
            />
          </div>
          
          <div className="form-group">
            <label htmlFor="password">Пароль</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              onClick={handleFieldClick}
              placeholder="Введите пароль"
              required
              disabled={loading}
            />
          </div>
          
          <button 
            type="submit" 
            className="login-button"
            disabled={loading}
            style={{ marginBottom: '10px' }}
          >
            {loading ? (
              <>
                <span className="spinner"></span> Вход...
              </>
            ) : 'Войти'}
          </button>
          
          <button
            type="button"
            onClick={handleRegisterClick}
            className="register-button"
            disabled={loading}
            style={{
              width: '100%',
              padding: '12px',
              background: 'transparent',
              color: '#1976d2',
              border: '1px solid #1976d2',
              borderRadius: '4px',
              fontSize: '16px',
              cursor: loading ? 'not-allowed' : 'pointer',
              marginTop: '10px',
              opacity: loading ? 0.6 : 1
            }}
          >
            Регистрация
          </button>
        </form>

        {/* Демо-кнопки для тестирования */}
        <div style={{ marginTop: '25px', textAlign: 'center' }}>
          <p style={{ color: '#666', fontSize: '14px', marginBottom: '10px' }}>Тестовые пользователи:</p>
          <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap' }}>
            <button
              type="button"
              onClick={() => handleDemoLogin('client', 'client')}
              disabled={loading}
              className="demo-button"
              style={{
                padding: '8px 12px',
                background: '#e3f2fd',
                border: '1px solid #1976d2',
                borderRadius: '4px',
                fontSize: '12px',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.6 : 1
              }}
            >
              Клиент
            </button>
            <button
              type="button"
              onClick={() => handleDemoLogin('client2', 'client2')}
              disabled={loading}
              className="demo-button"
              style={{
                padding: '8px 12px',
                background: '#60ade4ff',
                border: '1px solid #1976d2',
                borderRadius: '4px',
                fontSize: '12px',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.6 : 1
              }}
            >
              Клиент
            </button>
            <button
              type="button"
              onClick={() => handleDemoLogin('admin', 'admin')}
              disabled={loading}
              className="demo-button"
              style={{
                padding: '8px 12px',
                background: '#e8f5e9',
                border: '1px solid #388e3c',
                borderRadius: '4px',
                fontSize: '12px',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.6 : 1
              }}
            >
              Админ
            </button>
            <button
              type="button"
              onClick={() => handleDemoLogin('courier', 'courier')}
              disabled={loading}
              className="demo-button"
              style={{
                padding: '8px 12px',
                background: '#fff3e0',
                border: '1px solid #ef6c00',
                borderRadius: '4px',
                fontSize: '12px',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.6 : 1
              }}
            >
              Курьер
            </button>
            <button
              type="button"
              onClick={() => handleDemoLogin('collector', 'collector')}
              disabled={loading}
              className="demo-button"
              style={{
                padding: '8px 12px',
                background: '#f3e5f5',
                border: '1px solid #7b1fa2',
                borderRadius: '4px',
                fontSize: '12px',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.6 : 1
              }}
            >
              Сборщик
            </button>
            <button
              type="button"
              onClick={() => handleDemoLogin('banned', 'banned')}
              disabled={loading}
              className="demo-button"
              style={{
                padding: '8px 12px',
                background: '#ffebee',
                border: '1px solid #d32f2f',
                borderRadius: '4px',
                fontSize: '12px',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.6 : 1
              }}
            >
              Заблокированный
            </button>
            <button
              type="button"
              onClick={() => handleDemoLogin('office', 'office')}
              disabled={loading}
              className="demo-button"
              style={{
                padding: '8px 12px',
                background: '#e8f5e9',
                border: '1px solid #045e0b',
                borderRadius: '4px',
                fontSize: '12px',
                cursor: loading ? 'not-allowed' : 'pointer',
                opacity: loading ? 0.6 : 1
              }}
            >
              Офис
            </button>
          </div>
        </div>

        <div style={{ marginTop: '20px', textAlign: 'center', fontSize: '12px', color: '#999' }}>
          <p>Нет тестового аккаунта? Нажмите на кнопку выше для автозаполнения</p>
        </div>
      </div>
    </div>
  );
};

export default Login;