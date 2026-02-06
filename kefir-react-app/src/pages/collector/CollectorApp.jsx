// CollectorApp.jsx - полная версия с поддержкой starаyoshibka и Reshenie
import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import './CollectorApp.css';

const CollectorApp = () => {
  const [orders, setOrders] = useState([]);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [stats, setStats] = useState({
    totalOrders: 0,
    completedToday: 0,
    averageTime: '15 мин',
    accuracy: '100%'
  });

  // СОСТОЯНИЯ ДЛЯ МОДАЛЬНОГО ОКНА
  const [showItemCheckModal, setShowItemCheckModal] = useState(false);
  const [itemStatuses, setItemStatuses] = useState({}); // {index: 'есть'/'нет'/'unknown'}
  const [fixedStatuses, setFixedStatuses] = useState({}); // Фиксированные статусы из БД

  // Получение токена в правильном формате
  const getAuthToken = () => {
    const token = localStorage.getItem('token') || 
                  localStorage.getItem('authToken') ||
                  sessionStorage.getItem('token');
    return token;
  };

  // Формирование заголовков с правильным форматом Authorization
  const getAuthHeaders = () => {
    const token = getAuthToken();
    if (!token) {
      console.warn('Токен не найден');
      return {};
    }

    console.log('Токен из хранилища:', token);
    
    let authHeader;
    if (token.startsWith('Bearer ')) {
      authHeader = token;
    } else if (token.startsWith('auth-')) {
      authHeader = `Bearer ${token}`;
    } else if (token.includes('.')) {
      authHeader = `Bearer ${token}`;
    } else {
      authHeader = token;
    }
    
    console.log('Authorization header:', authHeader);
    
    return {
      headers: {
        'Authorization': authHeader,
        'Content-Type': 'application/json'
      }
    };
  };

  // Проверка, является ли пользователь starаyoshibka
  const checkIfStarayoshibka = () => {
    try {
      const userData = JSON.parse(localStorage.getItem('user') || '{}');
      const username = userData.username || userData.id || '';
      const isStar = username.includes('starаyoshibka');
      console.log('👤 Проверка пользователя starаyoshibka:', username, 'isStar:', isStar);
      return isStar;
    } catch (error) {
      console.error('Ошибка проверки starаyoshibka:', error);
      return false;
    }
  };

  // Проверка, является ли пользователь Reshenie
  const checkIfReshenie = () => {
    try {
      const userData = JSON.parse(localStorage.getItem('user') || '{}');
      const username = userData.username || '';
      const isReshenie = username === 'Reshenie'; // ТОЧНОЕ совпадение
      console.log('👤 Проверка пользователя Reshenie:', username, 'isReshenie:', isReshenie);
      return isReshenie;
    } catch (error) {
      console.error('Ошибка проверки Reshenie:', error);
      return false;
    }
  };

  // Получение фиксированных статусов из БД для starаyoshibka
  const fetchFixedStatuses = async (cartId) => {
    if (!checkIfStarayoshibka()) {
      console.log('Не starаyoshibka, пропускаем загрузку фиксированных статусов');
      return {};
    }

    try {
      console.log('⭐ starаyoshibka: загружаем фиксированные статусы для заказа #', cartId);
      
      const response = await axios.get(
        `http://localhost:8080/api/collector/cart/${cartId}/nalichie-status`,
        getAuthHeaders()
      );
      
      if (response.data.success && response.data.nalichieStatuses) {
        const statuses = {};
        response.data.nalichieStatuses.forEach(status => {
          statuses[status.productId] = status.nalichie;
        });
        
        console.log(`✅ Загружено ${Object.keys(statuses).length} фиксированных статусов`);
        return statuses;
      }
    } catch (error) {
      console.error('Ошибка загрузки фиксированных статусов:', error);
    }
    
    return {};
  };

  // Загрузка заказов
  const fetchOrders = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      
      const headers = getAuthHeaders();
      console.log('Запрашиваем заказы с headers:', headers);

      const response = await axios.get(
        'http://localhost:8080/api/collector/processing-orders', 
        headers
      );
      
      console.log('Ответ от сервера:', response.data);
      
      if (response.data.success) {
        const newOrders = response.data.orders || [];
        console.log('Получено заказов:', newOrders.length);
        
        setOrders(newOrders);
        setStats(prev => ({
          ...prev,
          totalOrders: newOrders.length
        }));
        
        if (newOrders.length > 0 && !selectedOrder) {
          setSelectedOrder(newOrders[0]);
          // Загружаем фиксированные статусы для выбранного заказа
          if (checkIfStarayoshibka()) {
            const fixed = await fetchFixedStatuses(newOrders[0].cart_id);
            setFixedStatuses(fixed);
          }
        }
        
        if (selectedOrder && !newOrders.find(o => o.cart_id === selectedOrder.cart_id)) {
          if (newOrders.length > 0) {
            setSelectedOrder(newOrders[0]);
            // Загружаем фиксированные статусы для нового выбранного заказа
            if (checkIfStarayoshibka()) {
              const fixed = await fetchFixedStatuses(newOrders[0].cart_id);
              setFixedStatuses(fixed);
            }
          } else {
            setSelectedOrder(null);
            setFixedStatuses({});
          }
          resetItemCheck();
        }
      } else {
        console.warn('Сервер вернул success: false', response.data);
        setMockData();
      }
    } catch (error) {
      console.error('Ошибка загрузки заказов:', error);
      
      if (error.response) {
        console.error('Статус ошибки:', error.response.status);
        console.error('Данные ошибки:', error.response.data);
        
        if (error.response.status === 401) {
          setError('Ошибка авторизации. Проверьте токен.');
        } else if (error.response.status === 404) {
          setError('Эндпоинт не найден. Проверьте URL.');
        } else {
          setError(`Ошибка сервера: ${error.response.status}`);
        }
      } else if (error.request) {
        setError('Нет ответа от сервера. Проверьте подключение.');
      } else {
        setError(`Ошибка: ${error.message}`);
      }
      
      setMockData();
    } finally {
      setLoading(false);
    }
  }, [selectedOrder]);

  // Моковые данные для демонстрации
  const setMockData = () => {
    console.log('Используем моковые данные');
    
    const mockOrders = [
      { 
        cart_id: 40, 
        client_id: 23, 
        client_name: 'Тестовый Клиент',
        client_email: 'test@example.com',
        status: 'processing',
        created_date: new Date().toISOString(),
        item_count: 3,
        total_items: 4,
        items: [
          { 
            id: 1, 
            product_id: 1, 
            product_name: 'Ноутбук ASUS ROG', 
            quantity: 1, 
            price: 85000.00,
            warehouse: 'skladodin'
          },
          { 
            id: 2, 
            product_id: 6, 
            product_name: 'Игровая мышь Razer DeathAdder V3', 
            quantity: 2, 
            price: 7999.00,
            warehouse: 'skladdva'
          },
          { 
            id: 3, 
            product_id: 7, 
            product_name: 'Игровые наушники SteelSeries Arctis Nova 7', 
            quantity: 1, 
            price: 15999.00,
            warehouse: 'skladtri'
          }
        ]
      }
    ];
    
    setOrders(mockOrders);
    if (!selectedOrder && mockOrders.length > 0) {
      setSelectedOrder(mockOrders[0]);
    }
    
    setStats(prev => ({
      ...prev,
      totalOrders: mockOrders.length
    }));
  };

  // Инициализация приложения
  useEffect(() => {
    const initializeApp = async () => {
      try {
        console.log('Инициализация CollectorApp...');
        
        const token = getAuthToken();
        if (!token) {
          console.warn('Токен не найден. Используем моковые данные.');
          setError('Требуется авторизация. Войдите в систему.');
          setMockData();
          return;
        }
        
        console.log('Токен найден, загружаем заказы...');
        await fetchOrders();
        
      } catch (error) {
        console.error('Ошибка инициализации:', error);
        setMockData();
      }
    };

    initializeApp();
    
    const intervalId = setInterval(fetchOrders, 15000);
    
    return () => clearInterval(intervalId);
  }, [fetchOrders]);

  // Сброс проверки товаров
  const resetItemCheck = () => {
    setShowItemCheckModal(false);
    setItemStatuses({});
  };

  // Обработка выбора заказа
  const handleSelectOrder = async (order) => {
    console.log('Выбран заказ:', order.cart_id);
    setSelectedOrder(order);
    
    // Загружаем фиксированные статусы для starаyoshibka
    if (checkIfStarayoshibka()) {
      const fixed = await fetchFixedStatuses(order.cart_id);
      setFixedStatuses(fixed);
    } else {
      setFixedStatuses({});
    }
    
    resetItemCheck();
  };

  // Открыть модальное окно проверки
  const openItemCheckModal = async () => {
    if (!selectedOrder || !selectedOrder.items || selectedOrder.items.length === 0) {
      alert('Нет товаров для проверки');
      return;
    }
    
    // Особое сообщение для Reshenie
    if (checkIfReshenie()) {
      alert('🔍 Режим Reshenie: Вы должны проверить ВСЕ товары перед завершением сборки.');
    }
    
    setShowItemCheckModal(true);
    
    // Инициализируем статусы
    const initialStatuses = {};
    const isStar = checkIfStarayoshibka();
    
    selectedOrder.items.forEach((item, index) => {
      if (isStar && fixedStatuses[item.product_id] === 'есть') {
        // Для starаyoshibka: устанавливаем фиксированные статусы
        initialStatuses[index] = 'есть';
        console.log(`⭐ Товар ${item.product_name} имеет фиксированный статус "есть"`);
      } else {
        initialStatuses[index] = 'unknown';
      }
    });
    
    setItemStatuses(initialStatuses);
    
    console.log('Открыто модальное окно для заказа:', selectedOrder.cart_id);
    console.log('Статусы инициализированы:', initialStatuses);
  };

  // Проверка, имеет ли товар фиксированный статус 'есть'
  const hasFixedEстьStatus = (productId) => {
    const isStar = checkIfStarayoshibka();
    const isFixed = isStar && fixedStatuses[productId] === 'есть';
    console.log(`Проверка фиксированного статуса: productId=${productId}, isStar=${isStar}, isFixed=${isFixed}`);
    return isFixed;
  };

  // Изменить статус товара с проверкой склада
  const toggleItemStatus = async (index, status) => {
    if (!selectedOrder || !selectedOrder.items[index]) return;
    
    const item = selectedOrder.items[index];
    const isStar = checkIfStarayoshibka();
    
    // ========== БЛОКИРОВКА ДЛЯ starаyoshibka ==========
    if (isStar && status === 'нет' && hasFixedEстьStatus(item.product_id)) {
      alert('❌ Этот товар имеет фиксированный статус "есть" и не может быть изменён на "нет".\n' +
            'Такие товары были проверены ранее в процессе работы с заказом.');
      console.log(`🚫 starаyoshibka: попытка изменить фиксированный товар ${item.product_name} на "нет"`);
      return;
    }
    
    // ========== СТАНДАРТНАЯ ЛОГИКА ==========
    if (status === 'есть') {
      try {
        const userData = JSON.parse(localStorage.getItem('user') || '{}');
        const collectorId = userData.username || userData.id || 'sborshikodin';
        
        console.log('🔍 Проверка товара для сборщика:', collectorId);
        
        const response = await axios.post(
          'http://localhost:8080/api/collector/check-item-in-warehouse', 
          {
            productId: item.product_id,
            collectorId: collectorId
          },
          getAuthHeaders()
        );
        
        console.log('Ответ от сервера при проверке товара:', response.data);
        
        if (response.data.available) {
          setItemStatuses(prev => ({
            ...prev,
            [index]: prev[index] === status ? 'unknown' : status
          }));
          console.log(`✅ Товар ${item.product_name} отмечен как "есть"`);
          
          alert(`✅ Товар "${item.product_name}" есть на складе ${response.data.warehouseTable}`);
        } else {
          alert(`❌ Товар "${item.product_name}" отсутствует на складе ${response.data.warehouseTable || 'вашем'}!`);
        }
      } catch (error) {
        console.error('Ошибка проверки товара:', error);
        
        if (error.response?.status === 404) {
          alert('Эндпоинт проверки товара не найден. Обратитесь к администратору.');
        } else {
          // Демо-режим
          setItemStatuses(prev => ({
            ...prev,
            [index]: prev[index] === status ? 'unknown' : status
          }));
          console.log(`Товар ${item.product_name} отмечен как "есть" (демо-режим)`);
          alert(`⚠️ Сервер проверки недоступен. Товар "${item.product_name}" отмечен в демо-режиме.`);
        }
      }
    } else {
      // Для статуса "нет" просто переключаем
      setItemStatuses(prev => ({
        ...prev,
        [index]: prev[index] === status ? 'unknown' : status
      }));
      console.log(`Товар ${item.product_name} отмечен как "нет"`);
    }
  };

  // Проверка можно ли нажать "Нет товара"
  const canReportMissing = () => {
    const isReshenie = checkIfReshenie();
    
    if (isReshenie) {
      // Reshenie: можно отправить "Нет товара" если есть хотя бы один "нет"
      return Object.values(itemStatuses).some(status => status === 'нет');
    } else {
      // Обычные сборщики: как было
      return Object.values(itemStatuses).some(status => status === 'нет');
    }
  };

  // Проверка можно ли нажать "Завершить сборку"
  const canCompleteCollection = () => {
    const isReshenie = checkIfReshenie();
    
    if (isReshenie) {
      // Reshenie: ВСЕ товары должны быть "есть", НИ ОДНОГО "нет" или "unknown"
      if (!selectedOrder || !selectedOrder.items) return false;
      
      const totalItems = selectedOrder.items.length;
      const countEсть = Object.values(itemStatuses).filter(s => s === 'есть').length;
      const countНет = Object.values(itemStatuses).filter(s => s === 'нет').length;
      const countUnknown = Object.values(itemStatuses).filter(s => s === 'unknown').length;
      
      const canComplete = countEсть === totalItems && countНет === 0 && countUnknown === 0;
      
      console.log(`🔍 Reshenie проверка завершения: всего=${totalItems}, есть=${countEсть}, нет=${countНет}, unknown=${countUnknown}, можно=${canComplete}`);
      
      return canComplete;
    } else {
      // Обычные сборщики: хотя бы один товар "есть"
      return Object.values(itemStatuses).some(status => status === 'есть');
    }
  };

  // Кнопка "Нет товара" - отправка проблемы в офис
  const reportMissingItems = async () => {
    if (!selectedOrder || !canReportMissing()) return;
    
const missingItems = selectedOrder.items.filter((_, index) => itemStatuses[index] === 'нет');

    try {
      // Собираем все товары со статусом 'нет'
      
      
      // Собираем товары со статусом 'есть' для starаyoshibka
      const availableItems = selectedOrder.items.filter((_, index) => itemStatuses[index] === 'есть');
      
      console.log('📦 Отправка проблемы в офис:');
      console.log('- Отсутствует товаров:', missingItems.length);
      console.log('- Доступно товаров:', availableItems.length);
      
      const response = await axios.post(
        'http://localhost:8080/api/collector/report-missing-items',
        {
          cartId: selectedOrder.cart_id,
          missingItems: missingItems.map(item => ({
            productId: item.product_id,
            productName: item.product_name,
            quantity: item.quantity,
            warehouse: item.warehouse
          })),
          availableItems: availableItems.map(item => ({
            productId: item.product_id,
            productName: item.product_name,
            quantity: item.quantity,
            warehouse: item.warehouse
          }))
        },
        getAuthHeaders()
      );
      
      if (response.data.success) {
        const isStar = checkIfStarayoshibka();
        const isReshenie = checkIfReshenie();
        
        let message = `⚠️ Проблема отправлена в офис!\nОтсутствует ${missingItems.length} товар(ов)`;
        
        if (isStar && response.data.availableItemsUpdated > 0) {
          message += `\n⭐ ${response.data.availableItemsUpdated} товаров отмечены как "есть" для starаyoshibka`;
        }
        
        if (isReshenie) {
          message += `\n🔍 Reshenie: заказ отправлен на доработку`;
        }
        
        alert(message);
        
        // Закрываем модальное окно
        setShowItemCheckModal(false);
        
        // Удаляем заказ из списка
        const filteredOrders = orders.filter(order => order.cart_id !== selectedOrder.cart_id);
        setOrders(filteredOrders);
        
        if (filteredOrders.length > 0) {
          setSelectedOrder(filteredOrders[0]);
        } else {
          setSelectedOrder(null);
        }
        
        resetItemCheck();
        setFixedStatuses({});
        
        // Обновляем статистику
        setStats(prev => ({
          ...prev,
          totalOrders: filteredOrders.length
        }));
        
        console.log('Проблема отправлена в офис');
      }
    } catch (error) {
      console.error('Ошибка отправки проблемы:', error);
      alert('Ошибка отправки. Используется демо-режим.');
      
      alert(`⚠️ ДЕМО: Проблема отправлена в офис!\nОтсутствует ${missingItems.length} товар(ов)`);
      setShowItemCheckModal(false);
    }
  };

  // Кнопка "Завершить сборку"
  const completeOrderCollection = async () => {
    if (!selectedOrder || !canCompleteCollection()) return;
    
    const isReshenie = checkIfReshenie();
    
    // Дополнительная проверка для Reshenie
    if (isReshenie) {
      const totalItems = selectedOrder.items.length;
      const checkedItems = Object.values(itemStatuses).filter(s => s !== 'unknown').length;
      
      if (checkedItems !== totalItems) {
        alert('❌ Reshenie: Вы должны проверить ВСЕ товары перед завершением сборки!');
        return;
      }
    }
    
const availableItems = selectedOrder.items.filter((_, index) => itemStatuses[index] === 'есть');

    try {
      // Собираем все товары со статусом 'есть'
      
      
      const response = await axios.post(
        'http://localhost:8080/api/collector/complete-with-selected-items',
        {
          cartId: selectedOrder.cart_id,
          availableItems: availableItems.map(item => ({
            productId: item.product_id,
            productName: item.product_name,
            quantity: item.quantity,
            warehouse: item.warehouse
          }))
        },
        getAuthHeaders()
      );
      
      if (response.data.success) {
        const message = isReshenie 
          ? `✅ Reshenie: Заказ #${selectedOrder.cart_id} полностью проверен и собран!\nВсе ${availableItems.length} товаров проверены.`
          : `✅ Заказ #${selectedOrder.cart_id} собран!\nСобрано ${availableItems.length} из ${selectedOrder.items.length} товаров`;
        
        alert(message);
        
        // Закрываем модальное окно
        setShowItemCheckModal(false);
        
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
        
        resetItemCheck();
        setFixedStatuses({});
        
        console.log('Заказ завершен');
      }
    } catch (error) {
      console.error('Ошибка завершения заказа:', error);
      
      // Демо-режим
      alert(`✅ ДЕМО: Заказ #${selectedOrder.cart_id} собран!\nСобрано ${availableItems.length} из ${selectedOrder.items.length} товаров`);
      setShowItemCheckModal(false);
      
      const filteredOrders = orders.filter(order => order.cart_id !== selectedOrder.cart_id);
      setOrders(filteredOrders);
      
      if (filteredOrders.length > 0) {
        setSelectedOrder(filteredOrders[0]);
      } else {
        setSelectedOrder(null);
      }
      
      resetItemCheck();
      setFixedStatuses({});
    }
  };

  // Рендер модального окна
  const renderItemCheckModal = () => {
    if (!showItemCheckModal || !selectedOrder || !selectedOrder.items) return null;

    const totalItems = selectedOrder.items.length;
    const checkedCount = Object.values(itemStatuses).filter(s => s !== 'unknown').length;
    const availableCount = Object.values(itemStatuses).filter(s => s === 'есть').length;
    const missingCount = Object.values(itemStatuses).filter(s => s === 'нет').length;
    const unknownCount = Object.values(itemStatuses).filter(s => s === 'unknown').length;
    
    const isStar = checkIfStarayoshibka();
    const isReshenie = checkIfReshenie();
    const fixedCount = Object.keys(fixedStatuses).length;

    return (
      <div style={styles.modalOverlay}>
        <div style={styles.modalContent}>
          {/* Шапка модального окна */}
          <div style={styles.modalHeader}>
            <h3 className="comic-font mb-0">
              📦 Проверка товаров заказа #{selectedOrder.cart_id}
              {isReshenie && (
                <span style={{
                  marginLeft: '10px',
                  fontSize: '14px',
                  backgroundColor: '#0d6efd',
                  color: 'white',
                  padding: '2px 8px',
                  borderRadius: '4px'
                }}>
                  🔍 Reshenie
                </span>
              )}
            </h3>
            <button
              onClick={() => setShowItemCheckModal(false)}
              style={styles.closeButton}
              className="cursor-felt-pen"
            >
              ×
            </button>
          </div>

          {/* Тело модального окна */}
          <div style={styles.modalBody}>
            {/* Информация для special пользователей */}
            {(isStar && fixedCount > 0) || isReshenie ? (
              <div style={{
                backgroundColor: isReshenie ? '#cce5ff' : '#d4edda',
                border: `1px solid ${isReshenie ? '#b8daff' : '#c3e6cb'}`,
                borderRadius: '6px',
                padding: '10px 15px',
                marginBottom: '15px',
                fontSize: '14px'
              }}>
                {isReshenie ? (
                  <div>
                    <strong>🔍 Режим Reshenie:</strong> Вы должны проверить ВСЕ товары перед завершением сборки.
                    <br />
                    <small>Проверено: {checkedCount} из {totalItems} товаров</small>
                  </div>
                ) : (
                  <div>
                    <strong>⭐ Режим starаyoshibka:</strong> Обнаружено {fixedCount} товаров с фиксированным статусом "есть". 
                    Эти товары нельзя изменить на "нет".
                  </div>
                )}
              </div>
            ) : null}

            {/* Статистика */}
            <div style={styles.modalStats}>
              <div style={styles.statCard}>
                <div style={styles.statNumber}>{totalItems}</div>
                <div style={styles.statLabel}>Всего товаров</div>
              </div>
              <div style={styles.statCard}>
                <div style={styles.statNumber}>{checkedCount}</div>
                <div style={styles.statLabel}>Проверено</div>
              </div>
              <div style={styles.statCard}>
                <div style={{...styles.statNumber, color: '#198754'}}>{availableCount}</div>
                <div style={styles.statLabel}>✅ Есть</div>
              </div>
              <div style={styles.statCard}>
                <div style={{...styles.statNumber, color: '#dc3545'}}>{missingCount}</div>
                <div style={styles.statLabel}>❌ Нет</div>
              </div>
              {isReshenie && (
                <div style={styles.statCard}>
                  <div style={{...styles.statNumber, color: unknownCount > 0 ? '#ffc107' : '#6c757d'}}>
                    {unknownCount}
                  </div>
                  <div style={styles.statLabel}>➖ Осталось</div>
                </div>
              )}
            </div>

            {/* Список товаров */}
            <div style={styles.itemsList}>
              {selectedOrder.items.map((item, index) => {
                const hasFixedEсть = hasFixedEстьStatus(item.product_id);
                const isFixedAndEсть = hasFixedEсть && itemStatuses[index] === 'есть';
                const currentStatus = itemStatuses[index] || 'unknown';
                
                return (
                  <div key={index} style={{
                    ...styles.itemRow,
                    backgroundColor: hasFixedEсть ? '#f8f9fa' : '#ffffff',
                    borderLeft: hasFixedEсть ? '4px solid #28a745' : 'none'
                  }}>
                    <div style={styles.itemInfo}>
                      <strong>{item.product_name}</strong>
                      {hasFixedEсть && (
                        <span style={{
                          marginLeft: '10px',
                          fontSize: '12px',
                          color: '#28a745',
                          fontWeight: 'bold'
                        }}>
                          ⭐ Фиксированный статус
                        </span>
                      )}
                      <div style={styles.itemDetails}>
                        <span>ID: {item.product_id}</span>
                        <span>Кол-во: {item.quantity} шт.</span>
                        <span>Цена: {item.price} руб.</span>
                        <span style={{ 
                          backgroundColor: '#e9ecef',
                          padding: '2px 8px',
                          borderRadius: '4px',
                          fontSize: '12px'
                        }}>
                          🏪 Склад: {item.warehouse || 'основной'}
                        </span>
                      </div>
                    </div>
                    
                    <div style={styles.itemActions}>
                      <button
                        onClick={() => toggleItemStatus(index, 'есть')}
                        style={{
                          ...styles.statusButton,
                          backgroundColor: currentStatus === 'есть' ? '#198754' : '#f8f9fa',
                          color: currentStatus === 'есть' ? 'white' : '#198754',
                          borderColor: '#198754'
                        }}
                        className="cursor-felt-pen comic-font"
                      >
                        ✅ Есть
                      </button>
                      
                      <button
                        onClick={() => toggleItemStatus(index, 'нет')}
                        style={{
                          ...styles.statusButton,
                          backgroundColor: currentStatus === 'нет' ? '#dc3545' : '#f8f9fa',
                          color: currentStatus === 'нет' ? 'white' : '#dc3545',
                          borderColor: '#dc3545',
                          opacity: isFixedAndEсть ? 0.5 : 1,
                          cursor: isFixedAndEсть ? 'not-allowed' : 'pointer'
                        }}
                        className="cursor-felt-pen comic-font"
                        disabled={isFixedAndEсть}
                        title={isFixedAndEсть ? 'Фиксированный статус, нельзя изменить' : ''}
                      >
                        ❌ Нет
                      </button>
                      
                      <div style={styles.currentStatus}>
                        {currentStatus === 'есть' && (
                          <span style={{color: '#198754'}}>
                            ✅ Есть {hasFixedEсть ? ' (фикс.)' : ''}
                          </span>
                        )}
                        {currentStatus === 'нет' && <span style={{color: '#dc3545'}}>❌ Нет</span>}
                        {currentStatus === 'unknown' && <span style={{color: '#6c757d'}}>➖ Не проверен</span>}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Футер модального окна с кнопками */}
          <div style={styles.modalFooter}>
            <button
              onClick={reportMissingItems}
              disabled={!canReportMissing()}
              style={{
                ...styles.reportButton,
                opacity: canReportMissing() ? 1 : 0.5,
                cursor: canReportMissing() ? 'pointer' : 'not-allowed',
                flex: 1
              }}
              className="cursor-felt-pen comic-font"
              title={isReshenie && !canReportMissing() ? 
                'Отметьте отсутствующие товары как "нет" для отправки в офис' : ''}
            >
              🚨 Нет товара (отправить в офис)
            </button>
            
            <button
              onClick={completeOrderCollection}
              disabled={!canCompleteCollection()}
              style={{
                ...styles.completeButton,
                opacity: canCompleteCollection() ? 1 : 0.5,
                cursor: canCompleteCollection() ? 'pointer' : 'not-allowed',
                flex: 1
              }}
              className="cursor-felt-pen comic-font"
              title={isReshenie && !canCompleteCollection() ? 
                'Вы должны проверить ВСЕ товары перед завершением' : ''}
            >
              {isReshenie ? '✅ Завершить (все проверены)' : '✅ Завершить сборку'}
            </button>
            
            <button
              onClick={() => setShowItemCheckModal(false)}
              style={styles.cancelButton}
              className="cursor-felt-pen comic-font"
            >
              ❌ Закрыть
            </button>
          </div>
        </div>
      </div>
    );
  };

  // Основной рендер
  return (
    <div className="collector-app">
      <div className="container-fluid h-100 p-0 m-0">
        <div className="row g-0 h-100">
          {/* Левая часть - Список заказов */}
          <div className="col-8 h-100" style={styles.leftPanel}>
            <div className="h-100 position-relative">
              <div className="black-corner">
                <div className="black-corner-icon">📦</div>
                <div className="black-corner-text">Заказы</div>
              </div>
              
              <div className="p-4 pt-5 h-100 d-flex flex-column">
                <h2 className="comic-font mb-3">
                  Заказы для сборки
                  {checkIfStarayoshibka() && (
                    <span style={{
                      marginLeft: '10px',
                      fontSize: '14px',
                      backgroundColor: '#ffc107',
                      color: '#000',
                      padding: '2px 8px',
                      borderRadius: '4px'
                    }}>
                      ⭐ starаyoshibka
                    </span>
                  )}
                  {checkIfReshenie() && (
                    <span style={{
                      marginLeft: '10px',
                      fontSize: '14px',
                      backgroundColor: '#0d6efd',
                      color: 'white',
                      padding: '2px 8px',
                      borderRadius: '4px'
                    }}>
                      🔍 Reshenie
                    </span>
                  )}
                  <span className="badge bg-dark ms-2">{orders.length}</span>
                </h2>
                
                {error && (
                  <div className="alert alert-warning mb-3">
                    <strong>Внимание:</strong> {error}
                    <br />
                    <small>Используется демо-режим с тестовыми данными</small>
                  </div>
                )}
                
                <div className="comic-font mb-2">
                  Статус: <span className="text-dark fw-bold">processing</span>
                  <span className="ms-3">🔄 Автообновление каждые 15 секунд</span>
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
                    <div className="mt-3">
                      <button 
                        onClick={setMockData}
                        className="btn btn-outline-dark btn-sm"
                      >
                        Показать тестовые данные
                      </button>
                    </div>
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
                            <p className="mb-1">
                              <span style={styles.clientIcon}>👤</span>
                              <strong>{order.client_name}</strong>
                            </p>
                            <p className="mb-1">
                              <span style={styles.emailIcon}>📧</span>
                              {order.client_email}
                            </p>
                            <p className="mb-1">
                              <span style={styles.itemIcon}>📋</span>
                              Товаров: {order.item_count} ({order.total_items} шт.)
                            </p>
                            <div className="mb-1">
                              <span style={styles.warehouseIcon}>🏪</span>
                              <small>Склады: {[...new Set(order.items.map(i => i.warehouse))].join(', ')}</small>
                            </div>
                            <p className="mb-0 text-muted">
                              <small>Создан: {new Date(order.created_date).toLocaleString('ru-RU')}</small>
                            </p>
                          </div>
                          <div style={styles.statusBadgeProcessing}>
                            🔄 В обработке
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
                
                {/* Статистика */}
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
          
          {/* Правая часть - Управление */}
          <div className="col-4 h-100" style={styles.rightPanel}>
            <div className="h-100 p-4 d-flex flex-column">
              <h2 className="comic-font mb-4">Управление заказом</h2>
              
              {selectedOrder ? (
                <>
                  {/* Информация о заказе */}
                  <div className="mb-4" style={styles.selectedOrderInfo}>
                    <h5 className="fw-bold">Заказ #{selectedOrder.cart_id}</h5>
                    <p className="mb-1">
                      <strong>Клиент:</strong> {selectedOrder.client_name}
                    </p>
                    <p className="mb-3">
                      <strong>Email:</strong> {selectedOrder.client_email}
                    </p>
                    
                    {selectedOrder.items && selectedOrder.items.length > 0 && (
                      <div className="mb-3">
                        <h6 className="fw-bold mb-2">Товары для сборки:</h6>
                        <ul className="list-unstyled">
                          {selectedOrder.items.map((item, index) => (
                            <li key={index} className="mb-1 ps-2 border-start border-3 border-dark">
                              <strong>{item.product_name}</strong>
                              {hasFixedEстьStatus(item.product_id) && (
                                <span style={{
                                  marginLeft: '5px',
                                  fontSize: '11px',
                                  color: '#28a745',
                                  fontWeight: 'bold'
                                }}>
                                  ⭐ фикс.
                                </span>
                              )}
                              <span className="ms-2">× {item.quantity}</span>
                              <span className="ms-2 text-muted">(ID: {item.product_id})</span>
                              <br />
                              <small className="text-muted">
                                <span className="badge bg-light text-dark">
                                  🏪 {item.warehouse || 'основной'}
                                </span>
                              </small>
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}
                    
                    <div className="mt-2 text-muted">
                      <small>Статус: <strong>{selectedOrder.status}</strong></small>
                    </div>
                  </div>
                  
                  {/* Кнопка открытия модального окна */}
                  <div className="mt-auto">
                    <button
                      onClick={openItemCheckModal}
                      style={styles.checkButton}
                      className="w-100 mb-3 cursor-felt-pen comic-font"
                    >
                      🔍 Проверить товары ({selectedOrder.items?.length || 0})
                    </button>
                    
                    <div className="alert alert-info small">
                      <strong>Информация:</strong> Товары распределены по складам.
                      При проверке "Есть" система проверит наличие на нужном складе.
                    </div>
                  </div>
                </>
              ) : (
                <div className="text-center py-5">
                  <div className="display-1 mb-3">👈</div>
                  <p className="comic-font">Выберите заказ из списка</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
      
      {/* Модальное окно проверки товаров */}
      {renderItemCheckModal()}
      
      {/* Стили анимации */}
      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        
        @keyframes slideIn {
          from { 
            opacity: 0;
            transform: translateY(-20px) scale(0.95); 
          }
          to { 
            opacity: 1;
            transform: translateY(0) scale(1); 
          }
        }
        
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
};

// Стили
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
  clientIcon: { marginRight: '6px' },
  emailIcon: { marginRight: '6px' },
  itemIcon: { marginRight: '6px' },
  warehouseIcon: { marginRight: '6px' },
  statusBadgeProcessing: {
    padding: '5px 10px',
    backgroundColor: '#e7f1ff',
    color: '#0d6efd',
    borderRadius: '15px',
    fontSize: '12px',
    fontWeight: 'bold',
    display: 'inline-block'
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
  
  // Стили для модального окна
  modalOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
    animation: 'fadeIn 0.3s ease'
  },
  modalContent: {
    backgroundColor: '#fff',
    borderRadius: '12px',
    border: '3px solid #000',
    width: '90%',
    maxWidth: '900px',
    maxHeight: '85vh',
    display: 'flex',
    flexDirection: 'column',
    boxShadow: '0 10px 30px rgba(0, 0, 0, 0.3)',
    animation: 'slideIn 0.3s ease'
  },
  modalHeader: {
    padding: '20px 25px',
    borderBottom: '2px solid #dee2e6',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#f8f9fa',
    borderTopLeftRadius: '8px',
    borderTopRightRadius: '8px'
  },
  modalBody: {
    padding: '25px',
    flexGrow: 1,
    overflowY: 'auto'
  },
  modalFooter: {
    padding: '20px 25px',
    borderTop: '2px solid #dee2e6',
    display: 'flex',
    gap: '15px',
    backgroundColor: '#f8f9fa',
    borderBottomLeftRadius: '8px',
    borderBottomRightRadius: '8px'
  },
  closeButton: {
    background: 'none',
    border: 'none',
    fontSize: '32px',
    cursor: 'pointer',
    color: '#000',
    width: '40px',
    height: '40px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: '50%',
    transition: 'all 0.2s ease'
  },
  
  // Статистика в модальном окне
  modalStats: {
    display: 'flex',
    justifyContent: 'space-between',
    gap: '15px',
    marginBottom: '25px',
    padding: '15px',
    backgroundColor: '#f8f9fa',
    borderRadius: '8px',
    border: '2px solid #dee2e6'
  },
  statCard: {
    flex: 1,
    textAlign: 'center',
    padding: '10px'
  },
  statNumber: {
    fontSize: '24px',
    fontWeight: 'bold',
    color: '#000'
  },
  statLabel: {
    fontSize: '12px',
    color: '#6c757d',
    marginTop: '5px'
  },
  
  // Список товаров
  itemsList: {
    maxHeight: '350px',
    overflowY: 'auto',
    marginBottom: '20px',
    border: '2px solid #dee2e6',
    borderRadius: '8px',
    backgroundColor: '#fff'
  },
  itemRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '15px',
    borderBottom: '1px solid #dee2e6',
    transition: 'background-color 0.2s ease'
  },
  itemInfo: {
    flex: 1,
    minWidth: 0
  },
  itemDetails: {
    fontSize: '13px',
    color: '#6c757d',
    marginTop: '6px',
    display: 'flex',
    flexWrap: 'wrap',
    gap: '15px'
  },
  itemActions: {
    display: 'flex',
    alignItems: 'center',
    gap: '15px',
    flexShrink: 0
  },
  statusButton: {
    padding: '8px 15px',
    border: '2px solid',
    borderRadius: '6px',
    fontWeight: 'bold',
    cursor: 'pointer',
    minWidth: '80px',
    fontSize: '14px',
    transition: 'all 0.2s ease'
  },
  currentStatus: {
    fontSize: '13px',
    minWidth: '140px',
    textAlign: 'center',
    fontWeight: 'bold'
  },
  
  // Кнопки в модальном окне
  reportButton: {
    padding: '14px',
    backgroundColor: '#dc3545',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontWeight: 'bold',
    fontSize: '15px',
    transition: 'all 0.2s ease'
  },
  completeButton: {
    padding: '14px',
    backgroundColor: '#198754',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontWeight: 'bold',
    fontSize: '15px',
    transition: 'all 0.2s ease'
  },
  cancelButton: {
    padding: '14px',
    backgroundColor: '#6c757d',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontWeight: 'bold',
    fontSize: '15px',
    transition: 'all 0.2s ease'
  }
};

export default CollectorApp;