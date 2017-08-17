package com.titizz.shorturl.service.impl;

import com.titizz.shorturl.NumberUtils;
import com.titizz.shorturl.cache.Cache;
import com.titizz.shorturl.exception.FailToAcquireDistributeLockException;
import com.titizz.shorturl.exception.NoMoreInitialCodeException;
import com.titizz.shorturl.exception.NoQueryResultReturnException;
import com.titizz.shorturl.repository.UrlMappingDao;
import com.titizz.shorturl.service.InitialCodeService;
import com.titizz.shorturl.service.ShortUrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class ShortUrlServiceImpl implements ShortUrlService {

    private static final Logger logger = LoggerFactory.getLogger(ShortUrlServiceImpl.class);

	@Autowired
	private InitialCodeService initialCodeService;

    @Autowired
    private UrlMappingDao urlMappingDao;

	@Autowired
	private Cache cache;
	
	@Value("${site:titizz.com/}")
	private String site;

	private volatile Long initialCode;

	@PostConstruct
	public void init() throws InterruptedException,
            NoMoreInitialCodeException, FailToAcquireDistributeLockException {

        initialCode = initialCodeService.updateInitialCode();
        logger.info("initialize, get initial code {}", initialCode);
    }
	
	public String compress(String url) throws InterruptedException,
            NoMoreInitialCodeException, FailToAcquireDistributeLockException {

		Long code = cache.getCode(url);
		if (code != null) {
            logger.info("hit cache, code {} correspond with url {}", code, url);
            return assembleShortUrl(code);
		}

		do {
            code = urlMappingDao.insert(initialCode, url);
            if (urlMappingDao.lessThanMaxCode(code)) {
                logger.info("get a code from database, code {} correspond with url {}", code, url);
                break;
            }

            logger.info("no available code, prepare to update initial code");
            urlMappingDao.delete(code);
            synchronized (this) {
                if (urlMappingDao.hasMoreSpace(initialCode)) {
                    continue;
                }
                initialCodeService.updateInitialCode();
                logger.info("get new initial code {}", initialCode);
            }
        } while (true);

		cache.put(code.toString(), url);

        return assembleShortUrl(code);
	}
	
	public String decompress(String base62) throws NoQueryResultReturnException {

		Long code = NumberUtils.base62ToDecimal(base62);
		String url = cache.getUrl(code.toString());
		if (url != null) {
            logger.info("hit cache, url {} correspond with code {}, base62 {}", url, code, base62);
            return url;
		}

		try {
            url = urlMappingDao.queryUrl(code);
        } catch (EmptyResultDataAccessException e) {
            logger.info("no query result, code {}", code);
            throw new NoQueryResultReturnException();
        }

        cache.put(code.toString(), url);
		return url;
	}

	private String assembleShortUrl(Long code) {
        return assembleShortUrl(NumberUtils.decimal2base62(code));
    }

    private String assembleShortUrl(String base62) {
        return site.endsWith("/") ? site + base62 : site + "/" + base62;
    }
}
