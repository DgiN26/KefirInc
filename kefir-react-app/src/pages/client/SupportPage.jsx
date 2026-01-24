// src/pages/client/SupportPage.jsx
import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import './SupportPage.css';

const SupportPage = () => {
  const [step, setStep] = useState(1); // 1 - выбор проблемы, 2 - найденные товары, 3 - варианты действий
  const [selectedProblem, setSelectedProblem] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  
  // Данные о недопоставленных товарах
  const [unavailableItems, setUnavailableItems] = useState([]);
  const [selectedItems, setSelectedItems] = useState([]);
  const [totalRefundAmount, setTotalRefundAmount] = useState(0);
  
  const [actionType, setActionType] = useState(''); // 'refund' или 'recollect'
  const [isProcessing, setIsProcessing] = useState(false);

  // Получение clientId из localStorage
  const getClientId = useCallback(() => {
    try {
      const userJson = localStorage.getItem('user');
      if (userJson) {
        const user = JSON.parse(userJson);
        return user.id;
      }
    } catch (e) {
      console.error('Ошибка получения clientId:', e);
    }
    return null;
  }, []);

  // Проверка недопоставленных товаров
  const checkUnavailableItems = useCallback(async () => {
    const clientId = getClientId();
    if (!clientId) {
      setError('Пользователь не авторизован');
      return;
    }

    setLoading(true);
    setError('');
    
    try {
      const response = await axios.get(
        `http://localhost:8080/api/support/unavailable-items/${clientId}`
      );
      
      if (response.data.success) {
        const items = response.data.items || [];
        setUnavailableItems(items);
        
        // Автоматически выбираем все товары
        const allItemIds = items.map(item => item.id);
        setSelectedItems(allItemIds);
        
        // Рассчитываем общую сумму
        const total = items.reduce((sum, item) => {
          return sum + (item.price * item.quantity);
        }, 0);
        setTotalRefundAmount(total);
        
        if (items.length > 0) {
          setStep(2); // Переходим к шагу 2 (показ товаров)
        } else {
          setError('Не найдено недопоставленных товаров в ваших заказах');
        }
      } else {
        setError(response.data.error || 'Ошибка при проверке товаров');
      }
    } catch (err) {
      console.error('Ошибка при проверке товаров:', err);
      setError(err.response?.data?.error || err.message || 'Ошибка сети');
    } finally {
      setLoading(false);
    }
  }, [getClientId]);

  // Обработка выбора проблемы
  const handleProblemSelect = (problemType) => {
    setSelectedProblem(problemType);
    
    if (problemType === 'missing_part') {
      // Если выбрана "Не привезли часть заказа", проверяем товары
      checkUnavailableItems();
    }
  };

  // Переключение выбора товара
  const toggleItemSelection = (itemId) => {
    setSelectedItems(prev => {
      if (prev.includes(itemId)) {
        return prev.filter(id => id !== itemId);
      } else {
        return [...prev, itemId];
      }
    });
  };

  // Выбор действия
  const handleActionSelect = async (action) => {
    setActionType(action);
    setIsProcessing(true);
    setError('');
    setSuccess('');
    
    const clientId = getClientId();
    if (!clientId) {
      setError('Пользователь не авторизован');
      setIsProcessing(false);
      return;
    }
    
    try {
      let response;
      const selectedItemsData = unavailableItems.filter(item => 
        selectedItems.includes(item.id)
      );
      
      if (action === 'refund') {
        // Вернуть деньги
        response = await axios.post('http://localhost:8080/api/support/refund-items', {
          clientId,
          items: selectedItemsData,
          totalAmount: totalRefundAmount
        });
        
        if (response.data.success) {
          setSuccess(`Деньги в размере ${totalRefundAmount.toFixed(2)}₽ успешно возвращены!`);
          setStep(4); // Шаг завершения
        }
      } else if (action === 'recollect') {
        // Пересобрать заказ
        response = await axios.post('http://localhost:8080/api/support/recollect-order', {
          clientId,
          cartIds: [...new Set(selectedItemsData.map(item => item.cart_id))],
          items: selectedItemsData
        });
        
        if (response.data.success) {
          setSuccess('Заказ поставлен в очередь на повторную сборку!');
          setStep(4); // Шаг завершения
        }
      }
    } catch (err) {
      console.error('Ошибка при выполнении действия:', err);
      setError(err.response?.data?.error || err.message || 'Ошибка выполнения');
    } finally {
      setIsProcessing(false);
    }
  };

  // Сброс формы
  const resetForm = () => {
    setStep(1);
    setSelectedProblem('');
    setUnavailableItems([]);
    setSelectedItems([]);
    setTotalRefundAmount(0);
    setActionType('');
    setError('');
    setSuccess('');
  };

  return (
    <div className="support-container">
      <div className="support-header">
        <h1>📞 Поддержка</h1>
        <p>Помощь по заказам и возвратам</p>
      </div>

      <div className="support-stepper">
        <div className={`step ${step >= 1 ? 'active' : ''}`}>
          <div className="step-number">1</div>
          <div className="step-label">Выбор проблемы</div>
        </div>
        <div className={`step ${step >= 2 ? 'active' : ''}`}>
          <div className="step-number">2</div>
          <div className="step-label">Найденные товары</div>
        </div>
        <div className={`step ${step >= 3 ? 'active' : ''}`}>
          <div className="step-number">3</div>
          <div className="step-label">Варианты решения</div>
        </div>
        <div className={`step ${step >= 4 ? 'active' : ''}`}>
          <div className="step-number">4</div>
          <div className="step-label">Завершение</div>
        </div>
      </div>

      {loading && (
        <div className="loading-overlay">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Загрузка...</span>
          </div>
          <p>Проверяем ваши заказы...</p>
        </div>
      )}

      {error && (
        <div className="alert alert-danger alert-dismissible fade show">
          <strong>Ошибка:</strong> {error}
          <button 
            type="button" 
            className="btn-close" 
            onClick={() => setError('')}
          ></button>
        </div>
      )}

      {success && (
        <div className="alert alert-success alert-dismissible fade show">
          <strong>Успешно:</strong> {success}
          <button 
            type="button" 
            className="btn-close" 
            onClick={() => setSuccess('')}
          ></button>
        </div>
      )}

      <div className="support-content">
        {/* Шаг 1: Выбор проблемы */}
        {step === 1 && (
          <div className="problem-selection">
            <h3>Выберите тип проблемы:</h3>
            <div className="problem-options">
              <button
                className={`problem-option ${selectedProblem === 'missing_part' ? 'selected' : ''}`}
                onClick={() => handleProblemSelect('missing_part')}
              >
                <div className="problem-icon">📦</div>
                <div className="problem-text">
                  <h5>Не привезли часть заказа</h5>
                  <p>Если вам доставили не все товары из заказа</p>
                </div>
              </button>
              
              <button
                className="problem-option"
                onClick={() => {
                  setError('Данный функционал находится в разработке');
                }}
              >
                <div className="problem-icon">⚠️</div>
                <div className="problem-text">
                  <h5>Поврежденный товар</h5>
                  <p>Товар пришел с дефектами или повреждениями</p>
                </div>
              </button>
              
              <button
                className="problem-option"
                onClick={() => {
                  setError('Данный функционал находится в разработке');
                }}
              >
                <div className="problem-icon">❌</div>
                <div className="problem-text">
                  <h5>Отменить заказ</h5>
                  <p>Полный возврат неполученного заказа</p>
                </div>
              </button>
            </div>
          </div>
        )}

        {/* Шаг 2: Найденные недопоставленные товары */}
        {step === 2 && (
          <div className="items-list">
            <h3>Найдены недопоставленные товары:</h3>
            <p className="text-muted mb-3">Отметьте товары, по которым хотите оформить претензию:</p>
            
            <div className="items-container">
              {unavailableItems.map(item => (
                <div 
                  key={item.id} 
                  className={`item-card ${selectedItems.includes(item.id) ? 'selected' : ''}`}
                  onClick={() => toggleItemSelection(item.id)}
                >
                  <div className="form-check">
                    <input
                      type="checkbox"
                      className="form-check-input"
                      checked={selectedItems.includes(item.id)}
                      onChange={() => {}}
                    />
                  </div>
                  
                  <div className="item-info">
                    <h6>{item.product_name}</h6>
                    <div className="item-details">
                      <span>Количество: {item.quantity} шт.</span>
                      <span>Цена: {item.price}₽</span>
                      <span>Сумма: {(item.price * item.quantity).toFixed(2)}₽</span>
                    </div>
                    <small className="text-muted">
                      Заказ #{item.cart_id} • {new Date(item.created_date).toLocaleDateString()}
                    </small>
                  </div>
                  
                  <div className="item-status">
                    <span className="badge bg-warning">Не доставлен</span>
                  </div>
                </div>
              ))}
            </div>
            
            <div className="total-refund">
              <h5>Общая сумма к возврату:</h5>
              <h3 className="text-primary">{totalRefundAmount.toFixed(2)}₽</h3>
            </div>
            
            <div className="navigation-buttons">
              <button 
                className="btn btn-outline-secondary"
                onClick={() => setStep(1)}
              >
                ← Назад
              </button>
              <button 
                className="btn btn-primary"
                onClick={() => setStep(3)}
                disabled={selectedItems.length === 0}
              >
                Далее →
              </button>
            </div>
          </div>
        )}

        {/* Шаг 3: Выбор действия */}
        {step === 3 && (
          <div className="action-selection">
            <h3>Выберите вариант решения:</h3>
            <p className="text-muted mb-4">
              Для {selectedItems.length} товаров на сумму {totalRefundAmount.toFixed(2)}₽
            </p>
            
            <div className="action-options">
              <div 
                className={`action-option ${actionType === 'refund' ? 'selected' : ''}`}
                onClick={() => !isProcessing && setActionType('refund')}
              >
                <div className="action-icon">💰</div>
                <div className="action-text">
                  <h4>Вернуть деньги</h4>
                  <p>Получить возврат за недопоставленные товары</p>
                  <ul>
                    <li>Деньги вернутся на карту в течение 3-5 дней</li>
                    <li>Сумма: {totalRefundAmount.toFixed(2)}₽</li>
                    <li>Оформление электронного чека</li>
                  </ul>
                </div>
                {actionType === 'refund' && (
                  <button 
                    className="btn btn-success"
                    onClick={() => handleActionSelect('refund')}
                    disabled={isProcessing}
                  >
                    {isProcessing ? 'Обработка...' : 'Вернуть деньги'}
                  </button>
                )}
              </div>
              
              <div 
                className={`action-option ${actionType === 'recollect' ? 'selected' : ''}`}
                onClick={() => !isProcessing && setActionType('recollect')}
              >
                <div className="action-icon">🚚</div>
                <div className="action-text">
                  <h4>Привезти заказ</h4>
                  <p>Заказать повторную сборку и доставку недостающих товаров</p>
                  <ul>
                    <li>Заказ будет собран повторно</li>
                    <li>Бесплатная доставка</li>
                    <li>Срок: 1-2 рабочих дня</li>
                  </ul>
                </div>
                {actionType === 'recollect' && (
                  <button 
                    className="btn btn-primary"
                    onClick={() => handleActionSelect('recollect')}
                    disabled={isProcessing}
                  >
                    {isProcessing ? 'Обработка...' : 'Заказать доставку'}
                  </button>
                )}
              </div>
            </div>
            
            <div className="navigation-buttons">
              <button 
                className="btn btn-outline-secondary"
                onClick={() => setStep(2)}
                disabled={isProcessing}
              >
                ← Назад
              </button>
            </div>
          </div>
        )}

        {/* Шаг 4: Завершение */}
        {step === 4 && (
          <div className="completion-step">
            <div className="success-icon">✅</div>
            <h3>Заявка успешно оформлена!</h3>
            <p>{success}</p>
            
            <div className="completion-details">
              {actionType === 'refund' && (
                <>
                  <p><strong>Номер заявки:</strong> REF-{Date.now().toString().slice(-8)}</p>
                  <p><strong>Сумма возврата:</strong> {totalRefundAmount.toFixed(2)}₽</p>
                  <p><strong>Срок возврата:</strong> 3-5 рабочих дней</p>
                </>
              )}
              
              {actionType === 'recollect' && (
                <>
                  <p><strong>Номер заявки:</strong> RECOL-{Date.now().toString().slice(-8)}</p>
                  <p><strong>Статус заказа:</strong> Ожидает повторной сборки</p>
                  <p><strong>Ожидаемая доставка:</strong> 1-2 рабочих дня</p>
                </>
              )}
            </div>
            
            <div className="action-buttons">
              <button 
                className="btn btn-primary"
                onClick={() => window.location.href = '/client'}
              >
                Вернуться в кабинет
              </button>
              <button 
                className="btn btn-outline-primary"
                onClick={resetForm}
              >
                Создать новую заявку
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default SupportPage;