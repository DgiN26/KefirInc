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

  // Состояния для добавления карты
  const [showAddCardForm, setShowAddCardForm] = useState(false);
  const [addingCard, setAddingCard] = useState(false);
  const [addCardError, setAddCardError] = useState(null);
  const [addCardSuccess, setAddCardSuccess] = useState(false);

  // НОВЫЕ СОСТОЯНИЯ ДЛЯ ПОДТВЕРЖДЕНИЯ CVV
  const [showCvvModal, setShowCvvModal] = useState(false);
  const [cvvValue, setCvvValue] = useState('');
  const [cvvError, setCvvError] = useState('');
  const [cardBalance, setCardBalance] = useState(null);
  const [selectedCardNumber, setSelectedCardNumber] = useState('');
  const [selectedCardId, setSelectedCardId] = useState(null);

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

  const handleCvvChange = (e) => {
    const value = e.target.value.replace(/\D/g, '').slice(0, 3);
    setCvvValue(value);
    setCvvError(value.length === 3 ? '' : 'CVV должен содержать 3 цифры');
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
      setShowAddCardForm(false);
      
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
          
          // Получаем информацию о карте
          try {
            const cardResponse = await axios.get(
              `${API_BASE_URL}/payments/card-info/${userId}`,
              getAuthHeaders()
            );
            
            if (cardResponse.data && cardResponse.data.id) {
              // Карта есть
              setAccountInfo({
                userId: userId,
                balance: balanceResponse.data?.balance || 0,
                accountNumber: `PA-${userId.toString().padStart(8, '0')}`,
                cardNumber: cardResponse.data?.cardNumber || '**** **** **** ****',
                cardBalance: cardResponse.data?.balance || 0,
                cardId: cardResponse.data?.id || null
              });
            } else {
              // Карты нет - показываем форму добавления
              setAccountInfo({
                userId: userId,
                balance: balanceResponse.data?.balance || 0,
                accountNumber: `PA-${userId.toString().padStart(8, '0')}`,
                cardNumber: null,
                cardBalance: 0,
                cardId: null
              });
              setShowAddCardForm(true);
            }
          } catch (cardErr) {
            // Ошибка при получении карты - считаем что карты нет
            setAccountInfo({
              userId: userId,
              balance: balanceResponse.data?.balance || 0,
              accountNumber: `PA-${userId.toString().padStart(8, '0')}`,
              cardNumber: null,
              cardBalance: 0,
              cardId: null
            });
            setShowAddCardForm(true);
          }
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
      const cleanCardNumber = cardNumber.replace(/\s/g, '');
      
      const accountResponse = await axios.post(
        `${API_BASE_URL}/payments/create-account`,
        {
          user_id: userId,
          role: 'client'
        },
        getAuthHeaders()
      );

      if (!accountResponse.data || accountResponse.data.status !== 'success') {
        throw new Error(accountResponse.data?.message || 'Ошибка при создании счета');
      }
      
      const cartResponse = await axios.post(
        `${API_BASE_URL}/payments/create-cart`,
        {
          user_id: userId,
          card_number: cleanCardNumber
        },
        getAuthHeaders()
      );

      if (!cartResponse.data || cartResponse.data.status !== 'success') {
        throw new Error(cartResponse.data?.message || 'Ошибка при сохранении карты');
      }
      
      setAccountCreated(true);
      
      const balanceResponse = await axios.get(
        `${API_BASE_URL}/payments/my-balance`,
        getAuthHeaders()
      );
      
      setAccountInfo({
        userId: userId,
        balance: balanceResponse.data?.balance || 0,
        accountNumber: `PA-${userId.toString().padStart(8, '0')}`,
        cardNumber: formatCardNumber(cleanCardNumber),
        cardBalance: cartResponse.data?.balance || 0,
        cardId: cartResponse.data?.id || null
      });
      
      setShowCreateAccountForm(false);
      setShowAddCardForm(false);
      setTimeout(() => setAccountCreated(false), 3000);
      
    } catch (err) {
      console.error('❌ Ошибка создания счета', err);
      setCreateAccountError(err.response?.data?.message || err.message || 'Ошибка при создании счета');
    } finally {
      setCreatingAccount(false);
    }
  };

  // Добавление карты к существующему счету
  const handleAddCard = async () => {
    if (cardNumber.length !== 16) {
      setCardNumberError('Номер карты должен содержать 16 цифр');
      return;
    }

    const userId = getUserIdFromStorage();
    
    if (!userId) {
      setAddCardError('Не удалось определить пользователя');
      return;
    }

    setAddingCard(true);
    setAddCardError(null);
    
    try {
      const cleanCardNumber = cardNumber.replace(/\s/g, '');
      
      const cartResponse = await axios.post(
        `${API_BASE_URL}/payments/create-cart`,
        {
          user_id: userId,
          card_number: cleanCardNumber
        },
        getAuthHeaders()
      );

      if (!cartResponse.data || cartResponse.data.status !== 'success') {
        throw new Error(cartResponse.data?.message || 'Ошибка при сохранении карты');
      }
      
      setAddCardSuccess(true);
      
      setAccountInfo(prev => ({
        ...prev,
        cardNumber: formatCardNumber(cleanCardNumber),
        cardBalance: cartResponse.data?.balance || 0,
        cardId: cartResponse.data?.id || null
      }));
      
      setShowAddCardForm(false);
      setTimeout(() => setAddCardSuccess(false), 3000);
      
    } catch (err) {
      console.error('❌ Ошибка добавления карты', err);
      setAddCardError(err.response?.data?.message || err.message || 'Ошибка при добавлении карты');
    } finally {
      setAddingCard(false);
    }
  };

  // Обработка выбора способа оплаты "Карта"
  const handleCardPaymentClick = () => {
    if (!accountInfo.cardId) {
      setShowAddCardForm(true);
      return;
    }
    
    setSelectedCardNumber(accountInfo.cardNumber);
    setCardBalance(accountInfo.cardBalance);
    setSelectedCardId(accountInfo.cardId);
    setShowCvvModal(true);
  };

  // Подтверждение оплаты по CVV
 // Подтверждение оплаты по CVV
const handleCvvConfirm = async () => {
  if (cvvValue.length !== 3) {
    setCvvError('CVV должен содержать 3 цифры');
    return;
  }

  setPaymentProcessing(true);
  
  try {
    // 1. СНАЧАЛА СОЗДАЕМ ЗАКАЗ
    const orderData = {
      userId: getUserIdFromStorage(),
      items: orderDetails.items.map(item => ({
        productId: item.productId,
        productName: item.productName || item.name,
        quantity: item.quantity,
        price: item.price
      })),
      totalAmount: orderDetails.totalAmount,
      status: 'pending'
    };

    const orderResponse = await axios.post(
      `${API_BASE_URL}/orders`,
      orderData,
      getAuthHeaders()
    );

    if (!orderResponse.data || !orderResponse.data.success) {
      throw new Error('Ошибка при создании заказа');
    }

    const realOrderId = orderResponse.data.id || orderResponse.data.orderId;

    // 2. ПОТОМ СПИСЫВАЕМ ДЕНЬГИ С КАРТЫ
    const response = await axios.post(
      `${API_BASE_URL}/payments/card-payment`,
      {
        card_id: selectedCardId,
        user_id: getUserIdFromStorage(),
        amount: orderDetails.totalAmount,
        order_id: realOrderId,
        cvv: cvvValue
      },
      getAuthHeaders()
    );

    if (response.data && response.data.status === 'success') {
      setShowCvvModal(false);
      setCvvValue('');
      setCvvError('');
      
      // 3. ПОДТВЕРЖДАЕМ ОПЛАТУ ЗАКАЗА
      try {
        await axios.post(
          `${API_BASE_URL}/orders/${realOrderId}/confirm-payment`,
          { amount: orderDetails.totalAmount },
          getAuthHeaders()
        );
      } catch (confirmErr) {
        console.error('❌ Ошибка списания товаров:', confirmErr);
      }
      
      setPaymentSuccess(true);
      setAccountInfo(prev => ({
        ...prev,
        cardBalance: response.data.new_card_balance,
        balance: response.data.new_balance || prev.balance
      }));
      
      if (onConfirm) {
        onConfirm({
          ...response.data,
          orderId: realOrderId
        });
      }
      if (onClearCart) onClearCart();
      
      setTimeout(() => {
        setPaymentProcessing(false);
        setPaymentSuccess(false);
        onClose();
        window.dispatchEvent(new CustomEvent('payment-completed'));
      }, 2000);
      
    } else {
      setCvvValue('');
      setCvvError(response.data?.message || 'Ошибка при оплате');
      setPaymentProcessing(false);
    }
  } catch (err) {
    console.error('❌ Ошибка:', err);
    setCvvValue('');
    setCvvError(err.response?.data?.message || 'Ошибка при оплате');
    setPaymentProcessing(false);
  }
};
  const handleCvvClose = () => {
    setShowCvvModal(false);
    setCvvValue('');
    setCvvError('');
  };

  // Оплата с баланса
 // Оплата с баланса
const handleBalancePayment = async () => {
  if (paymentProcessing) return;
  
  setPaymentProcessing(true);
  setPaymentError(null);
  
  try {
    // 1. СНАЧАЛА СОЗДАЕМ ЗАКАЗ
    const orderData = {
      userId: getUserIdFromStorage(),
      items: orderDetails.items.map(item => ({
        productId: item.productId,
        productName: item.productName || item.name,
        quantity: item.quantity,
        price: item.price
      })),
      totalAmount: orderDetails.totalAmount,
      status: 'pending'
    };

    const orderResponse = await axios.post(
      `${API_BASE_URL}/orders`,
      orderData,
      getAuthHeaders()
    );

    if (!orderResponse.data || !orderResponse.data.success) {
      throw new Error('Ошибка при создании заказа');
    }

    const realOrderId = orderResponse.data.id || orderResponse.data.orderId;
    
    // 2. ПОТОМ СПИСЫВАЕМ ДЕНЬГИ С БАЛАНСА
    const withdrawResponse = await axios.post(
      `${API_BASE_URL}/payments/withdraw`,
      {
        user_id: getUserIdFromStorage(),
        amount: orderDetails.totalAmount,
        order_id: realOrderId,
        description: `Оплата заказа #${realOrderId}`
      },
      getAuthHeaders()
    );

    if (withdrawResponse.data && withdrawResponse.data.status === 'success') {
      
      // 3. ПОДТВЕРЖДАЕМ ОПЛАТУ
      try {
        await axios.post(
          `${API_BASE_URL}/orders/${realOrderId}/confirm-payment`,
          { amount: orderDetails.totalAmount },
          getAuthHeaders()
        );
      } catch (confirmErr) {
        console.error('❌ Ошибка списания товаров:', confirmErr);
      }
      
      setPaymentSuccess(true);
      setAccountInfo(prev => ({ ...prev, balance: withdrawResponse.data.new_balance }));
      
      if (onConfirm) {
        onConfirm({
          ...withdrawResponse.data,
          orderId: realOrderId
        });
      }
      if (onClearCart) onClearCart();
      
      setTimeout(() => {
        setPaymentProcessing(false);
        setPaymentSuccess(false);
        onClose();
        window.dispatchEvent(new CustomEvent('payment-completed'));
      }, 2000);
    } else {
      setPaymentError(withdrawResponse.data?.message || 'Ошибка при оплате');
      setPaymentProcessing(false);
    }
  } catch (err) {
    console.error('❌ Ошибка оплаты:', err);
    setPaymentError(err.response?.data?.message || 'Ошибка при оплате');
    setPaymentProcessing(false);
  }
};

  useEffect(() => {
    if (show) {
      setPaymentProcessing(false);
      setPaymentSuccess(false);
      setPaymentError(null);
      setCvvValue('');
      setCvvError('');
      setShowAddCardForm(false);
    }
  }, [show]);

  if (!show) return null;

  return (
    <>
      {/* Основное модальное окно */}
      <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            
            <div className="modal-header" style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', color: 'white' }}>
              <h5 className="modal-title">
                <i className="bi bi-credit-card me-2"></i>
                KEFIR Pay
              </h5>
              <button type="button" className="btn-close btn-close-white" onClick={onClose}></button>
            </div>
            
            <div className="modal-body">
              
              {loadingAccount && (
                <div className="text-center py-4">
                  <div className="spinner-border text-primary"></div>
                  <p className="mt-2">Загрузка...</p>
                </div>
              )}
              
              {/* Форма создания счета (первый вход) */}
              {showCreateAccountForm && !loadingAccount && (
                <>
                  <div className="alert alert-warning">
                    <i className="bi bi-exclamation-triangle-fill me-2"></i>
                    У вас нет платежного счета. Создайте его для оплаты.
                  </div>
                  
                  {accountCreated && (
                    <div className="alert alert-success">✅ Счет успешно создан!</div>
                  )}
                  
                  {createAccountError && (
                    <div className="alert alert-danger">{createAccountError}</div>
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
                    {cardNumberError && <div className="invalid-feedback">{cardNumberError}</div>}
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

              {/* Форма добавления карты (есть счет, нет карты) */}
              {showAddCardForm && !loadingAccount && !showCreateAccountForm && (
                <>
                  <div className="alert alert-info">
                    <i className="bi bi-info-circle-fill me-2"></i>
                    У вас есть счет, но нет привязанной карты. Добавьте карту для оплаты.
                  </div>
                  
                  {addCardSuccess && (
                    <div className="alert alert-success">✅ Карта успешно добавлена!</div>
                  )}
                  
                  {addCardError && (
                    <div className="alert alert-danger">{addCardError}</div>
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
                      disabled={addingCard}
                    />
                    {cardNumberError && <div className="invalid-feedback">{cardNumberError}</div>}
                    <small className="text-muted">
                      Тестовые карты: 4111 1111 1111 1111 (Visa), 5555 5555 5555 4444 (MasterCard)
                    </small>
                  </div>
                  
                  <div className="d-grid gap-2">
                    <button
                      className="btn btn-primary"
                      onClick={handleAddCard}
                      disabled={addingCard || cardNumber.length !== 16}
                    >
                      {addingCard ? 'Добавление...' : 'Добавить карту'}
                    </button>
                    <button
                      className="btn btn-outline-secondary"
                      onClick={() => setShowAddCardForm(false)}
                    >
                      Отмена
                    </button>
                  </div>
                </>
              )}
              
              {/* Информация о счете (есть и счет и карта) */}
              {!showCreateAccountForm && !showAddCardForm && accountInfo && !loadingAccount && (
                <>
                  <div className="bg-light p-3 rounded mb-3">
                    <div className="d-flex justify-content-between mb-2">
                      <span className="text-muted">Номер счета:</span>
                      <span className="fw-bold">{accountInfo.accountNumber}</span>
                    </div>
                    <div className="d-flex justify-content-between">
                      <span className="text-muted">Баланс счета:</span>
                      <span className="fw-bold text-success">{accountInfo.balance?.toFixed(2)} ₽</span>
                    </div>
                  </div>
                  
                  <h6 className="fw-bold mb-2">Заказ #{orderDetails?.orderId}</h6>
                  <div className="mb-3" style={{ maxHeight: '200px', overflowY: 'auto' }}>
                    {orderDetails?.items?.map((item, i) => {
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
                          <div style={{ flex: 1, textAlign: 'center' }}>{quantity} шт.</div>
                          <div style={{ flex: 1, textAlign: 'right' }}>{total} ₽</div>
                        </div>
                      );
                    })}
                  </div>

                  <div className="d-flex justify-content-between fw-bold mb-3">
                    <span>Итого:</span>
                    <span className="text-primary">{orderDetails?.totalAmount?.toFixed(2)} ₽</span>
                  </div>
                  
                  {/* Способы оплаты */}
                  <div className="btn-group w-100 mb-3">
                    <button
                      className={`btn ${paymentMethod === 'card' ? 'btn-primary' : 'btn-outline-primary'}`}
                      onClick={() => {
                        setPaymentMethod('card');
                        handleCardPaymentClick();
                      }}
                    >
                      Карта
                    </button>
                    <button
                      className={`btn ${paymentMethod === 'balance' ? 'btn-primary' : 'btn-outline-primary'}`}
                      onClick={() => {
                        setPaymentMethod('balance');
                      }}
                    >
                      С баланса
                    </button>
                  </div>
                  
                  {paymentError && <div className="alert alert-danger py-2">{paymentError}</div>}
                  {paymentSuccess && <div className="alert alert-success py-2">✅ Оплата прошла успешно!</div>}
                </>
              )}
            </div>
            
            {/* Footer с кнопкой Оплатить */}
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={onClose}>Отмена</button>
              
              {!showCreateAccountForm && !showAddCardForm && accountInfo && paymentMethod === 'balance' && (
                <button
                  className="btn btn-primary"
                  onClick={handleBalancePayment}
                  disabled={paymentProcessing || paymentSuccess}
                >
                  {paymentProcessing ? 'Обработка...' : `Оплатить ${orderDetails?.totalAmount?.toFixed(2)} ₽`}
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Модальное окно CVV */}
      {showCvvModal && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog modal-dialog-centered modal-sm">
            <div className="modal-content">
              <div className="modal-header" style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', color: 'white' }}>
                <h5 className="modal-title">
                  <i className="bi bi-shield-lock me-2"></i>
                  Подтверждение
                </h5>
                <button type="button" className="btn-close btn-close-white" onClick={handleCvvClose}></button>
              </div>
              <div className="modal-body">
                <div className="text-center mb-3">
                  <div className="bg-light p-2 rounded">
                    <small className="text-muted">Карта</small>
                    <div className="fw-bold">{selectedCardNumber}</div>
                  </div>
                  
                  {/* ОТОБРАЖЕНИЕ БАЛАНСА КАРТЫ */}
                  {cardBalance !== null && (
                    <div className="mt-2 p-2 bg-light rounded">
                      <small className="text-muted">Баланс карты</small>
                      <div className={`fw-bold fs-5 ${cardBalance >= orderDetails?.totalAmount ? 'text-success' : 'text-danger'}`}>
                        {cardBalance.toFixed(2)} ₽
                      </div>
                    </div>
                  )}
                  
                  <div className="mt-2">
                    <small className="text-muted">Сумма к оплате</small>
                    <div className="fw-bold text-primary fs-4">{orderDetails?.totalAmount?.toFixed(2)} ₽</div>
                  </div>

                  {/* ПРОВЕРКА ДОСТАТОЧНОСТИ СРЕДСТВ */}
                  {cardBalance !== null && cardBalance < orderDetails?.totalAmount && (
                    <div className="alert alert-danger py-1 mt-2 mb-0 small">
                      <i className="bi bi-exclamation-triangle-fill me-1"></i>
                      Недостаточно средств на карте
                    </div>
                  )}
                </div>
                
                <div className="mb-3">
                  <label className="form-label">CVV код</label>
                  <input
                    type="password"
                    className={`form-control text-center ${cvvError ? 'is-invalid' : ''}`}
                    placeholder="***"
                    value={cvvValue}
                    onChange={handleCvvChange}
                    maxLength="3"
                    style={{ fontSize: '1.5rem', letterSpacing: '4px' }}
                    autoFocus
                    disabled={paymentProcessing || (cardBalance !== null && cardBalance < orderDetails?.totalAmount)}
                  />
                  {cvvError && <div className="invalid-feedback">{cvvError}</div>}
                </div>
              </div>
              <div className="modal-footer">
                <button className="btn btn-secondary" onClick={handleCvvClose} disabled={paymentProcessing}>
                  Отмена
                </button>
                <button
                  className="btn btn-primary"
                  onClick={handleCvvConfirm}
                  disabled={cvvValue.length !== 3 || paymentProcessing || (cardBalance !== null && cardBalance < orderDetails?.totalAmount)}
                >
                  {paymentProcessing ? 'Обработка...' : 'Оплатить'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default PaymentModal;

// *** КОНЕЦ ФАЙЛА PaymentModal.jsx ***