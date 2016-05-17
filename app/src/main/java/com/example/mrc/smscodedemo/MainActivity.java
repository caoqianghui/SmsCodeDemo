package com.example.mrc.smscodedemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

import static com.mob.tools.utils.R.getStringRes;

public class MainActivity extends AppCompatActivity {

	@Bind(R.id.phoneNum)
	TextInputLayout phoneNum;
	@Bind(R.id.smsCode)
	TextInputLayout smsCode;
	@Bind(R.id.but_getCode)
	Button butGetCode;
	@Bind(R.id.but_submit)
	Button butSum;
	@Bind(R.id.now)
	TextView now;
	private int time = 60;
	private boolean flag = true;

	private String iPhone;
	private String iCord;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);

		SMSSDK.initSDK(this, "11f3752b5a0e8", "21a858969156167b6ae8acbc6e7c0288");
		EventHandler eh = new EventHandler() {

			@Override
			public void afterEvent(int event, int result, Object data) {

				Message msg = new Message();
				msg.arg1 = event;
				msg.arg2 = result;
				msg.obj = data;
				handler.sendMessage(msg);
			}

		};
		//注册短信回调
		SMSSDK.registerEventHandler(eh);
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//解除注册，防止内存泄漏
		SMSSDK.unregisterAllEventHandler();
	}
	//验证码送成功后进行倒计时60秒
	private void reminderText() {
		handlerText.sendEmptyMessageDelayed(1, 1000);
	}

	//倒计时60秒
	Handler handlerText = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				butGetCode.setVisibility(View.GONE);//获取验证码按扭隐藏
				now.setVisibility(View.VISIBLE);//显示倒计时文本
				if (time > 0) {
					now.setText("已发送" + time + "秒");
					time--;
					handlerText.sendEmptyMessageDelayed(1, 1000);//自身循环一秒一次
				} else {
					//倒计时完成还原状态
					time = 60;
					now.setVisibility(View.GONE);
					butGetCode.setVisibility(View.VISIBLE);
				}
			} else {
				smsCode.getEditText().setText("");
				time = 60;
				now.setVisibility(View.GONE);
				butGetCode.setVisibility(View.VISIBLE);
			}
		}
	};

	//向服务器发送请求获取验证码，及验证
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			int event = msg.arg1;
			int result = msg.arg2;
			Object data = msg.obj;
			Log.e("event", "event="+event);
			//回调完成
			if (result == SMSSDK.RESULT_COMPLETE) {
				//短信注册成功后，返回MainActivity,然后提示新好友
				if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {//提交验证码成功,验证通过
					Toast.makeText(getApplicationContext(), "验证码校验成功", Toast.LENGTH_SHORT).show();
					handlerText.sendEmptyMessage(2);
				} else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE){//服务器验证码发送成功
					reminderText();
					Toast.makeText(getApplicationContext(), "验证码已经发送", Toast.LENGTH_SHORT).show();
				}else if (event ==SMSSDK.EVENT_GET_SUPPORTED_COUNTRIES){//返回支持发送验证码的国家列表
					Toast.makeText(getApplicationContext(), "获取国家列表成功", Toast.LENGTH_SHORT).show();
				}
			} else {//回调失败
				if(flag){
					butGetCode.setVisibility(View.VISIBLE);
					Toast.makeText(MainActivity.this, "验证码获取失败，请重新获取", Toast.LENGTH_SHORT).show();
					phoneNum.requestFocus();
				}else{
					((Throwable) data).printStackTrace();
					int resId = getStringRes(MainActivity.this, "smssdk_network_error");
					Toast.makeText(MainActivity.this, "验证码错误", Toast.LENGTH_SHORT).show();
					//验证码错误时，将输入框中的验证码设为全选状态着重标记
					smsCode.getEditText().selectAll();
					if (resId > 0) {
						Toast.makeText(MainActivity.this, resId, Toast.LENGTH_SHORT).show();
					}
				}

			}

		}
	};

	/**
	 * 提交及获取验证码
	 * 
	 * @param view
	 */
	@OnClick({ R.id.but_getCode, R.id.but_submit})
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.but_getCode:
			if (!TextUtils.isEmpty(phoneNum.getEditText().getText().toString().trim())) {
				if (phoneNum.getEditText().getText().toString().trim().length() == 11) {
					iPhone = phoneNum.getEditText().getText().toString().trim();
					//getVerificationCode请求获取短信验证码
					SMSSDK.getVerificationCode("86", iPhone);
					smsCode.requestFocus();
				} else {
					Toast.makeText(MainActivity.this, "电话号码非法", Toast.LENGTH_SHORT).show();
					phoneNum.requestFocus();
				}
			} else {
				Toast.makeText(MainActivity.this, "请输入您的电话号码", Toast.LENGTH_SHORT).show();
				phoneNum.requestFocus();
			}
			break;
		case R.id.but_submit:
			//如果验证码输入框不为空
			if (!TextUtils.isEmpty(smsCode.getEditText().getText().toString().trim())) {
				//简单检测验证码
				if (smsCode.getEditText().getText().toString().trim().length() == 4) {
					iCord = smsCode.getEditText().getText().toString().trim();
					//submitVerificationCode提交短信验证码
					SMSSDK.submitVerificationCode("86", iPhone, iCord);
					flag = false;
				} else {
					Toast.makeText(MainActivity.this, "请输入完整验证码", Toast.LENGTH_SHORT).show();
					smsCode.requestFocus();
				}
			} else {//如果为空
				Toast.makeText(MainActivity.this, "请输入验证码", Toast.LENGTH_SHORT).show();
				smsCode.requestFocus();
			}

			break;
		}
	}
}
