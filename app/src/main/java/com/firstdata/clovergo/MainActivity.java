package com.firstdata.clovergo;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Toast;

import com.firstdata.clovergo.client.model.CloverGoConstants;
import com.firstdata.clovergo.client.util.CloverGo;

import java.util.ArrayList;
import java.util.List;

import fragments.MainFragment;
import fragments.SendReceiptFragment;

public class MainActivity extends FragmentActivity {

    private final String API_KEY = "N1sWE4VKuYErF55TUj1WGjOtSgG7lPrx";
    private final String API_SECRET = "5546ac1e6727a528d8rf46acf94cdeca13rt54fb03b1eeef1e346fcd10e34343";
    private final CloverGoConstants.ENV ENVIRONMENT = CloverGoConstants.ENV.TEST;

    private final String EMPLOYEE_ID = "D3WBSX1RE88Y2";
    private final String MERCHANT_ID = "19TZTEEHW4TF4";
    private final String DEVICE_ID = "46fd97d2-a216-42d2-b035-1e325dd2d8dd";

    private static CloverGo mCloverGo;

    public static CloverGo getCloverGo() {
        return mCloverGo;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCloverGo = new CloverGo(MainActivity.this, API_KEY, API_SECRET, ENVIRONMENT, DEVICE_ID, MERCHANT_ID, EMPLOYEE_ID);

        if (Build.VERSION.SDK_INT >= 23 && (!checkPermission(Manifest.permission.RECORD_AUDIO) || !checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)))
            requestPermissions();

        addFragment(new MainFragment());
    }

    public void replaceFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frame_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    public void addFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frame_container, fragment);
        fragmentTransaction.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            SendReceiptFragment sendReceiptFragment = new SendReceiptFragment();
            sendReceiptFragment.setArguments(data.getExtras());
            replaceFragment(sendReceiptFragment);
        }
    }

    public void requestPermissions() {
        final Dialog alertDialog = new Dialog(this);
        alertDialog.setContentView(R.layout.permission_dialog_layout);
        alertDialog.setCancelable(true);
        alertDialog.setCanceledOnTouchOutside(true);

        if (checkPermission(Manifest.permission.RECORD_AUDIO))
            alertDialog.findViewById(R.id.permissionAudioBtn).setEnabled(false);
        if (checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION))
            alertDialog.findViewById(R.id.permissionLocationBtn).setEnabled(false);

        alertDialog.findViewById(R.id.permissionAudioBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23 && !checkPermission(Manifest.permission.RECORD_AUDIO)) {
                    requestPermissionsFromUser(new String[]{Manifest.permission.RECORD_AUDIO});
                } else {
                    Toast.makeText(MainActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
                }
            }
        });

        alertDialog.findViewById(R.id.permissionLocationBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23 && !checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    requestPermissionsFromUser(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION});
                } else {
                    Toast.makeText(MainActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
                }
            }
        });

        alertDialog.findViewById(R.id.turnOnLocationBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLocationEnabled()) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                } else {
                    Toast.makeText(MainActivity.this, "GPS already turned on", Toast.LENGTH_SHORT).show();
                }
            }
        });

        alertDialog.findViewById(R.id.turnOnLBluetoothBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnBluetooth();
            }
        });

        alertDialog.findViewById(R.id.permissionDoneBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        alertDialog.setTitle("Enable App Permissions");
        alertDialog.show();
    }

    public void requestPermissionsFromUser(String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissions == null || permissions.length < 1)
                return;

            List<String> pendingPermissionRequest = new ArrayList();
            for (String permission : permissions) {
                int currentPermission = ContextCompat.checkSelfPermission(this, permission);
                if (currentPermission == PackageManager.PERMISSION_DENIED)
                    pendingPermissionRequest.add(permission);
            }
            if (!pendingPermissionRequest.isEmpty())
                ActivityCompat.requestPermissions(this, pendingPermissionRequest.toArray(new String[pendingPermissionRequest.size()]), 455);
        }
    }

    public boolean checkPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int currentPermission = ContextCompat.checkSelfPermission(this, permission);
            if (currentPermission == PackageManager.PERMISSION_GRANTED)
                return true;
            else
                return false;
        } else
            return true;
    }

    public boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void turnOnBluetooth() {
        BluetoothAdapter.getDefaultAdapter().enable();
    }
}