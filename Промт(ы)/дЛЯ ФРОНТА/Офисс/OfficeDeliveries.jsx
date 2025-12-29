// src/pages/office/OfficeDeliveries.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import OfficeSidebar from '../../components/office/OfficeSidebar';

const OfficeDeliveries = () => {
  const [deliveries, setDeliveries] = useState([]);
  const [couriers, setCouriers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDelivery, setSelectedDelivery] = useState(null);
  const [assigning, setAssigning] = useState(false);

  useEffect(() => {
    fetchDeliveries();
    fetchCouriers();
  }, []);

  const fetchDeliveries = async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/deliveries');
      setDeliveries(response.data || []);
    } catch (error) {
      console.error('Ошибка при загрузке доставок:', error);
      // Заглушка для демонстрации
      setDeliveries([
        { id: 1, orderId: 1001, clientName: 'Иван Иванов', address: 'ул. Ленина, 10', status: 'В ожидании', courierId: null },
        { id: 2, orderId: 1002, clientName: 'Мария Петрова', address: 'ул. Пушкина, 25', status: 'Назначена', courierId: 101, courierName: 'Алексей Курьеров' },
        { id: 3, orderId: 1003, clientName: 'Сергей Сидоров', address: 'пр. Мира, 15', status: 'В пути', courierId: 102, courierName: 'Дмитрий Доставкин' },
        { id: 4, orderId: 1004, clientName: 'Анна Ковалева', address: 'ул. Садовая, 7', status: 'Доставлена', courierId: 101, courierName: 'Алексей Курьеров' },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const fetchCouriers = async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/couriers');
      setCouriers(response.data || []);
    } catch (error) {
      console.error('Ошибка при загрузке курьеров:', error);
      setCouriers([
        { id: 101, name: 'Алексей Курьеров', status: 'Свободен' },
        { id: 102, name: 'Дмитрий Доставкин', status: 'Занят' },
        { id: 103, name: 'Екатерина Быстрая', status: 'Свободен' },
      ]);
    }
  };

  const handleAssignCourier = async (deliveryId, courierId) => {
    try {
      setAssigning(true);
      await axios.post(`http://localhost:8080/api/deliveries/${deliveryId}/assign`, {
        courierId
      });
      alert('Курьер успешно назначен!');
      fetchDeliveries();
    } catch (error) {
      console.error('Ошибка при назначении курьера:', error);
      alert('Ошибка при назначении курьера');
    } finally {
      setAssigning(false);
    }
  };

  const handleUpdateStatus = async (deliveryId, status) => {
    try {
      await axios.post(`http://localhost:8080/api/deliveries/${deliveryId}/status`, {
        status
      });
      alert('Статус обновлен!');
      fetchDeliveries();
    } catch (error) {
      console.error('Ошибка при обновлении статуса:', error);
      alert('Ошибка при обновлении статуса');
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'В ожидании': return 'bg-yellow-100 text-yellow-800';
      case 'Назначена': return 'bg-blue-100 text-blue-800';
      case 'В пути': return 'bg-purple-100 text-purple-800';
      case 'Доставлена': return 'bg-green-100 text-green-800';
      case 'Отменена': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="flex h-screen bg-gray-50">
      <OfficeSidebar />
      
      <div className="flex-1 p-8 overflow-y-auto">
        <div className="max-w-7xl mx-auto">
          {/* Заголовок */}
          <div className="flex justify-between items-center mb-8">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">🚚 Управление доставками</h1>
              <p className="text-gray-600">Назначение курьеров и отслеживание статусов доставок</p>
            </div>
            <div className="flex gap-3">
              <button
                onClick={fetchDeliveries}
                className="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2.5 rounded-lg font-medium flex items-center gap-2"
              >
                <span>🔄</span>
                <span>Обновить</span>
              </button>
              <button className="bg-green-500 hover:bg-green-600 text-white px-4 py-2.5 rounded-lg font-medium flex items-center gap-2">
                <span>➕</span>
                <span>Новая доставка</span>
              </button>
            </div>
          </div>

          {/* Карточки статистики */}
          <div className="grid grid-cols-4 gap-4 mb-8">
            {[
              { title: 'Все доставки', value: deliveries.length, color: 'bg-blue-500', icon: '📦' },
              { title: 'В ожидании', value: deliveries.filter(d => d.status === 'В ожидании').length, color: 'bg-yellow-500', icon: '⏳' },
              { title: 'В пути', value: deliveries.filter(d => d.status === 'В пути').length, color: 'bg-purple-500', icon: '🚚' },
              { title: 'Доставлено', value: deliveries.filter(d => d.status === 'Доставлена').length, color: 'bg-green-500', icon: '✅' },
            ].map((stat, index) => (
              <div key={index} className="bg-white rounded-xl shadow p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-600">{stat.title}</p>
                    <p className="text-3xl font-bold mt-2">{stat.value}</p>
                  </div>
                  <div className={`${stat.color} w-12 h-12 rounded-lg flex items-center justify-center`}>
                    <span className="text-2xl text-white">{stat.icon}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {loading ? (
            <div className="text-center py-20">
              <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
              <p className="mt-4 text-gray-500">Загрузка доставок...</p>
            </div>
          ) : (
            <div className="bg-white rounded-xl shadow overflow-hidden">
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        ID доставки
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Заказ
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Клиент
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Адрес
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Статус
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Курьер
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Действия
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {deliveries.map((delivery) => (
                      <tr key={delivery.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm font-medium text-gray-900">#{delivery.id}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm font-medium text-blue-600">Заказ #{delivery.orderId}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-gray-900">{delivery.clientName}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-gray-900 max-w-xs truncate">{delivery.address}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(delivery.status)}`}>
                            {delivery.status}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          {delivery.courierName ? (
                            <div className="flex items-center">
                              <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center mr-2">
                                <span className="text-blue-600 text-sm">👤</span>
                              </div>
                              <div>
                                <div className="text-sm font-medium">{delivery.courierName}</div>
                                <div className="text-xs text-gray-500">ID: {delivery.courierId}</div>
                              </div>
                            </div>
                          ) : (
                            <span className="text-gray-400 text-sm">Не назначен</span>
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                          {delivery.status === 'В ожидании' ? (
                            <div className="flex gap-2">
                              <select
                                className="border rounded-lg px-3 py-1 text-sm"
                                onChange={(e) => handleAssignCourier(delivery.id, e.target.value)}
                                disabled={assigning}
                              >
                                <option value="">Назначить курьера</option>
                                {couriers.filter(c => c.status === 'Свободен').map(courier => (
                                  <option key={courier.id} value={courier.id}>
                                    {courier.name} (ID: {courier.id})
                                  </option>
                                ))}
                              </select>
                            </div>
                          ) : (
                            <div className="flex gap-2">
                              <select
                                className="border rounded-lg px-3 py-1 text-sm"
                                onChange={(e) => handleUpdateStatus(delivery.id, e.target.value)}
                                value={delivery.status}
                              >
                                <option value="В ожидании">В ожидании</option>
                                <option value="Назначена">Назначена</option>
                                <option value="В пути">В пути</option>
                                <option value="Доставлена">Доставлена</option>
                                <option value="Отменена">Отменена</option>
                              </select>
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Карта доставок (заглушка) */}
          <div className="mt-8 bg-white rounded-xl shadow p-6">
            <h2 className="text-xl font-bold text-gray-900 mb-4">🗺️ Карта доставок</h2>
            <div className="bg-gray-100 rounded-lg h-64 flex items-center justify-center">
              <div className="text-center">
                <div className="text-5xl mb-4">🗺️</div>
                <p className="text-gray-600">Карта доставок будет отображаться здесь</p>
                <p className="text-sm text-gray-500 mt-2">Интеграция с картографическим сервисом в разработке</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OfficeDeliveries;