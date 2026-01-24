// src/components/layout/CollectorLayout.jsx
import React, { useState, useEffect, useRef } from 'react';

const CollectorLayout = ({ children, userRole, userData, onLogout, dbConnected, dbError }) => {
  const [showCaptcha, setShowCaptcha] = useState(false);
  const [captchaAnswer, setCaptchaAnswer] = useState('');
  const [captchaError, setCaptchaError] = useState(false);
  const [captchaQuestion] = useState(() => generateMathQuestion());
  const [timeLeft, setTimeLeft] = useState(15);
  const [isTimeUp, setIsTimeUp] = useState(false);
  const timerRef = useRef(null);

  // Генерация математического вопроса
  function generateMathQuestion() {
    const questions = [
      { question: "Сколько будет 5 × 8?", answer: "40" },
      { question: "Сколько будет 12 + 15?", answer: "27" },
      { question: "Сколько будет 100 ÷ 4?", answer: "25" },
      { question: "Сколько будет 17 - 9?", answer: "8" },
      { question: "Сколько будет 6 × 7?", answer: "42" },
      { question: "Сколько будет 81 ÷ 9?", answer: "9" },
      { question: "Сколько будет 13 + 22?", answer: "35" },
      { question: "Сколько будет 64 ÷ 8?", answer: "8" },
      { question: "Сколько будет 7 × 6?", answer: "42" },
      { question: "Сколько будет 45 - 18?", answer: "27" },
      { question: "Назови число π до 12 знака после запятой (первые 12 цифр после запятой)", answer: "141592653589" },
      { question: "Квадратный корень из 144?", answer: "12" },
      { question: "Сколько градусов в прямом угле?", answer: "90" },
      { question: "3² + 4² = ?", answer: "25" },
      { question: "10% от 250?", answer: "25" },
      { question: "15 × 6 = ?", answer: "90" },
      { question: "Сумма углов треугольника?", answer: "180" },
      { question: "2 в степени 5?", answer: "32" },
      { question: "Площадь квадрата со стороной 5?", answer: "25" },
      { question: "Сколько секунд в часе?", answer: "3600" }
    ];
    
    return questions[Math.floor(Math.random() * questions.length)];
  }

  // Запуск таймера
  const startTimer = () => {
    setTimeLeft(15);
    setIsTimeUp(false);
    
    if (timerRef.current) {
      clearInterval(timerRef.current);
    }
    
    timerRef.current = setInterval(() => {
      setTimeLeft((prevTime) => {
        if (prevTime <= 1) {
          clearInterval(timerRef.current);
          setIsTimeUp(true);
          return 0;
        }
        return prevTime - 1;
      });
    }, 1000);
  };

  // Остановка таймера
  const stopTimer = () => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  };

  // Обработчик выхода
  const handleLogoutClick = () => {
    setShowCaptcha(true);
    setCaptchaAnswer('');
    setCaptchaError(false);
    startTimer();
  };

  // Проверка капчи
  const handleCaptchaSubmit = (e) => {
    e.preventDefault();
    
    if (isTimeUp) {
      setCaptchaError(true);
      setCaptchaAnswer('');
      setCaptchaError("⏰ Время вышло! Попробуйте снова.");
      return;
    }
    
    if (captchaAnswer.trim().toLowerCase() === captchaQuestion.answer.toLowerCase()) {
      // Капча пройдена - выполняем выход
      stopTimer();
      setShowCaptcha(false);
      onLogout();
    } else {
      // Неправильный ответ
      setCaptchaError(true);
      setCaptchaAnswer('');
      
      // Меняем вопрос через 1.5 секунды
      setTimeout(() => {
        const newQuestion = generateMathQuestion();
        captchaQuestion.question = newQuestion.question;
        captchaQuestion.answer = newQuestion.answer;
        setCaptchaError(false);
        // Перезапускаем таймер при смене вопроса
        startTimer();
      }, 1500);
    }
  };

  // Отмена выхода
  const handleCancelLogout = () => {
    stopTimer();
    setShowCaptcha(false);
    setCaptchaAnswer('');
    setCaptchaError(false);
  };

  // Очистка таймера при размонтировании
  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
      }
    };
  }, []);

  // Эффект при завершении времени
  useEffect(() => {
    if (isTimeUp && showCaptcha) {
      setCaptchaError("⏰ Время вышло! Попробуйте снова.");
      setCaptchaAnswer('');
      
      // Автоматически меняем вопрос через 2 секунды
      setTimeout(() => {
        const newQuestion = generateMathQuestion();
        captchaQuestion.question = newQuestion.question;
        captchaQuestion.answer = newQuestion.answer;
        setCaptchaError(false);
        startTimer();
      }, 2000);
    }
  }, [isTimeUp, showCaptcha]);

  return (
    <div className="collector-layout" style={styles.layout}>
      {/* Простая шапка с кнопкой выхода */}
      <header style={styles.header}>
        <div className="container-fluid">
          <div className="row align-items-center">
            <div className="col-10">
              <h1 className="comic-font mb-0" style={styles.title}>
                📦 Приложение сборщика
              </h1>
            </div>
            <div className="col-2 text-end">
              <button
                onClick={handleLogoutClick}
                style={styles.logoutButton}
                className="cursor-felt-pen comic-font"
                title="Выйти из системы"
              >
                <div style={styles.questionIcon}>?</div>
                <span style={styles.logoutText}>Выйти</span>
              </button>
            </div>
          </div>
        </div>
      </header>
      
      {/* Основной контент */}
      <main className="collector-main">
        {children}
      </main>
      
      {/* Футер */}
      <footer style={styles.footer}>
        <div className="container-fluid">
          <div className="row">
            <div className="col-12 text-center">
              <small className="text-muted comic-font">
                © {new Date().getFullYear()} KEFIR System • Сборщик: {userData?.username || 'COLLECTOR_1'} • Смена: 08:00-20:00
              </small>
            </div>
          </div>
        </div>
      </footer>

      {/* Модальное окно капчи */}
      {showCaptcha && (
        <div style={styles.captchaOverlay}>
          <div style={styles.captchaModal}>
            <div style={styles.captchaHeader}>
              <div style={styles.largeQuestionIcon}>❓</div>
              <h3 className="comic-font mb-0">Подтвердите выход</h3>
            </div>
            
            <p className="comic-font mb-2" style={styles.captchaInstruction}>
              Решите простую задачу, чтобы подтвердить, что вы человек:
            </p>
            
            {/* Таймер */}
            <div style={styles.timerContainer}>
              <div style={styles.timer}>
                <div style={styles.timerCircle}>
                  <svg width="60" height="60" viewBox="0 0 44 44">
                    <circle
                      cx="22"
                      cy="22"
                      r="20"
                      fill="none"
                      stroke={timeLeft > 5 ? "#198754" : timeLeft > 2 ? "#ffc107" : "#dc3545"}
                      strokeWidth="3"
                      strokeDasharray="125.6"
                      strokeDashoffset={125.6 - (125.6 * timeLeft) / 15}
                      transform="rotate(-90 22 22)"
                    />
                  </svg>
                  <div style={styles.timerText}>
                    {timeLeft}s
                  </div>
                </div>
                <div style={styles.timerLabel}>
                  {timeLeft > 10 ? "⏱️ Время есть" : timeLeft > 5 ? "⏳ Поторопитесь" : "🔥 СРОЧНО!"}
                </div>
              </div>
            </div>
            
            <div style={styles.captchaQuestion}>
              <h4 className="comic-font" style={styles.questionText}>
                {captchaQuestion.question}
              </h4>
              {captchaQuestion.question.includes("π") && (
                <small className="text-muted d-block mt-1" style={styles.piHint}>
                  Подсказка: π ≈ 3.14159265358979323846...
                </small>
              )}
            </div>
            
            <form onSubmit={handleCaptchaSubmit}>
              <input
                type="text"
                value={captchaAnswer}
                onChange={(e) => setCaptchaAnswer(e.target.value)}
                placeholder="Введите ответ..."
                style={styles.captchaInput}
                autoFocus
                className="comic-font"
                disabled={isTimeUp}
              />
              
              {captchaError && (
                <div style={{
                  ...styles.errorMessage,
                  backgroundColor: isTimeUp ? '#f8d7da' : '#fff3cd',
                  borderColor: isTimeUp ? '#f5c6cb' : '#ffecb5'
                }} className="comic-font">
                  {isTimeUp ? "⏰ Время вышло! Попробуйте снова." : "❌ Неправильный ответ! Попробуйте еще раз..."}
                </div>
              )}
              
              <div style={styles.captchaButtons}>
                <button
                  type="submit"
                  style={{
                    ...styles.confirmButton,
                    opacity: isTimeUp ? 0.6 : 1,
                    cursor: isTimeUp ? 'not-allowed' : 'pointer'
                  }}
                  className="cursor-felt-pen comic-font"
                  disabled={isTimeUp}
                >
                  ✅ Подтвердить
                </button>
                <button
                  type="button"
                  onClick={handleCancelLogout}
                  style={styles.cancelButton}
                  className="cursor-felt-pen comic-font ms-2"
                >
                  ❌ Отмена
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

const styles = {
  layout: {
    display: 'flex',
    flexDirection: 'column',
    height: '100vh',
    backgroundColor: '#f8f9fa',
    cursor: 'url(\'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32"><path d="M8 28l16-16-4-4L4 24z" fill="black"/><path d="M24 4l4 4-16 16-4-4z" fill="%23000000"/></svg>\') 4 28, auto',
    fontFamily: '\'Comic Sans MS\', \'Comic Neue\', cursive, sans-serif',
    overflow: 'hidden'
  },
  header: {
    padding: '10px 20px',
    backgroundColor: '#ffffff',
    borderBottom: '2px solid #000',
    height: '60px'
  },
  title: {
    fontSize: '1.2rem',
    color: '#000',
    margin: 0
  },
  logoutButton: {
    padding: '8px 12px',
    backgroundColor: '#ffffff',
    color: '#198754',
    border: '2px solid #198754',
    borderRadius: '30px',
    fontWeight: 'bold',
    fontSize: '14px',
    transition: 'all 0.3s ease',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '8px',
    minWidth: '110px',
    height: '40px',
    '&:hover': {
      backgroundColor: '#198754',
      color: 'white',
      transform: 'translateY(-2px)',
      boxShadow: '0 4px 12px rgba(25, 135, 84, 0.3)'
    },
    '&:active': {
      transform: 'translateY(0)',
      boxShadow: '0 2px 6px rgba(25, 135, 84, 0.2)'
    }
  },
  questionIcon: {
    width: '24px',
    height: '24px',
    backgroundColor: '#198754',
    color: 'white',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '16px',
    fontWeight: 'bold',
    border: '2px solid #198754',
    transition: 'all 0.3s ease'
  },
  logoutText: {
    fontSize: '14px',
    fontWeight: 'bold',
    transition: 'all 0.3s ease'
  },
  footer: {
    padding: '5px',
    backgroundColor: '#fff',
    borderTop: '2px solid #000',
    fontSize: '0.8rem'
  },
  captchaOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
    animation: 'fadeIn 0.3s ease'
  },
  captchaModal: {
    backgroundColor: '#fff',
    padding: '30px',
    borderRadius: '15px',
    border: '3px solid #198754',
    maxWidth: '500px',
    width: '90%',
    boxShadow: '0 15px 35px rgba(25, 135, 84, 0.2)',
    animation: 'slideIn 0.4s ease'
  },
  captchaHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '15px',
    marginBottom: '20px',
    paddingBottom: '15px',
    borderBottom: '2px dashed #dee2e6'
  },
  largeQuestionIcon: {
    fontSize: '32px',
    color: '#198754',
    backgroundColor: '#f0f9f4',
    width: '50px',
    height: '50px',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    border: '2px solid #198754'
  },
  captchaInstruction: {
    color: '#495057',
    fontSize: '15px',
    textAlign: 'center',
    marginBottom: '20px'
  },
  timerContainer: {
    margin: '15px 0',
    padding: '15px',
    backgroundColor: '#f0f9f4',
    borderRadius: '10px',
    border: '2px solid #d1e7dd'
  },
  timer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '8px'
  },
  timerCircle: {
    position: 'relative',
    width: '60px',
    height: '60px',
    margin: '0 auto'
  },
  timerText: {
    position: 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    fontSize: '16px',
    fontWeight: 'bold',
    color: '#198754',
    fontFamily: '\'Comic Sans MS\', \'Comic Neue\', cursive'
  },
  timerLabel: {
    fontSize: '13px',
    color: '#198754',
    fontWeight: 'bold',
    textAlign: 'center',
    padding: '4px 12px',
    backgroundColor: '#d1e7dd',
    borderRadius: '15px'
  },
  captchaQuestion: {
    backgroundColor: '#f0f9f4',
    padding: '20px',
    borderRadius: '10px',
    border: '2px solid #d1e7dd',
    margin: '15px 0',
    textAlign: 'center'
  },
  questionText: {
    color: '#198754',
    margin: 0,
    fontSize: '18px'
  },
  piHint: {
    color: '#6c757d',
    fontStyle: 'italic',
    fontSize: '12px'
  },
  captchaInput: {
    width: '100%',
    padding: '14px',
    border: '2px solid #198754',
    borderRadius: '8px',
    fontSize: '16px',
    textAlign: 'center',
    marginBottom: '15px',
    fontFamily: '\'Comic Sans MS\', \'Comic Neue\', cursive',
    transition: 'all 0.3s ease',
    backgroundColor: '#f8f9fa',
    '&:focus': {
      outline: 'none',
      borderColor: '#198754',
      boxShadow: '0 0 0 3px rgba(25, 135, 84, 0.25)',
      backgroundColor: 'white'
    },
    '&:disabled': {
      backgroundColor: '#e9ecef',
      borderColor: '#adb5bd',
      cursor: 'not-allowed'
    }
  },
  errorMessage: {
    color: '#dc3545',
    backgroundColor: '#fff3cd',
    padding: '12px',
    borderRadius: '6px',
    marginBottom: '15px',
    textAlign: 'center',
    border: '1px solid #ffecb5',
    animation: 'shake 0.5s ease',
    fontSize: '14px'
  },
  captchaButtons: {
    display: 'flex',
    justifyContent: 'center',
    gap: '15px',
    marginTop: '15px'
  },
  confirmButton: {
    padding: '12px 25px',
    backgroundColor: '#198754',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontWeight: 'bold',
    fontSize: '16px',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    flex: 1,
    '&:hover:not(:disabled)': {
      backgroundColor: '#157347',
      transform: 'translateY(-2px)',
      boxShadow: '0 4px 12px rgba(25, 135, 84, 0.3)'
    },
    '&:active:not(:disabled)': {
      transform: 'translateY(0)'
    }
  },
  cancelButton: {
    padding: '12px 25px',
    backgroundColor: '#6c757d',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    fontWeight: 'bold',
    fontSize: '16px',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    flex: 1,
    '&:hover': {
      backgroundColor: '#5a6268',
      transform: 'translateY(-2px)',
      boxShadow: '0 4px 12px rgba(108, 117, 125, 0.3)'
    },
    '&:active': {
      transform: 'translateY(0)'
    }
  }
};

// Добавляем анимации в стили
const keyframes = `
  @keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
  }
  
  @keyframes slideIn {
    from { 
      opacity: 0;
      transform: translateY(-30px) scale(0.9); 
    }
    to { 
      opacity: 1;
      transform: translateY(0) scale(1); 
    }
  }
  
  @keyframes shake {
    0%, 100% { transform: translateX(0); }
    10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
    20%, 40%, 60%, 80% { transform: translateX(5px); }
  }
  
  @keyframes pulse {
    0% { transform: scale(1); }
    50% { transform: scale(1.05); }
    100% { transform: scale(1); }
  }
  
  @keyframes greenPulse {
    0% { 
      box-shadow: 0 0 0 0 rgba(25, 135, 84, 0.4);
      transform: scale(1);
    }
    70% { 
      box-shadow: 0 0 0 10px rgba(25, 135, 84, 0);
      transform: scale(1.05);
    }
    100% { 
      box-shadow: 0 0 0 0 rgba(25, 135, 84, 0);
      transform: scale(1);
    }
  }
`;

// Вставляем CSS анимации в документ
const styleSheet = document.createElement("style");
styleSheet.innerText = keyframes;
document.head.appendChild(styleSheet);

export default CollectorLayout;