package zerobase.stockdividend.scraper;

import zerobase.stockdividend.model.Company;
import zerobase.stockdividend.model.ScrapedResult;

public interface Scraper {
    Company scrapCompanyByTicker(String ticker);
    ScrapedResult scrap(Company company);
}
