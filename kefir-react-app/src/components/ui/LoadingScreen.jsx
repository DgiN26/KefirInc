import React from 'react';
import './LoadingScreen.css';

const LoadingScreen = ({ message = 'Загрузка...' }) => {
  return (
    <div className="loading-screen">
      <div className="loading-content">
        <div className="loading-spinner">
          <div className="spinner-circle"></div>
          <div className="spinner-gradient"></div>
        </div>
        <div className="loading-message">{message}</div>
      </div>
    </div>
  );
};

export default LoadingScreen;