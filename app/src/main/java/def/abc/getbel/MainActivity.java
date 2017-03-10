package def.abc.getbel;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

@SuppressLint("NewApi")
public class MainActivity extends AppCompatActivity {

    // ファイル名に挿入する日付を求める
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf_YMD = new SimpleDateFormat("yyyyMMdd");
    String strDate = sdf_YMD.format(cal.getTime());
    // 時刻の獲得．ファイルの書き込みの際に使う
    SimpleDateFormat sdf_time = new SimpleDateFormat("HH:mm:ss:SS");
    String strTime = sdf_time.format(cal.getTime());
    // 間隔を空けてファイルに書き込む
    SimpleDateFormat sdf_filer = new SimpleDateFormat("SS");
    String filTime = sdf_filer.format(cal.getTime());

    // 機種固有のパス．Nexus5の場合は"内部ストレージの直下のようだ"
    String envPath = Environment.getExternalStorageDirectory().getPath();
    // 保存先とファイル名を指示する
    String filePath = envPath + "/sdcard/" + strDate + "rssi_data.txt";
    // openFileOutputの宣言
    FileOutputStream fos = null;

    private final String TAG = "MainActivity";
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private BluetoothAdapter.LeScanCallback LsCallback;
    private BluetoothLeScanner btLeScanner;
    private ScanCallback sCallback;

    private String textData = "";
    private TextView varScan;

    int count = 1;
    int count_pre = 0;
    int time1;

    //buttonを取得
//    Button btnR = (Button)findViewById(R.id.resteButton);
//    Button btnC = (Button)findViewById(R.id.counter);

    //===================== onCreate =========================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        // APIレベルで5以上かどうかを判定して処理を分ける
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btLeScanner = btAdapter.getBluetoothLeScanner();
            initScanCallback();
        } else {
            initLeScanCallback();
        }
    }
    //=====================  =========================
    @Override
    protected void onResume() {
        super.onResume();
        if (!btAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "BluetoothをONにして下さい", Toast.LENGTH_SHORT).show();
            return;
        }
        startBLEScan();
    }
    //===================== スキャンの停止 =========================
    @Override
    protected void onPause() {
        super.onPause();
        stopBLEScan();
    }
    //===================== APIレベル5以上 =========================
    private void initScanCallback() {
        sCallback = new ScanCallback() {
            Button btnC = (Button)findViewById(R.id.counter);
            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                for (ScanResult result : results) {
                    scanResult(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                }
            }

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                try {
                    super.onScanResult(callbackType, result);
                    btnC.setOnClickListener(clicked_counter);
//                    Thread.sleep(200);
                    fos = new FileOutputStream(filePath, true);
                    varScan = (TextView) findViewById(R.id.show_data);

                    scanResult(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());

                    varScan.setText(textData);
                    // ファイルの書き込み
                    fos.write(textData.getBytes());
                    // ファイルのクローズ
                    count_pre = time1;
                    fos.close();

                } catch (Exception e) {
                    Log.e("Error", e.getMessage());
                }
            }
            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };
    }
    //===================== APIレベル5未満 =========================
    private void initLeScanCallback() {
        LsCallback = new BluetoothAdapter.LeScanCallback() {
            int flag = 1;

            Button btnC = (Button)findViewById(R.id.counter);

            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                try {
                    // ボタン押しで一回止める
                    btnC.setOnClickListener(clicked_counter);
//                    Thread.sleep(200);

                    fos = new FileOutputStream(filePath, true);
                    varScan = (TextView) findViewById(R.id.show_data);

                    scanResult(device, rssi, scanRecord);


                    varScan.setText(textData);
                    // ファイルの書き込み
                    fos.write(textData.getBytes());
                    // ファイルのクローズ
                    count_pre = time1;
                    fos.close();


                } catch (Exception e) {
                    Log.e("Error", e.getMessage());
                }
                try {
                    getApplicationContext().sendBroadcast(
                            new Intent(
                                    Intent.ACTION_MEDIA_MOUNTED,
                                    Uri.parse("file://" + Environment.getExternalStorageDirectory())
                            )
                    );
                } catch (Exception exception) {
                    System.out.println(exception.getMessage());
                }

            }
        };
    }
    //=====================  =========================
    private void startBLEScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btLeScanner.startScan(sCallback);
        } else {
            btAdapter.startLeScan(LsCallback);
        }
    }

    //=====================  =========================
    private void stopBLEScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btLeScanner.stopScan(sCallback);
        } else {
            btAdapter.stopLeScan(LsCallback);
        }
    }
    //=====================  =========================
    private void scanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Calendar cal = Calendar.getInstance();  //現在時間獲得
        strTime = sdf_time.format(cal.getTime());//フォーマット変換
        filTime = sdf_filer.format(cal.getTime());
        time1 = Integer.parseInt(filTime);

        if(scanRecord.length > 30) {
            String uuid = getUUID(scanRecord);
            int major = Integer.parseInt(getMajor(scanRecord));
            int minor = Integer.parseInt(getMinor(scanRecord));

            if (major == 111 || major == 406 || major == 175) {
                textData = strTime + "uuid = " + uuid + ", major = " + major + ", minor = " + minor + ", rssi = " + rssi + " label " + count + "\r\n";
            }else{
                textData = "";
            }
        }
    }
    //=====================  =========================
    private String getUUID(byte[] scanRecord) {
        String uuid = IntToHex2(scanRecord[9] & 0xff)
                + IntToHex2(scanRecord[10] & 0xff)
                + IntToHex2(scanRecord[11] & 0xff)
                + IntToHex2(scanRecord[12] & 0xff)
                + "-"
                + IntToHex2(scanRecord[13] & 0xff)
                + IntToHex2(scanRecord[14] & 0xff)
                + "-"
                + IntToHex2(scanRecord[15] & 0xff)
                + IntToHex2(scanRecord[16] & 0xff)
                + "-"
                + IntToHex2(scanRecord[17] & 0xff)
                + IntToHex2(scanRecord[18] & 0xff)
                + "-"
                + IntToHex2(scanRecord[19] & 0xff)
                + IntToHex2(scanRecord[20] & 0xff)
                + IntToHex2(scanRecord[21] & 0xff)
                + IntToHex2(scanRecord[22] & 0xff)
                + IntToHex2(scanRecord[23] & 0xff)
                + IntToHex2(scanRecord[24] & 0xff);
        return uuid;
    }

    private String getMajor(byte[] scanRecord) {
        String hexMajor = IntToHex2(scanRecord[25] & 0xff) + IntToHex2(scanRecord[26] & 0xff);
        return String.valueOf(Integer.parseInt(hexMajor, 16));
    }

    private String getMinor(byte[] scanRecord) {
        String hexMinor = IntToHex2(scanRecord[27] & 0xff) + IntToHex2(scanRecord[28] & 0xff);
        return String.valueOf(Integer.parseInt(hexMinor, 16));
    }

    // 16進2桁に変換
    private String IntToHex2(int i) {
        char hex_2[]     = { Character.forDigit((i >> 4) & 0x0f, 16), Character.forDigit(i & 0x0f, 16) };
        String hex_2_str = new String(hex_2);
        return hex_2_str.toUpperCase();
    }
    //=====================  =========================
    View.OnClickListener clicked_counter = new View.OnClickListener() {
        public void onClick(View v) {
            count_pre = count;
            count += 1;
            Log.v("Button","onClick");
        }
    };
}
/*---------------　参考　-------------------
Android4と5、両方でBeaconを利用する際の処理方法
http://marunouchi-tech.i-studio.co.jp/2098/
------------------------------------*/