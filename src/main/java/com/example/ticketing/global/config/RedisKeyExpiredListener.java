package com.example.ticketing.global.config;

import com.example.ticketing.seat.service.SeatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RedisKeyExpiredListener extends KeyExpirationEventMessageListener {
    private final SeatService seatService;

    public RedisKeyExpiredListener(RedisMessageListenerContainer listenerContainer, SeatService seatService) {
        super(listenerContainer);
        this.seatService = seatService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        // The expiredKey will be something like "seat:123"
        if (expiredKey != null && expiredKey.startsWith("seat:")) {
            try {
                Long seatId = Long.parseLong(expiredKey.split(":")[1]);
                // Safely update seat back to AVAILABLE in DB (if not already SOLD)
                seatService.releaseSeatInDbIfAvailable(seatId);
                log.info("Seat {} released from HOLD due to Redis TTL expiration.", seatId);
            } catch (Exception e) {
                log.error("Failed to release seat for expired key: {}", expiredKey, e);
            }
        }
    }
}
