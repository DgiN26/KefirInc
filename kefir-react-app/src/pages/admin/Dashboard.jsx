// src/pages/admin/Dashboard.jsx
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

const Dashboard = () => {
  const [stats, setStats] = useState({
    clients: 0,
    products: 0,
    deliveries: 0,
    collectors: 0
  });

  useEffect(() => {
    // Имитация загрузки данных
    setTimeout(() => {
      setStats({
        clients: 42,
        products: 156,
        deliveries: 18,
        collectors: 8
      });
    }, 500);
  }, []);

  const serviceCards = [
    { 
      title: 'Клиенты', 
      icon: 'fas fa-users', 
      color: 'primary',
      description: 'Управление клиентами', 
      link: '/admin/clients',
      count: stats.clients
    },
    { 
      title: 'Товары', 
      icon: 'fas fa-box', 
      color: 'success',
      description: 'Каталог товаров', 
      link: '/admin/products',
      count: stats.products
    },
    { 
      title: 'Корзины', 
      icon: 'fas fa-shopping-cart', 
      color: 'info',
      description: 'Управление корзинами', 
      link: '/admin/carts',
      count: 23
    },
    { 
      title: 'Склад', 
      icon: 'fas fa-warehouse', 
      color: 'warning',
      description: 'Управление запасами', 
      link: '/admin/warehouse',
      count: stats.products
    },
    { 
      title: 'Курьеры', 
      icon: 'fas fa-bicycle', 
      color: 'danger',
      description: 'Назначение курьеров', 
      link: '/admin/couriers',
      count: 12
    },
    { 
      title: 'Офис', 
      icon: 'fas fa-building', 
      color: 'secondary',
      description: 'Панель управления', 
      link: '/admin/office',
      count: 5
    },
    { 
      title: 'Доставки', 
      icon: 'fas fa-truck', 
      color: 'dark',
      description: 'Управление доставками', 
      link: '/admin/deliveries',
      count: stats.deliveries
    },
    { 
      title: 'Сборщики', 
      icon: 'fas fa-people-carry', 
      color: 'primary',
      description: 'Управление сборщиками', 
      link: '/admin/collectors',
      count: stats.collectors
    }
  ];

  return (
    <div className="container-fluid mt-4">
      <div className="row mb-4">
        <div className="col-12">
          <div className="d-flex justify-content-between align-items-center">
            <div>
              <h1 className="display-5 fw-bold">
                <i className="fas fa-tachometer-alt me-2"></i>
                Панель управления
              </h1>
              <p className="text-muted">Управление логистикой и доставками KEFIR</p>
            </div>
            <div className="d-flex gap-2">
              <button className="btn btn-outline-primary">
                <i className="fas fa-sync-alt me-1"></i>Обновить
              </button>
              <button className="btn btn-primary">
                <i className="fas fa-plus me-1"></i>Создать
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Быстрые действия */}
      <div className="row mb-4">
        <div className="col-12">
          <div className="card">
            <div className="card-body">
              <h5 className="card-title">
                <i className="fas fa-bolt me-2"></i>
                Быстрые действия
              </h5>
              <div className="d-flex flex-wrap gap-2">
                <button className="btn btn-outline-primary">
                  <i className="fas fa-user-plus me-1"></i>Добавить клиента
                </button>
                <button className="btn btn-outline-success">
                  <i className="fas fa-box me-1"></i>Добавить товар
                </button>
                <button className="btn btn-outline-info">
                  <i className="fas fa-truck me-1"></i>Создать доставку
                </button>
                <button className="btn btn-outline-warning">
                  <i className="fas fa-chart-bar me-1"></i>Отчет за день
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Карточки сервисов */}
      <div className="row mb-4">
        {serviceCards.map((service, index) => (
          <div className="col-xl-3 col-lg-4 col-md-6 mb-4" key={index}>
            <div className={`card border-${service.color} shadow-sm h-100`}>
              <div className="card-body">
                <div className="d-flex justify-content-between align-items-start">
                  <div>
                    <div className={`text-${service.color} mb-2`}>
                      <i className={`${service.icon} fa-2x`}></i>
                    </div>
                    <h5 className="card-title">{service.title}</h5>
                    <p className="card-text text-muted small">{service.description}</p>
                  </div>
                  <span className={`badge bg-${service.color} fs-6`}>
                    {service.count}
                  </span>
                </div>
              </div>
              <div className="card-footer bg-transparent border-top-0">
                <div className="d-flex justify-content-between align-items-center">
                  <Link to={service.link} className="btn btn-sm btn-outline-primary">
                    <i className="fas fa-arrow-right me-1"></i>Перейти
                  </Link>
                  <small className="text-muted">
                    <i className="fas fa-chart-line me-1"></i>
                    +12%
                  </small>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Статистика */}
      <div className="row mt-5">
        <div className="col-12">
          <div className="card shadow">
            <div className="card-header bg-white">
              <h5 className="mb-0">
                <i className="fas fa-chart-bar me-2"></i>
                📊 Системная статистика
              </h5>
            </div>
            <div className="card-body">
              <div className="row text-center">
                <div className="col-md-3 mb-3">
                  <div className="card bg-light">
                    <div className="card-body">
                      <h2 className="text-primary">{stats.clients}</h2>
                      <p className="text-muted mb-0">
                        <i className="fas fa-users me-1"></i>
                        Клиентов
                      </p>
                      <small className="text-success">
                        <i className="fas fa-arrow-up me-1"></i>
                        +5 за неделю
                      </small>
                    </div>
                  </div>
                </div>
                <div className="col-md-3 mb-3">
                  <div className="card bg-light">
                    <div className="card-body">
                      <h2 className="text-success">{stats.products}</h2>
                      <p className="text-muted mb-0">
                        <i className="fas fa-box me-1"></i>
                        Товаров
                      </p>
                      <small className="text-success">
                        <i className="fas fa-arrow-up me-1"></i>
                        +12 за неделю
                      </small>
                    </div>
                  </div>
                </div>
                <div className="col-md-3 mb-3">
                  <div className="card bg-light">
                    <div className="card-body">
                      <h2 className="text-info">{stats.deliveries}</h2>
                      <p className="text-muted mb-0">
                        <i className="fas fa-truck me-1"></i>
                        Доставок
                      </p>
                      <small className="text-success">
                        <i className="fas fa-arrow-up me-1"></i>
                        +8 сегодня
                      </small>
                    </div>
                  </div>
                </div>
                <div className="col-md-3 mb-3">
                  <div className="card bg-light">
                    <div className="card-body">
                      <h2 className="text-warning">{stats.collectors}</h2>
                      <p className="text-muted mb-0">
                        <i className="fas fa-people-carry me-1"></i>
                        Сборщиков
                      </p>
                      <small className="text-success">
                        <i className="fas fa-arrow-up me-1"></i>
                        +2 за месяц
                      </small>
                    </div>
                  </div>
                </div>
              </div>
              
              {/* Дополнительная информация */}
              <div className="row mt-4">
                <div className="col-12">
                  <div className="alert alert-info">
                    <div className="d-flex justify-content-between align-items-center">
                      <div>
                        <i className="fas fa-info-circle me-2"></i>
                        <strong>Сегодня:</strong> 12 новых заказов, 8 доставок завершено
                      </div>
                      <button className="btn btn-sm btn-outline-info">
                        Подробнее
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Панель управления */}
      <div className="row mt-4">
        <div className="col-12">
          <div className="card">
            <div className="card-header bg-white">
              <h5 className="mb-0">
                <i className="fas fa-cogs me-2"></i>
                Управление системой
              </h5>
            </div>
            <div className="card-body">
              <div className="row">
                <div className="col-md-4 mb-3">
                  <div className="list-group">
                    <button className="list-group-item list-group-item-action">
                      <i className="fas fa-database me-2"></i>
                      Резервное копирование
                    </button>
                    <button className="list-group-item list-group-item-action">
                      <i className="fas fa-chart-pie me-2"></i>
                      Аналитика системы
                    </button>
                    <button className="list-group-item list-group-item-action">
                      <i className="fas fa-bell me-2"></i>
                      Уведомления
                    </button>
                  </div>
                </div>
                <div className="col-md-4 mb-3">
                  <div className="list-group">
                    <button className="list-group-item list-group-item-action">
                      <i className="fas fa-user-shield me-2"></i>
                      Безопасность
                    </button>
                    <button className="list-group-item list-group-item-action">
                      <i className="fas fa-wrench me-2"></i>
                      Настройки системы
                    </button>
                    <button className="list-group-item list-group-item-action">
                      <i className="fas fa-question-circle me-2"></i>
                      Помощь и поддержка
                    </button>
                  </div>
                </div>
                <div className="col-md-4 mb-3">
                  <div className="list-group">
                    <button className="list-group-item list-group-item-action">
                      <i className="fas fa-file-export me-2"></i>
                      Экспорт данных
                    </button>
                    <button className="list-group-item list-group-item-action">
                      <i className="fas fa-users-cog me-2"></i>
                      Управление ролями
                    </button>
                    <button className="list-group-item list-group-item-action text-danger">
                      <i className="fas fa-sign-out-alt me-2"></i>
                      Выйти из системы
                    </button>
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

export default Dashboard;
