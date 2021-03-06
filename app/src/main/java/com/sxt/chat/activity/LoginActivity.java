package com.sxt.chat.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import com.sxt.chat.App;
import com.sxt.chat.R;
import com.sxt.chat.base.HeaderActivity;
import com.sxt.chat.db.SQLiteUserDao;
import com.sxt.chat.db.User;
import com.sxt.chat.json.ResponseInfo;
import com.sxt.chat.utils.Prefs;
import com.sxt.chat.ws.BmobRequest;

import cn.bmob.v3.BmobUser;

public class LoginActivity extends HeaderActivity implements View.OnClickListener {

    private AutoCompleteTextView editTextUser;
    private TextInputLayout input_user_name, input_password;
    private EditText editTextPwd;
    private String userName;
    private Handler handler = new Handler();
    private final String CMD_LOGIN = this.getClass().getName() + "CMD_LOGIN";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle(R.string.login);
        showToolbar(false);
        User currentUser = BmobUser.getCurrentUser(User.class);
        if (currentUser == null) {
            initView();
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.KEY_IS_AUTO_LOGIN, true);
            startActivity(intent);
            finish();
        }
    }

    private void initView() {
        findViewById(R.id.root).setVisibility(View.VISIBLE);
        input_user_name = findViewById(R.id.input_user_name);
        input_password = findViewById(R.id.input_password);
        editTextUser = findViewById(R.id.tv_user_name);
        editTextPwd = findViewById(R.id.tv_password);

        findViewById(R.id.btn_login_confirm).setOnClickListener(this);
        findViewById(R.id.icon_login).setOnClickListener(this);
        findViewById(R.id.register).setOnClickListener(this);
        findViewById(R.id.forget_pwd).setOnClickListener(this);
        String name = getIntent().getStringExtra(Prefs.KEY_CURRENT_USER_NAME);
        if (name != null) {
            this.userName = name;
            editTextUser.setText(name);
            editTextPwd.requestFocus();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login_confirm:
                checkUserName();
                break;
            case R.id.register:
                startActivity(new Intent(this, RegisterActivity.class));
                break;
        }
    }

    private void checkUserName() {
        input_user_name.setError(null);
        input_password.setError(null);

        userName = editTextUser.getText().toString();
        String passwd = editTextPwd.getText().toString();

        if (TextUtils.isEmpty(userName)) {
            input_user_name.setError(getString(R.string.input_number));
            return;
        }
        if (TextUtils.isEmpty(passwd)) {
            input_password.setError(getString(R.string.input_pwd));
            return;
        }

        loading.show();
        BmobRequest.getInstance(this).login(userName, passwd, CMD_LOGIN);
    }

    @Override
    public void onMessage(ResponseInfo resp) {
        if (ResponseInfo.OK == resp.getCode()) {
            if (CMD_LOGIN.equals(resp.getCmd())) {
                loading.dismiss();
                SQLiteUserDao.getInstance(App.getCtx()).addUser(resp.getUser());
                Prefs prefs = Prefs.getInstance(App.getCtx());
                prefs.setTicket(resp.getUser().getUsername(), resp.getUser().getTicket(), resp.getUser().getAccountId() == null ? 0 : resp.getUser().getAccountId());
                prefs.putString(Prefs.KEY_USER_HEADER_IMAGE_FLAG, prefs.getUserId() + "-" + System.currentTimeMillis());
                Intent intent = new Intent(App.getCtx(), MainActivity.class);
                intent.putExtra(MainActivity.KEY_IS_WILL_GO_LOGIN_ACTIVITY, true);
                startActivity(intent);
                finish();
            }
        } else {
            loading.dismiss();
            Toast(resp.getError());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
