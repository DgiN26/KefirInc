import { useState, useEffect } from 'react';

export const useThemeMode = () => {
  const [themeMode, setThemeMode] = useState('light');

  useEffect(() => {
    const savedTheme = localStorage.getItem('themeMode');
    if (savedTheme) {
      setThemeMode(savedTheme);
    }
  }, []);

  const toggleTheme = () => {
    const newTheme = themeMode === 'light' ? 'dark' : 'light';
    setThemeMode(newTheme);
    localStorage.setItem('themeMode', newTheme);
  };

  return { themeMode, toggleTheme };
};