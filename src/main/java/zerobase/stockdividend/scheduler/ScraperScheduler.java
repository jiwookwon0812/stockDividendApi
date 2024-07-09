package zerobase.stockdividend.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import zerobase.stockdividend.model.Company;
import zerobase.stockdividend.model.ScrapedResult;
import zerobase.stockdividend.model.constants.CacheKey;
import zerobase.stockdividend.persist.entity.CompanyEntity;
import zerobase.stockdividend.persist.entity.DividendEntity;
import zerobase.stockdividend.persist.repository.CompanyRepository;
import zerobase.stockdividend.persist.repository.DividendRepository;
import zerobase.stockdividend.scraper.Scraper;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@EnableCaching
@AllArgsConstructor
public class ScraperScheduler {
    private final CompanyRepository companyRepository;

    private final Scraper yahooFinanceScraper;
    // Scraper 은 인터페이스인데?
    // -> 빈으로 등록된 YahooFinanceScraper 을 자동으로 주입받아서
    // scrap 메서드 등도 이 YahooFinanceScraper 의 필드에 해당하는 것
    private final DividendRepository dividendRepository;

    // 스케줄러 이용해서 일정 주기마다 수행 (배당금 업데이트)
    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true) // 스케줄러 동작할 때마다 캐시 초기화
    @Scheduled(cron = "${scheduler.scrap.yahoo}")
    public void yahooFinanceScheduler() {
        log.info("scraping scheduler is started");
        // 저장되어있는 회사 목록을 조회
        List<CompanyEntity> companies = companyRepository.findAll();

        // 회사마다 배당금 정보를 새로 스크래핑
        // (업데이트 된 배당 정보를 스크래핑)
        for (var company : companies) {
            log.info("scraping schduler is started ->" + company.getName());
            ScrapedResult scrapedResult = yahooFinanceScraper
                    .scrap(new Company(company.getTicker(), company.getName()));
            // 한 회사의 배당금 정보 스크래핑한 결과 => scrapResult 로

            // 스크래핑한 배당금 정보 중 데이터베이스에 없는 값은 저장
            scrapedResult.getDividends().stream()
                    // 디비든 모델을 디비든 엔티티로 매핑
                    .map(e -> new DividendEntity(company.getId(), e))
                    // 엘리멘트를 하나씩 디비든 레파지토리에 삽입
                    .forEach(e -> {
                        boolean exists = dividendRepository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate());
                        if (!exists) {
                            dividendRepository.save(e);
                            log.info("insert new dividend -> " + e.toString());
                        }
                    });

            // 연속적으로 스크래핑 대상 사이트 서버에 요청을 날리지 않도록 일시정지
            try {
                Thread.sleep(3000); // 3초 쉬고
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }
}
