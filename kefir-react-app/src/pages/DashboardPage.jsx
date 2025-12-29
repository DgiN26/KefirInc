import React from 'react';
import DashboardLayout from '../components/layout/DashboardLayout';
import Button from '../components/ui/Button';
import Card from '../components/ui/Card';
import Input from '../components/ui/Input';
import './DashboardPage.css';

const DashboardPage = ({ userRole }) => {
  const stats = [
    { label: 'Новые заказы', value: '24', icon: '📦', change: '+12%', color: 'blue' },
    { label: 'Доставки сегодня', value: '18', icon: '🚚', change: '+5%', color: 'green' },
    { label: 'Активные клиенты', value: '156', icon: '👥', change: '+8%', color: 'purple' },
    { label: 'Выручка', value: '₽124,560', icon: '💰', change: '+23%', color: 'orange' },
  ];

  const recentOrders = [
    { id: '#001', client: 'Иванов А.', amount: '₽4,200', status: 'Доставлен', time: '2 ч назад' },
    { id: '#002', client: 'Петрова М.', amount: '₽3,800', status: 'В пути', time: '4 ч назад' },
    { id: '#003', client: 'Сидоров П.', amount: '₽5,600', status: 'Обработка', time: '6 ч назад' },
    { id: '#004', client: 'Козлова Е.', amount: '₽2,900', status: 'Доставлен', time: '1 день назад' },
  ];

  return (
    <DashboardLayout userRole={userRole}>
      <div className="dashboard-page">
        {/* Заголовок и быстрые действия */}
        <div className="dashboard-header">
          <div>
            <h1 className="page-title">Добро пожаловать, Алексей! 👋</h1>
            <p className="page-subtitle">Вот что происходит в вашей системе сегодня</p>
          </div>
          <div className="header-actions">
            <Button icon="➕" variant="primary">
              Новый заказ
            </Button>
            <Button icon="📊" variant="glass">
              Экспорт отчета
            </Button>
          </div>
        </div>

        {/* Статистика */}
        <div className="stats-grid">
          {stats.map((stat, index) => (
            <Card key={index} className="stat-card">
              <div className="stat-content">
                <div className="stat-icon" style={{ background: `var(--${stat.color}-gradient)` }}>
                  {stat.icon}
                </div>
                <div className="stat-info">
                  <div className="stat-value">{stat.value}</div>
                  <div className="stat-label">{stat.label}</div>
                </div>
                <div className={`stat-change ${stat.change.startsWith('+') ? 'positive' : 'negative'}`}>
                  {stat.change}
                </div>
              </div>
            </Card>
          ))}
        </div>

        {/* Основной контент */}
        <div className="content-grid">
          {/* Последние заказы */}
          <Card title="📋 Последние заказы" className="orders-card">
            <div className="orders-table">
              {recentOrders.map((order) => (
                <div key={order.id} className="order-row">
                  <div className="order-id">{order.id}</div>
                  <div className="order-client">{order.client}</div>
                  <div className="order-amount">{order.amount}</div>
                  <div className={`order-status status-${order.status.toLowerCase()}`}>
                    {order.status}
                  </div>
                  <div className="order-time">{order.time}</div>
                </div>
              ))}
            </div>
            <div className="card-footer">
              <Button variant="glass" size="small">
                Показать все заказы →
              </Button>
            </div>
          </Card>

          {/* Быстрые действия */}
          <Card title="⚡ Быстрые действия" className="quick-actions-card">
            <div className="quick-actions-grid">
              <button className="quick-action">
                <span className="action-icon">👤</span>
                <span className="action-label">Добавить клиента</span>
              </button>
              <button className="quick-action">
                <span className="action-icon">📦</span>
                <span className="action-label">Новый товар</span>
              </button>
              <button className="quick-action">
                <span className="action-icon">🚚</span>
                <span className="action-label">Создать доставку</span>
              </button>
              <button className="quick-action">
                <span className="action-icon">📊</span>
                <span className="action-label">Отчет за день</span>
              </button>
              <button className="quick-action">
                <span className="action-icon">💰</span>
                <span className="action-label">Платежи</span>
              </button>
              <button className="quick-action">
                <span className="action-icon">📱</span>
                <span className="action-label">Уведомления</span>
              </button>
            </div>
          </Card>
        </div>

        {/* Поиск и фильтры */}
        <Card title="🔍 Поиск и фильтры" className="filters-card">
          <div className="filters-grid">
            <Input 
              icon="🔍"
              placeholder="Поиск по клиентам, заказам..."
            />
            <div className="filter-buttons">
              <Button variant="glass" size="small">Сегодня</Button>
              <Button variant="glass" size="small">Неделя</Button>
              <Button variant="glass" size="small">Месяц</Button>
              <Button variant="primary" size="small">Применить фильтры</Button>
            </div>
          </div>
        </Card>
      </div>
    </DashboardLayout>
  );
};

export default DashboardPage;