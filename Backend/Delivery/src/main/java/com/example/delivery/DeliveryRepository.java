package com.example.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Integer> {
    
    List<Delivery> findByClientId(Integer clientId);
    
    List<Delivery> findByAssignedCourierId(Integer courierId);
    
    List<Delivery> findByDeliveryStatus(DeliveryStatus status);
    
    // ВАЖНО: Этот метод должен возвращать List<Delivery>
    List<Delivery> findByOrderId(Integer orderId);
    
    @Query("SELECT d FROM Delivery d WHERE d.deliveryStatus IN :statuses")
    List<Delivery> findByDeliveryStatusIn(@Param("statuses") List<DeliveryStatus> statuses);
    
    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.assignedCourierId = :courierId AND d.deliveryStatus IN :statuses")
    Long countActiveDeliveriesByCourier(@Param("courierId") Integer courierId, 
                                       @Param("statuses") List<DeliveryStatus> statuses);
}