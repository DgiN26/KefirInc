// src/components/office/PollingManager.jsx
import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';

const PollingManager = ({ onProblemsUpdate, onStatsUpdate, children }) => {
    const [interval, setInterval] = useState(15000);
    const [isActive, setIsActive] = useState(true);
    const [lastUpdate, setLastUpdate] = useState(null);
    const [errorCount, setErrorCount] = useState(0);
    
    const pollingRef = useRef(null);
    const timeoutRef = useRef(null);

    // Запуск polling
    const startPolling = () => {
        stopPolling();
        
        const poll = async () => {
            try {
                const params = { interval: interval / 1000 };
                if (lastUpdate) {
                    params.lastUpdate = lastUpdate;
                }
                
                const response = await axios.get(
                    'http://localhost:8080/api/office/problems/active',
                    { params, timeout: 10000 }
                );
                
                if (response.data.success) {
                    onProblemsUpdate?.(response.data.problems);
                    setLastUpdate(response.data.lastUpdatedTimestamp);
                    setErrorCount(0);
                    
                    // Получаем статистику
                    if (onStatsUpdate) {
                        const statsResponse = await axios.get(
                            'http://localhost:8080/api/office/problems/stats'
                        );
                        onStatsUpdate(statsResponse.data);
                    }
                }
            } catch (error) {
                console.error('Polling error:', error);
                setErrorCount(prev => prev + 1);
                
                // Экспоненциальная backoff при ошибках
                if (errorCount > 3) {
                    const backoffDelay = Math.min(interval * (2 ** errorCount), 300000); // max 5 мин
                    console.warn(`Backoff ${backoffDelay}ms due to ${errorCount} errors`);
                    
                    if (timeoutRef.current) {
                        clearTimeout(timeoutRef.current);
                    }
                    
                    timeoutRef.current = setTimeout(() => {
                        if (isActive) {
                            poll();
                        }
                    }, backoffDelay);
                    return;
                }
            }
            
            // Следующий polling
            if (isActive) {
                pollingRef.current = setTimeout(poll, interval);
            }
        };
        
        poll();
    };

    const stopPolling = () => {
        if (pollingRef.current) {
            clearTimeout(pollingRef.current);
            pollingRef.current = null;
        }
        if (timeoutRef.current) {
            clearTimeout(timeoutRef.current);
            timeoutRef.current = null;
        }
    };

    // Управление polling
    useEffect(() => {
        if (isActive) {
            startPolling();
        } else {
            stopPolling();
        }
        
        return () => {
            stopPolling();
        };
    }, [isActive, interval]);

    // Автоматическое переподключение
    useEffect(() => {
        if (errorCount > 10) {
            console.warn('Too many errors, pausing polling');
            setIsActive(false);
            
            // Автоматическое возобновление через 30 секунд
            setTimeout(() => {
                setErrorCount(0);
                setIsActive(true);
            }, 30000);
        }
    }, [errorCount]);

    return children({
        interval,
        setInterval,
        isActive,
        setIsActive,
        lastUpdate,
        errorCount,
        startPolling,
        stopPolling
    });
};

export default PollingManager;