package zyz.com.meetroom.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.JsonReader;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import org.json.JSONStringer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import zyz.com.meetroom.R;
import zyz.com.meetroom.adapter.MeetingItemAdapter;
import zyz.com.meetroom.entity.AppointmentBriefModel;
import zyz.com.meetroom.entity.LoginModel;
import zyz.com.meetroom.http.HttpCallbackListener;
import zyz.com.meetroom.http.HttpUtil;
import zyz.com.meetroom.http.UrlHandler;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @BindView(R.id.name_txt_main)
    TextView nameMain;

    TextView nameHeader;

    @BindView(R.id.nav_main)
    NavigationView navigationView;

    @BindView(R.id.draw_main)
    DrawerLayout drawerLayout;

    @BindView(R.id.refresh_main)
    SwipeRefreshLayout swipeRefreshLayout;

    @OnClick(R.id.mail_click_main)
    void checkMsg() {
        startActivity(new Intent(this,MsgListActivity.class));
    }

    SharedPreferences sp;

    List<AppointmentBriefModel> appointmentBriefModels = new ArrayList<>();
    MeetingItemAdapter adapter = new MeetingItemAdapter();

    @BindView(R.id.rv_main)
    RecyclerView recyclerView;

    @OnClick(R.id.fab_main) void gotoApply() {
        startActivity(new Intent(this,ApplyMeetingRoomActivity.class));
    }

    @SuppressLint("RtlHardcoded")
    @OnClick(R.id.drawer_click_main) void openDrawer() {
        drawerLayout.openDrawer(Gravity.LEFT);
    }

    private boolean rvInit = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        sp = getSharedPreferences("login_data",MODE_PRIVATE);
        nameHeader = navigationView.getHeaderView(0).findViewById(R.id.name_header);
        swipeRefreshLayout.setOnRefreshListener(()->{
            if(swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            initMeetings();
        });
        initName();
    }

    @Override
    protected void onStart() {
        super.onStart();
        swipeRefreshLayout.setRefreshing(true);
        initMeetings();
    }

    private void initMeetings() {
        HttpUtil.getInstance().get(UrlHandler.getMeetingList(), new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                try {
                    Log.i(TAG, "onFinish: response" + response);
                    JsonArray jsonArray = new JsonParser().parse(response).getAsJsonArray();
                    Log.i(TAG, "onFinish: " + jsonArray.toString());
                    JsonElement element = jsonArray.get(0);
                    Log.i(TAG, "onFinish: " + element.toString());
                    //注册long->Date的转换机制，策略模式
                    Gson gson = new GsonBuilder()
                            .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) ->
                                            new Date(json.getAsJsonPrimitive().getAsLong())).create();
                    appointmentBriefModels = gson
                            .fromJson(element,
                                    new TypeToken<List<AppointmentBriefModel>>() {
                                    }.getType());
                    Log.i(TAG, "onFinish: 转换成功" + appointmentBriefModels.size());
                    runOnUiThread(() -> {
                        if (!rvInit) {
                            adapter.setModels(appointmentBriefModels);
                            recyclerView.setAdapter(adapter);
                            recyclerView.setLayoutManager(
                                    new LinearLayoutManager(MainActivity.this));
                            rvInit = true;
                        } else {
                            adapter.setModels(appointmentBriefModels);
                            adapter.notifyDataSetChanged();
                        }
                        if(swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "onFinish: ",e );
                }

            }
        });

    }

    private void initName() {

        RequestBody requestBody;
        requestBody = new FormBody.Builder().add("phone",sp.getString("phone",""))
                .add("identity","user").build();
        HttpUtil.getInstance().post(UrlHandler.getFakeLoginUrl(), requestBody, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                Log.i("","访问成功"+response);
                runOnUiThread(()->{
                    LoginModel model = new Gson()
                            .fromJson(response,LoginModel.class);
                    sp.edit().putString("id", String.valueOf(model.getId())).apply();
                    nameHeader.setText(model.getStaffName());
                    nameMain.setText(model.getStaffName());
                });
            }

            @Override
            public void onError(Exception e) {
                super.onError(e);
                e.printStackTrace();
            }
        });
    }
}
