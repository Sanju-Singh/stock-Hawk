package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.CustomSpinnerAdapter;
import com.sam_chordas.android.stockhawk.retrofit.ApiService;
import com.sam_chordas.android.stockhawk.retrofit.RestClient;
import com.sam_chordas.android.stockhawk.retrofit.model.Quote;
import com.sam_chordas.android.stockhawk.retrofit.model.QuoteData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class LineGraphActivity extends AppCompatActivity {
    LineChartView lineView;
    private final String LOG_TAG = LineGraphActivity.class.getSimpleName();
    private Context context = null;
    ArrayList<Quote> quoteArray = null;
    public String symbol;
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    SimpleDateFormat dateFormat;
    Calendar cal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        symbol = getIntent().getExtras().getString("symbol");

        setTitle(symbol);

        dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        String endDate = dateFormat.format(System.currentTimeMillis());

        cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MONTH, -1);
        String startDate = dateFormat.format(cal.getTimeInMillis());

        fetchQuoteHistoryData(symbol, startDate, endDate);

        context = this;
        lineView = (LineChartView) findViewById(R.id.linechart);
    }

    private void fetchQuoteHistoryData(String symbol, String startDate, String endDate){

        ApiService apiService = RestClient.getApiService();

        String q = "select * from yahoo.finance.historicaldata where symbol = \""+symbol+"\" and startDate = \""+startDate+"\" and endDate = \""+endDate+"\"";
        String diagnostics = "true";
        String env = "store://datatables.org/alltableswithkeys";
        String format = "json";

        Call<QuoteData> call = apiService.getHistoryData(q, diagnostics, env, format);

        call.enqueue(new Callback<QuoteData>() {
            @Override
            public void onResponse(Response<QuoteData> response, Retrofit retrofit) {
                QuoteData data = response.body();

                ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
                progressBar.setVisibility(View.GONE);

                if(data != null){

                    if(data.query != null) {
                        if (data.query.results != null) {
                            if (data.query.results.quote != null) {

                                quoteArray = data.query.results.quote;
                                Collections.reverse(quoteArray);

                                if (quoteArray.size() > 0) {
                                    lineView.setVisibility(View.VISIBLE);
                                    displayGraph();
                                } else {
                                    Toast.makeText(context, "No Data Received", Toast.LENGTH_SHORT).show();
                                    Log.e(LOG_TAG, "NO DATA: ");
                                }
                            }
                        }
                    }

                } else{
                    Toast.makeText(context, "Some Error Occurred", Toast.LENGTH_SHORT).show();
                    Log.e(LOG_TAG, "ERROR");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Toast.makeText(context, "Call to server failed", Toast.LENGTH_SHORT).show();
                Log.e(LOG_TAG, "Call to server failed: " + t.getMessage());
            }
        });
    }

    public void displayGraph() {

        LineSet dataset = new LineSet();
        double minValue = 0;
        double maxValue = 0;
        int calls = 1;

        if (quoteArray.size() > 0) {

            double bidprice;
            bidprice = Double.parseDouble(quoteArray.get(0).close);
            minValue = bidprice;
            maxValue = bidprice;

            for (Quote quote : quoteArray) {

                bidprice = Double.parseDouble(quote.close);
                dataset.addPoint(new Point(String.valueOf(calls), Float.parseFloat(quote.close)));

                if (minValue > bidprice) {
                    minValue = bidprice;
                }
                if (maxValue < bidprice) {
                    maxValue = bidprice;
                }
                calls++;
            }

            Paint paint = new Paint();
            paint.setColor(Color.WHITE);

            dataset.setDotsColor(getResources().getColor(R.color.material_red_700));
            dataset.setColor(getResources().getColor(R.color.material_green_700));

            dataset.setDotsRadius(8.0f);
            dataset.setThickness(7.0f);


            lineView.dismiss();
            lineView.addData(dataset);
            lineView.setAxisBorderValues((int) minValue - 2, (int) maxValue + 2);
            lineView.setAxisColor(Color.WHITE);
            lineView.setLabelsColor(Color.WHITE);

            lineView.setStep((int)((maxValue - minValue)/10));

            lineView.setXLabels(AxisController.LabelPosition.NONE);
            lineView.setGrid(ChartView.GridType.FULL, paint);
            lineView.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.stock_graph, menu);

        MenuItem item = menu.findItem(R.id.spinner);
        Spinner spinner = (Spinner) MenuItemCompat.getActionView(item);

        ArrayList<String> list = new ArrayList<String>();

        list.add(getString(R.string.one_month));
        list.add(getString(R.string.six_month));
        list.add(getString(R.string.one_year));

        CustomSpinnerAdapter spinAdapter = new CustomSpinnerAdapter(getApplicationContext(), list);
        spinner.setAdapter(spinAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapter, View v,
                                       int position, long id) {

                String endDate = dateFormat.format(System.currentTimeMillis());
                String item = adapter.getItemAtPosition(position).toString();
                Date date = new Date();
                String startDate;

                switch (item)
                {
                    case "1 Month":
                        cal.setTime(date);
                        cal.add(Calendar.MONTH, -1);
                        startDate = dateFormat.format(cal.getTimeInMillis());
                        fetchQuoteHistoryData(symbol,startDate, endDate);
                        break;
                    case "6 Month":
                        cal.setTime(date);
                        cal.add(Calendar.MONTH, -6);
                        startDate = dateFormat.format(cal.getTimeInMillis());
                        fetchQuoteHistoryData(symbol,startDate, endDate);
                        break;
                    case "1 Year":
                        cal.setTime(date);
                        cal.add(Calendar.YEAR, -1);
                        startDate = dateFormat.format(cal.getTimeInMillis());
                        fetchQuoteHistoryData(symbol,startDate, endDate);
                        break;
                }
             }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });
        return true;
    }
}
