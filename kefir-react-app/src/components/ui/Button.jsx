import React from 'react';
import './Button.css';

const Button = ({
  children,
  variant = 'primary',
  size = 'medium',
  icon,
  onClick,
  className = '',
  disabled = false,
  type = 'button',
  ...props
}) => {
  const getVariantClass = () => {
    switch (variant) {
      case 'primary': return 'btn-primary-gradient';
      case 'secondary': return 'btn-secondary-gradient';
      case 'success': return 'btn-success-gradient';
      case 'warning': return 'btn-warning-gradient';
      case 'danger': return 'btn-danger-gradient';
      case 'glass': return 'btn-glass';
      default: return 'btn-primary-gradient';
    }
  };

  const getSizeClass = () => {
    switch (size) {
      case 'small': return 'btn-small';
      case 'medium': return 'btn-medium';
      case 'large': return 'btn-large';
      default: return 'btn-medium';
    }
  };

  return (
    <button
      type={type}
      className={`btn ${getVariantClass()} ${getSizeClass()} ${className}`}
      onClick={onClick}
      disabled={disabled}
      {...props}
    >
      {icon && <span className="btn-icon">{icon}</span>}
      <span className="btn-text">{children}</span>
    </button>
  );
};

export default Button;