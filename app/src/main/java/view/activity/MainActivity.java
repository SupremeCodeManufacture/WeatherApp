package view.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.squareup.picasso.Picasso;
import com.supreme.manufacture.weather.R;
import com.supreme.manufacture.weather.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import data.App;
import data.GenericConstants;
import data.model.CurrentWeatherObj;
import data.model.DataRs;
import data.model.ForecastDayObj;
import data.model.ForecastObj;
import data.model.HourWeatherObj;
import data.view_model.MainActivityViewModel;
import logic.adapters.HoursWeatherAdapter;
import logic.async_await.OnAsyncDoneRsObjListener;
import logic.helpers.DataFormatConverter;
import logic.listeners.OnFetchDataErrListener;
import logic.network.RequestManager;
import view.custom.WrapLayoutManager;

public class MainActivity extends BaseActivity implements
        View.OnClickListener,
        OnAsyncDoneRsObjListener,
        OnFetchDataErrListener {

    private ActivityMainBinding mActivityBinding;
    private MainActivityViewModel mViewModel;
    private static final int NEW_LOC_SELECTION_CODE = 567;
    private String mCoordQuery;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        bindViewModel();

        // if there is a selected city use it - if no try to detect location - if permissions
        // TODO: 24/02/2019
        onLoadLocationWeather("", "60.0875092,30.2659864");
    }


    private void bindViewModel() {
        mViewModel = ViewModelProviders.of(MainActivity.this).get(MainActivityViewModel.class);
        mViewModel.getCurData().observe(this, new Observer<DataRs>() {
            @Override
            public void onChanged(final DataRs dataRs) {
                if (dataRs != null) {
                    onLoadData(dataRs);

                } else {
                    showSnack(mActivityBinding.mainCoord, App.getAppCtx().getResources().getString(R.string.txt_oops), true);
                }
            }
        });
    }

    private void onLoadData(DataRs dataRs) {
        mActivityBinding.tvToolbarPlace.setText(dataRs.getLocation().getName());

        loadCurWeather(dataRs.getCurrent());

        loadTodaysHoursWeather(dataRs.getForecast());

        loadDaysWeather(dataRs.getForecast());
    }

    private void loadCurWeather(CurrentWeatherObj currentWeatherObj) {
        mActivityBinding.tvTemp.setText(String.valueOf(currentWeatherObj.getTemp_c()));
        mActivityBinding.tvMoodType.setText(currentWeatherObj.getCondition().getText());

        mActivityBinding.tvWind.setText(String.valueOf(currentWeatherObj.getWind_kph()) + " km/h");
        mActivityBinding.tvFeels.setText(String.valueOf(currentWeatherObj.getFeelslike_c()) + " °C");
        mActivityBinding.tvPressure.setText(String.valueOf(currentWeatherObj.getPressure_mb()) + " Mbar");
        mActivityBinding.tvUvIndex.setText(String.valueOf(currentWeatherObj.getUv()));
        mActivityBinding.tvHum.setText(String.valueOf(currentWeatherObj.getHumidity()) + " %");
        mActivityBinding.tvVisibility.setText(String.valueOf(currentWeatherObj.getVis_km()) + " km");
    }

    private void loadTodaysHoursWeather(ForecastObj forecastObj) {
        List<HourWeatherObj> list = DataFormatConverter.getTodayHoursWeather(forecastObj); // TODO: 24/02/2019  async

        if (list != null && list.size() > 0) {
            mActivityBinding.zoneTodayHours.setVisibility(View.VISIBLE);

            mActivityBinding.rvWeather24Items.setAdapter(new HoursWeatherAdapter(list));
            mActivityBinding.rvWeather24Items.setLayoutManager(new WrapLayoutManager(1, StaggeredGridLayoutManager.HORIZONTAL));
            mActivityBinding.rvWeather24Items.setHasFixedSize(true);

        } else {
            mActivityBinding.zoneTodayHours.setVisibility(View.GONE);
        }
    }

    private void loadDaysWeather(ForecastObj forecastObj) {
        ForecastDayObj[] forecastdays = forecastObj.getForecastday();
        if (forecastdays.length == 3) {
            String todayMood = App.getAppCtx().getResources().getString(R.string.txt_today) + " · " + forecastdays[0].getDay().getCondition().getText();
            mActivityBinding.tvMoodToday.setText(todayMood);
            mActivityBinding.tvTempMinToday.setText(String.valueOf(forecastdays[0].getDay().getMintemp_c()));
            mActivityBinding.tvTempMaxToday.setText(forecastdays[0].getDay().getMaxtemp_c() + " °C");
            Picasso.with(MainActivity.this)
                    .load("http://" + forecastdays[0].getDay().getCondition().getIcon())
                    .fit()
                    .centerCrop()
                    .into(mActivityBinding.ivMmodToday);

            String tomorowMood = App.getAppCtx().getResources().getString(R.string.txt_tomorrow) + " · " + forecastdays[1].getDay().getCondition().getText();
            mActivityBinding.tvMoodTomorrow.setText(tomorowMood);
            mActivityBinding.tvTempMinTomorrow.setText(String.valueOf(forecastdays[1].getDay().getMintemp_c()));
            mActivityBinding.tvTempMaxTomorrow.setText(forecastdays[1].getDay().getMaxtemp_c() + " °C");
            Picasso.with(MainActivity.this)
                    .load("http://" + forecastdays[1].getDay().getCondition().getIcon())
                    .fit()
                    .centerCrop()
                    .into(mActivityBinding.ivMmodTomorrow);

            String afterTomWeekDay = new SimpleDateFormat("EE", Locale.ENGLISH).format(forecastdays[2].getDate_epoch() * 1000);
            String afterTomorrowMood = afterTomWeekDay + " · " + forecastdays[2].getDay().getCondition().getText();
            mActivityBinding.tvMoodAftTomorrow.setText(afterTomorrowMood);
            mActivityBinding.tvTempMinAftTomorrow.setText(String.valueOf(forecastdays[2].getDay().getMintemp_c()));
            mActivityBinding.tvTempMaxAftTomorrow.setText(forecastdays[2].getDay().getMaxtemp_c() + " °C");
            Picasso.with(MainActivity.this)
                    .load("http://" + forecastdays[2].getDay().getCondition().getIcon())
                    .fit()
                    .centerCrop()
                    .into(mActivityBinding.ivMmodAftTomorrow);
        }
    }

    private void onLoadLocationWeather(String locName, String locQuery) {
        mActivityBinding.tvToolbarPlace.setText(locName);
        mCoordQuery = locQuery;

        onProgressShow(mActivityBinding.progressBar);
        RequestManager.asyncGetForecastWeather(locQuery, "3", MainActivity.this, MainActivity.this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_details:
                // TODO: 21/02/2019 to set on internal brawser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.apixu.com/weather/"));
                startActivity(browserIntent);
                break;

            case R.id.tv_next_days:
                Intent intent = new Intent(MainActivity.this, WeatherDetailsActivity.class);
                intent.putExtra(GenericConstants.KEY_EXTRA_LOC_COORDONATES, mCoordQuery);
                startActivity(intent);
                break;

            case R.id.btn_add:
                startActivityForResult(new Intent(MainActivity.this, PlacesActivity.class), NEW_LOC_SELECTION_CODE);
                break;

            case R.id.btn_more:
                // TODO: 25/02/2019  
                showSnack(mActivityBinding.mainCoord, "Settings comming soon", true);
                break;
        }
    }


    @Override
    public <T> void onDone(T obj) {
        onProgressDismiss(mActivityBinding.progressBar);
        mViewModel.setCurData((DataRs) obj);
    }

    @Override
    public void onErrNptify(String errStr) {
        onProgressDismiss(mActivityBinding.progressBar);
        showSnack(mActivityBinding.mainCoord, errStr, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == NEW_LOC_SELECTION_CODE && data != null) {
            String name = data.getStringExtra(GenericConstants.KEY_EXTRA_LOC_NAME);
            String coord = data.getStringExtra(GenericConstants.KEY_EXTRA_LOC_COORDONATES);

            if (name != null && coord != null)
                onLoadLocationWeather(name, coord);
        }
    }
}