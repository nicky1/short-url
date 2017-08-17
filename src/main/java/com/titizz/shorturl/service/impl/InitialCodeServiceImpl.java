package com.titizz.shorturl.service.impl;

import com.titizz.shorturl.cache.InitialCodeCache;
import com.titizz.shorturl.cache.ServerUUIDCache;
import com.titizz.shorturl.exception.FailToAcquireDistributeLockException;
import com.titizz.shorturl.exception.NoMoreInitialCodeException;
import com.titizz.shorturl.repository.InitialCodeDao;
import com.titizz.shorturl.repository.UrlMappingDao;
import com.titizz.shorturl.service.InitialCodeService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by code4wt on 17/8/2.
 */
@Service
public class InitialCodeServiceImpl implements InitialCodeService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final Long MAX_INITIAL_CODE = 999L;

    public static final String DISTRIBUTED_LOCK_NAME = "initial_code_in_use_lock";

    @Autowired
    private InitialCodeDao initialCodeDao;

    @Autowired
    private UrlMappingDao urlMappingDao;

    @Autowired
    private InitialCodeCache initialCodeCache;

    @Autowired
    private ServerUUIDCache serverUUIDCache;

    @Autowired
    private RedissonClient redissonClient;

    private String uuid;

    private volatile Long initialCode;

    @PostConstruct
    public void init() {
        uuid = UUID.randomUUID().toString();
        logger.info("get server uuid is {}", uuid);
    }

    public Long updateInitialCode() throws InterruptedException,
            NoMoreInitialCodeException, FailToAcquireDistributeLockException {

        initialCode = tryGetInitialCode();
        logger.info("get a initial code {}", initialCode);
        return initialCode;
    }

    private Long tryGetInitialCode() throws InterruptedException,
            NoMoreInitialCodeException, FailToAcquireDistributeLockException {

        int retryCount = 3;
        while (retryCount > 0) {
            try {
                return getInitialCode();
            } catch (FailToAcquireDistributeLockException e) {
                continue;
            }
        }

        throw new FailToAcquireDistributeLockException();
    }

    /**
     * 此方法用于获取一个 initial code，方法中使用了基于 redis 的分布式锁，
     * 用于防止不同的机器获取相同的 initial code。考虑这样一种情况，某台机器
     * 服务崩溃，这台机器使用的 initial code 对应的 url_mapping 表还有空间
     * 可用。如果 initial_code 表中无新的 initial code 可分配，那么新的服务
     * 将会去"捡漏"，尝试获取崩溃的服务所使用的 initial code。当多个服务竞争
     * 同一个 initial code 时，若不加控制，则会导致不同的服务获得了同一个
     * initial code。虽然多个服务使用同一个 initial code 并不会导致错误发生，
     * 但在程序设计上，建议一个服务对应一个 initial code。
     *
     *
     * @return initial code
     * @throws NoMoreInitialCodeException
     * @throws InterruptedException
     * @throws FailToAcquireDistributeLockException
     */
    private Long getInitialCode() throws NoMoreInitialCodeException,
            InterruptedException, FailToAcquireDistributeLockException {

        RLock lock = redissonClient.getLock(DISTRIBUTED_LOCK_NAME);
        if (!lock.tryLock(3, 30, TimeUnit.SECONDS)) {
            throw new FailToAcquireDistributeLockException();
        }

        try {
            initialCode = initialCodeDao.insert();
            if (initialCode <= MAX_INITIAL_CODE) {
                initialCodeCache.addInUseCache(initialCode);
                return initialCode;
            }

            Set<String> inUseInitialCodes = initialCodeCache.readInUseCache();
            for (Long i = 0L; i < MAX_INITIAL_CODE; i++) {
                if (inUseInitialCodes.contains(i.toString())) {
                    continue;
                }

                if (urlMappingDao.hasMoreSpace(i)) {
                    initialCodeCache.addInUseCache(i);
                    return i;
                }
            }
        } finally {
            lock.unlock();
        }

        throw new NoMoreInitialCodeException("no available initial code");
    }

    @Scheduled (initialDelay = 10 * 1000, fixedDelay = 2 * 60 * 1000)
    public void timedUpdateInUseInitialCodeSet() {
        initialCodeCache.refreshExpiration(initialCode);
        RLock lock = redissonClient.getLock(DISTRIBUTED_LOCK_NAME);
        try {
            if (!lock.tryLock(3, 30, TimeUnit.SECONDS)) {
                return;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        try {
            String serverUUID = serverUUIDCache.getServerUUID();
            logger.info("server uuid {}", serverUUID);

            if (serverUUID == null) {
                serverUUIDCache.setServerUUID(uuid);
                logger.info("server uuid is null, set uuid " + uuid);
                return;
            }

            if (uuid.equals(serverUUID)) {
                logger.info("refresh server uuid and remove expired initial code");
                serverUUIDCache.refreshExpiration();
                initialCodeCache.removeExpiredInitialCode();
            }
        } finally {
            lock.unlock();
        }
    }
}
