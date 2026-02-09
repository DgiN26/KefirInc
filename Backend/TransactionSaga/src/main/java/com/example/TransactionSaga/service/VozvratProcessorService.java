package com.example.TransactionSaga.service;

import com.example.TransactionSaga.dto.CartItemVozvratProjection;
import com.example.TransactionSaga.entity.PayBack;
import com.example.TransactionSaga.repository.CartItemCustomRepository;
import com.example.TransactionSaga.repository.PayBackRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VozvratProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(VozvratProcessorService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private CartItemCustomRepository cartItemCustomRepository;

    @Autowired
    private PayBackRepository payBackRepository;

    @Autowired
    private LoggingService loggingService;

    @Value("${vozvrat.batch.size:100}")
    private int batchSize;

    @Transactional
    public void processVozvratRecords() {
        try {
            // 1. Найти записи с vozvrat = 'tc'
            List<CartItemVozvratProjection> records = cartItemCustomRepository.findVozvratTcRecords(batchSize);

            if (records.isEmpty()) {
                loggingService.logInfo("No records found with vozvrat = 'tc'");
                return;
            }

            int processedCount = 0;
            int errorCount = 0;

            // 2. Обработать каждую запись в отдельной транзакции
            for (CartItemVozvratProjection record : records) {
                try {
                    processSingleRecord(record);
                    processedCount++;
                } catch (Exception e) {
                    errorCount++;
                    Map<String, Object> errorDetails = new HashMap<>();
                    errorDetails.put("cartItemId", record.getId());
                    errorDetails.put("cartId", record.getCartId());
                    errorDetails.put("error", e.getMessage());
                    loggingService.logError("Failed to process vozvrat record", e, errorDetails);
                }
            }

            // 3. Залогировать итоги
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalRecords", records.size());
            summary.put("processed", processedCount);
            summary.put("errors", errorCount);
            loggingService.logInfo("Vozvrat processing completed", summary);

        } catch (Exception e) {
            loggingService.logError("Error in processVozvratRecords", e);
        }
    }

    @Transactional
    public void processSingleRecord(CartItemVozvratProjection record) {
        try {
            // 1. Проверить, не обработан ли уже этот cart_id
            if (payBackRepository.existsByCartId(record.getCartId())) {
                logger.debug("Cart {} already processed, skipping", record.getCartId());
                return;
            }

            // 2. Создать запись в pay_back
            PayBack payBack = new PayBack(
                    record.getClientId(),
                    record.getCartId(),
                    record.getPrice(),
                    record.getCreatedDate()
            );

            payBackRepository.save(payBack);

            // 3. Обновить vozvrat на 'tcc'
            updateVozvratStatus(record.getId());

            // 4. Залогировать успех
            Map<String, Object> successDetails = new HashMap<>();
            successDetails.put("cartItemId", record.getId());
            successDetails.put("cartId", record.getCartId());
            successDetails.put("userId", record.getClientId());
            successDetails.put("price", record.getPrice());
            loggingService.logInfo("Successfully processed vozvrat record", successDetails);

        } catch (Exception e) {
            throw new RuntimeException("Failed to process record: " + record.getId(), e);
        }
    }

    private void updateVozvratStatus(Long cartItemId) {
        try {
            entityManager.createNativeQuery(
                            "UPDATE cart_items SET vozvrat = 'tcc' WHERE id = :id"
                    )
                    .setParameter("id", cartItemId)
                    .executeUpdate();

            entityManager.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update vozvrat status for id: " + cartItemId, e);
        }
    }
}