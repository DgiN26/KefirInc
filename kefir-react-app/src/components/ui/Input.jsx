import React from 'react';
import './Input.css';

const Input = ({ 
  label, 
  icon, 
  error, 
  success, 
  className = '', 
  ...props 
}) => {
  return (
    <div className={`input-group ${className}`}>
      {label && <label className="input-label">{label}</label>}
      <div className="input-wrapper">
        {icon && <span className="input-icon">{icon}</span>}
        <input className={`input-field ${error ? 'error' : ''} ${success ? 'success' : ''}`} {...props} />
      </div>
      {error && <span className="input-error">{error}</span>}
      {success && <span className="input-success">{success}</span>}
    </div>
  );
};

export default Input;