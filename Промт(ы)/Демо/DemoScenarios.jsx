import React, { useState } from 'react';
import { api } from '../services/api';
import './DemoScenarios.css';

const DemoScenarios = () => {
  const [runningDemo, setRunningDemo] = useState(null);
  const [demoResult, setDemoResult] = useState(null);
  
  const scenarios = [
    {
      id: 'complete',
      name: '🎯 Полная демонстрация',
      endpoint: api.runCompleteDemo,
      description: 'Проблема транзакции → Решение через Saga',
      steps: [
        '1. Запуск всех сервисов',
        '2. Создание заказа клиентом',
        '3. Ошибка: отсутствующий товар',
        '4. Звонок офиса клиенту',
        '5. ❌ Проблема: частичная доставка',
        '6. 💡 Решение: полный перезапуск через Saga',
        '7. ✅ Результат: полная доставка за 15 минут'
      ],
      color: '#4CAF50'
    },
    {
      id: 'problem',
      name: '⚠️ Демонстрация проблемы',
      endpoint: api.runProblemDemo,
      description: 'Только проблема: неполная доставка',
      steps: [
        '1. Клиент создает заказ на 5 товаров',
        '2. Сборщик находит только 3 товара',
        '3. Офис звонит клиенту',
        '4. Транзакция закрывается частично',
        '5. Клиент получает неполный заказ',
        '6. Доставка занимает 1 час вместо 15 минут',
        '7. Клиент недоволен'
      ],
      color: '#f44336'
    },
    {
      id: 'solution',
      name: '💡 Демонстрация решения',
      endpoint: api.runSolutionDemo,
      description: 'Решение через полный перезапуск транзакции',
      steps: [
        '1. Обнаружение ошибки',
        '2. Полная отмена транзакции',
        '3. Возврат ВСЕХ денег клиенту',
        '4. Создание новой транзакции',
        '5. Перепроверка ВСЕХ товаров',
        '6. Клиент оплачивает только доступное',
        '7. Доставка за 15 минут, клиент доволен'
      ],
      color: '#2196F3'
    }
  ];
  
  const runDemo = async (scenario) => {
    setRunningDemo(scenario.id);
    setDemoResult(null);
    
    try {
      const result = await scenario.endpoint();
      setDemoResult({
        success: true,
        data: result,
        timestamp: new Date().toLocaleTimeString()
      });
    } catch (error) {
      setDemoResult({
        success: false,
        error: error.message,
        timestamp: new Date().toLocaleTimeString()
      });
    } finally {
      setRunningDemo(null);
    }
  };
  
  return (
    <div className="demo-scenarios">
      <h2>🎬 Демо-сценарии KEFIR</h2>
      <p className="subtitle">Автоматическая демонстрация работы системы</p>
      
      <div className="scenarios-grid">
        {scenarios.map((scenario) => (
          <div 
            key={scenario.id} 
            className="scenario-card"
            style={{ borderLeftColor: scenario.color }}
          >
            <div className="scenario-header">
              <h3>{scenario.name}</h3>
              <span className="scenario-description">{scenario.description}</span>
            </div>
            
            <div className="scenario-steps">
              <h4>Шаги сценария:</h4>
              <ul>
                {scenario.steps.map((step, index) => (
                  <li key={index}>{step}</li>
                ))}
              </ul>
            </div>
            
            <button
              className="btn-run-demo"
              onClick={() => runDemo(scenario)}
              disabled={runningDemo === scenario.id}
              style={{ backgroundColor: scenario.color }}
            >
              {runningDemo === scenario.id ? '🔄 Выполняется...' : '▶️ Запустить демо'}
            </button>
          </div>
        ))}
      </div>
      
      {demoResult && (
        <div className={`demo-result ${demoResult.success ? 'success' : 'error'}`}>
          <h3>Результат демо:</h3>
          <div className="result-timestamp">Время: {demoResult.timestamp}</div>
          
          {demoResult.success ? (
            <div className="result-success">
              <h4>✅ Демо успешно выполнено!</h4>
              <div className="result-details">
                <strong>Статус:</strong> {demoResult.data.status || 'COMPLETED'}<br/>
                <strong>Операция:</strong> {demoResult.data.operation || 'demo'}<br/>
                {demoResult.data.message && (
                  <>
                    <strong>Сообщение:</strong> {demoResult.data.message}
                  </>
                )}
              </div>
            </div>
          ) : (
            <div className="result-error">
              <h4>❌ Ошибка выполнения демо</h4>
              <div className="result-details">
                <strong>Ошибка:</strong> {demoResult.error}
              </div>
            </div>
          )}
          
          <button 
            className="btn-close-result"
            onClick={() => setDemoResult(null)}
          >
            Закрыть
          </button>
        </div>
      )}
    </div>
  );
};

export default DemoScenarios;