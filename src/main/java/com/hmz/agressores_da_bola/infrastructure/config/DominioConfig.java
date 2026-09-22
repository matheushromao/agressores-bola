package com.hmz.agressores_da_bola.infrastructure.config;

import com.hmz.agressores_da_bola.domain.sorteio.BalanceadorDeTimes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Registra como beans as peças do domínio que não conhecem o Spring, e o
 * relógio que os casos de uso usam no lugar de {@code LocalDateTime.now()}
 * — trocável por um relógio fixo nos testes.
 */
@Configuration
public class DominioConfig {

    @Bean
    BalanceadorDeTimes balanceadorDeTimes() {
        return new BalanceadorDeTimes();
    }

    @Bean
    Clock relogio() {
        return Clock.systemDefaultZone();
    }
}
