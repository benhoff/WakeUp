package hoff.wakeup;

import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.util.Log;
import android.widget.TextView;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class MainActivity extends AppCompatActivity {
    public static final int PORT = 9;
    private static final String TAG = "WOL";
    private static final String MAC_VALUE = "MAC";
    public EditText edit_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // For testing, override NoNetworkOnMainThread exception
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        edit_text = (EditText)findViewById(R.id.macTextView);

        SharedPreferences settings = getSharedPreferences(TAG, 0);
        String stored = settings.getString(MAC_VALUE, "");
        edit_text.setText(stored, TextView.BufferType.EDITABLE);
    }

    @Override
    protected void onStop(){
        super.onStop();

        SharedPreferences settings = getSharedPreferences(TAG, 0);
        SharedPreferences.Editor editor = settings.edit();
        String edit_text_string = edit_text.getText().toString();
        editor.putString(MAC_VALUE, edit_text_string).apply();
    }

    public void button_clicked(View view) {
        // get mac string
        String mac_string_address = edit_text.getText().toString();
        Log.d(TAG, mac_string_address);
        // Convert mac string to bytes
        byte[] mac_string = get_media_access_control_bytes(mac_string_address);

        // Get the broadcast ip from the wifi ip information
        byte[] broadcast_ip = get_broadcast_address();

        // send the wakeup information
        wakeup(broadcast_ip, mac_string);
    }

    protected byte[] get_broadcast_address(){
        WifiManager wifi_manager = (WifiManager) getSystemService(WIFI_SERVICE);
        DhcpInfo dhcp_info = wifi_manager.getDhcpInfo();
        int broadcast_ip = dhcp_info.gateway | ~ dhcp_info.netmask;
        return BigInteger.valueOf(broadcast_ip).toByteArray();
    }

    protected byte[] get_media_access_control_bytes(String media_access_control) {
        byte[] result = new byte[6];
        String[] mac_address_parts = media_access_control.split(":");
        for(int i=0; i<6; i++){
            Integer hex = Integer.parseInt(mac_address_parts[i], 16);
            result[i] = hex.byteValue();
        }
        return result;
    }

    public void wakeup(byte[] broadcast_ip, byte[] mac) {
        try {
            InetAddress address = InetAddress.getByName("192.168.1.255");
            byte[] bytes = new byte[102];

            // fill first 6 bytes
            for (int i=0; i<6; i++){
                bytes[i] = (byte) 0xff;
            }

            // fill remaining bytes with target MAC
            for(int i=6; i<bytes.length; i+=mac.length) {
                System.arraycopy(mac, 0, bytes, i, mac.length);
            }
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, PORT);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();
        }
        catch (Exception e) {
            System.out.println(e);
            System.out.println("failed to send Wake-On-Lan");
        }
    }
}
