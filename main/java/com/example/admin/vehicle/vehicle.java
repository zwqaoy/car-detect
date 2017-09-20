package com.example.admin.vehicle;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import android.graphics.BitmapFactory;
import android.database.Cursor;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

import android.os.Handler;

import org.json.JSONArray;
import org.json.JSONObject;

public class vehicle extends AppCompatActivity {

    private TextView textView_result;
    private ImageView imageView_car;
    private Button button_take_photo;
    private Button button_pick_picture;
    private Handler handler=null;
    private String content=null;
    private static int RESULT_LOAD_IMAGE = 1;
    private String TAG = "vehicle_detect";
    private String base64 = null;
    private String httpArg = null;
    private String access_token = null;
    public static final String APP_ID = "10123710";
    public static final String API_KEY = "Gd9bxXsl7yyw8HWDkpnb8q0C";
    public static final String SECRET_KEY = "xi8d3x9xsMXAuLnLznWsiv0CwZATBPHI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle);

        textView_result = (TextView)findViewById(R.id.car_result);
        imageView_car = (ImageView) findViewById(R.id.image_car);
        button_take_photo = (Button)findViewById(R.id.take_photo);
        button_pick_picture = (Button)findViewById(R.id.pick_picture);

        button_take_photo.setOnClickListener(takePhoto);
        button_pick_picture.setOnClickListener(pickPicture);
        imageView_car.setOnClickListener(imageCarDetect);
        handler = new Handler();
    }

    Button.OnClickListener takePhoto = new Button.OnClickListener() {
        public void onClick(View v){
            try {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    Button.OnClickListener pickPicture = new Button.OnClickListener() {
        public void onClick(View v){
            Intent i = new Intent(
                    Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, RESULT_LOAD_IMAGE);
        }
    };

    ImageView.OnClickListener imageCarDetect = new ImageView.OnClickListener() {
        public void onClick(View v){
            if (httpArg != null)
                sendRequestWithHttpURLConnection();
        }
    };
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String picturePath = null;
        Bitmap bitmap = null;
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {

            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            Log.d(TAG, picturePath);
            cursor.close();
            if (!picturePath.isEmpty())
                imageView_car.setImageURI(selectedImage);
        }
        else if (requestCode == 2 && resultCode == RESULT_OK && null != data) {
            Bundle bundle = data.getExtras();
            bitmap = (Bitmap)bundle.get("data");
            imageView_car.setImageBitmap(bitmap);
            imgToBase64(null, bitmap);
        }

        if (bitmap != null || picturePath != null) {
            base64 = imgToBase64(picturePath, bitmap);
            base64 = base64.replace("\r\n", "");
            try {
                base64 = URLEncoder.encode(base64, "utf-8");
                httpArg= "imagetype=1"+ "&image="+base64 +"&top_num=4";
                sendRequestWithHttpURLConnection();
            } catch (Exception e) {
                return;
            }
        }
    }

	private void get_access_token() {
        HttpURLConnection connection=null;
        String uri = "https://aip.baidubce.com/oauth/2.0/token?" +
                "grant_type=client_credentials" + "&client_id=" + API_KEY + "&client_secret=" + SECRET_KEY;
        try {
            URL url = new URL(uri);
            connection =(HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.connect();

            InputStream in=connection.getInputStream();
            BufferedReader reader=new BufferedReader(new InputStreamReader(in));
            StringBuilder response=new StringBuilder();

            String line;
            while((line=reader.readLine())!=null){
                response.append(line);
            }
            content = response.toString();
            Log.e(TAG, "get_access_token: " + content);

            JSONObject jsonObject=new JSONObject(content);
            access_token = jsonObject.getString("access_token");
            Log.e(TAG, "get_access_token: access_token: " + access_token);
        }  catch (Exception e) {
            e.printStackTrace();
            content = "网络连接超时，请点击图片重试...";
            handler.post(runnableUi);
        } finally{
            if(connection!=null)
                connection.disconnect();
        }
	}
	
    private void sendRequestWithHttpURLConnection(){
        //开启线程来发起网络请求
        new Thread(new Runnable(){
            public void run() {
                if (access_token == null)
                    get_access_token();
                HttpURLConnection connection=null;
                content = "查询中...";
                handler.post(runnableUi);
                try {
                    String uri = "https://aip.baidubce.com/rest/2.0/image-classify/v1/car?access_token=" + access_token;
                    URL url=new URL(uri);
                    connection =(HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setDoOutput(true);
                    connection.getOutputStream().write(httpArg.getBytes("UTF-8"));
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    connection.connect();

                    InputStream in=connection.getInputStream();
                    BufferedReader reader=new BufferedReader(new InputStreamReader(in));
                    StringBuilder response=new StringBuilder();

                    String line;
                    while((line=reader.readLine())!=null){
                        response.append(line);
                    }
                    content = response.toString();
                    Log.e(TAG, "run: " + content);
                    parseJSONWithJSONObject(content);
                }catch (Exception e) {
                    e.printStackTrace();
                    content = "网络连接超时，请点击图片重试...";
                    handler.post(runnableUi);
                }
                finally{
                    if(connection!=null){
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    Runnable   runnableUi=new  Runnable(){
        @Override
        public void run() {
            textView_result.setText(content);
        }
    };

    Button.OnClickListener selectPicture = new Button.OnClickListener(){
        public void onClick(View v){
            Intent i = new Intent(
                    Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, RESULT_LOAD_IMAGE);
        }
    };



    public static String imgToBase64(String imgPath, Bitmap bitmap) {
        if (imgPath !=null && imgPath.length() > 0) {
            bitmap = readBitmap(imgPath);
        }
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

            out.flush();
            out.close();

            byte[] imgBytes = out.toByteArray();
            return Base64.encodeToString(imgBytes, Base64.DEFAULT);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return null;
        }
    }

    private static Bitmap readBitmap(String imgPath) {
        try {
            return BitmapFactory.decodeFile(imgPath);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return null;
        }
    }

    //方法一：使用JSONObject
    private void parseJSONWithJSONObject(String jsonData) {
        content = "";
        try
        {
            JSONObject jsonObject=new JSONObject(jsonData);
            JSONArray jsonArray=jsonObject.getJSONArray("result");
            for (int i=0; i < jsonArray.length(); i++)    {
                JSONObject object = jsonArray.getJSONObject(i);
                float score1 = Float.parseFloat(object.getString("score")) * 100;
                String score = String.format("%.2f", score1);
                String name = object.getString("name");
                content += "\n 车型: " + name + " 可信度: " + score + "%";
            }
            handler.post(runnableUi);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
