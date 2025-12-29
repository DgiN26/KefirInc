// src/pages/client/ClientPortal.jsx
import React from 'react';

const ClientPortal = () => {
  return (
    <div className="container mt-4">
      <h1>🛍️ Магазин "KEFIR"</h1>
      <p>Добро пожаловать в наш магазин!</p>
      
      <div className="row mt-4">
        <div className="col-md-4 mb-3">
          <div className="card">
            <div className="card-body">
              <h5>📦 Каталог товаров</h5>
              <p>Выберите товары для покупки</p>
            </div>
          </div>
        </div>
        <div className="col-md-4 mb-3">
          <div className="card">
            <div className="card-body">
              <h5>🛒 Ваша корзина</h5>
              <p>Просмотрите выбранные товары</p>
            </div>
          </div>
        </div>
        <div className="col-md-4 mb-3">
          <div className="card">
            <div className="card-body">
              <h5>🚚 Доставка</h5>
              <p>Быстрая доставка за 2 часа</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ClientPortal;
