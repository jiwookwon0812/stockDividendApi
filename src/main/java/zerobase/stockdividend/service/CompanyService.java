package zerobase.stockdividend.service;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.Trie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import zerobase.stockdividend.exception.impl.NoCompanyException;
import zerobase.stockdividend.model.Company;
import zerobase.stockdividend.model.ScrapedResult;
import zerobase.stockdividend.persist.entity.CompanyEntity;
import zerobase.stockdividend.persist.entity.DividendEntity;
import zerobase.stockdividend.persist.repository.CompanyRepository;
import zerobase.stockdividend.persist.repository.DividendRepository;
import zerobase.stockdividend.scraper.Scraper;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CompanyService {
    private final Trie trie;

    private final Scraper yahooFinanceScraper;

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    public Company save(String ticker) {
        boolean exists = companyRepository.existsByTicker(ticker);
        if (exists) {
            throw new RuntimeException("already exists ticker -> " + ticker);
        }
        return storeCompanyAndDividend(ticker);
    }

    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return companyRepository.findAll(pageable);
        // 회사 정보 호출
    }
    private Company storeCompanyAndDividend(String ticker) {
        // ticker 을 기준으로 회사를 스크래핑
        Company company = yahooFinanceScraper.scrapCompanyByTicker(ticker);
        if (ObjectUtils.isEmpty(company)) {
            // company null 이면 (회사 정보 존재하지 않거나 이외의 오류 발생시)
            throw new RuntimeException("failed to scrap ticker -> " + ticker);
        }

        // 해당 회사가 존재할 경우, 회사의 배당금 정보를 스크래핑
        ScrapedResult scrapedResult = yahooFinanceScraper.scrap(company);

        // 스크래핑 결과 저장한 Company 반환
        CompanyEntity companyEntity = companyRepository.save(new CompanyEntity(company));
        // CompanyEntity 저장 후 반환 (왜반환한거..)
        List<DividendEntity> dividendEntities = scrapedResult.getDividends().stream()
                .map(e -> new DividendEntity(companyEntity.getId(), e))
                .collect(Collectors.toList());
        dividendRepository.saveAll(dividendEntities);
        // 자 여기서 내가 이해한대로 ..
        //일단 companyEntity 는 방금 저장한 회사정보고 내가 지금 저장할건 같은 회사의 배당금 정보니깐
        // 바로 가져와서 이 엔티티의 아이디를 dividendEntity 의 회사 id로 쓸거임
        //근데 같은 회사의 여러 배당금 정보를 가지고 있는거지 scrapResult 의 dividends는? ㅇㅇ
        //쨋든 그래서 하나의 scrapResult 에서 dividends 리스트를 하나의 요소씩 돌아가며, 회사 id는 동일하게?! ㅇㅇ
        // dividendentity 하나씩 저장해서 이걸 리스트로 묶은 게 dividendEntities
        // 그리고 이 리스트의 요소 하나를 dividendentity 객체 하나로.. 하나씩 저장
        // 즉 동일한 회사 id를 가지는 dividendentity 가 여러개 있는거임 (왜냐면 하나의 회사에서 배당금 정보는 여러개)

        return company;
    }

    // Like 이용한  -> 얘로 조회하면 따로 저장 기능 필요 X.....
    // 그냥 이미 저장되어있는 회사들 중에 자기가 알아서 찾아오는 거니깐
    public List<String> getCompanyNamesByKeyword(String keyword) {
        Pageable limit = PageRequest.of(0, 10);
        Page<CompanyEntity> companyEntities = companyRepository
                .findByNameStartingWithIgnoreCase(keyword, limit);

        return companyEntities.stream()
                .map(e -> e.getName())
                .collect(Collectors.toList());

    }

    // trie 이용한 자동완성(저장, 조회, 삭제)
    public void addAutoCompleteKeyword(String keyword) {
        trie.put(keyword, null);
    }
    public List<String> autoComplete(String keyword) {
        return (List<String>) trie.prefixMap(keyword).keySet()
                .stream()
                .collect(Collectors.toList());
    }
    public void deleteAutoCompleteKeyword(String keyword) {
        trie.remove(keyword);
    }

    public String deleteCompany(String ticker) {
        var company = companyRepository.findByTicker(ticker)
                .orElseThrow(() -> new NoCompanyException());

        dividendRepository.deleteByCompanyId(company.getId());
        companyRepository.delete(company);

        deleteAutoCompleteKeyword(company.getName());
        return company.getName();
    }
}
