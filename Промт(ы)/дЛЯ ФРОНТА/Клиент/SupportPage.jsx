// src/pages/client/SupportPage.jsx
import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import './SupportPage.css';

const SupportPage = () => {
  // Шаги: 1-выбор заказа, 2-выбор проблемы, 3-товары, 4-решение, 5-завершение, 99-офис
  const [step, setStep] = useState(1);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [selectedProblem, setSelectedProblem] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  
  // Данные
  const [ordersWithProblems, setOrdersWithProblems] = useState([]);
  const [unavailableItems, setUnavailableItems] = useState([]);
  const [selectedItems, setSelectedItems] = useState([]);
  const [totalRefundAmount, setTotalRefundAmount] = useState(0);
  const [actionType, setActionType] = useState(''); // 'refund' или 'recollect'
  const [isProcessing, setIsProcessing] = useState(false);
  const [eligibilityCheck, setEligibilityCheck] = useState(null);

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

  // Проверка eligibility заказа - УПРОЩЕННАЯ ЛОГИКА
  const checkOrderEligibility = useCallback(async (orderId) => {
    const clientId = getClientId();
    if (!clientId) return { eligible: false, reason: 'not_authorized' };
    
    try {
      // 1. Получаем заказы клиента
      const ordersResponse = await axios.get(
        `http://localhost:8080/api/cart/client/${clientId}`
      );
      
      const orders = ordersResponse.data.carts || [];
      const currentOrder = orders.find(o => o.id === orderId);
      
      if (!currentOrder) {
        return { eligible: false, reason: 'order_not_found' };
      }
      
      // 2. Проверяем статус в carts
      const cartStatus = currentOrder.status || '';
      const normalizedStatus = cartStatus.toLowerCase();
      
      // 3. ПРОВЕРЯЕМ ТОЛЬКО completed СТАТУС - УПРОЩЕНИЕ
      const isCompleted = normalizedStatus.includes('completed');
      
      if (!isCompleted) {
        return { 
          eligible: false, 
          reason: 'order_not_completed',
          message: `Заказ должен быть завершен. Текущий статус: ${cartStatus}`
        };
      }
      
      // 4. Проверяем, не является ли заказ уже обработанным по поддержке
      const alreadyProcessedStatuses = [
        'tc', 'taoshibka', 'completed_refund', 'transactioncompleted',
        'tasamaiaoshibka', 'recollecting', 'refunded'
      ];
      
      const isAlreadyProcessed = alreadyProcessedStatuses.some(status => 
        normalizedStatus.includes(status)
      );
      
      if (isAlreadyProcessed) {
        return { 
          eligible: false, 
          reason: 'already_processed',
          message: `Этот заказ уже обработан (статус: ${cartStatus})`
        };
      }
      
      // 5. Получаем недопоставленные товары
      const itemsResponse = await axios.get(
        `http://localhost:8080/api/support/unavailable-items/${clientId}`
      );
      
      const allItems = itemsResponse.data.items || [];
      const orderItems = allItems.filter(item => item.cart_id === orderId);
      
      // 6. Проверяем есть ли unknown товары
      if (orderItems.length === 0) {
        return { 
          eligible: false, 
          reason: 'no_unknown_items',
          message: 'В этом заказе нет недопоставленных товаров'
        };
      }
      
      // 7. Все условия выполнены
      return { 
        eligible: true, 
        reason: 'ok',
        cartStatus: cartStatus,
        unknownItemsCount: orderItems.length,
        order: currentOrder
      };
      
    } catch (error) {
      console.error('Error checking eligibility:', error);
      return { 
        eligible: false, 
        reason: 'error_checking',
        message: 'Ошибка при проверке заказа'
      };
    }
  }, [getClientId]);

  // 1. Загрузка заказов с проблемами - УПРОЩЕННАЯ ФИЛЬТРАЦИЯ
  const loadProblemOrders = useCallback(async () => {
    const clientId = getClientId();
    if (!clientId) {
      setError('Пользователь не авторизован');
      return;
    }

    setLoading(true);
    setError('');
    
    try {
      // Получаем все заказы клиента
      const ordersResponse = await axios.get(
        `http://localhost:8080/api/cart/client/${clientId}`
      );
      
      const allOrders = ordersResponse.data.carts || [];
      
      // Получаем товары с unknown статусом
      const itemsResponse = await axios.get(
        `http://localhost:8080/api/support/unavailable-items/${clientId}`
      );
      
      const unknownItems = itemsResponse.data.items || [];
      
      // Находим заказы, которые имеют unknown товары
      const orderIdsWithUnknown = [...new Set(unknownItems.map(item => item.cart_id))];
      
      // ПОКАЗЫВАЕМ ВСЕ ЗАКАЗЫ С unknown ТОВАРАМИ - УПРОЩЕНИЕ
      const problemOrders = allOrders.filter(order => {
        // Проверяем, есть ли у заказа unknown товары
        const hasUnknownItems = orderIdsWithUnknown.includes(order.id);
        return hasUnknownItems;
      }).map(order => ({
        ...order,
        unknown_items_count: unknownItems.filter(item => item.cart_id === order.id).length
      }));
      
      setOrdersWithProblems(problemOrders);
      
      if (problemOrders.length === 0) {
        // Проверяем, есть ли заказы в принципе
        if (allOrders.length === 0) {
          setError('У вас нет заказов');
        } else {
          const hasUnknownItemsAnywhere = unknownItems.length > 0;
          if (hasUnknownItemsAnywhere) {
            // Есть unknown товары, но нет заказов
            setError('У вас нет заказов с недопоставленными товарами');
          } else {
            setError('У вас нет заказов с недопоставленными товарами');
          }
        }
      }
    } catch (err) {
      console.error('Ошибка при загрузке заказов:', err);
      setError(err.response?.data?.error || err.message || 'Ошибка сети');
    } finally {
      setLoading(false);
    }
  }, [getClientId]);

  // 2. Загрузка недопоставленных товаров для выбранного заказа
  const loadUnavailableItemsForOrder = useCallback(async (cartId) => {
    setLoading(true);
    setError('');
    
    try {
      const clientId = getClientId();
      const response = await axios.get(
        `http://localhost:8080/api/support/unavailable-items/${clientId}`
      );
      
      if (response.data.success) {
        // Фильтруем товары только для выбранного заказа
        const allItems = response.data.items || [];
        const filteredItems = allItems.filter(item => item.cart_id === cartId);
        
        setUnavailableItems(filteredItems);
        
        // Автоматически выбираем все товары со статусом 'unknown'
        const unknownItemIds = filteredItems
          .filter(item => item.nalichie === 'unknown')
          .map(item => item.id);
        
        setSelectedItems(unknownItemIds);
        
        // Рассчитываем общую сумму ТОЛЬКО по unknown товарам
        const total = filteredItems
          .filter(item => item.nalichie === 'unknown')
          .reduce((sum, item) => {
            return sum + (item.price * item.quantity);
          }, 0);
        setTotalRefundAmount(total);
        
        if (filteredItems.length > 0) {
          setStep(3); // Переходим к шагу 3 (показ товаров)
        } else {
          setError('Для этого заказа не найдено недопоставленных товаров');
          setStep(1);
        }
      } else {
        setError(response.data.error || 'Ошибка при проверке товаров');
      }
    } catch (err) {
      console.error('Ошибка при проверке товаров:', err);
      setError(err.response?.data?.error || err.message || 'Ошибка сети');
      setStep(1);
    } finally {
      setLoading(false);
    }
  }, [getClientId]);

  // Инициализация - загружаем заказы с проблемами
  useEffect(() => {
    loadProblemOrders();
  }, [loadProblemOrders]);

  // Обработка выбора заказа
  const handleOrderSelect = (order) => {
    setSelectedOrder(order);
    setStep(2); // Переходим к выбору проблемы
  };

  // Обработка выбора проблемы
  const handleProblemSelect = async (problemType) => {
    setSelectedProblem(problemType);
    
    if (problemType === 'missing_part' && selectedOrder) {
      setLoading(true);
      setError('');
      
      // Проверяем eligibility
      const eligibility = await checkOrderEligibility(selectedOrder.id);
      setEligibilityCheck(eligibility);
      
      if (!eligibility.eligible) {
        setLoading(false);
        
        switch(eligibility.reason) {
          case 'order_not_completed':
            setError(`Этот заказ еще не завершен (статус: "${selectedOrder.status}"). ` +
                    `Для обработки претензии заказ должен быть завершен. ` +
                    `Пожалуйста, свяжитесь с офисом.`);
            setStep(99);
            break;
          case 'no_unknown_items':
            setError('В этом заказе нет недопоставленных товаров.');
            setStep(1);
            break;
          case 'already_processed':
            setError(`Этот заказ уже обработан (статус: "${selectedOrder.status}"). ` +
                    `Пожалуйста, выберите другой заказ.`);
            // Перезагружаем список заказов
            await loadProblemOrders();
            setStep(1);
            break;
          case 'order_not_found':
            setError('Заказ не найден.');
            setStep(1);
            break;
          case 'error_checking':
            setError('Ошибка при проверке заказа. Попробуйте позже.');
            setStep(1);
            break;
          default:
            setError('Невозможно обработать этот заказ. Свяжитесь с офисом.');
            setStep(99);
        }
        return;
      }
      
      // Если подходит - загружаем товары
      await loadUnavailableItemsForOrder(selectedOrder.id);
      setLoading(false);
    }
  };

  // Переключение выбора товара - ТОЛЬКО unknown товары можно выбирать
  const toggleItemSelection = (itemId) => {
    const item = unavailableItems.find(i => i.id === itemId);
    
    // Разрешаем выбирать только unknown товары
    if (item && item.nalichie !== 'unknown') {
      setError('Можно выбирать только недопоставленные товары (статус: unknown)');
      return;
    }
    
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
    
    // ПРОВЕРКА: Все ли выбранные товары имеют статус 'unknown'
    const selectedItemsData = unavailableItems.filter(item => 
      selectedItems.includes(item.id)
    );
    
    const allSelectedAreUnknown = selectedItemsData.every(item => 
      item.nalichie === 'unknown'
    );
    
    if (!allSelectedAreUnknown) {
      setError('Можно обрабатывать только недопоставленные (unknown) товары. Свяжитесь с офисом.');
      setStep(99);
      setIsProcessing(false);
      return;
    }
    
    try {
      let response;
      
      if (action === 'refund') {
        // 1. Вернуть деньги - расчет суммы
        const refundResponse = await axios.post('http://localhost:8080/api/support/refund-items', {
          items: selectedItemsData,
          totalAmount: totalRefundAmount
        });
        
        if (refundResponse.data.success) {
          // 2. Меняем статус заказа на 'tc' (короткая версия) - БЕЗ last_action
          const statusResponse = await axios.post('http://localhost:8080/api/support/update-order-status', {
            cartId: selectedOrder.id,
            newStatus: 'tc'
            // Убрано: action: 'refund'
          });
          
          if (statusResponse.data.success) {
            setSuccess(`${refundResponse.data.totalAmount.toFixed(2)} рублей будет возвращена. Статус заказа изменен на "завершен"`);
            setStep(5);
            // Обновляем список заказов
            setTimeout(() => {
              loadProblemOrders();
              setSelectedOrder(null);
              setSelectedItems([]);
            }, 1000);
          } else {
            // Проверяем, есть ли SQL ошибка
            const errorMsg = statusResponse.data.error || '';
            if (errorMsg.includes('SQL grammar') || errorMsg.includes('PreparedStatementCallback')) {
              setError('Ошибка базы данных при обновлении статуса. Обратитесь в техподдержку.');
            } else {
              setError('Ошибка при изменении статуса заказа: ' + errorMsg);
            }
          }
        } else {
          setError('Ошибка при расчете возврата');
        }
      } else if (action === 'recollect') {
        // 1. Пересобрать заказ - используем новый эндпоинт с коротким статусом
        const recollectResponse = await axios.post('http://localhost:8080/api/support/recollect-order', {
          cartIds: [selectedOrder.id],
          items: selectedItemsData
        });
        
        if (recollectResponse.data.success) {
          // 2. Дополнительно обновляем статус через update-order-status для consistency
          await axios.post('http://localhost:8080/api/support/update-order-status', {
            cartId: selectedOrder.id,
            newStatus: 'taoshibka'
            // Убрано: action: 'recollect'
          });
          
          // ИСПРАВЛЕННОЕ СООБЩЕНИЕ (используем короткий статус)
          setSuccess('Заказ отправлен на повторную сборку! Статус заказа изменен на "ошибка сборки"');
          setStep(5);
          // Обновляем список заказов
          setTimeout(() => {
            loadProblemOrders();
            setSelectedOrder(null);
            setSelectedItems([]);
          }, 1000);
        } else {
          setError('Ошибка при отправке на пересборку: ' + (recollectResponse.data.error || ''));
        }
      }
    } catch (err) {
      console.error('Ошибка при выполнении действия:', err);
      // Проверяем тип ошибки
      const errorMsg = err.response?.data?.error || err.message || 'Ошибка выполнения';
      if (errorMsg.includes('SQL grammar') || errorMsg.includes('PreparedStatementCallback')) {
        setError('Ошибка базы данных. Обратитесь в техподдержку.');
      } else {
        setError(errorMsg);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  // Сброс формы
  const resetForm = () => {
    setStep(1);
    setSelectedOrder(null);
    setSelectedProblem('');
    setUnavailableItems([]);
    setSelectedItems([]);
    setTotalRefundAmount(0);
    setActionType('');
    setError('');
    setSuccess('');
    setEligibilityCheck(null);
    loadProblemOrders();
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
          <div className="step-label">Выбор заказа</div>
        </div>
        <div className={`step ${step >= 2 ? 'active' : ''}`}>
          <div className="step-number">2</div>
          <div className="step-label">Выбор проблемы</div>
        </div>
        <div className={`step ${step >= 3 ? 'active' : ''}`}>
          <div className="step-number">3</div>
          <div className="step-label">Товары</div>
        </div>
        <div className={`step ${step >= 4 ? 'active' : ''}`}>
          <div className="step-number">4</div>
          <div className="step-label">Решение</div>
        </div>
        <div className={`step ${step >= 5 ? 'active' : ''}`}>
          <div className="step-number">5</div>
          <div className="step-label">Завершение</div>
        </div>
        {step === 99 && (
          <div className="step active">
            <div className="step-number">!</div>
            <div className="step-label">Связь с офисом</div>
          </div>
        )}
      </div>

      {loading && (
        <div className="loading-overlay">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Загрузка...</span>
          </div>
          <p>Проверяем заказ...</p>
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
        {/* Шаг 1: Выбор заказа */}
        {step === 1 && (
          <div className="order-selection">
            <h3>Выберите заказ с проблемой:</h3>
            
            {ordersWithProblems.length === 0 ? (
              <div className="empty-state">
                <div className="empty-icon">📦</div>
                <h4>Нет заказов с проблемами</h4>
                <p>{error || 'Все ваши заказы доставлены полностью'}</p>
                <button 
                  className="btn btn-primary mt-3"
                  onClick={loadProblemOrders}
                >
                  Обновить список
                </button>
              </div>
            ) : (
              <div className="orders-list">
                {ordersWithProblems.map(order => (
                  <div 
                    key={order.id}
                    className={`order-card ${selectedOrder?.id === order.id ? 'selected' : ''}`}
                    onClick={() => handleOrderSelect(order)}
                  >
                    <div className="order-header">
                      <h5>Заказ #{order.id}</h5>
                      <span className="order-date">
                        {new Date(order.created_date).toLocaleDateString()}
                      </span>
                    </div>
                    <div className="order-details">
                      <div>
                        <span>Статус: </span>
                        <strong className={
                          order.status.toLowerCase().includes('completed') ? 'text-success' :
                          order.status.toLowerCase().includes('processing') ? 'text-primary' :
                          order.status.toLowerCase().includes('tc') ? 'text-info' :
                          order.status.toLowerCase().includes('taoshibka') ? 'text-warning' :
                          'text-secondary'
                        }>
                          {order.status}
                        </strong>
                      </div>
                      <div>
                        <span>Недопоставлено: </span>
                        <strong>{order.unknown_items_count || 0} товаров</strong>
                      </div>
                    </div>
                    <div className="order-footer">
                      {order.status.toLowerCase().includes('tc') ? (
                        <span className="badge bg-info">Возврат завершен</span>
                      ) : order.status.toLowerCase().includes('taoshibka') ? (
                        <span className="badge bg-warning">На пересборке</span>
                      ) : order.status.toLowerCase().includes('completed') ? (
                        <span className="badge bg-success">Завершен</span>
                      ) : order.unknown_items_count > 0 ? (
                        <span className="badge bg-danger">Требует внимания</span>
                      ) : (
                        <span className="badge bg-secondary">OK</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
            
            <div className="navigation-buttons">
              <button 
                className="btn btn-primary"
                onClick={() => selectedOrder && setStep(2)}
                disabled={!selectedOrder || ordersWithProblems.length === 0}
              >
                Далее →
              </button>
            </div>
          </div>
        )}

        {/* Шаг 2: Выбор проблемы */}
        {step === 2 && selectedOrder && (
          <div className="problem-selection">
            <h3>Выберите тип проблемы для заказа #{selectedOrder.id}:</h3>
            <p className="text-muted">
              Статус заказа: 
              <strong className={
                selectedOrder.status.toLowerCase().includes('completed') ? 'text-success ms-1' :
                selectedOrder.status.toLowerCase().includes('processing') ? 'text-primary ms-1' :
                'text-secondary ms-1'
              }>
                {selectedOrder.status}
              </strong>
            </p>
            
            <div className="problem-options">
              <button
                className={`problem-option ${selectedProblem === 'missing_part' ? 'selected' : ''}`}
                onClick={() => handleProblemSelect('missing_part')}
                disabled={loading}
              >
                <div className="problem-icon">📦</div>
                <div className="problem-text">
                  <h5>Не привезли часть заказа</h5>
                  <p>Если вам доставили не все товары из заказа</p>
                  <p className="text-warning small">
                    <strong>Только для завершенных заказов</strong>
                  </p>
                  {loading && <small>Проверяем условия...</small>}
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
            
            <div className="navigation-buttons">
              <button 
                className="btn btn-outline-secondary"
                onClick={() => setStep(1)}
                disabled={loading}
              >
                ← Назад
              </button>
            </div>
          </div>
        )}

        {/* Шаг 3: Найденные недопоставленные товары */}
        {step === 3 && selectedOrder && eligibilityCheck?.eligible && (
          <div className="items-list">
            <div className="eligibility-badge">
              <span className="badge bg-success">✓ Заказ соответствует условиям возврата</span>
              <p className="text-muted small mt-1">
                Статус заказа: <strong>{selectedOrder.status}</strong> • 
                Недопоставлено: <strong>{unavailableItems.filter(i => i.nalichie === 'unknown').length} товаров</strong>
              </p>
            </div>
            
            <h3>Недопоставленные товары в заказе #{selectedOrder.id}:</h3>
            <p className="text-muted mb-3">
              Выберите <strong>только unknown товары</strong> для возврата/пересборки:
            </p>
            
            <div className="items-container">
              {unavailableItems.map(item => (
                <div 
                  key={item.id} 
                  className={`item-card ${selectedItems.includes(item.id) ? 'selected' : ''} ${item.nalichie !== 'unknown' ? 'not-selectable' : ''}`}
                  onClick={() => toggleItemSelection(item.id)}
                >
                  <div className="form-check">
                    <input
                      type="checkbox"
                      className="form-check-input"
                      checked={selectedItems.includes(item.id)}
                      disabled={item.nalichie !== 'unknown'}
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
                    <small className={
                      item.nalichie === 'unknown' ? 'text-danger' : 'text-success'
                    }>
                      <strong>Статус: {item.nalichie}</strong> • Артикул: {item.product_sku || 'нет'}
                    </small>
                  </div>
                  
                  <div className="item-status">
                    {item.nalichie === 'unknown' ? (
                      <span className="badge bg-danger">Не доставлен</span>
                    ) : (
                      <span className="badge bg-success">Доставлен</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
            
            <div className="total-refund">
              <h5>Общая сумма к возврату (только unknown товары):</h5>
              <h3 className="text-primary">{totalRefundAmount.toFixed(2)}₽</h3>
              <p className="text-muted small">
                Выбрано товаров: {selectedItems.length} / {unavailableItems.filter(i => i.nalichie === 'unknown').length}
              </p>
            </div>
            
            <div className="navigation-buttons">
              <button 
                className="btn btn-outline-secondary"
                onClick={() => setStep(2)}
              >
                ← Назад
              </button>
              <button 
                className="btn btn-primary"
                onClick={() => setStep(4)}
                disabled={selectedItems.length === 0}
              >
                Далее →
              </button>
            </div>
          </div>
        )}

        {/* Шаг 4: Выбор действия */}
        {step === 4 && selectedOrder && eligibilityCheck?.eligible && (
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
                    <li><strong>Статус заказа изменится на: "tc" (завершен)</strong></li>
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
                  <p>Заказать повторную сборку недостающих товаров</p>
                  <ul>
                    <li>Заказ будет собран повторно</li>
                    <li>Бесплатная доставка</li>
                    <li>Срок: 1-2 рабочих дня</li>
                    <li><strong>Статус заказа изменится на: "taoshibka" (ошибка сборки)</strong></li>
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
                onClick={() => setStep(3)}
                disabled={isProcessing}
              >
                ← Назад
              </button>
            </div>
          </div>
        )}

        {/* Шаг 5: Завершение */}
        {step === 5 && (
          <div className="completion-step">
            <div className="success-icon">✅</div>
            <h3>Заявка успешно оформлена!</h3>
            <p>{success}</p>
            
            <div className="completion-details">
              {actionType === 'refund' && (
                <>
                  <p><strong>Номер заявки:</strong> REF-{Date.now().toString().slice(-8)}</p>
                  <p><strong>Заказ №:</strong> {selectedOrder?.id}</p>
                  <p><strong>Сумма возврата:</strong> {totalRefundAmount.toFixed(2)}₽</p>
                  <p><strong>Новый статус заказа:</strong> <span className="badge bg-info">tc (завершен)</span></p>
                  <p><strong>Срок возврата:</strong> 3-5 рабочих дней</p>
                </>
              )}
              
              {actionType === 'recollect' && (
                <>
                  <p><strong>Номер заявки:</strong> RECOL-{Date.now().toString().slice(-8)}</p>
                  <p><strong>Заказ:</strong> #{selectedOrder?.id}</p>
                  <p><strong>Новый статус:</strong> <span className="badge bg-warning">taoshibka (ошибка сборки)</span></p>
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

        {/* Шаг 99: Связь с офисом */}
        {step === 99 && (
          <div className="office-contact-step">
            <div className="office-icon">📞</div>
            <h3>Свяжем с офисом</h3>
            <p>Для решения вашего вопроса требуется помощь оператора</p>
            
            <div className="office-details">
              <div className="office-contact">
                <p><strong>📱 Телефон:</strong> 8-800-123-45-67</p>
                <p><strong>✉️ Email:</strong> support@kefir-system.ru</p>
                <p><strong>🕒 Время работы:</strong> Пн-Пт 9:00-18:00</p>
              </div>
            </div>
            
            {selectedOrder && (
              <div className="order-info">
                <h5>Информация о заказе:</h5>
                <p><strong>Заказ №:</strong> {selectedOrder.id}</p>
                <p><strong>Статус:</strong> {selectedOrder.status}</p>
                <p><strong>Дата:</strong> {new Date(selectedOrder.created_date).toLocaleDateString()}</p>
                {eligibilityCheck?.reason === 'order_not_completed' && (
                  <p className="text-warning">⚠️ Заказ не завершен. Для обработки претензии заказ должен иметь статус "completed"</p>
                )}
              </div>
            )}
            
            <div className="action-buttons">
              <button 
                className="btn btn-primary"
                onClick={() => window.location.href = 'tel:88001234567'}
              >
                📞 Позвонить
              </button>
              <button 
                className="btn btn-outline-primary"
                onClick={() => setStep(1)}
              >
                ← Назад к выбору
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default SupportPage;