// src/pages/collector/CollectorApp.jsx
import React, { useState, useEffect } from 'react';

const CollectorApp = () => {
  const [orders, setOrders] = useState([]);
  const [inventory, setInventory] = useState([]);

  useEffect(() => {
    // Моковые данные
    setOrders([
      { 
        id: 1, 
        client: 'Иван Иванов', 
        items: [
          { name: 'Ноутбук', quantity: 1, status: 'available' },
          { name: 'Мышь', quantity: 2, status: 'available' }
        ], 
        status: 'pending' 
      },
      { 
        id: 2, 
        client: 'Петр Петров', 
        items: [
          { name: 'Футболка', quantity: 3, status: 'low_stock' },
          { name: 'Джинсы', quantity: 1, status: 'available' }
        ], 
        status: 'in_progress' 
      },
    ]);

    setInventory([
      { name: 'Ноутбук', count: 5, location: 'Стеллаж A-1' },
      { name: 'Мышь', count: 20, location: 'Стеллаж B-3' },
      { name: 'Футболка', count: 2, location: 'Стеллаж C-2' },
      { name: 'Джинсы', count: 8, location: 'Стеллаж D-1' },
    ]);
  }, []);

  return (
    <div className="container-fluid mt-4">
      <div className="row">
        <div className="col-12">
          <div className="card mb-4">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-center">
                <div>
                  <h1 className="mb-0">
                    <i className="fas fa-boxes me-2"></i>
                    Приложение сборщика
                  </h1>
                  <p className="text-muted">Склад: Главный склад • Смена: 08:00-20:00</p>
                </div>
                <button className="btn btn-primary">
                  <i className="fas fa-sync-alt me-1"></i>Обновить задания
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="row">
        <div className="col-md-7">
          <div className="card mb-4">
            <div className="card-header">
              <h5 className="mb-0">
                <i className="fas fa-tasks me-2"></i>
                Заказы для сборки
              </h5>
            </div>
            <div className="card-body">
              {orders.map(order => (
                <div key={order.id} className="card mb-3">
                  <div className="card-body">
                    <div className="d-flex justify-content-between align-items-start mb-2">
                      <div>
                        <h6>Заказ #{order.id}</h6>
                        <p className="mb-1"><strong>Клиент:</strong> {order.client}</p>
                      </div>
                      <span className={`badge bg-${order.status === 'pending' ? 'warning' : 'info'}`}>
                        {order.status === 'pending' ? 'Ожидает сборки' : 'В процессе'}
                      </span>
                    </div>
                    
                    <div className="mb-3">
                      <strong>Товары:</strong>
                      <ul className="list-group list-group-flush">
                        {order.items.map((item, index) => (
                          <li key={index} className="list-group-item d-flex justify-content-between align-items-center">
                            <div>
                              {item.name} × {item.quantity}
                              {item.status === 'low_stock' && (
                                <span className="badge bg-warning ms-2">Мало на складе</span>
                              )}
                            </div>
                            <span className={`badge bg-${item.status === 'available' ? 'success' : 'warning'}`}>
                              {item.status === 'available' ? 'Есть' : 'Мало'}
                            </span>
                          </li>
                        ))}
                      </ul>
                    </div>
                    
                    <div className="d-flex justify-content-end">
                      <button className="btn btn-sm btn-outline-primary me-2">
                        <i className="fas fa-info-circle"></i> Подробнее
                      </button>
                      <button className="btn btn-sm btn-success">
                        <i className="fas fa-check"></i> Завершить сборку
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="col-md-5">
          <div className="card mb-4">
            <div className="card-header">
              <h5 className="mb-0">
                <i className="fas fa-warehouse me-2"></i>
                Инвентарь на складе
              </h5>
            </div>
            <div className="card-body">
              <div className="table-responsive">
                <table className="table table-sm">
                  <thead>
                    <tr>
                      <th>Товар</th>
                      <th>Количество</th>
                      <th>Местоположение</th>
                      <th>Статус</th>
                    </tr>
                  </thead>
                  <tbody>
                    {inventory.map((item, index) => (
                      <tr key={index}>
                        <td>{item.name}</td>
                        <td>{item.count} шт.</td>
                        <td><small className="text-muted">{item.location}</small></td>
                        <td>
                          <span className={`badge bg-${item.count > 5 ? 'success' : item.count > 0 ? 'warning' : 'danger'}`}>
                            {item.count > 5 ? 'Много' : item.count > 0 ? 'Мало' : 'Нет'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <h5 className="mb-0">
                <i className="fas fa-chart-pie me-2"></i>
                Моя статистика
              </h5>
            </div>
            <div className="card-body">
              <div className="row text-center">
                <div className="col-6 mb-3">
                  <div className="card bg-light">
                    <div className="card-body">
                      <h2 className="text-primary">8</h2>
                      <p className="text-muted mb-0">Заказов сегодня</p>
                    </div>
                  </div>
                </div>
                <div className="col-6 mb-3">
                  <div className="card bg-light">
                    <div className="card-body">
                      <h2 className="text-success">47</h2>
                      <p className="text-muted mb-0">Товаров собрано</p>
                    </div>
                  </div>
                </div>
                <div className="col-6">
                  <div className="card bg-light">
                    <div className="card-body">
                      <h2 className="text-info">15 мин</h2>
                      <p className="text-muted mb-0">Среднее время</p>
                    </div>
                  </div>
                </div>
                <div className="col-6">
                  <div className="card bg-light">
                    <div className="card-body">
                      <h2 className="text-warning">100%</h2>
                      <p className="text-muted mb-0">Точность</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CollectorApp;
