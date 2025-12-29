package com.example.delivery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DeliveryService {
    
    @Autowired
    private DeliveryRepository deliveryRepository;
    
    public Delivery createDelivery(Integer orderId, Integer clientId, String address, String phone) {
        Delivery delivery = new Delivery(orderId, clientId, address, phone);
        return deliveryRepository.save(delivery);
    }
    
    public Delivery assignCourier(Integer deliveryId, Integer courierId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
        
        delivery.setAssignedCourierId(courierId);
        delivery.setDeliveryStatus(DeliveryStatus.ASSIGNED);
        delivery.setEstimatedDeliveryTime(LocalDateTime.now().plusHours(2));
        
        return deliveryRepository.save(delivery);
    }
    
    public Delivery updateStatus(Integer deliveryId, DeliveryStatus newStatus) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
        
        delivery.setDeliveryStatus(newStatus);
        
        if (newStatus == DeliveryStatus.DELIVERED) {
            delivery.setActualDeliveryTime(LocalDateTime.now());
        }
        
        return deliveryRepository.save(delivery);
    }
    
    public List<Delivery> getClientDeliveries(Integer clientId) {
        return deliveryRepository.findByClientId(clientId);
    }
    
    public List<Delivery> getCourierDeliveries(Integer courierId) {
        return deliveryRepository.findByAssignedCourierId(courierId);
    }
    
    public List<Delivery> getActiveDeliveries() {
        return deliveryRepository.findByDeliveryStatusIn(List.of(
            DeliveryStatus.ASSIGNED, 
            DeliveryStatus.PICKED_UP, 
            DeliveryStatus.IN_TRANSIT, 
            DeliveryStatus.OUT_FOR_DELIVERY
        ));
    }
    
    // ВАЖНО: Этот метод должен возвращать List<Delivery>
    public List<Delivery> getDeliveriesByOrderId(Integer orderId) {
        return deliveryRepository.findByOrderId(orderId);
    }

     // Получение ВСЕХ доставок
    public List<Delivery> getAllDeliveries() {
        return deliveryRepository.findAll();
    }
    
    // Этот метод возвращает Optional<Delivery>
    public Optional<Delivery> getFirstDeliveryByOrderId(Integer orderId) {
        List<Delivery> deliveries = deliveryRepository.findByOrderId(orderId);
        return deliveries.isEmpty() ? Optional.empty() : Optional.of(deliveries.get(0));
    }
    
    public Delivery cancelDelivery(Integer deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
        
        delivery.setDeliveryStatus(DeliveryStatus.CANCELLED);
        return deliveryRepository.save(delivery);
    }
    
    public Optional<Delivery> getDeliveryById(Integer deliveryId) {
        return deliveryRepository.findById(deliveryId);
    }
}