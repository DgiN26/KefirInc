// src/pages/office/OfficeProblemOrders.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import OfficeSidebar from '../../components/office/OfficeSidebar';

const OfficeProblemOrders = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedOrder, setSelectedOrder] = useState(null);
    const [orderDetails, setOrderDetails] = useState(null);

    useEffect(() => {
        fetchProblemOrders();
    }, []);

    const fetchProblemOrders = async () => {
        try {
            setLoading(true);
            const response = await axios.get('http://localhost:8080/api/office/orders/problems');
            
            if (response.data.success) {
                setOrders(response.data.orders || []);
                
                if (response.data.orders.length > 0 && !selectedOrder) {
                    handleSelectOrder(response.data.orders[0]);
                }
            }
        } catch (error) {
            console.error('Ошибка при загрузке заказов:', error);
            // Заглушка для демонстрации
            setOrders([
                {
                    order_id: 1,
                    client_name: 'Иван Иванов',
                    email: 'ivan@example.com',
                    created_date: '2025-01-20T10:30:00',
                    item_count: 3,
                    problem_count: 1,
                    city: 'Москва'
                },
                {
                    order_id: 2,
                    client_name: 'Мария Петрова',
                    email: 'maria@example.com',
                    created_date: '2025-01-20T11:45:00',
                    item_count: 2,
                    problem_count: 2,
                    city: 'Санкт-Петербург'
                }
            ]);
        } finally {
            setLoading(false);
        }
    };

    const handleSelectOrder = async (order) => {
        setSelectedOrder(order);
        try {
            const response = await axios.get(`http://localhost:8080/api/office/orders/${order.order_id}/details`);
            if (response.data.success) {
                setOrderDetails(response.data);
            }
        } catch (error) {
            console.error('Ошибка при получении деталей заказа:', error);
        }
    };

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleString('ru-RU');
    };

    return (
        <div className="flex h-screen bg-gray-50">
            <OfficeSidebar />
            
            <div className="flex-1 p-8 overflow-y-auto">
                <div className="max-w-7xl mx-auto">
                    {/* Заголовок */}
                    <div className="mb-8">
                        <h1 className="text-3xl font-bold text-gray-900">⚠️ Заказы с проблемами</h1>
                        <p className="text-gray-600">Заказы требующие внимания офиса</p>
                    </div>

                    {loading ? (
                        <div className="text-center py-20">
                            <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
                            <p className="mt-4 text-gray-500">Загрузка заказов...</p>
                        </div>
                    ) : (
                        <div className="grid grid-cols-3 gap-8">
                            {/* Левая колонка - список заказов */}
                            <div className="col-span-2">
                                <div className="bg-white rounded-xl shadow overflow-hidden">
                                    <div className="p-4 border-b border-gray-200">
                                        <h2 className="text-xl font-bold text-gray-900">
                                            Список заказов ({orders.length})
                                        </h2>
                                    </div>
                                    <div className="overflow-y-auto max-h-[600px]">
                                        {orders.length === 0 ? (
                                            <div className="text-center py-10">
                                                <p className="text-gray-500">Нет заказов с проблемами</p>
                                            </div>
                                        ) : (
                                            <div className="divide-y divide-gray-200">
                                                {orders.map((order) => (
                                                    <div
                                                        key={order.order_id}
                                                        onClick={() => handleSelectOrder(order)}
                                                        className={`p-4 cursor-pointer transition-colors hover:bg-gray-50 ${
                                                            selectedOrder?.order_id === order.order_id 
                                                            ? 'bg-blue-50 border-l-4 border-blue-500' 
                                                            : ''
                                                        }`}
                                                    >
                                                        <div className="flex justify-between items-start">
                                                            <div>
                                                                <h3 className="font-bold text-gray-900">
                                                                    Заказ #{order.order_id}
                                                                </h3>
                                                                <p className="text-sm text-gray-600 mt-1">
                                                                    {order.client_name || order.firstname}
                                                                </p>
                                                                <p className="text-xs text-gray-500 mt-1">
                                                                    {formatDate(order.created_date)}
                                                                </p>
                                                            </div>
                                                            <div className="text-right">
                                                                <div className="flex items-center gap-2">
                                                                    <span className="px-2 py-1 bg-red-100 text-red-800 text-xs font-bold rounded">
                                                                        {order.problem_count || 1} проблем
                                                                    </span>
                                                                    <span className="px-2 py-1 bg-gray-100 text-gray-800 text-xs font-bold rounded">
                                                                        {order.item_count || 0} товаров
                                                                    </span>
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div className="mt-2 flex items-center text-sm text-gray-600">
                                                            <span>📧 {order.email}</span>
                                                            <span className="mx-2">•</span>
                                                            <span>🏙️ {order.city}</span>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>

                            {/* Правая колонка - детали заказа */}
                            <div className="col-span-1">
                                <div className="bg-white rounded-xl shadow p-6">
                                    <h2 className="text-xl font-bold text-gray-900 mb-4">
                                        {selectedOrder ? `Заказ #${selectedOrder.order_id}` : 'Выберите заказ'}
                                    </h2>
                                    
                                    {orderDetails ? (
                                        <div className="space-y-4">
                                            {/* Информация о клиенте */}
                                            <div className="bg-gray-50 p-4 rounded-lg">
                                                <h3 className="font-bold text-gray-900 mb-2">👤 Клиент</h3>
                                                <p className="text-gray-800">{orderDetails.client?.firstname} {orderDetails.client?.username}</p>
                                                <p className="text-gray-600 text-sm mt-1">📧 {orderDetails.client?.email}</p>
                                                <p className="text-gray-600 text-sm">🏙️ {orderDetails.client?.city}</p>
                                            </div>

                                            {/* Товары */}
                                            <div>
                                                <h3 className="font-bold text-gray-900 mb-2">📦 Товары ({orderDetails.items?.length || 0})</h3>
                                                <div className="space-y-2">
                                                    {orderDetails.items?.map((item, index) => (
                                                        <div key={index} className="flex justify-between items-center p-2 bg-gray-50 rounded">
                                                            <div>
                                                                <p className="text-sm font-medium">{item.name || `Товар #${item.product_id}`}</p>
                                                                <p className="text-xs text-gray-500">Количество: {item.quantity}</p>
                                                            </div>
                                                            <p className="font-bold">{item.price} ₽</p>
                                                        </div>
                                                    ))}
                                                </div>
                                            </div>

                                            {/* Общая сумма */}
                                            <div className="border-t pt-4">
                                                <div className="flex justify-between items-center">
                                                    <span className="font-bold">Общая сумма:</span>
                                                    <span className="text-xl font-bold text-green-600">
                                                        {orderDetails.totalAmount?.toLocaleString('ru-RU') || '0'} ₽
                                                    </span>
                                                </div>
                                            </div>

                                            {/* Действия */}
                                            <div className="pt-4">
                                                <button
                                                    onClick={() => {
                                                        // Переход к управлению проблемами для этого заказа
                                                        window.location.href = `/office?orderId=${selectedOrder.order_id}`;
                                                    }}
                                                    className="w-full py-3 bg-black text-white font-bold rounded-lg hover:bg-gray-800"
                                                >
                                                    📋 Управлять проблемами
                                                </button>
                                            </div>
                                        </div>
                                    ) : (
                                        <div className="text-center py-10">
                                            <p className="text-gray-500">Выберите заказ для просмотра деталей</p>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default OfficeProblemOrders;