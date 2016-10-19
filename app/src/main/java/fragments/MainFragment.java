package fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.firstdata.clovergo.MainActivity;
import com.firstdata.clovergo.R;
import com.firstdata.clovergo.client.util.CloverGo;

/**
 * Created by f3vkhba on 6/2/16.
 */
public class MainFragment extends Fragment {
    private Button inventoryItem, customItem;
    private CloverGo mCloverGo;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        inventoryItem = (Button) view.findViewById(R.id.inventoryItem);
        customItem = (Button) view.findViewById(R.id.customItem);
        mCloverGo = MainActivity.getCloverGo();

        setHasOptionsMenu(true);

        getActivity().getActionBar().setTitle("Clover Go Sample");

        inventoryItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).replaceFragment(new InventoryFragment());
            }
        });
        customItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).replaceFragment(new CustomItemFragment());
            }
        });
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getActivity().getMenuInflater().inflate(R.menu.menu_values, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.version:
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setCancelable(true).create();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Version  ").append(mCloverGo.sdkVersion()).append("\n");
                alertDialog.setMessage(stringBuilder.toString());
                alertDialog.setTitle("SDK Version");
                alertDialog.show();
        }
        return true;
    }

}
