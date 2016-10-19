package com.firstdata.clovergo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.firstdata.clovergo.client.model.CloverGoConstants;
import com.firstdata.clovergo.client.util.CloverGo;

import fragments.MainFragment;
import fragments.SendReceiptFragment;

public class MainActivity extends FragmentActivity {

    private static CloverGo mCloverGo;

    public static CloverGo getCloverGo() {
        return mCloverGo;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCloverGo = new CloverGo(MainActivity.this, "N1sWE4VKuYErF55TUj1WGjOtSgG7lPrx", "5546ac1e6727a528d8rf46acf94cdeca13rt54fb03b1eeef1e346fcd10e34343", CloverGoConstants.ENV.STAGING);
        mCloverGo.setEmployeeId("D3WBSX1RE88Y2");
        mCloverGo.setDeviceId("46fd97d2-a216-42d2-b035-1e325dd2d8dd");
        mCloverGo.setMerchantId("19TZTEEHW4TF4");
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
}