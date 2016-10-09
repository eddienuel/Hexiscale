package com.socketbytes.hexiscale.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.socketbytes.hexiscale.util.ApplicationSession;
import com.wolkabout.wolkrestandroid.Credentials_;

import com.socketbytes.hexiscale.R;
import com.wolkabout.wolkrestandroid.dto.AuthenticationResponseDto;
import com.wolkabout.wolkrestandroid.dto.SignInDto;
import com.wolkabout.wolkrestandroid.service.AuthenticationService;
import com.wolkabout.wolkrestandroid.service.AuthenticationService_;


public class LoginActivity extends AppCompatActivity {

    EditText password, email;
    Button loginButton;
    AuthenticationService authenticationService;
    ApplicationSession session;
    Context context;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        password = (EditText)findViewById(R.id.password_field);
        email = (EditText)findViewById(R.id.email_field);
        loginButton = (Button)findViewById(R.id.btnLogin);
        session = ApplicationSession.getInstance();
        context = this;

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validateCredentials()) {
                    return;
                }
                signIn();
            }
        });
    }

    private boolean validateCredentials() {
        if (email.getText().toString().isEmpty()) {
            email.setError("This field is required.");
            return false;
        }

        if (password.getText().toString().isEmpty()) {
            email.setError("Password must have 8 or more characters.");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.getText().toString()).matches()) {
            email.setError("Input is not a valid email.");
            return false;
        }

        return true;
    }

    void signIn() {

        Thread thread = new Thread(){

            @Override
            public void run(){
                try {
                    authenticationService = new AuthenticationService_(context);
                    final String emailAddress = LoginActivity.this.email.getText().toString();
                    final String password = LoginActivity.this.password.getText().toString();

                    final AuthenticationResponseDto response = authenticationService.signIn(new SignInDto(emailAddress, password));

                    Credentials_ credentials = new Credentials_(LoginActivity.this);
                    credentials.username().put(response.getEmail());
                    credentials.accessToken().put(response.getAccessToken());
                    credentials.refreshToken().put(response.getRefreshToken());
                    credentials.accessTokenExpires().put(response.getAccessTokenExpires().getTime());
                    credentials.refreshTokenExpires().put(response.getRefreshTokenExpires().getTime());


//                    session.setUsername(response.getEmail());
//                    session.setAccessToken(response.getAccessToken());
//                    session.setRefreshToken(response.getRefreshToken());
//                    session.setAccessTokenExpires(response.getAccessTokenExpires().getTime());
//                    session.setRefreshTokenExpires(response.getRefreshTokenExpires().getTime());
                    Intent intent= new Intent(LoginActivity.this,DeviceScanActivity.class);
                    LoginActivity.this.startActivity(intent);
                    stopDialog();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
        startDialog("Signing in ...");

    }

    private void startDialog(String message){
        dialog = new ProgressDialog(this);
        dialog.setMessage(message);
        dialog.show();
    }

    private void stopDialog(){
        if(dialog!=null){
            dialog.dismiss();
        }
    }
}
