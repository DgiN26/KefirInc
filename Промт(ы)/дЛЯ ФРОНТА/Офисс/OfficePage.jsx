// src/pages/office/OfficePage.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

const OfficePage = ({ onLogout }) => {
    const [problems, setProblems] = useState([]);
    const [selectedProblem, setSelectedProblem] = useState(null);
    const [loading, setLoading] = useState(true);
    const [emailMessage, setEmailMessage] = useState('');
    const [actionMessage, setActionMessage] = useState('');

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

        fetchProblems();
        
        // Polling каждые 15 секунд
        const intervalId = setInterval(fetchProblems, 15000);
        
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
                
                .page-transition {
                    animation: pageFlip 0.6s ease-in-out;
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
            `}</style>
            
            {/* ЖИРНАЯ ЧЕРНАЯ КЛЯКСА для выхода */}
            <button
                onClick={onLogout}
                style={styles.exitBlob}
                className="cursor-felt-pen exit-blob"
                title="ВЫХОД"
            />
            
            {/* Левая часть (70%) - Список проблем */}
            <div className="w-[70%] p-6">
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
            
            {/* Правая часть (30%) - Форма отправки email */}
            <div className="w-[30%] p-6">
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
        position: 'relative'
    },
    // ЖИРНАЯ ЧЕРНАЯ КЛЯКСА 5x5 см
    exitBlob: {
        position: 'fixed',
        top: '20px',
        right: '20px',
        width: '80px', // немного больше для жирности
        height: '80px',
        backgroundColor: '#000',
        border: 'none',
        borderRadius: '60% 40% 30% 70% / 60% 30% 70% 40%', // Органичная форма кляксы
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
    collectorIcon: {
        marginRight: '4px'
    },
    emailContainer: {
        backgroundColor: 'white',
        border: '3px solid #000',
        borderRadius: '8px',
        padding: '20px',
        boxShadow: '6px 6px 0 #000, 12px 12px 0 rgba(0,0,0,0.1)'
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
    }
};

export default OfficePage;