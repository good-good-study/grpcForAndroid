/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sxt.grpc;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class MainActivity extends AppCompatActivity {

    private ScrollView scrollView;
    private LinearLayout responseContainer;
    private EditText editText;
    private MessageTask task;
    private final String TAG = this.getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        editText = findViewById(R.id.edit_text);
        scrollView = findViewById(R.id.scrollView);
        responseContainer = findViewById(R.id.response_layout);
    }

    public void sendMessage(View view) {
        String message = editText.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "消息内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (task != null) {
            task.cancel(true);
        }
        task = new MessageTask();
        task.execute("192.168.10.25", "8080", message);
    }

    public class MessageTask extends AsyncTask<String, Void, String> {

        private ManagedChannel channel;

        @Override
        protected String doInBackground(String... strings) {
            String host = strings[0];
            String portStr = strings[1];
            String message = strings[2];
            StringBuilder sb = new StringBuilder();
            try {
                int port = Integer.parseInt(portStr);
                channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
                TestServiceGrpc.TestServiceBlockingStub stub = TestServiceGrpc.newBlockingStub(channel);
                Response response = stub.getMessage(Message.newBuilder().setContent(message).build());
                sb.append(response.getMessage().getContent());
            } catch (Exception e) {
                e.printStackTrace();
                sb.append(String.format("发送失败 %s ", e.getMessage()));
            }
            return sb.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (channel != null) {
                try {
                    channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            TextView textView = new TextView(MainActivity.this);
            textView.setText(s);
            textView.setTextColor(Color.BLACK);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            responseContainer.addView(textView);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) textView.getLayoutParams();
            lp.gravity = Gravity.START;
            lp.topMargin = 8;
            lp.bottomMargin = 2;
            textView.setLayoutParams(lp);

            if (!isCover(textView)) {
                textView.measure(0, 0);
                scrollView.setScrollY(scrollView.getScrollY() + textView.getMeasuredHeight() * 2);
            }

            Log.e(TAG, s);
        }
    }

    public boolean isCover(View view) {
        Rect rect = new Rect();
        if (view.getGlobalVisibleRect(rect)) {
            if (rect.height() >= view.getMeasuredHeight()) {
                return true;
            }
        }
        return false;
    }
}
