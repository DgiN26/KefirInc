// src/pages/client/ClientCart.jsx
import React from 'react';

const ClientCart = () => {
  return (
    <div className="container mt-4">
      <h1>🛒 Корзина</h1>
      <p>Ваши товары в корзине</p>
      
      <div className="card mt-3">
        <div className="card-body">
          <h5>Товары в корзине:</h5>
          <ul className="list-group">
            <li className="list-group-item">Ноутбук - 1 шт. - 45,990 ₽</li>
            <li className="list-group-item">Мышь - 2 шт. - 2,980 ₽</li>
          </ul>
          <div className="mt-3">
            <strong>Итого: 48,970 ₽</strong>
          </div>
          <button className="btn btn-primary mt-3">
            Оформить заказ
          </button>
        </div>
      </div>
    </div>
  );
};

export default ClientCart;
