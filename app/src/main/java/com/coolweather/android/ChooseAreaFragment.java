package com.coolweather.android;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.coolweather.android.db.City;
import com.coolweather.android.db.Country;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * create by txy
 * create on 2020/12/10
 * description
 */
//问题记录：存在加载失败问题，可能是网络接口问题，最后修改，查看和风天气api
public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0 ;
    public static final int LEVEL_CITY = 1 ;
    public static final int LEVEL_COUNTRY = 2 ;
    private ProgressDialog progressDialog ;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String>adapter;
    private List<String> dataList =new ArrayList<>();

    //省列表
    private List<Province> provinceList;

    //市列表
    private List<City>cityList;

    //县列表
    private List<Country> countryList;

    //选中的省份
    private Province selectedProvince;
    //选中的城市
    private City selectedCity;
    //当前选中的级别
    private int currentLevel;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
       View view =inflater.inflate(R.layout.choose_area,container,false);
       titleText = (TextView)view.findViewById(R.id.title_text);
       backButton = view.findViewById(R.id.back_button);
       listView =view.findViewById(R.id.list_view);
       adapter =new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,dataList);
       listView.setAdapter(adapter);
      return  view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel ==LEVEL_PROVINCE){
                    selectedProvince =provinceList.get(position);
                    queryCitys();
                }else if(currentLevel == LEVEL_CITY){
                    selectedCity =cityList.get(position);
                   queryCountries();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTRY){
                    queryCountries();
                }else if (currentLevel ==LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }
    /*查询全国所有的省，优先从数据库查询，如果没有查询到在去服务器查询*/
    private void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size()>0){
            dataList.clear();
            for (Province province :provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else {
            String adress ="http://guoling.tech/api/china";
            queryFromServer(adress,"province");
        }
    }
    /**
     * 查询选中省内所有的市，优先从数据库查询，若果没有再到服务器去查询
     */
    private void queryCitys(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);//切换回退按钮显式
        cityList = DataSupport.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size()>0){
            dataList.clear();
            for (City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel =LEVEL_CITY;
        }else {
            int provinceCode =selectedProvince.getProvinceCode();
            String adress = "http://guoling.tech/api/china/"+provinceCode;
            queryFromServer(adress,"city");
        }
    }
    /**
     * 查詢选中的市内所有县，优先从数据库查询，如果没有查询到再去服务器查询
     */
    private void queryCountries(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countryList = DataSupport.where("city = ?",String.valueOf(selectedCity.getId())).find(Country.class);
        if (countryList.size()>0){
            dataList.clear();
            for (Country country :countryList){
                dataList.add(country.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel =LEVEL_COUNTRY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String adress ="http://guolin.tech/api/china/"+ provinceCode+"/"+cityCode;
            queryFromServer(adress,"country");
        }
    }
    /*
    根据穿额U的地址和类型从服务器上查询省市县数据
     */
    private void queryFromServer(String adress , final String type){
       showProgressDialog();
        HttpUtil.sendOkHttpClient(adress, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
              //通过 runOnuiTread方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgrcessDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();

                    }
                });

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
              String responseText =response.body().string();
              boolean result =false;
              if ("province".equals(type)){
                  result = Utility.handleProvinceResponse(responseText);
              }else if ("city".equals(type)){
                  result = Utility.handleCityResponse(responseText,selectedProvince.getId());
              }else if ("country".equals(type)){
                  result = Utility.handleCountryResponse(responseText,selectedCity.getId());
              }
              if (result){
                  getActivity().runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          closeProgrcessDialog();
                          if ("province".equals(type)){
                              queryProvinces();
                          }else if ("city".equals(type)){
                              queryCitys();
                          }else if ("country".equals(type)){
                              queryCountries();
                          }
                      }
                  });
              }
            }
        });
    }
    /**
     * 显示进度对话框
     */
    private void showProgressDialog(){
        if(progressDialog ==null){
            progressDialog =new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    /**
     * 关闭进度对话框
     */
    private void closeProgrcessDialog(){
        if (progressDialog !=null){
            progressDialog.dismiss();
        }
    }
}
