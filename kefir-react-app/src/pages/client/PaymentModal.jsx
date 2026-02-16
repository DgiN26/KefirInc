// *** НАЧАЛО ФАЙЛА PaymentModal.jsx ***

import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import './PaymentModal.css';

const PaymentModal = ({ show, onClose, orderDetails, onConfirm, onClearCart, authToken }) => {
  const [paymentMethod, setPaymentMethod] = useState('card');
  const [paymentProcessing, setPaymentProcessing] = useState(false);
  const [paymentSuccess, setPaymentSuccess] = useState(false);
  const [paymentError, setPaymentError] = useState(null);
  const [accountInfo, setAccountInfo] = useState(null);
  const [loadingAccount, setLoadingAccount] = useState(false);
  
  // Состояния для создания счета
  const [showCreateAccountForm, setShowCreateAccountForm] = useState(false);
  const [creatingAccount, setCreatingAccount] = useState(false);
  const [accountCreated, setAccountCreated] = useState(false);
  const [createAccountError, setCreateAccountError] = useState(null);
  
  // Поле для ввода номера карты (16 цифр)
  const [cardNumber, setCardNumber] = useState('');
  const [cardNumberError, setCardNumberError] = useState('');
  const [cardNumberFormatted, setCardNumberFormatted] = useState('');

  const API_BASE_URL = 'http://localhost:8080/api';

  // Функция для получения userId из localStorage
  const getUserIdFromStorage = useCallback(() => {
    try {
      const userJson = localStorage.getItem('user');
      if (userJson) {
        const user = JSON.parse(userJson);
        return user.id;
      }
    } catch (e) {
      console.error('❌ Ошибка получения userId:', e);
    }
    
    if (orderDetails?.userId) {
      return orderDetails.userId;
    }
    
    return null;
  }, [orderDetails]);

  // Функция для получения строкового orderId (с префиксом ORD-)
  const getNormalizedOrderId = useCallback(() => {
    if (!orderDetails?.orderId) return null;
    
    let orderId = orderDetails.orderId;
    
    // Если это число, добавляем префикс ORD-
    if (typeof orderId === 'number') {
      return `ORD-${orderId}`;
    }
    
    // Если это строка без префикса ORD-, добавляем его
    if (typeof orderId === 'string' && !orderId.startsWith('ORD-') && /^\d+$/.test(orderId)) {
      return `ORD-${orderId}`;
    }
    
    // Если это уже строка с ORD-
    return orderId;
  }, [orderDetails]);

  const getAuthHeaders = useCallback(() => {
    if (!authToken) return {};
    
    let cleanToken = authToken.replace(/^Bearer\s+/i, '');
    if (!cleanToken.startsWith('auth-') && !cleanToken.includes('-')) {
      cleanToken = `auth-${cleanToken}`;
    }

    return {
      headers: {
        'Authorization': `Bearer ${cleanToken}`,
        'Content-Type': 'application/json'
      }
    };
  }, [authToken]);

  // Форматирование номера карты
  const formatCardNumber = (value) => {
    const digits = value.replace(/\D/g, '').slice(0, 16);
    const parts = [];
    for (let i = 0; i < digits.length; i += 4) {
      parts.push(digits.substring(i, i + 4));
    }
    return parts.join(' ');
  };

  const handleCardNumberChange = (e) => {
    const input = e.target.value;
    const digits = input.replace(/\D/g, '').slice(0, 16);
    setCardNumber(digits);
    setCardNumberFormatted(formatCardNumber(digits));
    setCardNumberError(digits.length !== 16 ? 'Номер карты должен содержать 16 цифр' : '');
  };

  // Проверка существования счета
  useEffect(() => {
    const checkAccount = async () => {
      if (!show) return;
      
      const userId = getUserIdFromStorage();
      
      if (!userId) {
        setShowCreateAccountForm(true);
        setLoadingAccount(false);
        return;
      }
      
      setLoadingAccount(true);
      setShowCreateAccountForm(false);
      
      try {
        const response = await axios.get(
          `${API_BASE_URL}/payments/account-exists/${userId}`,
          getAuthHeaders()
        );
        
        if (response.data && response.data.account_exists) {
          const balanceResponse = await axios.get(
            `${API_BASE_URL}/payments/my-balance`,
            getAuthHeaders()
          );
          
          setAccountInfo({
            userId: userId,
            balance: balanceResponse.data?.balance || 0,
            accountNumber: `PA-${userId.toString().padStart(8, '0')}`
          });
        } else {
          setShowCreateAccountForm(true);
        }
      } catch (err) {
        console.error('❌ Ошибка проверки счета', err);
        setShowCreateAccountForm(true);
      } finally {
        setLoadingAccount(false);
      }
    };

    checkAccount();
  }, [show, getUserIdFromStorage, getAuthHeaders, API_BASE_URL]);

  // Создание счета
 const handleCreateAccount = async () => {
  if (cardNumber.length !== 16) {
    setCardNumberError('Номер карты должен содержать 16 цифр');
    return;
  }

  const userId = getUserIdFromStorage();
  
  if (!userId) {
    setCreateAccountError('Не удалось определить пользователя');
    return;
  }

  setCreatingAccount(true);
  setCreateAccountError(null);
  
  try {
    // ВАЖНО: очищаем номер карты от пробелов перед отправкой
    const cleanCardNumber = cardNumber.replace(/\s/g, '');
    
    console.log('📤 Отправляем данные:', {
      user_id: userId,
      role: 'client',
      card_number: cleanCardNumber
    });

    const response = await axios.post(
      `${API_BASE_URL}/payments/create-account`,
      {
        user_id: userId,
        role: 'client',
        card_number: cleanCardNumber // ← ЭТО КЛЮЧЕВОЕ ПОЛЕ!
      },
      getAuthHeaders()
    );

    console.log('✅ Ответ от сервера:', response.data);

    if (response.data && response.data.status === 'success') {
      setAccountCreated(true);
      
      const balanceResponse = await axios.get(
        `${API_BASE_URL}/payments/my-balance`,
        getAuthHeaders()
      );
      
      setAccountInfo({
        userId: userId,
        balance: balanceResponse.data?.balance || 0,
        accountNumber: `PA-${userId.toString().padStart(8, '0')}`
      });
      
      setShowCreateAccountForm(false);
      setTimeout(() => setAccountCreated(false), 3000);
    }
  } catch (err) {
    console.error('❌ Ошибка создания счета', err);
    setCreateAccountError(err.response?.data?.message || 'Ошибка при создании счета');
  } finally {
    setCreatingAccount(false);
  }
};

// Оплата заказа
const handlePayment = async (e) => {
  // Предотвращаем стандартное поведение браузера
  e.preventDefault();
  
  // Если уже оплачиваем - выходим
  if (paymentProcessing) {
    console.log('⛔ Платеж уже выполняется');
    return false;
  }
  
  console.log('✅ Начинаем платеж, блокируем кнопку');
  setPaymentProcessing(true);
  setPaymentError(null);
  
  const userId = getUserIdFromStorage();
  const orderNumber = getNormalizedOrderId();
  
  console.log('💰 PaymentModal: данные для оплаты', {
    userId,
    orderNumber,
    amount: orderDetails?.totalAmount
  });
  
  if (!userId || !orderNumber) {
    setPaymentError('Ошибка идентификации заказа или пользователя');
    setPaymentProcessing(false);
    return false;
  }
  
  try {
    // 1. СПИСЫВАЕМ ДЕНЬГИ
    const withdrawResponse = await axios.post(
      `${API_BASE_URL}/payments/withdraw`,
      {
        user_id: userId,
        amount: orderDetails.totalAmount,
        order_id: orderNumber,
        description: `Оплата заказа #${orderNumber}`
      },
      getAuthHeaders()
    );

    console.log('✅ Ответ от withdraw:', withdrawResponse.data);

    if (withdrawResponse.data && withdrawResponse.data.status === 'success') {
      
      // 2. ПОДТВЕРЖДАЕМ ОПЛАТУ
      try {
        const confirmResponse = await axios.post(
          `${API_BASE_URL}/orders/${orderNumber}/confirm-payment`,
          {
            amount: orderDetails.totalAmount
          },
          getAuthHeaders()
        );
        console.log('✅ Товары списаны со склада:', confirmResponse.data);
      } catch (confirmErr) {
        console.error('❌ Ошибка списания товаров:', confirmErr);
      }
      
      setPaymentSuccess(true);
      
      setAccountInfo(prev => ({
        ...prev,
        balance: withdrawResponse.data.new_balance
      }));
      
      if (onConfirm) {
        onConfirm(withdrawResponse.data);
      }
      
      if (typeof onClearCart === 'function') {
        onClearCart();
      }
      
      // СОХРАНЯЕМ ИНФОРМАЦИЮ ОБ ОПЛАЧЕННОМ ЗАКАЗЕ
      const paidOrderId = orderNumber;
      
      // Закрываем модалку через 2 секунды
      setTimeout(() => {
        setPaymentProcessing(false);
        setPaymentSuccess(false);
        onClose();
        
        // ОБНОВЛЯЕМ ТОВАРЫ ПОСЛЕ ЗАКРЫТИЯ МОДАЛКИ
        if (window.location.pathname.includes('client-portal')) {
          // Если мы на странице магазина, обновляем товары
          window.dispatchEvent(new CustomEvent('payment-completed', { 
            detail: { orderId: paidOrderId } 
          }));
        } else if (window.location.pathname.includes('client-cart')) {
          // Если мы на странице корзины, обновляем заказы
          window.dispatchEvent(new CustomEvent('payment-completed', { 
            detail: { orderId: paidOrderId } 
          }));
        }
      }, 2000);
    } else {
      setPaymentProcessing(false);
    }
  } catch (err) {
    console.error('❌ Ошибка оплаты:', err);
    setPaymentError(err.response?.data?.message || 'Ошибка при оплате');
    setPaymentProcessing(false);
  }
  
  return false;
};

// Добавьте этот эффект для сброса состояния при открытии модалки
useEffect(() => {
  if (show) {
    console.log('🔄 Модальное окно открыто, сбрасываем состояние');
    setPaymentProcessing(false);
    setPaymentSuccess(false);
    setPaymentError(null);
  }
}, [show]);

  if (!show) return null;

  return (
    <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content">
          
          {/* Заголовок */}
          <div className="modal-header" style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', color: 'white' }}>
            <h5 className="modal-title">
              <i className="bi bi-credit-card me-2"></i>
              KEFIR Pay
            </h5>
            <button type="button" className="btn-close btn-close-white" onClick={onClose}></button>
          </div>
          
          {/* Тело модального окна */}
          <div className="modal-body">
            
            {/* Загрузка */}
            {loadingAccount && (
              <div className="text-center py-4">
                <div className="spinner-border text-primary"></div>
                <p className="mt-2">Загрузка...</p>
              </div>
            )}
            
            {/* Форма создания счета */}
            {showCreateAccountForm && !loadingAccount && (
              <>
                <div className="alert alert-warning">
                  <i className="bi bi-exclamation-triangle-fill me-2"></i>
                  У вас нет платежного счета. Создайте его для оплаты.
                </div>
                
                {accountCreated && (
                  <div className="alert alert-success">
                    ✅ Счет успешно создан!
                  </div>
                )}
                
                {createAccountError && (
                  <div className="alert alert-danger">
                    {createAccountError}
                  </div>
                )}
                
                <div className="mb-3">
                  <label className="form-label">Номер карты</label>
                  <input
                    type="text"
                    className={`form-control ${cardNumberError ? 'is-invalid' : ''}`}
                    placeholder="XXXX XXXX XXXX XXXX"
                    value={cardNumberFormatted}
                    onChange={handleCardNumberChange}
                    maxLength="19"
                    disabled={creatingAccount}
                  />
                  {cardNumberError && (
                    <div className="invalid-feedback">{cardNumberError}</div>
                  )}
                  <small className="text-muted">
                    Тестовые карты: 4111 1111 1111 1111 (Visa), 5555 5555 5555 4444 (MasterCard)
                  </small>
                </div>
                
                <button
                  className="btn btn-primary w-100"
                  onClick={handleCreateAccount}
                  disabled={creatingAccount || cardNumber.length !== 16}
                >
                  {creatingAccount ? 'Создание...' : 'Создать счет'}
                </button>
              </>
            )}
            
            {/* Информация о счете */}
            {!showCreateAccountForm && accountInfo && !loadingAccount && (
              <>
                <div className="bg-light p-3 rounded mb-3">
                  <div className="d-flex justify-content-between">
                    <span className="text-muted">Баланс:</span>
                    <span className="fw-bold text-success">{accountInfo.balance?.toFixed(2)} ₽</span>
                  </div>
                </div>
                
                {/* Детали заказа */}
<h6 className="fw-bold mb-2">Заказ #{orderDetails?.orderId}</h6>
<div className="mb-3" style={{ maxHeight: '200px', overflowY: 'auto' }}>
  {orderDetails?.items && orderDetails.items.length > 0 ? (
    orderDetails.items.map((item, i) => {
      // Получаем название товара (пробуем разные варианты)
      const productName = item.productName || item.name || `Товар #${item.productId || i+1}`;
      const quantity = item.quantity || 1;
      const price = item.price || 0;
      const total = (price * quantity).toFixed(2);
      
      return (
        <div key={i} className="d-flex justify-content-between align-items-start py-2 border-bottom">
          <div style={{ flex: 3, paddingRight: '10px' }}>
            <div className="fw-medium">{productName}</div>
            <small className="text-muted">Код: {item.productId}</small>
          </div>
          <div style={{ flex: 1, textAlign: 'center' }}>
            {quantity} шт.
          </div>
          <div style={{ flex: 1, textAlign: 'right', whiteSpace: 'nowrap' }}>
            {total} ₽
          </div>
        </div>
      );
    })
  ) : (
    <div className="text-center text-muted py-3">
      <i className="bi bi-inbox me-2"></i>
      Нет товаров для отображения
    </div>
  )}
</div>

<div className="d-flex justify-content-between fw-bold mb-3">
  <span>Итого:</span>
  <span className="text-primary">{orderDetails?.totalAmount?.toFixed(2)} ₽</span>
</div>
                
                {/* Способы оплаты */}
                <div className="btn-group w-100 mb-3">
                  <button
                    className={`btn ${paymentMethod === 'card' ? 'btn-primary' : 'btn-outline-primary'}`}
                    onClick={() => setPaymentMethod('card')}
                  >
                    Карта
                  </button>
                  <button
                    className={`btn ${paymentMethod === 'balance' ? 'btn-primary' : 'btn-outline-primary'}`}
                    onClick={() => setPaymentMethod('balance')}
                    disabled={accountInfo.balance < orderDetails?.totalAmount}
                  >
                    С баланса
                  </button>
                </div>
                
                {paymentError && (
                  <div className="alert alert-danger py-2">{paymentError}</div>
                )}
                
                {paymentSuccess && (
                  <div className="alert alert-success py-2">✅ Оплата прошла успешно!</div>
                )}
              </>
            )}
          </div>
          
          {/* Footer */}
          <div className="modal-footer">
            <button className="btn btn-secondary" onClick={onClose}>
              Отмена
            </button>
            
            {!showCreateAccountForm && accountInfo && (
              <button
                className="btn btn-primary"
                onClick={handlePayment}
                disabled={paymentProcessing || paymentSuccess}
              >
                {paymentProcessing ? 'Обработка...' : `Оплатить ${orderDetails?.totalAmount?.toFixed(2)} ₽`}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default PaymentModal;

// *** КОНЕЦ ФАЙЛА PaymentModal.jsx ***