import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import PaymentModal from './PaymentModal';
import './ClientCart.css';

const ClientCart = () => {
  const [carts, setCarts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedCart, setSelectedCart] = useState(null);
  const [showModal, setShowModal] = useState(false);
  
  // Состояния для PaymentModal
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [currentOrderDetails, setCurrentOrderDetails] = useState(null);

  // Функция для получения clientId из localStorage
  const getClientId = useCallback(() => {
    try {
      const userJson = localStorage.getItem('user');
      if (userJson) {
        const user = JSON.parse(userJson);
        console.log('Текущий пользователь:', user);
        return user.id;
      }
    } catch (e) {
      console.error('Ошибка получения clientId:', e);
    }
    return null;
  }, []);

  // Функция для получения токена
  const getAuthToken = useCallback(() => {
    return localStorage.getItem('token');
  }, []);

  // Получаем корзины
  const fetchCarts = useCallback(async () => {
    try {
      const clientId = getClientId();
      
      if (!clientId) {
        setError('Пользователь не авторизован');
        return [];
      }

      const response = await axios.get(
        `http://localhost:8080/api/cart/client/${clientId}/full`
      );
      
      console.log('Ответ от API:', response.data);
      
      if (response.data.success) {
        return response.data.carts || [];
      }
    } catch (err) {
      console.error('Ошибка при загрузке корзин:', err);
    }
    return [];
  }, [getClientId]);

  // Основная функция загрузки данных
  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    
    try {
      const clientId = getClientId();
      
      if (!clientId) {
        setError('Пользователь не авторизован');
        setLoading(false);
        return;
      }

      console.log('Загружаем данные для клиента:', clientId);

      const cartsData = await fetchCarts();

      console.log('Получено корзин до сортировки:', cartsData.length);

      // СОРТИРУЕМ ПО ВОЗРАСТАНИЮ ID
      const sortedCarts = [...cartsData].sort((a, b) => {
        const idA = a.id || 0;
        const idB = b.id || 0;
        return idB - idA; // По убыыванию
      });

      console.log('Получено корзин после сортировки:', sortedCarts.length);

      setCarts(sortedCarts);

    } catch (err) {
      console.error('Ошибка при загрузке данных:', err);
      setError(err.response?.data?.message || err.message || 'Ошибка сети');
    } finally {
      setLoading(false);
    }
  }, [fetchCarts, getClientId]);

  const handleCartClick = (cart) => {
    setSelectedCart(cart);
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setSelectedCart(null);
  };

  // Функция открытия оплаты
  const handleOpenPayment = async (cart) => {
    console.log('📦 Открытие оплаты для корзины:', cart);
    
    const cartId = cart.id;
    let orderNumber = null;
    
    try {
      const orderResponse = await axios.get(
        `http://localhost:8080/api/orders/by-cart/${cartId}`,
        {
          headers: { 'Authorization': `Bearer ${getAuthToken()}` }
        }
      );
      
      if (orderResponse.data && orderResponse.data.success) {
        orderNumber = orderResponse.data.orderNumber;
        console.log('✅ Найден настоящий номер заказа:', orderNumber);
      }
    } catch (err) {
      console.log('⚠️ Не удалось получить номер заказа, используем ORD-' + cartId);
    }
    
    if (!orderNumber) {
      orderNumber = `ORD-${cartId}`;
    }
    
    const orderDetails = {
      userId: cart.clientId,
      orderId: orderNumber,
      cartId: cartId,
      totalAmount: cart.totalAmount,
      items: cart.items?.map(item => ({
        productId: item.productId,
        productName: item.productName,
        quantity: item.quantity,
        price: item.price
      })) || []
    };
    
    console.log('💰 Открытие оплаты для заказа:', orderDetails);
    
    setCurrentOrderDetails(orderDetails);
    setShowPaymentModal(true);
    setShowModal(false);
  };

  // Обработка успешной оплаты
  const handlePaymentSuccess = useCallback((paymentData) => {
    console.log('✅ Оплата успешна:', paymentData);
    setTimeout(() => {
    loadData(); // ← ОБНОВЛЯЕМ ЗАКАЗЫ ЧЕРЕЗ 2 СЕКУНДЫ
  }, 2000);
  }, [loadData]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  if (loading) return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      height: '100vh',
      flexDirection: 'column',
      gap: '20px'
    }}>
      <div className="spinner-border text-primary" style={{ width: '3rem', height: '3rem' }} role="status">
        <span className="visually-hidden">Загрузка...</span>
      </div>
      <div style={{ fontSize: '18px', color: '#666' }}>
        Загрузка данных...
      </div>
    </div>
  );
  
  if (error) return (
    <div style={{ 
      maxWidth: '1200px', 
      margin: '0 auto', 
      padding: '40px 20px',
      textAlign: 'center'
    }}>
      <div style={{ 
        color: '#d32f2f', 
        padding: '20px',
        background: '#ffebee',
        borderRadius: '8px',
        marginBottom: '20px'
      }}>
        <h3 style={{ marginBottom: '10px' }}>Ошибка</h3>
        <p>{error}</p>
      </div>
      <button 
        onClick={loadData}
        style={{
          padding: '10px 20px',
          background: '#1976d2',
          color: 'white',
          border: 'none',
          borderRadius: '4px',
          cursor: 'pointer'
        }}
      >
        Повторить попытку
      </button>
    </div>
  );

  if (carts.length === 0) {
    return (
      <div style={{ 
        maxWidth: '1200px', 
        margin: '0 auto', 
        padding: '40px 20px',
        textAlign: 'center'
      }}>
        <div style={{ fontSize: '24px', color: '#666', marginBottom: '20px' }}>
          У вас нет заказов
        </div>
        <p style={{ color: '#999', marginBottom: '30px' }}>
          Все ваши заказы будут отображаться здесь после оформления
        </p>
        <button 
          onClick={() => window.location.href = '/client-portal'}
          style={{
            padding: '12px 30px',
            background: '#1976d2',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontSize: '16px'
          }}
        >
          Перейти к покупкам
        </button>
      </div>
    );
  }

  // Статистика
  const totalCarts = carts.length;
  const totalAmount = carts.reduce((sum, cart) => sum + (cart.totalAmount || 0), 0);
  const completedCarts = carts.filter(cart => cart.status === 'completed').length;
  const pendingCarts = carts.filter(cart => cart.status === 'pending').length;

  // Функция для отображения статуса
  const renderStatus = (cart) => {
    const status = cart.status || 'active';
    
    const getStatusConfig = () => {
      const normalizedStatus = String(status).toLowerCase().trim();
      
      switch(normalizedStatus) {
        case 'completed':
          return {
            text: '✅ Завершен',
            bgColor: '#e8f5e9',
            textColor: '#2e7d32'
          };
        
        case 'collected':
          return {
            text: '📦 Собран',
            bgColor: '#e3f2fd',
            textColor: '#1565c0'
          };
        
        case 'processing':
        case 'in_progress':
          return {
            text: '⚙️ В обработке',
            bgColor: '#e2dede',
            textColor: '#797572'
          };
        
        case 'problem':
          return {
            text: '🚨 Проблема',
            bgColor: '#ffebee',
            textColor: '#c62828'
          };
        
        case 'waiting':
          return {
            text: '⏳ Ожидание',
            bgColor: '#f5f5f5',
            textColor: '#616161'
          };
        
        case 'pending':
          return {
            text: '⏳ Ожидание оплаты',
            bgColor: '#fff3e0',
            textColor: '#ed6c02'
          };
        
        case 'active':
          return {
            text: '🟢 Активен',
            bgColor: '#f1f8e9',
            textColor: '#689f38'
          };
        
        case 'created':
          return {
            text: '📝 Создан',
            bgColor: '#e3f2fd',
            textColor: '#1976d2'
          };
        
        default:
          return {
            text: `❓ ${status}`,
            bgColor: '#f5f5f5',
            textColor: '#757575'
          };
      }
    };

    const config = getStatusConfig();
    
    return (
      <span style={{
        backgroundColor: config.bgColor,
        color: config.textColor,
        padding: '4px 8px',
        borderRadius: '12px',
        fontSize: '12px',
        fontWeight: '500',
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px'
      }}>
        {config.text}
      </span>
    );
  };

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '20px' }}>
      {/* Заголовок и статистика */}
      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: '30px',
        gap: '20px'
      }}>
        <div style={{ flex: 1 }}>
          <h1 style={{ margin: '0 0 15px 0', fontSize: '28px', color: '#333' }}>
            Мои заказы
          </h1>
        </div>
        
        <div style={{ 
          display: 'flex', 
          gap: '30px',
          alignItems: 'flex-start'
        }}>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '12px', color: '#666', marginBottom: '5px' }}>
              Всего заказов
            </div>
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#182027ff' }}>
              {totalCarts}
            </div>
          </div>
          
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '12px', color: '#666', marginBottom: '5px' }}>
              В обработке
            </div>
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#1976d2' }}>
              {pendingCarts}
            </div>
          </div>
          
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '12px', color: '#666', marginBottom: '5px' }}>
              Завершено
            </div>
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#0b130cff' }}>
              {completedCarts}
            </div>
          </div>
          
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '12px', color: '#666', marginBottom: '5px' }}>
              Общая сумма
            </div>
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#4c0e58ff' }}>
              {totalAmount.toFixed(2)}<span style={{ fontSize: '14px', marginLeft: '2px' }}>₽</span>
            </div>
          </div>
        </div>
      </div>
      
      {/* Список заказов */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
        {carts.map(cart => (
          <div 
            key={cart.id} 
            onClick={() => handleCartClick(cart)}
            style={{
              border: '1px solid #e0e0e0',
              borderRadius: '8px',
              padding: '15px',
              background: 'white',
              cursor: 'pointer',
              transition: 'all 0.2s ease',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              boxShadow: '0 1px 3px rgba(0,0,0,0.05)'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.1)';
              e.currentTarget.style.borderColor = '#1976d2';
              e.currentTarget.style.transform = 'translateY(-2px)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.05)';
              e.currentTarget.style.borderColor = '#e0e0e0';
              e.currentTarget.style.transform = 'translateY(0)';
            }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: '5px', flex: 1 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
                <div style={{ fontWeight: 'bold', fontSize: '16px' }}>
                  Заказ #{cart.id}
                </div>
                {renderStatus(cart)}
              </div>
              
              <div style={{ display: 'flex', gap: '20px', fontSize: '14px', color: '#666' }}>
                <div>
                  <span style={{ color: '#999' }}>Товаров: </span>
                  {cart.itemsCount || 0}
                </div>
                <div>
                  <span style={{ color: '#999' }}>Дата: </span>
                  {new Date(cart.createdDate).toLocaleDateString('ru-RU')}
                </div>
              </div>
            </div>
            
            <div style={{ textAlign: 'right', marginLeft: '20px' }}>
              <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#333' }}>
                {cart.totalAmount?.toFixed(2)}<span style={{ fontSize: '14px', marginLeft: '2px' }}>₽</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Модальное окно с деталями заказа */}
      {showModal && selectedCart && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.5)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 1000,
          padding: '20px'
        }} onClick={closeModal}>
          <div style={{
            backgroundColor: 'white',
            borderRadius: '12px',
            width: '100%',
            maxWidth: '800px',
            maxHeight: '85vh',
            display: 'flex',
            flexDirection: 'column',
            position: 'relative',
            overflow: 'hidden'
          }} onClick={e => e.stopPropagation()}>
            
            <button 
              onClick={closeModal}
              style={{
                position: 'absolute',
                top: '15px',
                right: '15px',
                background: 'none',
                border: 'none',
                fontSize: '20px',
                cursor: 'pointer',
                color: '#666',
                zIndex: 10
              }}
            >
              ×
            </button>
            
            {/* Заголовок */}
            <div style={{ 
              padding: '25px 30px 15px 30px',
              borderBottom: '1px solid #e0e0e0'
            }}>
              <h2 style={{ margin: '0 0 5px 0', fontSize: '24px' }}>
                Заказ #{selectedCart.id}
              </h2>
              <div style={{ display: 'flex', gap: '15px', alignItems: 'center', flexWrap: 'wrap' }}>
                {renderStatus(selectedCart)}
                <div style={{ fontSize: '14px', color: '#666' }}>
                  Создан: {new Date(selectedCart.createdDate).toLocaleDateString('ru-RU')}
                </div>
              </div>
            </div>
            
            {/* Список товаров */}
            <div style={{ 
              padding: '20px 30px',
              overflowY: 'auto',
              flex: 1
            }}>
              {selectedCart.items && selectedCart.items.length > 0 ? (
                <div>
                  <h3 style={{ margin: '0 0 15px 0', fontSize: '18px', color: '#333' }}>
                    Состав заказа
                  </h3>
                  
                  <div style={{ 
                    border: '1px solid #e0e0e0',
                    borderRadius: '8px',
                    overflow: 'hidden'
                  }}>
                    {selectedCart.items.map((item, index) => (
                      <div 
                        key={item.id}
                        style={{
                          padding: '15px',
                          borderBottom: index < selectedCart.items.length - 1 ? '1px solid #f0f0f0' : 'none',
                          backgroundColor: index % 2 === 0 ? 'white' : '#fafafa',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center'
                        }}
                      >
                        <div style={{ flex: 1 }}>
                          <div style={{ fontWeight: '500', marginBottom: '5px' }}>
                            {item.productName}
                          </div>
                          <div style={{ fontSize: '12px', color: '#888' }}>
                            {item.articul && `Арт: ${item.articul}`} 
                            {item.category && ` • Категория: ${item.category}`}
                          </div>
                        </div>
                        
                        <div style={{ display: 'flex', gap: '30px', alignItems: 'center' }}>
                          <div style={{ textAlign: 'center', minWidth: '60px' }}>
                            <div style={{ fontSize: '12px', color: '#666', marginBottom: '2px' }}>
                              Кол-во
                            </div>
                            <div style={{ fontSize: '16px', fontWeight: 'bold' }}>
                              {item.quantity}
                            </div>
                          </div>
                          
                          <div style={{ textAlign: 'center', minWidth: '100px' }}>
                            <div style={{ fontSize: '12px', color: '#666', marginBottom: '2px' }}>
                              Цена
                            </div>
                            <div style={{ fontSize: '16px' }}>
                              {item.price?.toFixed(2)} ₽
                            </div>
                          </div>
                          
                          <div style={{ textAlign: 'center', minWidth: '100px' }}>
                            <div style={{ fontSize: '12px', color: '#666', marginBottom: '2px' }}>
                              Сумма
                            </div>
                            <div style={{ fontSize: '16px', fontWeight: 'bold', color: '#1976d2' }}>
                              {item.itemTotal?.toFixed(2)} ₽
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div style={{ 
                  padding: '40px', 
                  textAlign: 'center', 
                  color: '#999',
                  fontStyle: 'italic'
                }}>
                  В корзине нет товаров
                </div>
              )}
            </div>
            
            {/* Итог и кнопка оплаты */}
            <div style={{ 
              display: 'flex', 
              flexDirection: 'column',
              borderTop: '1px solid #e0e0e0'
            }}>
              <div style={{ 
                display: 'flex', 
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '20px 30px',
                background: '#f9f9f9'
              }}>
                <div style={{ textAlign: 'left' }}>
                  <div style={{ fontSize: '14px', color: '#666', marginBottom: '5px' }}>
                    Количество товаров
                  </div>
                  <div style={{ fontSize: '20px', fontWeight: 'bold', color: '#333' }}>
                    {selectedCart.itemsCount || 0}
                  </div>
                </div>
                
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '14px', color: '#666', marginBottom: '5px' }}>
                    Общая сумма заказа
                  </div>
                  <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#d32f2f' }}>
                    {selectedCart.totalAmount?.toFixed(2)} ₽
                  </div>
                </div>
              </div>
              
              {/* Кнопка оплаты - только для статуса pending */}
              {selectedCart.status === 'pending' && (
                <div style={{
                  padding: '15px 30px',
                  backgroundColor: '#fff3e0',
                  textAlign: 'center',
                  borderTop: '1px solid #ffe0b2'
                }}>
                  <button
                    className="btn btn-primary"
                    onClick={() => handleOpenPayment(selectedCart)}
                    style={{
                      backgroundColor: '#ed6c02',
                      color: 'white',
                      border: 'none',
                      borderRadius: '8px',
                      padding: '12px 30px',
                      fontSize: '16px',
                      fontWeight: 'bold'
                    }}
                  >
                    <i className="bi bi-credit-card"></i> Оплатить заказ
                  </button>
                </div>
              )}
              
              {selectedCart.status === 'completed' && (
                <div style={{
                  padding: '15px 30px',
                  backgroundColor: '#e8f5e9',
                  textAlign: 'center',
                  color: '#2e7d32',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px'
                }}>
                  <span style={{ fontSize: '16px' }}>✅</span>
                  Заказ завершен
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Модальное окно оплаты */}
      <PaymentModal
        show={showPaymentModal}
        onClose={() => {
          setShowPaymentModal(false);
          setCurrentOrderDetails(null);
        }}
        orderDetails={currentOrderDetails}
        onConfirm={handlePaymentSuccess}
        onClearCart={() => {}}
        authToken={getAuthToken()}
      />
    </div>
  );
};

export default ClientCart;