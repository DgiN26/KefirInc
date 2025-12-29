// src/pages/courier/CourierApp.jsx
import React, { useState, useEffect } from 'react';

const CourierApp = () => {
  const [deliveries, setDeliveries] = useState([]);
  const [status, setStatus] = useState('online');

  useEffect(() => {
    // Моковые данные доставок
    setDeliveries([
      { id: 1, address: 'ул. Ленина, 10', client: 'Иван Иванов', status: 'pending', items: ['Ноутбук', 'Мышь'] },
      { id: 2, address: 'пр. Мира, 25', client: 'Петр Петров', status: 'in_progress', items: ['Футболка', 'Джинсы'] },
      { id: 3, address: 'ул. Советская, 5', client: 'Сергей Сергеев', status: 'pending', items: ['Книги'] },
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
                    <i className="fas fa-bicycle me-2"></i>
                    Приложение курьера
                  </h1>
                  <p className="text-muted mb-0">Статус: 
                    <span className={`badge ms-2 bg-${status === 'online' ? 'success' : 'danger'}`}>
                      {status === 'online' ? 'Онлайн' : 'Офлайн'}
                    </span>
                  </p>
                </div>
                <div className="btn-group">
                  <button 
                    className={`btn ${status === 'online' ? 'btn-success' : 'btn-outline-success'}`}
                    onClick={() => setStatus('online')}
                  >
                    <i className="fas fa-play me-1"></i>Начать смену
                  </button>
                  <button 
                    className={`btn ${status === 'offline' ? 'btn-danger' : 'btn-outline-danger'}`}
                    onClick={() => setStatus('offline')}
                  >
                    <i className="fas fa-stop me-1"></i>Завершить смену
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="row">
        <div className="col-md-6">
          <div className="card">
            <div className="card-header">
              <h5 className="mb-0">
                <i className="fas fa-list me-2"></i>
                Мои доставки
              </h5>
            </div>
            <div className="card-body">
              {deliveries.map(delivery => (
                <div key={delivery.id} className="card mb-2">
                  <div className="card-body">
                    <h6>Доставка #{delivery.id}</h6>
                    <p className="mb-1"><strong>Клиент:</strong> {delivery.client}</p>
                    <p className="mb-1"><strong>Адрес:</strong> {delivery.address}</p>
                    <p className="mb-2"><strong>Товары:</strong> {delivery.items.join(', ')}</p>
                    <div className="d-flex justify-content-between">
                      <span className={`badge bg-${delivery.status === 'pending' ? 'warning' : 'info'}`}>
                        {delivery.status === 'pending' ? 'Ожидает' : 'В процессе'}
                      </span>
                      <div>
                        <button className="btn btn-sm btn-primary me-1">
                          <i className="fas fa-map-marker-alt"></i> Маршрут
                        </button>
                        <button className="btn btn-sm btn-success">
                          <i className="fas fa-check"></i> Завершить
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="col-md-6">
          <div className="card">
            <div className="card-header">
              <h5 className="mb-0">
                <i className="fas fa-chart-line me-2"></i>
                Статистика
              </h5>
            </div>
            <div className="card-body">
              <div className="row text-center">
                <div className="col-6 mb-3">
                  <div className="card bg-light">
                    <div className="card-body">
                      <h2 className="text-primary">12</h2>
                      <p className="text-muted mb-0">Доставок сегодня</p>
                    </div>
                  </div>
                </div>
                <div className="col-6 mb-3">
                  <div className="card bg-light">
                    <div className="card-body">
                      <h2 className="text-success">3,850 ₽</h2>
                      <p className="text-muted mb-0">Заработок</p>
                    </div>
                  </div>
                </div>
                <div className="col-6">
                  <div className="card bg-light">
                    <div className="card-body">
                      <h2 className="text-info">4.8</h2>
                      <p className="text-muted mb-0">Рейтинг</p>
                    </div>
                  </div>
                </div>
                <div className="col-6">
                  <div className="card bg-light">
                    <div className="card-body">
                      <h2 className="text-warning">45 мин</h2>
                      <p className="text-muted mb-0">Среднее время</p>
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

export default CourierApp;
