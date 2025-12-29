// src/components/office/ProblemCard.jsx
import React from 'react';

const ProblemCard = ({ problem, isSelected, onSelect }) => {
  return (
    <div
      onClick={onSelect}
      className={`bg-white rounded-lg border-2 p-4 cursor-pointer transition-all ${isSelected
        ? 'border-black bg-gray-50'
        : 'border-gray-300 hover:border-gray-400 hover:bg-gray-50'
      }`}
    >
      <div className="flex justify-between items-start mb-2">
        <div>
          <h3 className="font-bold text-black">Проблема #{problem.id}</h3>
          <p className="text-sm text-gray-600 mt-1">Заказ #{problem.orderId} • Товар #{problem.productId}</p>
        </div>
        <div className="flex flex-col items-end">
          <span className={`px-2 py-1 text-xs font-bold rounded ${problem.status === 'PENDING'
            ? 'bg-black text-white'
            : 'bg-gray-200 text-gray-800'
          }`}>
            {problem.status === 'PENDING' ? 'В ожидании' : 'Уведомлен'}
          </span>
          <span className="text-xs text-gray-500 mt-1">
            {new Date(problem.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </span>
        </div>
      </div>
      
      <p className="text-gray-800 text-sm mb-3">{problem.details}</p>
      
      <div className="flex items-center justify-between text-sm">
        <div className="flex items-center gap-4">
          <span className="flex items-center gap-1">
            <span>👷</span>
            <span className="text-gray-700">{problem.collectorId}</span>
          </span>
          <span className="flex items-center gap-1">
            <span>👤</span>
            <span className="text-gray-700">{problem.clientName}</span>
          </span>
        </div>
        {problem.clientEmail && (
          <span className="text-gray-600 text-xs truncate max-w-[120px]">
            {problem.clientEmail}
          </span>
        )}
      </div>
    </div>
  );
};

export default ProblemCard;