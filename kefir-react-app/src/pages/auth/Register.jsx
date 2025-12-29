import React, { useState, useCallback, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import './Login.css';

// Простые иконки
const CheckIcon = () => <span style={{color: '#28a745', fontSize: '14px'}}>✓</span>;
const XIcon = () => <span style={{color: '#dc3545', fontSize: '14px'}}>✗</span>;
const Spinner = () => (
  <span className="spinner" style={{display: 'inline-block', width: '14px', height: '14px', fontSize: '14px'}}>
    ↻
  </span>
);

const API_URL = 'http://localhost:8080';

const Register = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    username: '',
    password: '',
    confirmPassword: ''
  });
  const [fieldErrors, setFieldErrors] = useState({
    email: '',
    username: '',
    password: '',
    confirmPassword: '',
    name: ''
  });
  const [fieldValid, setFieldValid] = useState({
    emailAvailable: null,
    usernameAvailable: null
  });
  const [checking, setChecking] = useState({
    email: false,
    username: false
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Используем useRef для таймеров дебаунса
  const debounceTimers = useRef({
    email: null,
    username: null
  });

  // Простая дебаунс функция
  const debounce = (func, delay, key) => {
    return (...args) => {
      if (debounceTimers.current[key]) {
        clearTimeout(debounceTimers.current[key]);
      }
      debounceTimers.current[key] = setTimeout(() => func(...args), delay);
    };
  };

  // Проверка email на уникальность - прямой запрос к эндпоинту
  const checkEmailAvailability = useCallback(async (email) => {
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setFieldValid(prev => ({ ...prev, emailAvailable: null }));
      return;
    }
    
    setChecking(prev => ({ ...prev, email: true }));
    try {
      const response = await fetch(`${API_URL}/api/clients/check-email`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const data = await response.json();
      setFieldValid(prev => ({ 
        ...prev, 
        emailAvailable: data.available || false 
      }));
     
    } catch (error) {
      console.error('Ошибка проверки email:', error);
      setFieldValid(prev => ({ ...prev, emailAvailable: null }));
      setFieldErrors(prev => ({
        ...prev,
        email: 'Ошибка проверки сервера'
      }));
    } finally {
      setChecking(prev => ({ ...prev, email: false }));
    }
  }, []);

  // Проверка username на уникальность - прямой запрос к эндпоинту
  const checkUsernameAvailability = useCallback(async (username) => {
    if (!username || username.length < 3) {
      setFieldValid(prev => ({ ...prev, usernameAvailable: null }));
      return;
    }
    
    setChecking(prev => ({ ...prev, username: true }));
    try {
      const response = await fetch(`${API_URL}/api/clients/check-username`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username })
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const data = await response.json();
      setFieldValid(prev => ({ 
        ...prev, 
        usernameAvailable: data.available || false 
      }));
    
    } catch (error) {
      console.error('Ошибка проверки username:', error);
      setFieldValid(prev => ({ ...prev, usernameAvailable: null }));
      setFieldErrors(prev => ({
        ...prev,
        username: 'Ошибка проверки сервера'
      }));
    } finally {
      setChecking(prev => ({ ...prev, username: false }));
    }
  }, []);

  // Обработчик изменения полей
  const handleChange = (e) => {
    const { name, value } = e.target;
    
    // Обновляем данные формы
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Очищаем общую ошибку
    if (error) setError('');
    
    // Валидация в реальном времени
    switch (name) {
      case 'email':
        setFieldErrors(prev => ({ ...prev, email: '' }));
        setFieldValid(prev => ({ ...prev, emailAvailable: null }));
        
        // Базовая валидация email
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (value && !emailRegex.test(value)) {
          setFieldErrors(prev => ({ ...prev, email: 'Некорректный email' }));
        } else if (value) {
          // Проверка уникальности через дебаунс
          debounce(checkEmailAvailability, 500, 'email')(value);
        }
        break;
        
      case 'username':
        setFieldErrors(prev => ({ ...prev, username: '' }));
        setFieldValid(prev => ({ ...prev, usernameAvailable: null }));
        
        // Базовая валидация username
        if (value && value.length < 3) {
          setFieldErrors(prev => ({ 
            ...prev, 
            username: 'Логин должен быть не менее 3 символов' 
          }));
        } else if (value) {
          // Проверка уникальности через дебаунс
          debounce(checkUsernameAvailability, 500, 'username')(value);
        }
        break;
        
      case 'password':
        setFieldErrors(prev => ({ ...prev, password: '' }));
        if (value && value.length < 6) {
          setFieldErrors(prev => ({ 
            ...prev, 
            password: 'Пароль должен быть не менее 6 символов' 
          }));
        }
        break;
        
      case 'confirmPassword':
        setFieldErrors(prev => ({ ...prev, confirmPassword: '' }));
        if (value && value !== formData.password) {
          setFieldErrors(prev => ({ 
            ...prev, 
            confirmPassword: 'Пароли не совпадают' 
          }));
        }
        break;
        
      case 'name':
        setFieldErrors(prev => ({ ...prev, name: '' }));
        if (!value.trim()) {
          setFieldErrors(prev => ({ 
            ...prev, 
            name: 'Имя обязательно' 
          }));
        }
        break;
        
      default:
        break;
    }
  };

  // Проверяем можно ли отправить форму
  const isFormValid = () => {
    return (
      formData.name.trim() &&
      formData.email.trim() &&
      formData.username.trim() &&
      formData.password.length >= 6 &&
      formData.password === formData.confirmPassword &&
      fieldValid.emailAvailable !== false &&
      fieldValid.usernameAvailable !== false &&
      !fieldErrors.email &&
      !fieldErrors.username &&
      !fieldErrors.password &&
      !fieldErrors.confirmPassword
    );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Финальная проверка перед отправкой
    if (!isFormValid()) {
      alert('Пожалуйста, исправьте ошибки в форме');
      return;
    }
    
    setError('');
    setLoading(true);
    
    try {
      // Прямой запрос к эндпоинту регистрации
      const response = await fetch(`${API_URL}/api/clients/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: formData.username,
          firstname: formData.name,
          email: formData.email,
          password: formData.password
        })
      });
      
      const data = await response.json();
      console.log('API Response:', data);
      
      if (!response.ok || data.success === false) {
        // Обработка ошибок от сервера
        const errorMsg = data.error || data.message || 'Ошибка регистрации';
        
        if (data.details) {
          try {
            const details = JSON.parse(data.details);
            if (details.error) {
              alert(`Ошибка: ${details.error}`);
            }
          } catch (e) {
            console.error('Ошибка парсинга details:', e);
          }
        } else {
          alert(`Ошибка: ${errorMsg}`);
        }
        
        setError(errorMsg);
        return;
      }
      
      // Успешная регистрация
      alert('Регистрация успешна! Теперь вы можете войти.');
      navigate('/login');
      
    } catch (error) {
      console.error('Ошибка регистрации:', error);
      const errorMsg = 'Ошибка сети или сервера';
      alert(`Ошибка: ${errorMsg}`);
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2>Регистрация</h2>
        
        {error && (
          <div className="error-message">
            <strong>Ошибка:</strong> {error}
          </div>
        )}
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Имя *</label>
            <input 
              type="text" 
              name="name"
              value={formData.name}
              onChange={handleChange}
              placeholder="Введите ваше имя" 
              required 
              disabled={loading}
              className={fieldErrors.name ? 'has-error' : ''}
            />
            {fieldErrors.name && (
              <div className="field-error"><XIcon /> {fieldErrors.name}</div>
            )}
          </div>
          
          <div className="form-group">
            <label>Email *</label>
            <input 
              type="email" 
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="Введите email" 
              required 
              disabled={loading}
              className={fieldErrors.email || fieldValid.emailAvailable === false ? 'has-error' : ''}
            />
            
            <div className={`field-status ${
              checking.email ? 'checking' : 
              fieldValid.emailAvailable === true ? 'valid' : 
              fieldValid.emailAvailable === false ? 'invalid' : ''
            }`}>
              {checking.email ? (
                <><Spinner /> <span>Проверка...</span></>
              ) : fieldValid.emailAvailable === true ? (
                <><CheckIcon /> <span>Email свободен</span></>
              ) : fieldValid.emailAvailable === false ? (
                <><XIcon /> <span>Email уже занят</span></>
              ) : null}
            </div>
            
            {fieldErrors.email && (
              <div className="field-error"><XIcon /> {fieldErrors.email}</div>
            )}
          </div>
          
          <div className="form-group">
            <label>Логин *</label>
            <input 
              type="text" 
              name="username"
              value={formData.username}
              onChange={handleChange}
              placeholder="Придумайте логин (мин. 3 символа)" 
              required 
              disabled={loading}
              className={fieldErrors.username || fieldValid.usernameAvailable === false ? 'has-error' : ''}
            />
            
            <div className={`field-status ${
              checking.username ? 'checking' : 
              fieldValid.usernameAvailable === true ? 'valid' : 
              fieldValid.usernameAvailable === false ? 'invalid' : ''
            }`}>
              {checking.username ? (
                <><Spinner /> <span>Проверка...</span></>
              ) : fieldValid.usernameAvailable === true ? (
                <><CheckIcon /> <span>Логин свободен</span></>
              ) : fieldValid.usernameAvailable === false ? (
                <><XIcon /> <span>Логин уже занят</span></>
              ) : null}
            </div>
            
            {fieldErrors.username && (
              <div className="field-error"><XIcon /> {fieldErrors.username}</div>
            )}
          </div>
          
          <div className="form-group">
            <label>Пароль *</label>
            <input 
              type="password" 
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="Минимум 6 символов" 
              required 
              disabled={loading}
              minLength={6}
              className={fieldErrors.password ? 'has-error' : ''}
            />
            
            {formData.password && (
              <div className="password-strength">
                <div>Сложность пароля:</div>
                <div className="strength-bar">
                  <div className={`strength-fill ${
                    formData.password.length < 6 ? 'strength-weak' :
                    formData.password.length < 8 ? 'strength-fair' :
                    formData.password.length < 10 ? 'strength-good' :
                    'strength-strong'
                  }`} />
                </div>
              </div>
            )}
            
            {fieldErrors.password && (
              <div className="field-error"><XIcon /> {fieldErrors.password}</div>
            )}
          </div>
          
          <div className="form-group">
            <label>Подтвердите пароль *</label>
            <input 
              type="password" 
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              placeholder="Повторите пароль" 
              required 
              disabled={loading}
              minLength={6}
              className={fieldErrors.confirmPassword ? 'has-error' : ''}
            />
            
            {formData.confirmPassword && formData.password === formData.confirmPassword && (
              <div className="field-status valid"><CheckIcon /> Пароли совпадают</div>
            )}
            
            {fieldErrors.confirmPassword && (
              <div className="field-error"><XIcon /> {fieldErrors.confirmPassword}</div>
            )}
          </div>
          
          <button 
            type="submit" 
            className="login-button" 
            disabled={loading || !isFormValid()}
          >
            {loading ? 'Регистрация...' : 'Зарегистрироваться'}
          </button>
        </form>
        
        <div className="register-link">
          <Link to="/login">Уже есть аккаунт? Войти</Link>
        </div>
      </div>
    </div>
  );
};

export default Register;