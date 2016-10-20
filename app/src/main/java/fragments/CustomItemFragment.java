package fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firstdata.clovergo.MainActivity;
import com.firstdata.clovergo.R;
import com.firstdata.clovergo.client.callback.TaxRateCallBack;
import com.firstdata.clovergo.client.internal.util.AmountUtil;
import com.firstdata.clovergo.client.model.ErrorResponse;
import com.firstdata.clovergo.client.model.OrderItem;
import com.firstdata.clovergo.client.model.TaxRate;
import com.firstdata.clovergo.client.model.TaxRateResponse;
import com.firstdata.clovergo.client.util.CloverGo;

import java.math.BigDecimal;
import java.util.List;

import domain.SampleCloverConstants;

public class CustomItemFragment extends Fragment implements TaxRateCallBack {

    private Button cardReaderTransactionBtn, manualTransaction, loadTax;
    private EditText editText;
    private Spinner spinner;

    private ProgressDialog progressDialog;

    private CloverGo mCloverGo;
    private InputMethodManager inputMethodManager;
    private List<TaxRate> taxRateList;

    private String amount;
    private String taxRateId;
    private boolean isCheckedUpdating = false;
    private double taxAmount;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_custom_item, container, false);

        getActivity().getActionBar().setTitle("Custom Item");

        editText = (EditText) view.findViewById(R.id.customEditTxt);
        editText.requestFocus();

        cardReaderTransactionBtn = (Button) view.findViewById(R.id.cardReaderTransactionBtn);
        manualTransaction = (Button) view.findViewById(R.id.manualTransactionBtn);
        spinner = (Spinner) view.findViewById(R.id.customSpinner);
        spinner.setVisibility(View.GONE);
        loadTax = (Button) view.findViewById(R.id.loadTaxBtn);

        mCloverGo = MainActivity.getCloverGo();

        inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        cardReaderTransactionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                amount = editText.getText().toString().trim();

                if (!TextUtils.isEmpty(amount)) {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setName("Item 1");
                    orderItem.setPrice(BigDecimal.valueOf(Double.parseDouble(amount)));
                    orderItem.setUnitQuantity(1);
                    orderItem.setTaxRateAmount(taxAmount);
                    orderItem.setTaxRateId(taxRateId);

                    Bundle bundle = new Bundle();
                    bundle.putParcelable(SampleCloverConstants.BUNDLE_MAPPING.ORDER_ITEM.name(), orderItem);

                    Fragment fragment = new TransactionFragment();
                    fragment.setArguments(bundle);
                    ((MainActivity) getActivity()).replaceFragment(fragment);

                } else {
                    Toast.makeText(getActivity(), "Please Enter Amount", Toast.LENGTH_SHORT).show();

                }
            }
        });

        manualTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                amount = editText.getText().toString().trim();

                if (!TextUtils.isEmpty(amount)) {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setName("Item 1");
                    orderItem.setPrice(BigDecimal.valueOf(Double.parseDouble(amount)));
                    orderItem.setUnitQuantity(1);
                    orderItem.setTaxRateAmount(taxAmount);
                    orderItem.setTaxRateId(taxRateId);

                    Bundle bundle = new Bundle();
                    bundle.putParcelable(SampleCloverConstants.BUNDLE_MAPPING.ORDER_ITEM.name(), orderItem);

                    Fragment fragment = new ManualTransactionFragment();
                    fragment.setArguments(bundle);
                    ((MainActivity) getActivity()).replaceFragment(fragment);

                } else {
                    Toast.makeText(getActivity(), "Please Enter Amount", Toast.LENGTH_SHORT).show();

                }
            }
        });

        loadTax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                spinner.setVisibility(View.VISIBLE);

                progressDialog = new ProgressDialog(getActivity());
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                progressDialog.setMessage("Loading Tax Rates....");
                progressDialog.show();

                mCloverGo.loadTaxes(CustomItemFragment.this);
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isCheckedUpdating) {
                    isCheckedUpdating = true;
                    amount = editText.getText().toString().trim();
                    TaxRate taxRate = taxRateList.get(position);

                    if (!TextUtils.isEmpty(amount)) {
                        taxAmount = 0.0;
                        double taxPercentage = taxRate.getRate();
                        double taxableAmount = Double.parseDouble(amount);
                        taxAmount = taxableAmount * taxPercentage;
                        taxRateId = taxRate.getId();
                    }

                    isCheckedUpdating = false;
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return view;
    }

    private class TaxAdapter extends ArrayAdapter<TaxRate> {
        private final Context context;
        private final List<TaxRate> taxRateList;

        private TaxAdapter(Context context, List<TaxRate> taxRate) {
            super(context, R.layout.taxrate_item, taxRate);
            this.context = context;
            this.taxRateList = taxRate;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view = getCustomView(position, convertView, parent);
            return view;
        }

        private View getCustomView(final int position, View convertView, ViewGroup parent) {
            View view = convertView;

            if (view == null) {
                LayoutInflater layoutInflater;
                layoutInflater = LayoutInflater.from(context);
                view = layoutInflater.inflate(R.layout.taxrate_item, null);

            }

            final TaxRate taxRate = getItem(position);

            if (taxRate != null) {
                TextView taxRateStateName = (TextView) view.findViewById(R.id.taxRateStateName);
                TextView taxRatePercentage = (TextView) view.findViewById(R.id.taxRatePercentage);

                if (taxRateStateName != null) {
                    taxRateStateName.setText(taxRate.getName());
                }

                if (taxRatePercentage != null) {
                    taxRatePercentage.setText(String.format(getString(R.string.tax_percentage), AmountUtil.getFormattedPercentage(taxRate.getRate(), 2, 4)));
                }
            }

            return view;
        }
    }

    @Override
    public void onSuccess(TaxRateResponse taxRateResponse) {
        if (taxRateResponse != null) {
            taxRateList = taxRateResponse.getTaxRateList();
            TaxAdapter taxAdapter = new TaxAdapter(getActivity(), taxRateList);

            spinner.setAdapter(taxAdapter);
            progressDialog.dismiss();
        }
    }

    @Override
    public void onFailure(ErrorResponse errorResponse) {
        progressDialog.dismiss();
    }


    @Override
    public void onStop() {
        super.onStop();
        inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }
}