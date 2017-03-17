package me.costa.gustavo.predictStocks.pocs;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.analysis.criteria.AverageProfitCriterion;
import eu.verdelhan.ta4j.analysis.criteria.AverageProfitableTradesCriterion;
import eu.verdelhan.ta4j.analysis.criteria.BuyAndHoldCriterion;
import eu.verdelhan.ta4j.analysis.criteria.LinearTransactionCostCriterion;
import eu.verdelhan.ta4j.analysis.criteria.MaximumDrawdownCriterion;
import eu.verdelhan.ta4j.analysis.criteria.NumberOfTicksCriterion;
import eu.verdelhan.ta4j.analysis.criteria.NumberOfTradesCriterion;
import eu.verdelhan.ta4j.analysis.criteria.RewardRiskRatioCriterion;
import eu.verdelhan.ta4j.analysis.criteria.TotalProfitCriterion;
import eu.verdelhan.ta4j.analysis.criteria.VersusBuyAndHoldCriterion;
import eu.verdelhan.ta4j.indicators.oscillators.CCIIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.RSIIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

public class PoCTimeSeries {
	public static void main(String[] args) throws IOException {
		TimeSeries series = carregarTimeSeries();

		// Building the trading strategy
		//Strategy strategy = buildStrategy(series);
		Strategy strategy = buildStrategySMAIndicator(series);

		// Running the strategy
		TradingRecord tradingRecord = series.run(strategy);
		
		validar(series, tradingRecord);
	}

	public static Iterable<CSVRecord> carregarCSV() throws IOException{
		Reader in = new FileReader("C:\\Users\\Gustavo\\git\\predictStocks\\predictStocks\\target\\classes\\ptr4.csv");
		Iterable<CSVRecord> records = CSVFormat.newFormat(';').withHeader("Data", "Historico", "Fech", "Var.Dia",
				"Abertura", "Minimo", "Medio", "Maximo", "Volume", "Negocios").parse(in);
		return records;
	}
	
	public static TimeSeries carregarTimeSeries() throws IOException {
		List<Tick> ticks = new ArrayList<Tick>();
		Iterable<CSVRecord> records = carregarCSV();
		boolean cabecalho = true;
		for (CSVRecord record : records) {
			if (cabecalho) {
				cabecalho = false;
				continue;
			}
			String abertura = record.get("Abertura");
			String maximo = record.get("Maximo");
			String minimo = record.get("Minimo");
			String fechamento = record.get("Fech");
			String volume = record.get("Volume");
			String data = record.get("Data");

			DateTime dataDateTime = convertStringToDateTime(data);
			double aberturaDouble = convertStringToDouble(abertura);
			double maximoDouble = convertStringToDouble(maximo);
			double minimoDouble = convertStringToDouble(minimo);
			double fechamentoDouble = convertStringToDouble(fechamento);
			double volumeDouble = convertStringToDouble(volume);

			ticks.add(
					new Tick(dataDateTime, aberturaDouble, maximoDouble, minimoDouble, fechamentoDouble, volumeDouble));
		}
		TimeSeries timeSeries = new TimeSeries("PETR4_2015_2017", ticks);
		return timeSeries;
	}

	private static double convertStringToDouble(String num) {
		String temp = num.replaceAll("\\.", "");
		temp = temp.replace(',', '.');
		return Pattern.matches("^[\\+\\-]{0,1}[0-9]+[\\.\\,]{0,1}[0-9]+$", temp) ? Double.parseDouble(temp) : 0d;
	}

	private static DateTime convertStringToDateTime(String data) {
		DateTimeFormatter format = DateTimeFormat.forPattern("dd/MM/yyyy");
		DateTime time = format.parseDateTime(data);
		return time;
	}

	public static Strategy buildStrategy(TimeSeries series) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}

		CCIIndicator longCci = new CCIIndicator(series, 200);
		CCIIndicator shortCci = new CCIIndicator(series, 5);
		Decimal plus100 = Decimal.HUNDRED;
		Decimal minus100 = Decimal.valueOf(-100);

		Rule entryRule = new OverIndicatorRule(longCci, plus100) // Bull trend
				.and(new UnderIndicatorRule(shortCci, minus100)); // Signal

		Rule exitRule = new UnderIndicatorRule(longCci, minus100) // Bear trend
				.and(new OverIndicatorRule(shortCci, plus100)); // Signal

		Strategy strategy = new Strategy(entryRule, exitRule);
		strategy.setUnstablePeriod(5);
		return strategy;
	}

	
	 public static Strategy buildStrategySMAIndicator(TimeSeries series) {
	        if (series == null) {
	            throw new IllegalArgumentException("Series cannot be null");
	        }

	        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
	        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
	        SMAIndicator longSma = new SMAIndicator(closePrice, 200);

	        // We use a 2-period RSI indicator to identify buying
	        // or selling opportunities within the bigger trend.
	        RSIIndicator rsi = new RSIIndicator(closePrice, 2);
	        
	        // Entry rule
	        // The long-term trend is up when a security is above its 200-period SMA.
	        Rule entryRule = new OverIndicatorRule(shortSma, longSma) // Trend
	                .and(new CrossedDownIndicatorRule(rsi, Decimal.valueOf(5))) // Signal 1
	                .and(new OverIndicatorRule(shortSma, closePrice)); // Signal 2
	        
	        // Exit rule
	        // The long-term trend is down when a security is below its 200-period SMA.
	        Rule exitRule = new UnderIndicatorRule(shortSma, longSma) // Trend
	                .and(new CrossedUpIndicatorRule(rsi, Decimal.valueOf(95))) // Signal 1
	                .and(new UnderIndicatorRule(shortSma, closePrice)); // Signal 2
	        
	        // TODO: Finalize the strategy
	        
	        return new Strategy(entryRule, exitRule);
	}
	 
	 private static void validar(TimeSeries series, TradingRecord tradingRecord){
		 // Total profit
	        TotalProfitCriterion totalProfit = new TotalProfitCriterion();
	        System.out.println("Total profit: " + totalProfit.calculate(series, tradingRecord));
	        // Number of ticks
	        System.out.println("Number of ticks: " + new NumberOfTicksCriterion().calculate(series, tradingRecord));
	        // Average profit (per tick)
	        System.out.println("Average profit (per tick): " + new AverageProfitCriterion().calculate(series, tradingRecord));
	        // Number of trades
	        System.out.println("Number of trades: " + new NumberOfTradesCriterion().calculate(series, tradingRecord));
	        // Profitable trades ratio
	        System.out.println("Profitable trades ratio: " + new AverageProfitableTradesCriterion().calculate(series, tradingRecord));
	        // Maximum drawdown
	        System.out.println("Maximum drawdown: " + new MaximumDrawdownCriterion().calculate(series, tradingRecord));
	        // Reward-risk ratio
	        System.out.println("Reward-risk ratio: " + new RewardRiskRatioCriterion().calculate(series, tradingRecord));
	        // Total transaction cost
	        System.out.println("Total transaction cost (from $1000): " + new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord));
	        // Buy-and-hold
	        System.out.println("Buy-and-hold: " + new BuyAndHoldCriterion().calculate(series, tradingRecord));
	        // Total profit vs buy-and-hold
	System.out.println("Custom strategy profit vs buy-and-hold strategy profit: " + new VersusBuyAndHoldCriterion(totalProfit).calculate(series, tradingRecord));
	 }
}
