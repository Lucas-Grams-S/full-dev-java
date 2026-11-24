package com.payflow.lab;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import com.payflow.lab.service.BalanceService;
import com.payflow.lab.service.BalanceView;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
public class CacheConfig {

    private static RedisCacheConfiguration config() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(30))                       // TTL: a rede de segurança contra dado velho
                .disableCachingNullValues()                             // não guarda "não existe" (ver P2.09)
                .prefixCacheNameWith("payflow:")
                // Serializador TIPADO por cache: o JSON não leva o nome da classe (nada de @class), então não há
                // desserialização polimórfica. (O GenericJacksonJsonRedisSerializer sem tipagem padrão devolve LinkedHashMap.)
                .serializeValuesWith(SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(BalanceView.class)));
    }

    /** O que o Spring Data Redis 4 faz por padrão: escritas ASSÍNCRONAS/adiadas (put e evict). Só para o experimento. */
    @Bean
    RedisCacheManager asyncDefaultCacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory).withCacheConfiguration(BalanceService.CACHE, config()).build();
    }

    /** O do PayFlow: escritas IMEDIATAS (o put/evict só retorna depois de aplicado), sem lock. */
    @Bean
    @org.springframework.context.annotation.Primary
    RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(RedisCacheWriter.create(factory, w -> w.immediateWrites()))
                .withCacheConfiguration(BalanceService.CACHE, config()).build();
    }

    /** Escritas imediatas + lock por cache (chave de lock no Redis; vale entre instâncias): aí sync = true serializa a carga. */
    @Bean
    RedisCacheManager lockingCacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(RedisCacheWriter.create(factory, w -> w.immediateWrites().cacheLocking(l -> l.enable())))
                .withCacheConfiguration(BalanceService.CACHE, config()).build();
    }

    /** Escritas imediatas + put/evict ADIADOS até o commit da transação em andamento (TransactionAwareCacheDecorator). */
    @Bean
    RedisCacheManager txAwareCacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(RedisCacheWriter.create(factory, w -> w.immediateWrites()))
                .withCacheConfiguration(BalanceService.CACHE, config()).transactionAware().build();
    }

    /**
     * Um Redis que NÃO existe (porta 1): simula a queda do cache. A fábrica de conexões NÃO é um bean: um segundo bean
     * RedisConnectionFactory faria a autoconfiguração do Boot recuar e TODO o laboratório passaria a usar o Redis morto.
     */
    @Bean
    RedisCacheManager deadCacheManager() {
        var dead = new org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory("127.0.0.1", 1);
        dead.afterPropertiesSet();
        return RedisCacheManager.builder(RedisCacheWriter.create(dead, w -> w.immediateWrites()))
                .withCacheConfiguration(BalanceService.CACHE, config()).build();
    }

    /** O cache é OTIMIZAÇÃO, não dependência: se o Redis falhar, registra e segue para o banco (só quando lab.cache.tolerant=true). */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "lab.cache.tolerant", havingValue = "true")
    org.springframework.cache.annotation.CachingConfigurer tolerantCaching() {
        return new org.springframework.cache.annotation.CachingConfigurer() {
            @Override
            public org.springframework.cache.interceptor.CacheErrorHandler errorHandler() {
                return new org.springframework.cache.interceptor.CacheErrorHandler() {
                    private void warn(String op, RuntimeException e) {
                        System.out.println("CACHE INDISPONÍVEL (" + op + "): " + e.getClass().getSimpleName() + " -> seguindo para o banco");
                    }
                    @Override public void handleCacheGetError(RuntimeException e, org.springframework.cache.Cache c, Object k) { warn("get", e); }
                    @Override public void handleCachePutError(RuntimeException e, org.springframework.cache.Cache c, Object k, Object v) { warn("put", e); }
                    @Override public void handleCacheEvictError(RuntimeException e, org.springframework.cache.Cache c, Object k) { warn("evict", e); }
                    @Override public void handleCacheClearError(RuntimeException e, org.springframework.cache.Cache c) { warn("clear", e); }
                };
            }
        };
    }
}
