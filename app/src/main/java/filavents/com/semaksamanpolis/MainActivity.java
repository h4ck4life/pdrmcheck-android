package filavents.com.semaksamanpolis;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import filavents.com.semaksamanpolis.util.TypefaceUtil;
import github.nisrulz.androidutils.network.NetworkUtils;

public class MainActivity extends AppCompatActivity {

    private static final String PDRM_API = "http://pdrmcheck.filavents.com/v1/pdrm/summon";
    ProgressDialog barProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TypefaceUtil.overrideFont(getApplicationContext(), "Roboto Condensed", "fonts/RobotoCondensed-Regular.ttf");

        // Configure Google Mobile Ads
        AdView mAdView = (AdView) findViewById(R.id.adView);
        if (NetworkUtils.isConnected(getApplicationContext())) {
            mAdView.setVisibility(View.VISIBLE);
            MobileAds.initialize(getApplicationContext(), "ca-app-pub-9221126498873830~2847408987");
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        } else {
            mAdView.setVisibility(View.GONE);
        }

        // Progress dialog
        barProgressDialog = new ProgressDialog(MainActivity.this);
        barProgressDialog.setMessage("Maklumat sedang disemak..");
        barProgressDialog.setCancelable(false);

        // Get scrollview
        final LinearLayout scrollView = (LinearLayout) findViewById(R.id.summonContent);


        // Semak button handler
        Button checkBtn = (Button) findViewById(R.id.checkBtn);
        checkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Check if network connected
                if (!NetworkUtils.isConnected(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), "Tiada sambungan internet. Sila cuba semula", Toast.LENGTH_SHORT).show();
                    return;
                }

                final TextView icTxt = (TextView) findViewById(R.id.icTxt);

                // Check if IC number is empty
                if (icTxt.getText().length() < 1) {
                    Toast.makeText(getApplicationContext(), "Sila masukkan nombor IC untuk semak", Toast.LENGTH_SHORT).show();
                    icTxt.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(icTxt, InputMethodManager.SHOW_IMPLICIT);
                    return;
                }

                // Check if IC number is less than 12 digits
                if (icTxt.getText().length() < 12) {
                    Toast.makeText(getApplicationContext(), "Pastikan IC nombor adalah 12 digit", Toast.LENGTH_SHORT).show();
                    icTxt.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(icTxt, InputMethodManager.SHOW_IMPLICIT);
                    return;
                }


                barProgressDialog.show();

                RequestParams params = new RequestParams();
                params.put("ic_no", icTxt.getText());

                AsyncHttpClient client = new AsyncHttpClient();
                client.setTimeout(30000);
                client.post(PDRM_API, params, new JsonHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        // called before request is started
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        // called when response HTTP status is "200 OK"

                        try {

                            scrollView.removeAllViews();

                            // if no summon found
                            if ((response.get("Status").toString()).equals("false") && (response.get("StatusMessage").toString()).equals("No summon found")) {
                                LinearLayout inflateNoSummonView = (LinearLayout) View.inflate(MainActivity.this, R.layout.no_summon_view, null);

                                barProgressDialog.hide();
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(icTxt.getWindowToken(), 0);

                                scrollView.addView(inflateNoSummonView);
                                return;
                            }

                            // handle general error
                            if ((response.get("Status").toString()).equals("false")) {
                                LinearLayout inflateGeneralErrorView = (LinearLayout) View.inflate(MainActivity.this, R.layout.general_error_view, null);

                                barProgressDialog.hide();
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(icTxt.getWindowToken(), 0);

                                scrollView.addView(inflateGeneralErrorView);
                                return;
                            }


                            System.out.println(response.toString());

                            // Get summon name
                            String summonName = response.get("Name").toString();
                            int summonCount = response.getJSONArray("SummonData").length();
                            String summonTotalAmt = response.get("TotalAmount").toString();

                            LinearLayout summonInfoView = (LinearLayout) View.inflate(MainActivity.this, R.layout.summon_summary, null);
                            ((TextView) summonInfoView.findViewById(R.id.summonName)).setText(summonName);
                            ((TextView) summonInfoView.findViewById(R.id.summonCount)).setText(String.valueOf(summonCount) + " SAMAN DIJUMPAI");
                            ((TextView) summonInfoView.findViewById(R.id.summonTotalAmt)).setText("JUMLAH SAMAN RM " + summonTotalAmt);

                            scrollView.addView(summonInfoView);

                            // Get summon data
                            JSONArray summonData = response.getJSONArray("SummonData");

                            for (int i = 0; i < summonData.length(); i++) {
                                JSONObject summon = (JSONObject) summonData.get(i);

                                LinearLayout inflateRow = (LinearLayout) View.inflate(MainActivity.this, R.layout.row_layout, null);

                                ((TextView) inflateRow.findViewById(R.id.rowHeader)).setText("Saman No. " + (i + 1));
                                ((TextView) inflateRow.findViewById(R.id.rowValuesummonNoTxt)).setText(summon.get("SummonsNo").toString());
                                ((TextView) inflateRow.findViewById(R.id.rowValuevehicleNoTxt)).setText(summon.get("VehicleNo").toString());
                                ((TextView) inflateRow.findViewById(R.id.rowValueblacklistedTxt)).setText(summon.get("Blacklisted").toString());
                                ((TextView) inflateRow.findViewById(R.id.rowValueoffenceDataTxt)).setText(summon.get("OffenceDate").toString());
                                ((TextView) inflateRow.findViewById(R.id.rowValuenoncompoundTxt)).setText(summon.get("NonCompoundable").toString());
                                ((TextView) inflateRow.findViewById(R.id.rowValuedistricTxt)).setText(summon.get("District").toString());
                                ((TextView) inflateRow.findViewById(R.id.rowValueoffenceTxt)).setText(summon.get("Offence").toString());
                                ((TextView) inflateRow.findViewById(R.id.rowValuelocationTxt)).setText(summon.get("Location").toString());
                                ((TextView) inflateRow.findViewById(R.id.rowValueamountTxt)).setText("RM " + summon.get("Amount").toString());

                                scrollView.addView(inflateRow);

                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        barProgressDialog.hide();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(icTxt.getWindowToken(), 0);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                        super.onFailure(statusCode, headers, throwable, errorResponse);
                        barProgressDialog.hide();
                    }

                    @Override
                    public void onRetry(int retryNo) {
                        // called when request is retried
                    }
                });
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.info_actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_favorite:
                Toast.makeText(getApplicationContext(), "Information text", Toast.LENGTH_SHORT).show();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}
