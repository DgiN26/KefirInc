// src/pages/collector/CollectorApp.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './CollectorApp.css';

const CollectorApp = () => {
  const [orders, setOrders] = useState([]);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [checking, setChecking] = useState(false);
  const [showCompleteButton, setShowCompleteButton] = useState(false);
  const [checkResult, setCheckResult] = useState(null);
  const [stats, setStats] = useState({
    totalOrders: 0,
    completedToday: 0,
    averageTime: '15 мин',
    accuracy: '100%'
  });

  // Функция нормализации статуса (убирает квадратные скобки)
 // const normalizeStatus = (status) => {
 //   if (!status) return '';
    // Удаляем все квадратные скобки, кавычки и лишние пробелы
  //  return status.replace(/[\[\]"]/g, '').trim().toLowerCase();
 // };

  // Функция загрузки заказов
  const fetchOrders = async () => {
    try {
      setLoading(true);
      const response = await axios.get('http://localhost:8080/api/collector/processing-orders');
      
      if (response.data.success) {
        const newOrders = response.data.orders || [];
        setOrders(newOrders);
        setStats(prev => ({
          ...prev,
          totalOrders: newOrders.length
        }));
        
        // Если есть заказы и не выбран текущий, выбираем первый
        if (newOrders.length > 0 && !selectedOrder) {
          setSelectedOrder(newOrders[0]);
        }
        
        // Если выбранный заказ больше не в списке, сбрасываем выбор
        if (selectedOrder && !newOrders.find(o => o.cart_id === selectedOrder.cart_id)) {
          if (newOrders.length > 0) {
            setSelectedOrder(newOrders[0]);
          } else {
            setSelectedOrder(null);
          }
          setCheckResult(null);
          setShowCompleteButton(false);
        }
      }
    } catch (error) {
      console.error('Ошибка загрузки заказов:', error);
      // В случае ошибки показываем пустой список
      setOrders([]);
      setSelectedOrder(null);
    } finally {
      setLoading(false);
    }
  };

  // Инициализация приложения
  useEffect(() => {
    const initializeApp = async () => {
      try {
        // Инициализируем базу данных
        await axios.post('http://localhost:8080/api/collector/init-database');
        
        // Затем загружаем заказы
        await fetchOrders();
      } catch (error) {
        console.error('Ошибка инициализации:', error);
        await fetchOrders();
      }
    };

    initializeApp();
    
    // Polling каждые 15 секунд
    const intervalId = setInterval(fetchOrders, 15000);
    
    return () => clearInterval(intervalId);
  }, []);

  // Проверка статуса заказа
  const verifyCartStatus = async (cartId) => {
    try {
      const response = await axios.get(`http://localhost:8080/api/collector/cart/${cartId}/status`);
      if (response.data.success) {
        console.log(`Статус заказа #${cartId}:`, response.data.status);
        return response.data.status;
      }
    } catch (error) {
      console.error('Ошибка проверки статуса:', error);
    }
    return null;
  };

  // Обработка выбора заказа
  const handleSelectOrder = (order) => {
    setSelectedOrder(order);
    setCheckResult(null);
    setShowCompleteButton(false);
  };

  // Проверка наличия товара
  const checkProductAvailability = async () => {
    if (!selectedOrder) return;
    
    setChecking(true);
    setCheckResult(null);
    setShowCompleteButton(false);
    
    try {
      const response = await axios.post('http://localhost:8080/api/collector/check-product-availability', {
        cartId: selectedOrder.cart_id
      });
      
      if (response.data.success) {
        setCheckResult(response.data);
        setShowCompleteButton(response.data.allAvailable);
      } else {
        alert(`Ошибка: ${response.data.error || 'Неизвестная ошибка'}`);
      }
    } catch (error) {
      console.error('Ошибка проверки наличия:', error);
      alert('Ошибка при проверке наличия товаров');
    } finally {
      setChecking(false);
    }
  };

  // Сообщение об отсутствии товара
  const reportProductMissing = async () => {
    if (!selectedOrder || !selectedOrder.items || selectedOrder.items.length === 0) return;
    
    try {
      // Берем первый товар для примера
      const problemProduct = selectedOrder.items[0];
      const problemDetails = prompt('Опишите проблему с товаром:', 'отсутствует на складе');
      
      if (!problemDetails) return;
      
      console.log('Отправляем данные:', {
        cartId: selectedOrder.cart_id,
        productId: problemProduct.product_id,
        productName: problemProduct.product_name || 'Неизвестный товар',
        problemDetails: problemDetails
      });
      
      const response = await axios.post('http://localhost:8080/api/collector/report-product-missing', {
        cartId: selectedOrder.cart_id,
        productId: problemProduct.product_id,
        productName: problemProduct.product_name || 'Неизвестный товар',
        problemDetails: problemDetails,
        collectorId: 'COLLECTOR_1'
      });
      
      console.log('Ответ сервера:', response.data);
      
      if (response.data.success) {
        let message = `⚠️ Проблема зарегистрирована\n`;
        message += `ID проблемы: ${response.data.problemId || 'не присвоен'}\n`;
        message += `Товар: ${response.data.productName}\n`;
        message += `Причина: ${problemDetails}\n`;
        message += `Статус заказа: ${response.data.cartUpdated ? 'изменен на "problem"' : 'не изменился'}`;
        
        alert(message);
        
        // Если статус изменился, обновляем список
        if (response.data.cartUpdated) {
          // Обновляем список заказов
          const updatedOrders = orders.map(order => 
            order.cart_id === selectedOrder.cart_id 
            ? { ...order, status: 'problem' }
            : order
          );
          setOrders(updatedOrders);
          
          // Удаляем из списка через 2 секунды
          setTimeout(() => {
            const filteredOrders = orders.filter(order => order.cart_id !== selectedOrder.cart_id);
            setOrders(filteredOrders);
            if (filteredOrders.length > 0) {
              setSelectedOrder(filteredOrders[0]);
            } else {
              setSelectedOrder(null);
            }
            setCheckResult(null);
            setShowCompleteButton(false);
          }, 2000);
        }
      } else {
        alert(`Ошибка: ${response.data.error || 'Неизвестная ошибка'}`);
      }
    } catch (error) {
      console.error('Ошибка регистрации проблемы:', error);
      alert('Ошибка при регистрации проблемы: ' + (error.response?.data?.error || error.message));
    }
  };

  // Кнопка "Завершить сборку"
  const completeOrderCollection = async () => {
    if (!selectedOrder) return;
    
    try {
      const response = await axios.post('http://localhost:8080/api/collector/complete-collection', {
        cartId: selectedOrder.cart_id,
        collectorId: 'COLLECTOR_1'
      });
      
      if (response.data.success) {
        alert(`✅ Заказ #${selectedOrder.cart_id} успешно собран!\nID в orders: ${response.data.orderId}\n\nТовары перемещены в склад отгруженных товаров.`);
        
        // Обновляем статистику
        setStats(prev => ({
          ...prev,
          completedToday: prev.completedToday + 1
        }));
        
        // Удаляем заказ из списка
        const filteredOrders = orders.filter(order => order.cart_id !== selectedOrder.cart_id);
        setOrders(filteredOrders);
        if (filteredOrders.length > 0) {
          setSelectedOrder(filteredOrders[0]);
        } else {
          setSelectedOrder(null);
        }
        setCheckResult(null);
        setShowCompleteButton(false);
      } else {
        alert(`Ошибка: ${response.data.error || 'Неизвестная ошибка'}`);
      }
    } catch (error) {
      console.error('Ошибка завершения заказа:', error);
      alert(error.response?.data?.error || 'Ошибка при завершении заказа');
    }
  };

  return (
    <div className="collector-app">
      <div className="container-fluid h-100 p-0 m-0">
        <div className="row g-0 h-100">
          {/* Левая часть (70%) - Список заказов */}
          <div className="col-8 h-100" style={styles.leftPanel}>
            <div className="h-100 position-relative">
              {/* Черный правый верхний угол */}
              <div className="black-corner">
                <div className="black-corner-icon">📦</div>
                <div className="black-corner-text">Заказы</div>
              </div>
              
              <div className="p-4 pt-5 h-100 d-flex flex-column">
                <h2 className="comic-font mb-3">
                  Заказы для сборки
                  <span className="badge bg-dark ms-2">
                    {orders.length}
                  </span>
                </h2>
                
                <div className="comic-font mb-2">
                  Статус: <span className="text-dark fw-bold">processing</span>
                  <span className="ms-3">🔄 Проверка каждые 15 секунд</span>
                </div>
                
                {loading ? (
                  <div className="text-center py-5">
                    <div style={styles.loadingSpinner}></div>
                    <p className="comic-font mt-3">Загрузка заказов...</p>
                  </div>
                ) : orders.length === 0 ? (
                  <div className="text-center py-5">
                    <div className="display-1 mb-3">📭</div>
                    <p className="comic-font">Нет заказов для сборки</p>
                    <small className="text-muted">Ожидание заказов со статусом 'processing'...</small>
                  </div>
                ) : (
                  <div className="flex-grow-1 overflow-auto orders-list">
                    {orders.map((order) => (
                      <div
                        key={order.cart_id}
                        onClick={() => handleSelectOrder(order)}
                        style={selectedOrder?.cart_id === order.cart_id ? 
                          styles.orderCardSelected : 
                          styles.orderCard}
                        className="mb-3 cursor-felt-pen comic-font"
                      >
                        <div className="d-flex justify-content-between align-items-start">
                          <div>
                            <h5 className="fw-bold" style={styles.orderNumber}>
                              Заказ #{order.cart_id}
                            </h5>                      
                            <p className="mb-0 text-muted">
                              <small>Создан: {new Date(order.created_date).toLocaleString('ru-RU')}</small>
                            </p>
                          </div>
                          <div style={(order.status) === 'problem' ? 
                            styles.statusBadgeProblem : 
                            styles.statusBadgeProcessing}>
                            {(order.status) === 'problem' ? '⚠️ Проблема' : '🔄 В обработке'}
                          </div>
                        </div>
                        
                        {selectedOrder?.cart_id === order.cart_id && order.items && order.items.length > 0 && (
                          <div className="mt-3" style={styles.itemsSection}></div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
                
                {/* Статистика внизу */}
                <div className="mt-3 pt-3 border-top" style={styles.statsBottom}>
                  <div className="row text-center">
                    <div className="col-3">
                      <div style={styles.statItem}>
                        <div className="h4 mb-0">{stats.totalOrders}</div>
                        <div className="small">Заказов</div>
                      </div>
                    </div>
                    <div className="col-3">
                      <div style={styles.statItem}>
                        <div className="h4 mb-0">{stats.completedToday}</div>
                        <div className="small">Выполнено</div>
                      </div>
                    </div>
                    <div className="col-3">
                      <div style={styles.statItem}>
                        <div className="h4 mb-0">{stats.averageTime}</div>
                        <div className="small">Время</div>
                      </div>
                    </div>
                    <div className="col-3">
                      <div style={styles.statItem}>
                        <div className="h4 mb-0">{stats.accuracy}</div>
                        <div className="small">Точность</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          {/* Правая часть (30%) - Управление выбранным заказом */}
          <div className="col-4 h-100" style={styles.rightPanel}>
            <div className="h-100 p-4 d-flex flex-column">
              <h2 className="comic-font mb-4">
                Управление заказом
              </h2>
              
              {selectedOrder ? (
                <>
                  {/* Информация о выбранном заказе */}
                  <div className="mb-4" style={styles.selectedOrderInfo}>
                    <h5 className="fw-bold">Заказ #{selectedOrder.cart_id}</h5>                   
                    {selectedOrder.items && selectedOrder.items.length > 0 && (
                      <div className="mb-3">
                        <h6 className="fw-bold mb-2">Товары для сборки:</h6>
                        <ul className="list-unstyled">
                          {selectedOrder.items.map((item, index) => (
                            <li key={index} className="mb-1 ps-2 border-start border-3 border-dark">
                              <strong>{item.product_name}</strong>
                              <span className="ms-2">× {item.quantity}</span>
                              <span className="ms-2 text-muted">(ID: {item.product_id})</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}
                    
                    <div className="mt-2 text-muted">
                      <small>Статус: <strong>{(selectedOrder.status)}</strong></small>
                    </div>
                  </div>
                  
                  {/* Результат проверки */}
                  {checkResult && (
                    <div className={`mb-3 p-3 ${checkResult.allAvailable ? 'bg-light' : 'bg-warning bg-opacity-25'}`}
                         style={styles.checkResult}>
                      <div className="d-flex align-items-center">
                        <div style={styles.resultIcon}>
                          {checkResult.allAvailable ? '✅' : '⚠️'}
                        </div>
                        <div className="ms-2">
                          <strong>{checkResult.message}</strong>
                          {checkResult.unavailableItems && checkResult.unavailableItems.length > 0 && (
                            <div className="mt-2">
                              <small className="fw-bold">Отсутствующие товары:</small>
                              <ul className="mb-0 mt-1">
                                {checkResult.unavailableItems.map((item, idx) => (
                                  <li key={idx}>
                                    <small>{item.product_name} (ID: {item.product_id})</small>
                                  </li>
                                ))}
                              </ul>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                  
                  {/* Кнопки управления */}
                  <div className="mt-auto">
                    {/* Кнопка проверки наличия */}
                    <button
                      onClick={checkProductAvailability}
                      disabled={checking}
                      style={styles.checkButton}
                      className="w-100 mb-3 cursor-felt-pen comic-font"
                    >
                      {checking ? (
                        <>
                          <span style={styles.spinner}></span>
                          Проверка...
                        </>
                      ) : (
                        '🔍 Проверить наличие товара'
                      )}
                    </button>
                    
                    {/* Кнопка "Нет товара" */}
                    <button
                      onClick={reportProductMissing}
                      style={styles.problemButton}
                      className="w-100 mb-3 cursor-felt-pen comic-font"
                    >
                      ❌ Нет товара
                    </button>
                    
                    {/* Кнопка завершения сборки (появляется после проверки) */}
                    {showCompleteButton && (
                      <button
                        onClick={completeOrderCollection}
                        style={styles.completeButton}
                        className="w-100 mb-3 cursor-felt-pen comic-font"
                      >
                        ✅ Завершить сборку
                      </button>
                    )}
                  </div>
                </>
              ) : (
                <div className="text-center py-5">
                  <div className="display-1 mb-3">👈</div>
                  <p className="comic-font">Выберите заказ из списка</p>
                  <small className="text-muted">Нажмите на заказ в левой панели</small>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

// Встроенные стили (без изменений)
const styles = {
  leftPanel: {
    backgroundColor: '#ffffff',
    borderRight: '3px solid #000'
  },
  rightPanel: {
    backgroundColor: '#ffffff'
  },
  loadingSpinner: {
    width: '40px',
    height: '40px',
    margin: '0 auto',
    border: '3px solid #f3f4f6',
    borderTop: '3px solid #000',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite'
  },
  orderCard: {
    padding: '15px',
    border: '2px solid #dee2e6',
    borderRadius: '6px',
    cursor: 'pointer',
    transition: 'all 0.2s ease',
    backgroundColor: '#ffffff'
  },
  orderCardSelected: {
    padding: '15px',
    border: '3px solid #000',
    borderRadius: '6px',
    cursor: 'pointer',
    backgroundColor: '#f8f9fa',
    boxShadow: '3px 3px 0 #000'
  },
  orderNumber: {
    color: '#000',
    marginBottom: '8px'
  },
  statusBadgeProcessing: {
    padding: '5px 10px',
    backgroundColor: '#e7f1ff',
    color: '#0d6efd',
    borderRadius: '15px',
    fontSize: '12px',
    fontWeight: 'bold',
    display: 'inline-block'
  },
  statusBadgeProblem: {
    padding: '5px 10px',
    backgroundColor: '#f8d7da',
    color: '#dc3545',
    borderRadius: '15px',
    fontSize: '12px',
    fontWeight: 'bold',
    display: 'inline-block'
  },
  itemsSection: {
    backgroundColor: '#f8f9fa',
    padding: '10px',
    borderRadius: '4px',
    borderLeft: '3px solid #000'
  },
  statsBottom: {
    backgroundColor: '#f8f9fa',
    borderRadius: '6px'
  },
  statItem: {
    padding: '5px'
  },
  selectedOrderInfo: {
    backgroundColor: '#f8f9fa',
    padding: '15px',
    borderRadius: '6px',
    border: '2px solid #dee2e6'
  },
  checkResult: {
    borderRadius: '6px',
    border: '2px solid #dee2e6'
  },
  resultIcon: {
    fontSize: '24px'
  },
  spinner: {
    display: 'inline-block',
    width: '16px',
    height: '16px',
    marginRight: '8px',
    border: '2px solid #ffffff',
    borderTop: '2px solid transparent',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite'
  },
  checkButton: {
    padding: '12px',
    backgroundColor: '#000',
    color: 'white',
    border: 'none',
    borderRadius: '6px',
    fontWeight: 'bold',
    fontSize: '16px',
    transition: 'all 0.2s ease'
  },
  problemButton: {
    padding: '12px',
    backgroundColor: '#dc3545',
    color: 'white',
    border: 'none',
    borderRadius: '6px',
    fontWeight: 'bold',
    fontSize: '16px',
    transition: 'all 0.2s ease'
  },
  completeButton: {
    padding: '12px',
    backgroundColor: '#198754',
    color: 'white',
    border: 'none',
    borderRadius: '6px',
    fontWeight: 'bold',
    fontSize: '16px',
    transition: 'all 0.2s ease'
  }
};

export default CollectorApp;