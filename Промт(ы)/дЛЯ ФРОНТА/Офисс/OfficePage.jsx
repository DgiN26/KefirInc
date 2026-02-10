// src/pages/office/OfficePage.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

const OfficePage = ({ onLogout }) => {
    const [problems, setProblems] = useState([]);
    const [taoshibkaOrders, setTaoshibkaOrders] = useState([]);
    const [selectedProblem, setSelectedProblem] = useState(null);
    const [selectedTaoshibkaOrder, setSelectedTaoshibkaOrder] = useState(null);
    const [taoshibkaItems, setTaoshibkaItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [taoshibkaLoading, setTaoshibkaLoading] = useState(false);
    const [emailMessage, setEmailMessage] = useState('');
    const [actionMessage, setActionMessage] = useState('');
    const [showTaoshibkaModal, setShowTaoshibkaModal] = useState(false);
    const [findCollectorsLoading, setFindCollectorsLoading] = useState(false);

    // Polling для проверки новых проблем каждые 15 секунд
    useEffect(() => {
        const fetchProblems = async () => {
            try {
                // Ищем заказы со статусом "problem" в таблице carts
                const response = await axios.get('http://localhost:8080/api/office/problems/active');
                
                if (response.data.success) {
                    const newProblems = response.data.problems || [];
                    setProblems(newProblems);
                    
                    if (newProblems.length > 0 && !selectedProblem) {
                        // Загружаем полную информацию о товарах для первой проблемы
                        await loadProblemDetails(newProblems[0]);
                    }
                }
            } catch (error) {
                console.error('Ошибка загрузки проблем:', error);
                // Заглушка для демонстрации
                const demoProblems = [{
                    id: 1,
                    order_id: 1001,
                    client_id: 1,
                    client_name: 'Иван Иванов',
                    client_email: 'ivan@example.com',
                    collector_id: 'COLLECTOR_1',
                    details: 'Товар отсутствует на складе',
                    status: 'PENDING',
                    created_at: new Date().toISOString()
                }];
                setProblems(demoProblems);
                if (!selectedProblem) {
                    await loadProblemDetails(demoProblems[0]);
                }
            } finally {
                setLoading(false);
            }
        };

        // Polling для taoshibka заказов каждые 15 секунд
        const fetchTaoshibkaOrders = async () => {
            try {
                setTaoshibkaLoading(true);
                const response = await axios.get('http://localhost:8080/api/office/taoshibka-orders');
                
                if (response.data.success) {
                    const newOrders = response.data.orders || [];
                    setTaoshibkaOrders(newOrders);
                    
                    if (newOrders.length > 0 && !selectedTaoshibkaOrder) {
                        // Автоматически загружаем первый заказ
                        handleSelectTaoshibkaOrder(newOrders[0]);
                    }
                }
            } catch (error) {
                console.error('Ошибка загрузки taoshibka заказов:', error);
                // Заглушка для демонстрации
                setTaoshibkaOrders([]);
            } finally {
                setTaoshibkaLoading(false);
            }
        };

        // Первоначальная загрузка
        fetchProblems();
        fetchTaoshibkaOrders();
        
        // Polling каждые 15 секунд
        const intervalId = setInterval(() => {
            fetchProblems();
            fetchTaoshibkaOrders();
        }, 15000);
        
        return () => clearInterval(intervalId);
    }, []);

    // Загрузка деталей проблемы с реальными товарами
    const loadProblemDetails = async (problem) => {
        try {
            const cartId = problem.cart_id || problem.order_id;
            if (!cartId) return;
            
            const response = await axios.get(`http://localhost:8080/api/office/problems/full-info/${cartId}`);
            
            if (response.data.success) {
                const detailedProblem = {
                    ...problem,
                    ...response.data,
                    id: problem.id || cartId,
                    order_id: cartId
                };
                setSelectedProblem(detailedProblem);
                generateEmailMessage(detailedProblem);
            } else {
                setSelectedProblem(problem);
                generateEmailMessage(problem);
            }
        } catch (error) {
            console.error('Ошибка загрузки деталей:', error);
            setSelectedProblem(problem);
            generateEmailMessage(problem);
        }
    };

    // Генерация email сообщения с реальными товарами
    const generateEmailMessage = (data) => {
        if (!data) return;
        
        // Если пришла строка с сервера - используем ее
        if (typeof data === 'string') {
            setEmailMessage(data);
            return;
        }
        
        const clientName = data.client?.client_name || data.client_name || 'Клиент';
        const cartId = data.cart?.cart_id || data.cart_id || data.order_id || 'N/A';
        
        // Формируем сообщение с товарами из data.items
        if (data.items && data.items.length > 0) {
            const itemsList = data.items.map(item => 
                `• ${item.product_name || `Товар #${item.product_id}`} (Артикул: ${item.product_sku || 'N/A'}, Количество: ${item.quantity}, Цена: ${parseFloat(item.price || 0).toFixed(2)} ₽)`
            ).join('\n');
            
            const message = `Уважаемый(ая) ${clientName},

В вашем заказе #${cartId} обнаружена проблема.

Товары в заказе:
${itemsList}

Тип проблемы: Отсутствует товар на складе

Пожалуйста, выберите один из вариантов:
1. Продолжить сборку без проблемного товара
2. Отменить весь заказ
3. Подождать до появления товара

Для ответа используйте этот email или позвоните по телефону:
📞 +7 (495) 123-45-67

С уважением,
Команда KEFIR Logistics`;
            
            setEmailMessage(message);
        } else {
            // Если нет детальной информации о товарах
            const message = `Уважаемый(ая) ${clientName},

В вашем заказе #${cartId} обнаружена проблема.

Тип проблемы: Отсутствует товар на складе

Пожалуйста, выберите один из вариантов:
1. Продолжить сборку без этого товара
2. Отменить весь заказ
3. Подождать до появления товара

Для ответа используйте этот email или позвоните по телефону:
📞 +7 (495) 123-45-67

С уважением,
Команда KEFIR Logistics`;
            
            setEmailMessage(message);
        }
    };

    // Обработка выбора проблемы
    const handleSelectProblem = async (problem) => {
        await loadProblemDetails(problem);
    };

    // Обработка выбора taoshibka заказа
    const handleSelectTaoshibkaOrder = async (order) => {
        setSelectedTaoshibkaOrder(order);
        try {
            const response = await axios.get(`http://localhost:8080/api/office/taoshibka-orders/${order.cart_id}/items`);
            
            if (response.data.success) {
                setTaoshibkaItems(response.data.unknownItems || []);
                
                // Открываем модальное окно
                openTaoshibkaModal();
            }
        } catch (error) {
            console.error('Ошибка загрузки товаров заказа:', error);
            setTaoshibkaItems([]);
        }
    };

    // Отправка email клиенту
    const sendClientEmail = async () => {
        if (!selectedProblem) return;
        
        try {
            const response = await axios.post('http://localhost:8080/api/office/notify-client', {
                orderId: selectedProblem.order_id,
                message: emailMessage,
                clientEmail: selectedProblem.client_email,
                clientName: selectedProblem.client_name
            });
            
            if (response.data.success) {
                alert(`📧 Email отправлен клиенту: ${selectedProblem.client_email}`);
                
                // Обновляем статус проблемы
                const updatedProblems = problems.map(p => 
                    p.order_id === selectedProblem.order_id 
                    ? { ...p, status: 'NOTIFIED' }
                    : p
                );
                setProblems(updatedProblems);
                setSelectedProblem(prev => ({ ...prev, status: 'NOTIFIED' }));
            }
        } catch (error) {
            console.error('Ошибка отправки email:', error);
            alert('Ошибка отправки email');
        }
    };

    // Принятие решения
    const makeDecision = async (decision) => {
        if (!selectedProblem) return;
        
        try {
            const response = await axios.post('http://localhost:8080/api/office/make-decision', {
                orderId: selectedProblem.order_id,
                decision: decision,
                comments: `Решение принято офисом: ${decision}`
            });
            
            if (response.data.success) {
                alert(`✅ Решение принято! Статус заказа обновлен.`);
                
                // Удаляем проблему из списка
                const updatedProblems = problems.filter(p => p.order_id !== selectedProblem.order_id);
                setProblems(updatedProblems);
                
                if (updatedProblems.length > 0) {
                    await loadProblemDetails(updatedProblems[0]);
                } else {
                    setSelectedProblem(null);
                    setEmailMessage('');
                }
            }
        } catch (error) {
            console.error('Ошибка принятия решения:', error);
            alert('Ошибка при принятии решения');
        }
    };

    // Модальное окно для taoshibka заказов
    const openTaoshibkaModal = () => {
        setShowTaoshibkaModal(true);
    };

    const closeTaoshibkaModal = () => {
        setShowTaoshibkaModal(false);
        setSelectedTaoshibkaOrder(null);
        setTaoshibkaItems([]);
    };

    // Функция поиска сборщиков (автоматическая)
    const handleFindCollectors = async () => {
        if (!selectedTaoshibkaOrder) {
            alert('Сначала выберите заказ из списка');
            return;
        }
        
        try {
            setFindCollectorsLoading(true);
            const orderId = selectedTaoshibkaOrder.cart_id;
            
            const response = await axios.post(
                `http://localhost:8080/api/office/taoshibka-orders/${orderId}/find-collectors`
            );
            
            if (response.data.success) {
                if (response.data.found) {
                    // УСПЕХ: найден склад со всеми товарами, статус автоматически обновлен
                    const warehouseName = response.data.warehouseDisplay || response.data.warehouse;
                    
                    // 1. Удаляем заказ из списка (т.к. статус изменился на processing)
                    setTaoshibkaOrders(prev => 
                        prev.filter(order => order.cart_id !== orderId)
                    );
                    
                    // 2. Закрываем модальное окно
                    closeTaoshibkaModal();
                    
                    // 3. Показываем уведомление об успехе
                    showSuccessNotification(orderId, warehouseName, response.data);
                    
                } else {
                    // НЕУДАЧА: ни на одном складе нет всех товаров
                    showFailureNotification(orderId, response.data);
                }
            } else {
                alert('Ошибка при поиске склада: ' + (response.data.error || 'Неизвестная ошибка'));
            }
            
        } catch (error) {
            console.error('Error finding collectors:', error);
            const errorMessage = error.response?.data?.error || error.message;
            alert('Ошибка при поиске сборщиков: ' + errorMessage);
        } finally {
            setFindCollectorsLoading(false);
        }
    };

    // Показать уведомление об успешном поиске
    const showSuccessNotification = (orderId, warehouseName, responseData) => {
        const notificationHtml = `
            <div class="fixed top-6 right-6 bg-green-50 border-4 border-green-500 rounded-xl p-6 z-50 animate-slideInRight shadow-2xl max-w-md">
                <div class="flex items-start gap-4">
                    <div class="text-4xl">✅</div>
                    <div class="flex-1">
                        <h3 class="text-xl font-bold text-green-800 mb-2">Заказ обработан!</h3>
                        <p class="text-green-700 mb-1">Заказ <strong>#${orderId}</strong> переведен в статус "processing"</p>
                        <p class="text-green-600 mb-3">Склад: <strong>${warehouseName}</strong></p>
                        
                        <div class="bg-green-100 border border-green-300 rounded-lg p-3 mb-3">
                            <p class="text-sm text-green-800">
                                <span class="font-bold">✓ Все товары найдены</span><br>
                                Статус обновлен автоматически
                            </p>
                        </div>
                        
                        <button onclick="this.parentElement.parentElement.parentElement.remove()" 
                                class="mt-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 text-sm font-medium">
                            Закрыть
                        </button>
                    </div>
                </div>
            </div>
        `;
        
        const notification = document.createElement('div');
        notification.innerHTML = notificationHtml;
        document.body.appendChild(notification.firstElementChild);
        
        // Автоматически скрыть через 10 секунд
        setTimeout(() => {
            if (notification.firstElementChild && notification.firstElementChild.parentElement) {
                notification.firstElementChild.remove();
            }
        }, 10000);
    };

    // Показать уведомление о неудачном поиске
    const showFailureNotification = (orderId, responseData) => {
        const warehouseChecks = responseData.warehouseChecks || [];
        
        const notificationHtml = `
            <div class="fixed top-6 right-6 bg-red-50 border-4 border-red-500 rounded-xl p-6 z-50 animate-slideInRight shadow-2xl max-w-md">
                <div class="flex items-start gap-4">
                    <div class="text-4xl">❌</div>
                    <div class="flex-1">
                        <h3 class="text-xl font-bold text-red-800 mb-2">Склад не найден</h3>
                        <p class="text-red-700 mb-3">Для заказа <strong>#${orderId}</strong></p>
                        
                        <div class="bg-red-100 border border-red-300 rounded-lg p-3 mb-3">
                            <p class="text-sm text-red-800">
                                <span class="font-bold">Не удалось найти склад со всеми товарами</span><br>
                                Проверьте наличие на складах вручную
                            </p>
                        </div>
                        
                        <div class="mb-4">
                            <p class="text-sm font-medium text-gray-700 mb-2">Проверенные склады:</p>
                            <div class="space-y-2">
                                ${warehouseChecks.map(check => `
                                    <div class="flex justify-between items-center">
                                        <span class="text-sm">${check.warehouseDisplay || check.warehouseName}</span>
                                        <span class="px-2 py-1 text-xs rounded-full ${check.allAvailable ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}">
                                            ${check.availableItemsCount || 0}/${check.totalItems || 0}
                                        </span>
                                    </div>
                                `).join('')}
                            </div>
                        </div>
                        
                        <button onclick="this.parentElement.parentElement.parentElement.remove()" 
                                class="mt-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 text-sm font-medium">
                            Закрыть
                        </button>
                    </div>
                </div>
            </div>
        `;
        
        const notification = document.createElement('div');
        notification.innerHTML = notificationHtml;
        document.body.appendChild(notification.firstElementChild);
        
        setTimeout(() => {
            if (notification.firstElementChild && notification.firstElementChild.parentElement) {
                notification.firstElementChild.remove();
            }
        }, 15000);
    };

    // Тестирование эндпоинтов
    const testEndpoints = async () => {
        try {
            const response = await axios.get('http://localhost:8080/api/office/taoshibka-test');
            console.log('Taoshibka test response:', response.data);
            
            // Показать результат теста в модальном окне
            showTestResults(response.data);
            
        } catch (error) {
            console.error('Test error:', error);
            alert('❌ Ошибка при тестировании эндпоинтов');
        }
    };

    // Показать результаты теста
    const showTestResults = (testData) => {
        const modalHtml = `
            <div class="fixed inset-0 bg-black bg-opacity-80 flex items-center justify-center z-50 p-4">
                <div class="bg-white border-4 border-black rounded-xl p-8 max-w-4xl w-full max-h-[90vh] overflow-y-auto">
                    <div class="flex justify-between items-center mb-6">
                        <h2 class="text-2xl font-bold text-black">🧪 Результаты тестирования</h2>
                        <button onclick="this.closest('.fixed').remove()" 
                                class="w-10 h-10 bg-black text-white rounded-full flex items-center justify-center">
                            ✕
                        </button>
                    </div>
                    
                    <div class="space-y-6">
                        ${testData.test ? `
                            <div>
                                <h3 class="font-bold text-lg mb-3">📊 Таблицы в базе данных</h3>
                                <div class="grid grid-cols-2 gap-4">
                                    ${Object.entries(testData.test.tables || {}).map(([table, exists]) => `
                                        <div class="p-3 border rounded-lg ${exists ? 'bg-green-50 border-green-300' : 'bg-red-50 border-red-300'}">
                                            <div class="flex justify-between items-center">
                                                <span class="font-medium">${table}</span>
                                                <span class="px-2 py-1 text-xs rounded-full ${exists ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}">
                                                    ${exists ? '✅ Есть' : '❌ Нет'}
                                                </span>
                                            </div>
                                            ${testData.test.row_counts && testData.test.row_counts[table] !== undefined ? 
                                                `<div class="text-sm text-gray-600 mt-1">Записей: ${testData.test.row_counts[table]}</div>` : ''}
                                        </div>
                                    `).join('')}
                                </div>
                            </div>
                            
                            ${testData.test.carts_statuses ? `
                                <div>
                                    <h3 class="font-bold text-lg mb-3">📦 Статусы заказов (carts)</h3>
                                    <div class="bg-gray-50 border rounded-lg p-4">
                                        ${Array.isArray(testData.test.carts_statuses) ? testData.test.carts_statuses.map(status => `
                                            <div class="flex justify-between items-center py-2 border-b last:border-b-0">
                                                <span>${status.status || 'N/A'}</span>
                                                <span class="font-bold">${status.count || 0}</span>
                                            </div>
                                        `).join('') : '<p class="text-gray-500">Данные не доступны</p>'}
                                    </div>
                                </div>
                            ` : ''}
                            
                            ${testData.test.nalichie_types ? `
                                <div>
                                    <h3 class="font-bold text-lg mb-3">🏷️ Типы наличия (cart_items)</h3>
                                    <div class="bg-gray-50 border rounded-lg p-4">
                                        ${Array.isArray(testData.test.nalichie_types) ? testData.test.nalichie_types.map(item => `
                                            <div class="flex justify-between items-center py-2 border-b last:border-b-0">
                                                <span>${item.nalichie || 'N/A'}</span>
                                                <span class="font-bold">${item.count || 0}</span>
                                            </div>
                                        `).join('') : '<p class="text-gray-500">Данные не доступны</p>'}
                                    </div>
                                </div>
                            ` : ''}
                        ` : '<p class="text-gray-500">Нет данных тестирования</p>'}
                    </div>
                    
                    <div class="mt-8 text-center">
                        <button onclick="this.closest('.fixed').remove()" 
                                class="px-6 py-3 bg-black text-white rounded-lg hover:bg-gray-800 font-medium">
                            Закрыть
                        </button>
                    </div>
                </div>
            </div>
        `;
        
        const modalContainer = document.createElement('div');
        modalContainer.innerHTML = modalHtml;
        document.body.appendChild(modalContainer.firstElementChild);
    };

    return (
        <div className="office-page" style={styles.officePage}>
            {/* Встроенные стили */}
            <style>{`
                @keyframes pageFlip {
                    0% {
                        transform: perspective(1000px) rotateY(0deg);
                        opacity: 1;
                    }
                    50% {
                        transform: perspective(1000px) rotateY(-90deg);
                        opacity: 0.5;
                    }
                    100% {
                        transform: perspective(1000px) rotateY(0deg);
                        opacity: 1;
                    }
                }
                
                @keyframes slideInRight {
                    from {
                        transform: translateX(100%);
                        opacity: 0;
                    }
                    to {
                        transform: translateX(0);
                        opacity: 1;
                    }
                }
                
                .page-transition {
                    animation: pageFlip 0.6s ease-in-out;
                }
                
                .animate-slideInRight {
                    animation: slideInRight 0.5s ease-out;
                }
                
                .hand-drawn-border {
                    border: 3px solid #000 !important;
                    border-radius: 8px !important;
                    box-shadow: 
                        4px 4px 0 #000,
                        8px 8px 0 rgba(0,0,0,0.1) !important;
                }
                
                .handwritten {
                    background: linear-gradient(to right, transparent, transparent 50%, rgba(0,0,0,0.1) 50%) !important;
                    background-size: 4px 1px !important;
                    background-repeat: repeat-x !important;
                    background-position: 0 100% !important;
                }
                
                .cursor-felt-pen {
                    cursor: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32"><path d="M8 28l16-16-4-4L4 24z" fill="black"/><path d="M24 4l4 4-16 16-4-4z" fill="%23f59e0b"/></svg>') 4 28, auto !important;
                }
                
                @import url('https://fonts.googleapis.com/css2?family=Comic+Neue:wght@400;700&display=swap');
                
                .comic-font {
                    font-family: 'Comic Neue', cursive, sans-serif !important;
                }
                
                .wavy-border {
                    position: relative;
                    border: none;
                }
                
                .wavy-border::before {
                    content: '';
                    position: absolute;
                    top: -3px;
                    left: -3px;
                    right: -3px;
                    bottom: -3px;
                    border: 3px solid #000;
                    border-radius: 10px;
                    animation: wavy 3s infinite linear;
                }
                
                @keyframes wavy {
                    0%, 100% {
                        clip-path: polygon(0% 0%, 100% 0%, 100% 100%, 0% 100%);
                    }
                    25% {
                        clip-path: polygon(0% 5%, 100% 0%, 95% 100%, 0% 100%);
                    }
                    50% {
                        clip-path: polygon(0% 0%, 100% 5%, 100% 100%, 5% 100%);
                    }
                    75% {
                        clip-path: polygon(5% 0%, 100% 0%, 100% 95%, 0% 100%);
                    }
                }
                
                /* Стили для черной кляксы */
                .exit-blob {
                    animation: blobPulse 2s infinite alternate ease-in-out;
                }
                
                @keyframes blobPulse {
                    0% {
                        border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
                        transform: scale(1);
                    }
                    50% {
                        border-radius: 30% 60% 70% 40% / 50% 60% 30% 60%;
                        transform: scale(1.05);
                    }
                    100% {
                        border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
                        transform: scale(1);
                    }
                }
                
                .exit-blob:hover {
                    animation: blobHover 0.5s forwards;
                }
                
                @keyframes blobHover {
                    0% {
                        transform: scale(1);
                    }
                    100% {
                        transform: scale(1.15) rotate(5deg);
                    }
                }

                /* Стили для модального окна */
                .modal-overlay {
                    position: fixed;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                    background-color: rgba(0, 0, 0, 0.8);
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    z-index: 1000;
                    animation: fadeIn 0.3s ease-out;
                }

                @keyframes fadeIn {
                    from { opacity: 0; }
                    to { opacity: 1; }
                }

                .modal-content {
                    background: white;
                    border: 4px solid black;
                    border-radius: 12px;
                    width: 90%;
                    height: 90%;
                    overflow: hidden;
                    animation: slideUp 0.4s ease-out;
                    box-shadow: 8px 8px 0 black, 16px 16px 0 rgba(0,0,0,0.2);
                }

                @keyframes slideUp {
                    from {
                        transform: translateY(100px);
                        opacity: 0;
                    }
                    to {
                        transform: translateY(0);
                        opacity: 1;
                    }
                }

                .modal-close {
                    position: absolute;
                    top: 20px;
                    right: 20px;
                    background: black;
                    color: white;
                    border: none;
                    width: 50px;
                    height: 50px;
                    border-radius: 50%;
                    font-size: 24px;
                    cursor: pointer;
                    z-index: 10;
                    box-shadow: 3px 3px 0 #f59e0b;
                }

                .modal-close:hover {
                    background: #333;
                }
            `}</style>
            
            {/* ЖИРНАЯ ЧЕРНАЯ КЛЯКСА для выхода */}
            <button
                onClick={onLogout}
                style={styles.exitBlob}
                className="cursor-felt-pen exit-blob"
                title="ВЫХОД"
            />
            
            {/* Левая часть (33%) - Список проблем от сборщиков */}
            <div className="w-[33%] p-6">
                {/* Прямоугольник с черным правым верхним углом */}
                <div className="relative h-full" style={styles.problemContainer}>
                    {/* Черный угол - рисованный стиль */}
                    <div style={styles.blackCorner}>
                        <div style={styles.cornerIcon}>⚠️</div>
                        <div style={styles.cornerText}>Проблема</div>
                    </div>
                    
                    <div className="p-6 pt-10 h-full overflow-y-auto">
                        <h2 className="text-2xl font-bold mb-6 comic-font" style={styles.title}>
                            📝 Сообщения от сборщиков
                        </h2>
                        
                        {loading ? (
                            <div className="text-center py-10">
                                <div style={styles.loadingSpinner}></div>
                                <p className="comic-font mt-4">Загрузка проблем...</p>
                            </div>
                        ) : problems.length === 0 ? (
                            <div className="text-center py-10">
                                <div style={styles.emptyState}>
                                    <span style={{ fontSize: '3rem' }}>📭</span>
                                    <p className="comic-font mt-4 text-gray-600">Нет активных проблем</p>
                                </div>
                            </div>
                        ) : (
                            <div className="space-y-4">
                                {problems.map((problem, index) => (
                                    <div
                                        key={problem.id || problem.order_id || index}
                                        onClick={() => handleSelectProblem(problem)}
                                        style={selectedProblem?.order_id === problem.order_id ? 
                                            styles.problemCardSelected : 
                                            styles.problemCard}
                                        className="cursor-felt-pen comic-font"
                                    >
                                        <div className="flex justify-between">
                                            <div>
                                                <h3 className="font-bold text-lg" style={styles.orderNumber}>
                                                    Заказ #{problem.order_id || problem.cart_id}
                                                </h3>
                                                <p className="text-gray-600 mt-1">
                                                    <span style={styles.clientIcon}>👤</span>
                                                    {problem.client_name} 
                                                    <span style={styles.emailIcon}> 📧</span>
                                                    {problem.client_email}
                                                </p>
                                                <p className="mt-2 handwritten" style={styles.problemDetails}>
                                                    {problem.details}
                                                </p>
                                            </div>
                                            <div className="text-right">
                                                <div style={problem.status === 'PENDING' ? 
                                                    styles.statusBadgePending : 
                                                    styles.statusBadgeNotified}>
                                                    {problem.status === 'PENDING' ? '🆕 Новая' : '📧 Уведомлен'}
                                                </div>
                                                <p className="text-sm text-gray-500 mt-1">
                                                    {new Date(problem.created_at).toLocaleTimeString()}
                                                </p>
                                            </div>
                                        </div>
                                        <div className="mt-2 text-sm text-gray-600 comic-font">
                                            <span style={styles.collectorIcon}>👷</span> Сборщик: {problem.collector_id}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>
            
            {/* Центральная часть (33%) - Форма отправки email */}
            <div className="w-[33%] p-6">
                <div className="h-full flex flex-col" style={styles.emailContainer}>
                    <div className="mb-6">
                        <h2 className="text-2xl font-bold comic-font" style={styles.title}>
                            ✉️ Форма для клиента
                        </h2>
                        <p className="text-gray-600 comic-font handwritten">Отсутствует товар, ваше решение</p>
                    </div>
                    
                    {selectedProblem ? (
                        <>
                            <div className="mb-4">
                                <label className="block text-sm font-medium mb-2 comic-font">
                                    🧑‍💼 Клиент
                                </label>
                                <div className="p-3" style={styles.clientInfo}>
                                    <p className="font-medium comic-font">{selectedProblem.client_name}</p>
                                    <p className="text-gray-600 comic-font">{selectedProblem.client_email}</p>
                                </div>
                            </div>
                            
                            <div className="flex-1 mb-4">
                                <label className="block text-sm font-medium mb-2 comic-font">
                                    📝 Сообщение с товарами
                                </label>
                                <textarea
                                    value={emailMessage}
                                    onChange={(e) => setEmailMessage(e.target.value)}
                                    className="w-full h-full min-h-[200px] p-3 comic-font"
                                    style={styles.textarea}
                                    placeholder="Текст email с товарами будет сгенерирован автоматически..."
                                />
                            </div>
                            
                            <button
                                onClick={sendClientEmail}
                                disabled={selectedProblem.status === 'NOTIFIED'}
                                style={selectedProblem.status === 'NOTIFIED' ? 
                                    styles.sendButtonDisabled : 
                                    styles.sendButton}
                                className="cursor-felt-pen comic-font"
                            >
                                {selectedProblem.status === 'NOTIFIED' 
                                 ? '✅ Email отправлен' 
                                 : '✉️ Отправить email'}
                            </button>
                            
                            <div className="grid grid-cols-2 gap-3 mt-4">
                                <button
                                    onClick={() => makeDecision('APPROVE_WITHOUT_PRODUCT')}
                                    style={styles.approveButton}
                                    className="cursor-felt-pen comic-font"
                                >
                                    ✅ Одобрить
                                </button>
                                <button
                                    onClick={() => makeDecision('CANCEL_ORDER')}
                                    style={styles.cancelButton}
                                    className="cursor-felt-pen comic-font"
                                >
                                    ❌ Отменить
                                </button>
                            </div>
                            
                            <div className="mt-3">
                                <button
                                    onClick={() => makeDecision('WAIT_FOR_PRODUCT')}
                                    style={styles.waitButton}
                                    className="cursor-felt-pen comic-font w-full"
                                >
                                    ⏳ Ожидать товар
                                </button>
                            </div>
                        </>
                    ) : (
                        <div className="text-center py-10">
                            <div style={styles.emptySelection}>
                                <span style={{ fontSize: '3rem' }}>👈</span>
                                <p className="comic-font mt-4 text-gray-600">Выберите проблему из списка</p>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* Правая часть (33%) - Решение проблем (taoshibka заказы) */}
            <div className="w-[33%] p-6">
                <div className="h-full flex flex-col" style={styles.taoshibkaContainer}>
                    {/* Черный угол - рисованный стиль */}
                    <div style={styles.taoshibkaBlackCorner}>
                        <div style={styles.cornerIcon}>🛠️</div>
                        <div style={styles.cornerText}>Система</div>
                    </div>
                    
                    <div className="p-4 pt-10">
                        <h2 className="text-2xl font-bold comic-font mb-2" style={styles.title}>
                            🛠️ Решение проблем
                        </h2>
                        <p className="text-gray-600 comic-font handwritten mb-4">
                            Заказы с неизвестным наличием товаров
                        </p>
                    </div>
                    
                    <div className="flex-1 overflow-y-auto px-4 pb-4">
                        {taoshibkaLoading ? (
                            <div className="text-center py-10">
                                <div style={styles.loadingSpinner}></div>
                                <p className="comic-font mt-4">Загрузка заказов...</p>
                            </div>
                        ) : taoshibkaOrders.length === 0 ? (
                            <div className="text-center py-10">
                                <div style={styles.emptyState}>
                                    <span style={{ fontSize: '3rem' }}>😊</span>
                                    <p className="comic-font mt-4 text-gray-600">Нет заказов для решения</p>
                                    <p className="text-sm text-gray-500 mt-2">
                                        Все заказы обработаны
                                    </p>
                                </div>
                            </div>
                        ) : (
                            <div className="space-y-3">
                                {taoshibkaOrders.map((order, index) => (
                                    <div
                                        key={order.cart_id || index}
                                        onClick={() => handleSelectTaoshibkaOrder(order)}
                                        style={selectedTaoshibkaOrder?.cart_id === order.cart_id ? 
                                            styles.taoshibkaCardSelected : 
                                            styles.taoshibkaCard}
                                        className="cursor-felt-pen comic-font"
                                    >
                                        <div className="flex justify-between items-center">
                                            <div>
                                                <h3 className="font-bold text-lg" style={styles.orderNumber}>
                                                    Заказ #{order.cart_id}
                                                </h3>
                                                <p className="text-gray-600 text-sm mt-1">
                                                    <span style={styles.clientIcon}>👤</span>
                                                    {order.client_name || `Клиент #${order.client_id}`}
                                                </p>
                                                {order.client_email && (
                                                    <p className="text-gray-500 text-xs mt-1">
                                                        📧 {order.client_email}
                                                    </p>
                                                )}
                                            </div>
                                            <div className="text-right">
                                                <div style={styles.taoshibkaBadge}>
                                                    🔴 {order.unknown_count || 0} unknown
                                                </div>
                                                <p className="text-xs text-gray-500 mt-1">
                                                    {new Date(order.created_date).toLocaleDateString()}
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                    
                    <div className="p-4 border-t border-gray-300">
                        <button
                            onClick={testEndpoints}
                            style={styles.testButton}
                            className="cursor-felt-pen comic-font w-full mb-3"
                        >
                            🧪 Тест эндпоинтов
                        </button>
                        <div className="text-center text-xs text-gray-500">
                            Обновление: каждые 15 секунд
                        </div>
                    </div>
                </div>
            </div>

            {/* Модальное окно для taoshibka заказов */}
            {showTaoshibkaModal && (
                <div className="modal-overlay">
                    <button 
                        className="modal-close comic-font cursor-felt-pen"
                        onClick={closeTaoshibkaModal}
                    >
                        ✕
                    </button>
                    <div className="modal-content">
                        <div className="h-full overflow-y-auto p-6 comic-font">
                            <div className="mb-6">
                                <h2 className="text-3xl font-bold text-black mb-2">
                                    🛠️ Товары с неизвестным наличием
                                </h2>
                                <div className="flex items-center gap-4">
                                    <span className="px-4 py-2 bg-black text-white rounded-lg font-bold">
                                        Заказ #{selectedTaoshibkaOrder?.cart_id}
                                    </span>
                                    <span className="text-gray-600">
                                        👤 {selectedTaoshibkaOrder?.client_name || 'Клиент'}
                                    </span>
                                    <span className="text-gray-600">
                                        📧 {selectedTaoshibkaOrder?.client_email || 'Нет email'}
                                    </span>
                                </div>
                            </div>
                            
                            {taoshibkaItems.length === 0 ? (
                                <div className="text-center py-20">
                                    <div className="text-5xl mb-4">🤔</div>
                                    <p className="text-xl text-gray-600">Нет товаров с неизвестным наличием</p>
                                    <button
                                        onClick={closeTaoshibkaModal}
                                        className="mt-6 px-6 py-3 bg-black text-white rounded-lg hover:bg-gray-800"
                                    >
                                        Закрыть
                                    </button>
                                </div>
                            ) : (
                                <>
                                    <div className="mb-6">
                                        <div className="bg-red-50 border-2 border-red-300 rounded-lg p-4">
                                            <div className="flex items-center gap-3">
                                                <span className="text-2xl">⚠️</span>
                                                <div>
                                                    <p className="font-bold text-red-800">
                                                        Обнаружено {taoshibkaItems.length} товаров с неизвестным наличием
                                                    </p>
                                                    <p className="text-sm text-red-600 mt-1">
                                                        Нажмите кнопку ниже для автоматического поиска на складах
                                                    </p>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
                                        {taoshibkaItems.map((item, index) => (
                                            <div 
                                                key={item.item_id || index}
                                                className="bg-white border-2 border-gray-300 rounded-lg p-4 hover:border-black transition-colors"
                                            >
                                                <div className="flex justify-between items-start">
                                                    <div>
                                                        <h4 className="font-bold text-lg text-black mb-2">
                                                            {item.product_name || `Товар #${item.product_id}`}
                                                        </h4>
                                                        <div className="space-y-1">
                                                            <p className="text-gray-700">
                                                                <span className="font-medium">Количество:</span> {item.quantity} шт.
                                                            </p>
                                                            <p className="text-gray-700">
                                                                <span className="font-medium">Цена:</span> {parseFloat(item.price || 0).toFixed(2)} ₽
                                                            </p>
                                                            <p className="text-gray-700">
                                                                <span className="font-medium">Артикул:</span> {item.sku || 'N/A'}
                                                            </p>
                                                            {item.category && (
                                                                <p className="text-gray-700">
                                                                    <span className="font-medium">Категория:</span> {item.category}
                                                                </p>
                                                            )}
                                                        </div>
                                                    </div>
                                                    <div className="bg-red-100 text-red-800 px-3 py-1 rounded-full text-sm font-bold">
                                                        unknown
                                                    </div>
                                                </div>
                                                {item.description && item.description !== 'Нет описания' && (
                                                    <div className="mt-3 p-3 bg-gray-50 rounded">
                                                        <p className="text-sm text-gray-600 italic">
                                                            {item.description}
                                                        </p>
                                                    </div>
                                                )}
                                            </div>
                                        ))}
                                    </div>
                                    
                                    <div className="fixed bottom-6 left-6 right-6">
                                        <button
                                            onClick={handleFindCollectors}
                                            disabled={findCollectorsLoading}
                                            style={findCollectorsLoading ? styles.findCollectorsButtonDisabled : styles.findCollectorsButton}
                                            className="cursor-felt-pen comic-font w-full py-4"
                                        >
                                            {findCollectorsLoading ? (
                                                <>
                                                    <span className="inline-block animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full mr-2"></span>
                                                    🔍 ИЩЕМ СБОРЩИКОВ...
                                                </>
                                            ) : (
                                                '🔍 ПОИСК СБОРЩИКОВ'
                                            )}
                                        </button>
                                        <div className="text-center text-xs text-gray-500 mt-2">
                                            Система автоматически проверит все склады и обновит статус заказа
                                        </div>
                                    </div>
                                </>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

// Встроенные стили в виде JavaScript объекта
const styles = {
    officePage: {
        display: 'flex',
        height: '100vh',
        backgroundColor: '#f9fafb',
        cursor: 'url(\'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32"><path d="M8 28l16-16-4-4L4 24z" fill="black"/><path d="M24 4l4 4-16 16-4-4z" fill="%23f59e0b"/></svg>\') 4 28, auto',
        fontFamily: '\'Comic Neue\', cursive, sans-serif',
        position: 'relative',
        overflow: 'hidden'
    },
    exitBlob: {
        position: 'fixed',
        top: '20px',
        right: '20px',
        width: '80px',
        height: '80px',
        backgroundColor: '#000',
        border: 'none',
        borderRadius: '60% 40% 30% 70% / 60% 30% 70% 40%',
        cursor: 'pointer',
        zIndex: 1000,
        boxShadow: `
            0 0 0 6px #000,
            0 0 0 12px rgba(0,0,0,0.8),
            8px 8px 0 rgba(0,0,0,0.3),
            16px 16px 0 rgba(0,0,0,0.1)
        `,
        transition: 'all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)',
        outline: 'none',
    },
    problemContainer: {
        height: '100%',
        border: '3px solid #000',
        borderRadius: '8px',
        position: 'relative',
        backgroundColor: 'white',
        boxShadow: '6px 6px 0 #000, 12px 12px 0 rgba(0,0,0,0.1)'
    },
    taoshibkaContainer: {
        height: '100%',
        border: '3px solid #000',
        borderRadius: '8px',
        position: 'relative',
        backgroundColor: 'white',
        boxShadow: '6px 6px 0 #000, 12px 12px 0 rgba(0,0,0,0.1)',
        display: 'flex',
        flexDirection: 'column'
    },
    blackCorner: {
        position: 'absolute',
        top: 0,
        right: 0,
        width: '80px',
        height: '80px',
        backgroundColor: '#000',
        clipPath: 'polygon(0 0, 100% 0, 100% 100%, 0 0)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        paddingTop: '10px'
    },
    taoshibkaBlackCorner: {
        position: 'absolute',
        top: 0,
        right: 0,
        width: '80px',
        height: '80px',
        backgroundColor: '#000',
        clipPath: 'polygon(0 0, 100% 0, 100% 100%, 0 0)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        paddingTop: '10px'
    },
    cornerIcon: {
        color: 'white',
        fontSize: '24px',
        marginBottom: '2px'
    },
    cornerText: {
        color: 'white',
        fontSize: '12px',
        fontWeight: 'bold',
        transform: 'rotate(45deg)',
        marginRight: '15px',
        marginTop: '-5px'
    },
    title: {
        color: '#000',
        textShadow: '2px 2px 0 #f59e0b'
    },
    loadingSpinner: {
        width: '50px',
        height: '50px',
        margin: '0 auto',
        border: '4px solid #f3f4f6',
        borderTop: '4px solid #f59e0b',
        borderRadius: '50%',
        animation: 'spin 1s linear infinite'
    },
    emptyState: {
        opacity: 0.6
    },
    problemCard: {
        padding: '16px',
        border: '2px solid #d1d5db',
        borderRadius: '8px',
        cursor: 'pointer',
        transition: 'all 0.3s ease',
        backgroundColor: 'white',
        position: 'relative'
    },
    problemCardSelected: {
        padding: '16px',
        border: '3px solid #000',
        borderRadius: '8px',
        cursor: 'pointer',
        backgroundColor: '#fef3c7',
        boxShadow: '3px 3px 0 #000',
        position: 'relative'
    },
    taoshibkaCard: {
        padding: '16px',
        border: '2px solid #d1d5db',
        borderRadius: '8px',
        cursor: 'pointer',
        transition: 'all 0.3s ease',
        backgroundColor: 'white',
        position: 'relative'
    },
    taoshibkaCardSelected: {
        padding: '16px',
        border: '3px solid #000',
        borderRadius: '8px',
        cursor: 'pointer',
        backgroundColor: '#e0f2fe',
        boxShadow: '3px 3px 0 #000',
        position: 'relative'
    },
    orderNumber: {
        color: '#000',
        textDecoration: 'underline',
        textDecorationStyle: 'wavy',
        textDecorationColor: '#f59e0b'
    },
    clientIcon: {
        marginRight: '4px'
    },
    emailIcon: {
        marginLeft: '12px',
        marginRight: '4px'
    },
    problemDetails: {
        color: '#374151',
        borderLeft: '3px solid #f59e0b',
        paddingLeft: '8px'
    },
    statusBadgePending: {
        padding: '4px 8px',
        backgroundColor: '#fee2e2',
        color: '#991b1b',
        borderRadius: '12px',
        fontSize: '12px',
        fontWeight: 'bold',
        display: 'inline-block'
    },
    statusBadgeNotified: {
        padding: '4px 8px',
        backgroundColor: '#fef3c7',
        color: '#92400e',
        borderRadius: '12px',
        fontSize: '12px',
        fontWeight: 'bold',
        display: 'inline-block'
    },
    taoshibkaBadge: {
        padding: '6px 10px',
        backgroundColor: '#fee2e2',
        color: '#991b1b',
        borderRadius: '12px',
        fontSize: '12px',
        fontWeight: 'bold',
        display: 'inline-block',
        border: '2px solid #dc2626'
    },
    collectorIcon: {
        marginRight: '4px'
    },
    emailContainer: {
        backgroundColor: 'white',
        border: '3px solid #000',
        borderRadius: '8px',
        padding: '20px',
        boxShadow: '6px 6px 0 #000, 12px 12px 0 rgba(0,0,0,0.1)',
        height: '100%',
        display: 'flex',
        flexDirection: 'column'
    },
    clientInfo: {
        backgroundColor: '#f9fafb',
        border: '2px dashed #d1d5db',
        borderRadius: '6px'
    },
    textarea: {
        border: '3px solid #000',
        borderRadius: '6px',
        resize: 'none',
        outline: 'none',
        backgroundColor: '#f9fafb',
        fontFamily: '\'Comic Neue\', cursive, sans-serif',
        fontSize: '14px',
        lineHeight: '1.5'
    },
    sendButton: {
        width: '100%',
        padding: '12px',
        backgroundColor: '#000',
        color: 'white',
        border: 'none',
        borderRadius: '8px',
        fontWeight: 'bold',
        fontSize: '16px',
        boxShadow: '3px 3px 0 #f59e0b'
    },
    sendButtonDisabled: {
        width: '100%',
        padding: '12px',
        backgroundColor: '#9ca3af',
        color: '#6b7280',
        border: 'none',
        borderRadius: '8px',
        fontWeight: 'bold',
        fontSize: '16px',
        cursor: 'not-allowed'
    },
    approveButton: {
        padding: '12px',
        backgroundColor: '#10b981',
        color: 'white',
        border: 'none',
        borderRadius: '8px',
        fontWeight: 'bold',
        fontSize: '14px',
        boxShadow: '2px 2px 0 #047857'
    },
    cancelButton: {
        padding: '12px',
        backgroundColor: '#ef4444',
        color: 'white',
        border: 'none',
        borderRadius: '8px',
        fontWeight: 'bold',
        fontSize: '14px',
        boxShadow: '2px 2px 0 #b91c1c'
    },
    waitButton: {
        padding: '12px',
        backgroundColor: '#f59e0b',
        color: 'white',
        border: 'none',
        borderRadius: '8px',
        fontWeight: 'bold',
        fontSize: '14px',
        boxShadow: '2px 2px 0 #d97706'
    },
    emptySelection: {
        opacity: 0.5
    },
    testButton: {
        padding: '12px',
        backgroundColor: '#000',
        color: 'white',
        border: 'none',
        borderRadius: '8px',
        fontWeight: 'bold',
        fontSize: '14px',
        boxShadow: '3px 3px 0 #f59e0b',
        cursor: 'pointer'
    },
    findCollectorsButton: {
        padding: '16px',
        backgroundColor: '#10b981',
        color: 'white',
        border: 'none',
        borderRadius: '8px',
        fontWeight: 'bold',
        fontSize: '18px',
        boxShadow: '4px 4px 0 #047857',
        cursor: 'pointer',
        textTransform: 'uppercase',
        letterSpacing: '1px'
    },
    findCollectorsButtonDisabled: {
        padding: '16px',
        backgroundColor: '#6ee7b7',
        color: '#047857',
        border: 'none',
        borderRadius: '8px',
        fontWeight: 'bold',
        fontSize: '18px',
        cursor: 'not-allowed',
        textTransform: 'uppercase',
        letterSpacing: '1px'
    }
};

export default OfficePage;