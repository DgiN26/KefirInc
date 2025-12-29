// src/components/office/OfficeProblems.jsx
import React, { useState } from 'react';

const OfficeProblems = ({ onBack }) => {
  const [problems, setProblems] = useState([
    {
      id: 1,
      orderId: 1001,
      productId: 501,
      collectorId: 'COLLECTOR_1',
      clientName: 'Иван Иванов',
      clientEmail: 'ivan@example.com',
      details: 'Товар "Ноутбук ASUS ROG" отсутствует на складе',
      status: 'PENDING',
      createdAt: new Date().toISOString(),
      selected: true
    },
    {
      id: 2,
      orderId: 1002,
      productId: 502,
      collectorId: 'COLLECTOR_2',
      clientName: 'Мария Петрова',
      clientEmail: 'maria@example.com',
      details: 'Товар "Мышь Logitech MX" поврежден при осмотре',
      status: 'PENDING',
      createdAt: new Date(Date.now() - 3600000).toISOString(),
      selected: false
    },
    {
      id: 3,
      orderId: 1003,
      productId: 503,
      collectorId: 'COLLECTOR_3',
      clientName: 'Алексей Сидоров',
      clientEmail: 'alexey@example.com',
      details: 'Товар "Клавиатура Mechanical" не соответствует заказу',
      status: 'NOTIFIED',
      createdAt: new Date(Date.now() - 7200000).toISOString(),
      selected: false
    },
    {
      id: 4,
      orderId: 1004,
      productId: 504,
      collectorId: 'COLLECTOR_1',
      clientName: 'Екатерина Волкова',
      clientEmail: 'ekaterina@example.com',
      details: 'Товар "Монитор 27"" временно отсутствует',
      status: 'PENDING',
      createdAt: new Date(Date.now() - 1800000).toISOString(),
      selected: false
    }
  ]);

  const [emailMessage, setEmailMessage] = useState('');
  const [actionMessage, setActionMessage] = useState('');

  const selectedProblem = problems.find(p => p.selected) || problems[0];

  const handleSelectProblem = (problemId) => {
    const updatedProblems = problems.map(p => ({
      ...p,
      selected: p.id === problemId
    }));
    setProblems(updatedProblems);
    
    const selected = updatedProblems.find(p => p.id === problemId);
    if (selected) {
      setEmailMessage(`Уважаемый(ая) ${selected.clientName},\n\nВ вашем заказе #${selected.orderId} возникла проблема: ${selected.details}\n\nПожалуйста, выберите один из вариантов:\n1. Продолжить сборку без этого товара\n2. Отменить весь заказ\n3. Подождать до появления товара\n\nС уважением,\nКоманда KEFIR Logistics`);
      setActionMessage('');
    }
  };

  const handleSendEmail = () => {
    if (!selectedProblem) return;
    
    alert(`Email отправлен клиенту: ${selectedProblem.clientEmail}`);
    
    const updatedProblems = problems.map(p => 
      p.id === selectedProblem.id 
        ? { ...p, status: 'NOTIFIED' }
        : p
    );
    setProblems(updatedProblems);
  };

  const handleTakeAction = (action) => {
    if (!selectedProblem) return;
    
    let message = '';
    let newStatus = 'RESOLVED';
    
    switch(action) {
      case 'APPROVE':
        message = `Заказ #${selectedProblem.orderId} одобрен для продолжения сборки без товара #${selectedProblem.productId}. Сборщик уведомлен.`;
        break;
      case 'CANCEL':
        message = `Заказ #${selectedProblem.orderId} отменен. Клиент уведомлен, деньги возвращены.`;
        break;
      case 'WAIT':
        message = `Заказ #${selectedProblem.orderId} поставлен на ожидание. Клиент уведомлен о задержке.`;
        newStatus = 'WAITING';
        break;
      default:
        return;
    }
    
    setActionMessage(message);
    
    const updatedProblems = problems.map(p => 
      p.id === selectedProblem.id 
        ? { ...p, status: newStatus }
        : p
    );
    setProblems(updatedProblems);
  };

  const getStatusColor = (status) => {
    switch(status) {
      case 'PENDING': return 'bg-red-100 text-red-800';
      case 'NOTIFIED': return 'bg-yellow-100 text-yellow-800';
      case 'WAITING': return 'bg-blue-100 text-blue-800';
      case 'RESOLVED': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusText = (status) => {
    switch(status) {
      case 'PENDING': return 'В ожидании';
      case 'NOTIFIED': return 'Клиент уведомлен';
      case 'WAITING': return 'Ожидание';
      case 'RESOLVED': return 'Решено';
      default: return status;
    }
  };

  return (
    <div className="h-full">
      {/* Заголовок */}
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <button
            onClick={onBack}
            className="text-gray-600 hover:text-black flex items-center gap-2"
          >
            <span>←</span>
            <span>Назад к дашборду</span>
          </button>
          <h1 className="text-3xl font-bold text-black">⚠️ Управление проблемами</h1>
        </div>
        <div className="text-right">
          <p className="text-sm text-gray-600">Активных проблем: {problems.filter(p => p.status === 'PENDING').length}</p>
          <p className="text-sm text-gray-600">Требуют внимания: {problems.filter(p => p.status === 'PENDING' || p.status === 'NOTIFIED').length}</p>
        </div>
      </div>

      {/* Основное содержимое */}
      <div className="flex gap-8 h-[calc(100vh-200px)]">
        {/* Левая колонка - список проблем (30%) */}
        <div className="w-[30%] flex flex-col">
          <div className="bg-white border-2 border-black rounded-xl p-4 mb-4">
            <h3 className="font-bold text-black mb-3">Фильтры</h3>
            <div className="flex flex-wrap gap-2">
              {['Все', 'В ожидании', 'Уведомлены', 'Решены'].map((filter) => (
                <button
                  key={filter}
                  className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm hover:bg-gray-50"
                >
                  {filter}
                </button>
              ))}
            </div>
          </div>

          <div className="flex-1 overflow-y-auto">
            <div className="space-y-3">
              {problems.map((problem) => (
                <div
                  key={problem.id}
                  onClick={() => handleSelectProblem(problem.id)}
                  className={`p-4 border-2 rounded-xl cursor-pointer transition-all ${problem.selected
                    ? 'border-black bg-black text-white'
                    : 'border-gray-300 hover:border-gray-400'
                  }`}
                >
                  <div className="flex justify-between items-start mb-2">
                    <div>
                      <h4 className={`font-bold ${problem.selected ? 'text-white' : 'text-black'}`}>
                        Заказ #{problem.orderId}
                      </h4>
                      <p className={`text-sm ${problem.selected ? 'text-gray-300' : 'text-gray-600'} mt-1`}>
                        {problem.clientName}
                      </p>
                    </div>
                    <span className={`px-2 py-1 text-xs font-bold rounded ${problem.selected
                      ? 'bg-white text-black'
                      : getStatusColor(problem.status)
                    }`}>
                      {getStatusText(problem.status)}
                    </span>
                  </div>
                  
                  <p className={`text-sm mb-3 ${problem.selected ? 'text-gray-300' : 'text-gray-800'}`}>
                    {problem.details.length > 80 ? `${problem.details.substring(0, 80)}...` : problem.details}
                  </p>
                  
                  <div className="flex items-center justify-between text-xs">
                    <span className={problem.selected ? 'text-gray-300' : 'text-gray-600'}>
                      👷 {problem.collectorId}
                    </span>
                    <span className={problem.selected ? 'text-gray-300' : 'text-gray-600'}>
                      {new Date(problem.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Центральная колонка - детали проблемы (40%) */}
        <div className="w-[40%] flex flex-col">
          <div className="bg-white border-2 border-black rounded-xl p-6 flex-1">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-bold text-black">
                Проблема #{selectedProblem?.id || 'N/A'}
              </h2>
              <span className="px-3 py-1 bg-black text-white text-sm font-bold rounded-lg">
                Заказ #{selectedProblem?.orderId || 'N/A'}
              </span>
            </div>

            {/* Информация о проблеме */}
            <div className="space-y-4 mb-8">
              <div>
                <h4 className="text-sm font-medium text-gray-600 mb-1">Сборщик</h4>
                <p className="text-lg font-bold text-black">{selectedProblem?.collectorId || 'Не указан'}</p>
              </div>
              
              <div>
                <h4 className="text-sm font-medium text-gray-600 mb-1">Клиент</h4>
                <p className="text-lg font-bold text-black">{selectedProblem?.clientName || 'Не указан'}</p>
                <p className="text-gray-600">{selectedProblem?.clientEmail || 'Нет email'}</p>
              </div>
              
              <div>
                <h4 className="text-sm font-medium text-gray-600 mb-1">Товар</h4>
                <p className="text-lg font-bold text-black">#{selectedProblem?.productId || 'N/A'}</p>
              </div>
              
              <div>
                <h4 className="text-sm font-medium text-gray-600 mb-1">Описание проблемы</h4>
                <div className="p-4 bg-gray-50 border border-gray-300 rounded-lg">
                  <p className="text-gray-800">{selectedProblem?.details || 'Нет описания'}</p>
                </div>
              </div>
              
              <div>
                <h4 className="text-sm font-medium text-gray-600 mb-1">Время создания</h4>
                <p className="text-gray-800">
                  {selectedProblem ? new Date(selectedProblem.createdAt).toLocaleString('ru-RU') : 'Неизвестно'}
                </p>
              </div>
            </div>

            {/* Действия */}
            <div className="space-y-4">
              <h3 className="font-bold text-black">Действия по проблеме</h3>
              
              <div className="grid grid-cols-3 gap-3">
                <button
                  onClick={() => handleTakeAction('APPROVE')}
                  className="p-4 bg-green-50 border-2 border-green-500 text-green-700 rounded-lg hover:bg-green-100 flex flex-col items-center"
                  disabled={selectedProblem?.status === 'RESOLVED'}
                >
                  <span className="text-2xl mb-2">✅</span>
                  <span className="text-sm font-medium">Одобрить</span>
                  <span className="text-xs mt-1">Продолжить без товара</span>
                </button>
                
                <button
                  onClick={() => handleTakeAction('CANCEL')}
                  className="p-4 bg-red-50 border-2 border-red-500 text-red-700 rounded-lg hover:bg-red-100 flex flex-col items-center"
                  disabled={selectedProblem?.status === 'RESOLVED'}
                >
                  <span className="text-2xl mb-2">❌</span>
                  <span className="text-sm font-medium">Отменить</span>
                  <span className="text-xs mt-1">Весь заказ</span>
                </button>
                
                <button
                  onClick={() => handleTakeAction('WAIT')}
                  className="p-4 bg-blue-50 border-2 border-blue-500 text-blue-700 rounded-lg hover:bg-blue-100 flex flex-col items-center"
                  disabled={selectedProblem?.status === 'RESOLVED'}
                >
                  <span className="text-2xl mb-2">⏳</span>
                  <span className="text-sm font-medium">Ожидать</span>
                  <span className="text-xs mt-1">До появления</span>
                </button>
              </div>
            </div>

            {/* Сообщение о результате */}
            {actionMessage && (
              <div className="mt-6 p-4 bg-green-50 border border-green-500 rounded-lg">
                <div className="flex items-center gap-3">
                  <span className="text-2xl">✅</span>
                  <div>
                    <p className="font-medium text-green-800">Действие выполнено</p>
                    <p className="text-sm text-green-700 mt-1">{actionMessage}</p>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Правая колонка - отправка email (30%) */}
        <div className="w-[30%]">
          <div className="bg-white border-2 border-black rounded-xl p-6 h-full flex flex-col">
            <h2 className="text-xl font-bold text-black mb-6">📧 Отправка клиенту</h2>
            
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Email клиента
              </label>
              <div className="p-3 bg-gray-50 border border-gray-300 rounded-lg font-mono text-sm">
                {selectedProblem?.clientEmail || 'Нет email'}
              </div>
            </div>
            
            <div className="flex-1 mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Сообщение
              </label>
              <textarea
                value={emailMessage}
                onChange={(e) => setEmailMessage(e.target.value)}
                className="w-full h-full min-h-[200px] p-3 border-2 border-gray-300 rounded-lg focus:border-black focus:ring-0 font-mono text-sm"
                placeholder="Текст письма..."
              />
            </div>
            
            <div className="space-y-3">
              <button
                onClick={handleSendEmail}
                disabled={!selectedProblem?.clientEmail || selectedProblem?.status === 'NOTIFIED'}
                className={`w-full py-3 rounded-lg font-bold text-lg ${!selectedProblem?.clientEmail || selectedProblem?.status === 'NOTIFIED'
                  ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                  : 'bg-black text-white hover:bg-gray-800'
                }`}
              >
                {selectedProblem?.status === 'NOTIFIED' ? '✅ Уже уведомлен' : '📧 Отправить письмо'}
              </button>
              
              <button
                onClick={() => {
                  setEmailMessage(`Уважаемый(ая) ${selectedProblem?.clientName},\n\nВаш заказ #${selectedProblem?.orderId} обрабатывается.\n\nСтатус: ${getStatusText(selectedProblem?.status)}\n\nС уважением,\nKEFIR Logistics`);
                }}
                className="w-full py-2 border-2 border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50"
              >
                📝 Использовать шаблон
              </button>
            </div>
            
            <div className="mt-6 p-4 bg-gray-50 rounded-lg">
              <p className="text-sm text-gray-600">
                <strong>Форма:</strong> Отсутствует товар, ваше решение
              </p>
              <p className="text-xs text-gray-500 mt-1">
                Клиенту предлагается выбрать один из вариантов решения
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OfficeProblems;