package com.shoppay.wynfc;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.footer.ClassicsFooter;
import com.scwang.smartrefresh.layout.header.ClassicsHeader;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.shoppay.wynfc.adapter.ViplistAdapter;
import com.shoppay.wynfc.bean.VipList;
import com.shoppay.wynfc.tools.ActivityStack;
import com.shoppay.wynfc.tools.CommonUtils;
import com.shoppay.wynfc.tools.DialogUtil;
import com.shoppay.wynfc.tools.LogUtils;
import com.shoppay.wynfc.tools.PreferenceHelper;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

/**
 * Created by songxiaotao on 2017/6/30.
 */

public class VipListActivity extends Activity implements View.OnClickListener {
    private RelativeLayout rl_left;
    private EditText et_chose;
    private TextView tv_title, tv_vipnum;
    private ImageView img_chose;
    private ViplistAdapter adapter;
    private Context ac;
    private Dialog dialog;
    private List<VipList> list;
    private int mindex = 1;
    private int num = 10;
    ListView mListview;
    private SmartRefreshLayout mSartRefresh;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viplist);
        ac = MyApplication.context;
        dialog = DialogUtil.loadingDialog(VipListActivity.this, 1);
        ActivityStack.create().addActivity(VipListActivity.this);
        initView();
        mSartRefresh.setRefreshHeader(new ClassicsHeader(this));
        mSartRefresh.setRefreshFooter(new ClassicsFooter(this));
        list=new ArrayList<VipList>();
        adapter = new ViplistAdapter(getApplicationContext(), list);
        mListview.setAdapter(adapter);

        mSartRefresh.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                obtainViplist(1, 1);
            }
        });
        mSartRefresh.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                // 这里写上拉加载更多的任务
                mindex = mindex + 1;
//                        li_nomsg.setVisibility(View.GONE);
                obtainViplist(mindex, 2);
            }
        });
        mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                  VipList vipList=(VipList) adapterView.getItemAtPosition(i);
                Intent intent=new Intent(VipListActivity.this,VipDetailActivity.class);
                intent.putExtra("viplist",vipList);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mindex=1;
        obtainViplist(1, 0);
    }

    /**
     * @param index
     * @param state
     *            0：第一次 1：刷新 2上拉加载 3搜索
     */
    private void obtainViplist(int index, final int state) {
        // TODO Auto-generated method stub
        dialog.show();
        AsyncHttpClient client = new AsyncHttpClient();
        final PersistentCookieStore myCookieStore = new PersistentCookieStore(this);
        client.setCookieStore(myCookieStore);
        RequestParams params = new RequestParams();
//        卡号（完整）
//        姓名（模糊）
//        电话（完整）
//        卡面号（完整）
        params.put("memCard",et_chose.getText().toString());
        params.put("index",index);
        params.put("size",num);
        Log.d("xx",params.toString());
        client.post( PreferenceHelper.readString(ac, "shoppay", "yuming", "123") + "/mobile/app/api/appAPI.ashx?Method=AppGetMemBriefList", params, new AsyncHttpResponseHandler()
        {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody)
            {
                try {
                    dialog.dismiss();
                    LogUtils.d("xxVipListS",new String(responseBody,"UTF-8"));
                    JSONObject jso=new JSONObject(new String(responseBody,"UTF-8"));
                    if(jso.getBoolean("success")){
                        mSartRefresh.finishRefresh();
                        mSartRefresh.finishLoadMore();
                      JSONObject jo= jso.getJSONObject("data");
                        Gson gson = new Gson();
                        Type listType = new TypeToken<List<VipList>>(){}.getType();
                        List<VipList> viplist = gson.fromJson(jo.getString("list"), listType);
                        tv_vipnum.setText("共"+jo.getLong("pageCounts")+"位会员");
                            if (jo.getLong("pageCounts") == 0) {
                                if (state == 2) {// 加载
                                    if (list.size() > 0) {
                                        mindex = mindex - 1;
                                    } else {
                                        mindex = mindex - 1;
                                        list.clear();
                                        adapter.notifyDataSetChanged();
                                    }
                                } else {
                                    list.clear();
                                    adapter.notifyDataSetChanged();
                                }
                            } else {
                                if (state == 0) {
                                    mindex = 1;
                                    list.clear();
                                    list.addAll(viplist);
                                    adapter.notifyDataSetChanged();
                                } else if (state == 1) {
                                    mindex = 1;// 刷新
                                    list.clear();
                                    list.addAll(viplist);
                                    adapter.notifyDataSetChanged();
                                    // 调用该方法结束刷新，否则加载圈会一直在
                                    mSartRefresh.finishRefresh();
                                    mSartRefresh.finishLoadMore();
                                } else if (state == 2) {// 加载
                                    list.addAll(viplist);
                                    adapter.notifyDataSetChanged();
                                    // 加载完后调用该方法
                                    mSartRefresh.finishRefresh();
                                    mSartRefresh.finishLoadMore();
                                } else {
                                    list.clear();
                                    list.addAll(viplist);
                                    adapter.notifyDataSetChanged();
                                    // 调用该方法结束刷新，否则加载圈会一直在
                                    mSartRefresh.finishRefresh();
                                    mSartRefresh.finishLoadMore();
                                }
                            }
                    }else{
                        mindex = mindex - 1;
                        adapter.notifyDataSetChanged();
                        mSartRefresh.finishRefresh();
                        mSartRefresh.finishLoadMore();
                    }
                }catch (Exception e){
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error)
            {
                dialog.dismiss();
                LogUtils.d("xxVipInfoE",new String(responseBody));
                mindex = mindex - 1;
                Toast.makeText(getApplicationContext(), "服务器异常，请稍后再试",
                        Toast.LENGTH_SHORT).show();
                mSartRefresh.finishRefresh();
                mSartRefresh.finishLoadMore();
            }
        });
    }


    private void initView() {
        rl_left = (RelativeLayout) findViewById(R.id.rl_left);
        et_chose = (EditText) findViewById(R.id.viplist_et_search);
        tv_title = (TextView) findViewById(R.id.tv_title);
        tv_vipnum = (TextView) findViewById(R.id.viplist_tv_num);
        img_chose= (ImageView) findViewById(R.id.viplist_img_search);
        mListview = findViewById(R.id.listview);
        mSartRefresh= findViewById(R.id.smartRefresh);
        tv_title.setText("会员列表");

        rl_left.setOnClickListener(this);
        img_chose.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_left:
                finish();
                break;
            case R.id.viplist_img_search:
//                if (et_chose.getText().toString().equals("")
//                        || et_chose.getText().toString() == null) {
//                    Toast.makeText(getApplicationContext(), "请输入会员卡号/会员姓名/手机号码",
//                            Toast.LENGTH_SHORT).show();
//                }
//                else {
                    if (CommonUtils.checkNet(getApplicationContext())) {
                        try {
                            mindex=1;
                            obtainViplist(1,1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "请检查网络是否可用",
                                Toast.LENGTH_SHORT).show();
                    }
//                }
                break;

        }
    }

}
