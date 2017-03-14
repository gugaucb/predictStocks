package me.costa.gustavo.predictStocks.pocs;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.commons.csv.CSVRecord;
import org.junit.Test;

import junit.framework.Assert;

public class PoCTimeSeriesTest {

	@Test
	public void deveQuantidadeTimeSeriesIgualAoCSV() throws IOException {
		Iterable<CSVRecord> records = PoCTimeSeries.carregarCSV();
		boolean cabecalho = true;
		int quantCVSRecord = 0;
		for (CSVRecord record : records) {
			if (cabecalho) {
				cabecalho = false;
				continue;
			}
			quantCVSRecord++;
		}
		
			System.out.println(quantCVSRecord);
		assertEquals(quantCVSRecord, PoCTimeSeries.carregarTimeSeries().getTickCount());
	}

}
