import React from 'react';
import './Card.css';

const Card = ({ children, title, icon, footer, className = '', hoverable = true, ...props }) => {
  return (
    <div className={`card ${hoverable ? 'card-hoverable' : ''} ${className}`} {...props}>
      {(title || icon) && (
        <div className="card-header">
          {icon && <span className="card-icon">{icon}</span>}
          {title && <h3 className="card-title">{title}</h3>}
        </div>
      )}
      <div className="card-body">
        {children}
      </div>
      {footer && (
        <div className="card-footer">
          {footer}
        </div>
      )}
    </div>
  );
};

export default Card;