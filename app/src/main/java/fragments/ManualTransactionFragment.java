package fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firstdata.clovergo.MainActivity;
import com.firstdata.clovergo.R;
import com.firstdata.clovergo.SignatureActivity;
import com.firstdata.clovergo.client.callback.TransactionCallBack;
import com.firstdata.clovergo.client.event.TransactionEvent;
import com.firstdata.clovergo.client.internal.util.AmountUtil;
import com.firstdata.clovergo.client.model.ErrorResponse;
import com.firstdata.clovergo.client.model.KeyedTransactionRequest;
import com.firstdata.clovergo.client.model.OrderItem;
import com.firstdata.clovergo.client.model.TransactionResponse;
import com.firstdata.clovergo.client.util.CloverGo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import domain.SampleCloverConstants;
import domain.Validator;

/**
 * Created by f3vkhba on 6/6/16.
 */
public class ManualTransactionFragment extends Fragment implements TransactionCallBack {
    private EditText mCardNumber, mExpiration, mCvv;
    private Button mManualPayBtn;
    private KeyedTransactionRequest keyedTransactionRequest;
    private CloverGo mCloverGo;
    private TextView mAmountTxt, mSubtotalAmount, mTaxRateAmount;
    private AlertDialog alertDialog;
    private InputMethodManager inputMethodManager;
    private ProgressDialog progressDialog;

    private Dialog mChoiceDialog;
    private Button mGet450Btn;
    private String btMac, btName;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manual_transaction, container, false);

        mCloverGo = MainActivity.getCloverGo();

        mAmountTxt = (TextView) view.findViewById(R.id.amountEditTxt);
        mSubtotalAmount = (TextView) view.findViewById(R.id.transactionSubTotal);
        mTaxRateAmount = (TextView) view.findViewById(R.id.taxTotal);

        mCardNumber = (EditText) view.findViewById(R.id.cardNumberEditText);
        mExpiration = (EditText) view.findViewById(R.id.expirationEditText);
        mCvv = (EditText) view.findViewById(R.id.cvvEditText);

        mManualPayBtn = (Button) view.findViewById(R.id.manualPayBtn);

        mGet450Btn = (Button) view.findViewById(R.id.get450ReadersBtn);
        mGet450Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<BTReaderDevice> list = new ArrayList<BTReaderDevice>();
                BTReaderDevice device = new BTReaderDevice();
                device.macAddress = "123";
                device.name = "123name";
                list.add(device);
                device = new BTReaderDevice();
                device.macAddress = "456";
                device.name = "456name";
                list.add(device);

                showBluetoothReaders(list);
            }
        });

        getActivity().getActionBar().setTitle("Manual Transaction");

        mCardNumber.requestFocus();

        inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);


        final Bundle bundle = getArguments();
        final ArrayList<OrderItem> mOrderUnsavedItems = bundle.getParcelableArrayList(SampleCloverConstants.BUNDLE_MAPPING.ORDER_ITEMS.name());
        final OrderItem customOrderItem = bundle.getParcelable(SampleCloverConstants.BUNDLE_MAPPING.ORDER_ITEM.name());
        Double totalAmount = 0.0;
        double amount = 0;
        double taxAmount = 0;

        if (mOrderUnsavedItems != null && mOrderUnsavedItems.size() > 0) {

            HashMap<Double, BigDecimal> taxGroup = new HashMap<>();
            for (OrderItem orderItem : mOrderUnsavedItems) {
                amount += orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getUnitQuantity())).doubleValue();
                AmountUtil.addTaxRateToHashMap(taxGroup, orderItem);
            }
            taxAmount = AmountUtil.getTotalTaxFromHashMap(taxGroup);
            totalAmount = amount + taxAmount;

        } else if (customOrderItem != null) {
            amount += customOrderItem.getPrice().doubleValue();
            taxAmount += customOrderItem.getTaxRateAmount();
            totalAmount = amount + taxAmount;
        }


        alertDialog = new AlertDialog.Builder(getActivity()).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setCancelable(false).create();


        mTaxRateAmount.setText(AmountUtil.getCurrencyFormattedAmount(taxAmount));
        mAmountTxt.setText(AmountUtil.getCurrencyFormattedAmount(totalAmount));
        mSubtotalAmount.setText(AmountUtil.getCurrencyFormattedAmount(amount));

        mManualPayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cardNumber = mCardNumber.getText().toString();
                if (cardNumber.matches("")) {
                    Toast.makeText(getActivity(), "Please enter a valid card number", Toast.LENGTH_SHORT).show();
                    return;
                }
                String expiration = (mExpiration.getText().toString().replace("/", ""));
                if (expiration.matches("")) {
                    Toast.makeText(getActivity(), "Please enter a valid card expiration", Toast.LENGTH_SHORT).show();
                    return;
                }
                String cvv = mCvv.getText().toString();
                if (cvv.matches("")) {
                    Toast.makeText(getActivity(), "Please enter a valid cvv number", Toast.LENGTH_SHORT).show();
                    return;
                }

                keyedTransactionRequest = new KeyedTransactionRequest(cardNumber, expiration, cvv);
                if (Validator.validateCardNumber(cardNumber) &&
                        Validator.validateCardExpiry(expiration)) {
                    keyedTransactionRequest.setCardNumber(cardNumber);
                    keyedTransactionRequest.setExpDate(expiration);
                    keyedTransactionRequest.setCvv(cvv);
                    keyedTransactionRequest.setCardPresent(true);
                    keyedTransactionRequest.setExternalPaymentId("999");
                    if (mOrderUnsavedItems != null && mOrderUnsavedItems.size() > 0) {
                        keyedTransactionRequest.setOrderItemList(mOrderUnsavedItems);
                    } else if (customOrderItem != null) {
                        ArrayList<OrderItem> orderItems = new ArrayList<OrderItem>();
                        orderItems.add(customOrderItem);
                        keyedTransactionRequest.setOrderItemList(orderItems);
                    }
                }
                progressDialog = new ProgressDialog(getActivity());
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                progressDialog.setTitle("Processing Transaction");
                progressDialog.setMessage("Loading....");
                progressDialog.show();

                mCloverGo.doKeyedTransaction(keyedTransactionRequest, ManualTransactionFragment.this);
            }
        });
        return view;

    }

    @Override
    public boolean proceedOnError(TransactionEvent transactionEvent) {
        return false;
    }

    @Override
    public void onSuccess(final TransactionResponse transactionResponse) {
        progressDialog.dismiss();
        alertDialog.dismiss();

        keyedTransactionRequest.setExternalPaymentId(transactionResponse.getTransactionId());

        alertDialog = new AlertDialog.Builder(getActivity()).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent(getActivity(), SignatureActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(SampleCloverConstants.BUNDLE_MAPPING.TRANSACTION_ID.name(), transactionResponse.getTransactionId());
                bundle.putString(SampleCloverConstants.BUNDLE_MAPPING.ORDER_ID.name(), transactionResponse.getOrderId());
                intent.putExtras(bundle);
                startActivityForResult(intent, 0);
            }

        }).setCancelable(false).create();
        alertDialog.setMessage(transactionResponse.getStatus());
        alertDialog.setTitle("Transaction Status");
        alertDialog.show();
    }

    @Override
    public void onFailure(ErrorResponse errorResponse) {
        progressDialog.dismiss();
        alertDialog.setMessage(errorResponse.getMessage() + "Manual transaction failed");
        alertDialog.setTitle("Transaction Failed");
        alertDialog.show();
    }

    @Override
    public void onStop() {
        super.onStop();
        inputMethodManager.hideSoftInputFromWindow(mCvv.getWindowToken(), 0);
    }

    private void showBluetoothReaders(final List<BTReaderDevice> pairedReaders) {
        List<String> readers = new ArrayList();

        for (BTReaderDevice btReaderDevice : pairedReaders)
            readers.add(btReaderDevice.name);

        showSingleSelectChoices("Choose a Bluetooth Reader", readers, new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BTReaderDevice device = pairedReaders.get(position);

                btMac = device.macAddress;
                btName = device.name;

                Toast.makeText(getActivity(), btMac + ":" + btName, Toast.LENGTH_SHORT).show();
                mChoiceDialog.dismiss();
            }
        }, null, getActivity());
    }

    public class BTReaderDevice {
        public String name;
        public String macAddress;
        public List<UUID> UUIDS;
    }

    public void showSingleSelectChoices(String title, List<String> choices, AdapterView.OnItemClickListener itemClickListener, View.OnClickListener buttonClickListener, Context context) {


        mChoiceDialog = new Dialog(context, R.style.myDialog);
        mChoiceDialog.setContentView(R.layout.choice_dialog_layout);
        mChoiceDialog.setCancelable(true);
        mChoiceDialog.setCanceledOnTouchOutside(false);
        mChoiceDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mChoiceDialog.dismiss();
            }
        });

        mChoiceDialog.findViewById(R.id.choiceDiaBttn).setVisibility(View.VISIBLE);
        mChoiceDialog.findViewById(R.id.choiceDiaBttn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChoiceDialog.dismiss();
            }
        });

        ((TextView) mChoiceDialog.findViewById(R.id.choiceDiaTitle)).setText(title);

        ListView choiceList = (ListView) mChoiceDialog.findViewById(R.id.choiceDiaList);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, android.R.id.text1, choices);
        choiceList.setAdapter(arrayAdapter);
        choiceList.setOnItemClickListener(itemClickListener);

        mChoiceDialog.show();
    }

}
