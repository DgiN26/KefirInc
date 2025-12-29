import React, { useState } from 'react';
import { realClientsAPI } from '../services/realApi';
import useApi from '../hooks/useApi';

const ClientList = () => {
  const [searchQuery, setSearchQuery] = useState('');
  
  // Используем хук для получения клиентов
  const { data: clients, loading, error, execute: refreshClients } = 
    useApi(realClientsAPI.getAll, [], true);

  // Поиск клиентов
  const handleSearch = async () => {
    try {
      const result = await realClientsAPI.search(searchQuery);
      // Обработка результатов поиска
    } catch (err) {
      console.error('Search error:', err);
    }
  };

  // Создание нового клиента
  const handleCreate = async () => {
    try {
      await realClientsAPI.create({
        username: 'newuser',
        email: 'newuser@example.com',
        status: 'active'
      });
      refreshClients(); // Обновляем список
    } catch (err) {
      console.error('Create error:', err);
    }
  };

  if (loading && !clients.length) {
    return <div>Загрузка клиентов...</div>;
  }

  if (error) {
    return <div>Ошибка: {error.message}</div>;
  }

  return (
    <div>
      <h2>Клиенты</h2>
      
      <div>
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Поиск клиентов..."
        />
        <button onClick={handleSearch}>Найти</button>
        <button onClick={handleCreate}>Добавить клиента</button>
      </div>

      <ul>
        {clients.map(client => (
          <li key={client.id}>
            {client.username} - {client.email} ({client.status})
          </li>
        ))}
      </ul>
    </div>
  );
};

export default ClientList;