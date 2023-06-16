
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private RestTemplate restTemplate;


  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }








  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  private Double getOpeningPriceOnStartDate(List<Candle> candles) {

    return candles.get(0).getOpen();
  }


  private Double getClosingPriceOnEndDate(List<Candle> candles) {
    int l = candles.size();
    return candles.get(l-1).getClose();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
        String url = buildUri(symbol, from, to);
        TiingoCandle[] tc = restTemplate.getForObject(url, TiingoCandle[].class);
     return Arrays.asList(tc);
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
       String uriTemplate = "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?"
            + "startDate=" + startDate.toString() + "&endDate=" + endDate.toString() + "&token=41896d711715c51e31cd06a5296d45bb19a801a3";
            return uriTemplate;
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
        double totalReturns = (sellPrice - buyPrice)/buyPrice;
        double daysBetween = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);
        double yrs = daysBetween/365;
        double annualized_returns = Math.pow((1 + totalReturns), 1.0/yrs) - 1;
        return new AnnualizedReturn(trade.getSymbol(), annualized_returns, totalReturns);
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) {
    // TODO Auto-generated method stub
    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
        for(PortfolioTrade pt: portfolioTrades) {
          try{
            List<Candle> cl = getStockQuote(pt.getSymbol(), pt.getPurchaseDate(), endDate);
            double buyPrice = getOpeningPriceOnStartDate(cl);
            double sellPrice = getClosingPriceOnEndDate(cl);
            AnnualizedReturn ar = calculateAnnualizedReturns(endDate, pt, buyPrice, sellPrice);
            annualizedReturns.add(ar);
          } catch (JsonProcessingException ex) {
            ex.printStackTrace();
          }
        }
        Collections.sort(annualizedReturns, getComparator());
     return annualizedReturns;
  }
}
