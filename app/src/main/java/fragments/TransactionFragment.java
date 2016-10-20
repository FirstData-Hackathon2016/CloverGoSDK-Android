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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firstdata.clovergo.MainActivity;
import com.firstdata.clovergo.R;
import com.firstdata.clovergo.SignatureActivity;
import com.firstdata.clovergo.client.callback.CardReaderCallBack;
import com.firstdata.clovergo.client.callback.TransactionCallBack;
import com.firstdata.clovergo.client.event.CardReaderErrorEvent;
import com.firstdata.clovergo.client.event.CardReaderEvent;
import com.firstdata.clovergo.client.event.TransactionEvent;
import com.firstdata.clovergo.client.internal.util.AmountUtil;
import com.firstdata.clovergo.client.model.ApplicationIdentifier;
import com.firstdata.clovergo.client.model.CardReaderInfo;
import com.firstdata.clovergo.client.model.CloverGoConstants;
import com.firstdata.clovergo.client.model.ErrorResponse;
import com.firstdata.clovergo.client.model.OrderItem;
import com.firstdata.clovergo.client.model.TransactionRequest;
import com.firstdata.clovergo.client.model.TransactionResponse;
import com.firstdata.clovergo.client.util.CloverGo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import domain.SampleCloverConstants;

public class TransactionFragment extends Fragment implements CardReaderCallBack, TransactionCallBack {

    private TextView mAmountTxt, mSubtotalAmount, mTaxRateAmount;
    private Button mPayBtn;
    private TransactionRequest transactionRequest;
    private AlertDialog alertDialog;
    private ProgressDialog progressDialog, resetProgressDialog;
    private boolean readerReady;
    private CardReaderInfo mCardReaderInfo;
    private CloverGo mCloverGo;

    private ArrayList<CardReaderInfo> mDetectedBTReadersList;
    private ArrayList<String> mDetectedBTReadersStringsList;
    private ArrayAdapter<String> mDetectedReadersArrayAdapter;

    private ListView mDetectedBTReadersListVw;
    private Dialog mChoiceDialog;
    private Button mGet450Btn;
    private String btMac, btName;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction, container, false);
        setHasOptionsMenu(true);
        mCloverGo = MainActivity.getCloverGo();
        getActivity().getActionBar().setTitle("Card Reader Transaction");

        mDetectedBTReadersList = new ArrayList<>();
        mDetectedBTReadersStringsList = new ArrayList<>();

        mAmountTxt = (TextView) view.findViewById(R.id.amountEditTxt);
        mSubtotalAmount = (TextView) view.findViewById(R.id.transactionSubTotal);
        mTaxRateAmount = (TextView) view.findViewById(R.id.taxTotal);
        mPayBtn = (Button) view.findViewById(R.id.payBttn);
        mPayBtn.setEnabled(false);

        mGet450Btn = (Button) view.findViewById(R.id.get450ReadersBtn);
        mGet450Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBluetoothReaders();
            }
        });

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
        mTaxRateAmount.setText(AmountUtil.getCurrencyFormattedAmount(taxAmount));
        mAmountTxt.setText(AmountUtil.getCurrencyFormattedAmount(totalAmount));
        mSubtotalAmount.setText(AmountUtil.getCurrencyFormattedAmount(amount));
        mPayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transactionRequest = new TransactionRequest();
                transactionRequest.setTips(0);
                transactionRequest.setExternalPaymentId("999");
                if (mOrderUnsavedItems != null && mOrderUnsavedItems.size() > 0) {
                    transactionRequest.setOrderItemList(mOrderUnsavedItems);
                } else if (customOrderItem != null) {
                    ArrayList<OrderItem> orderItems = new ArrayList<OrderItem>();
                    orderItems.add(customOrderItem);
                    transactionRequest.setOrderItemList(orderItems);
                }
                mCloverGo.doReaderTransaction(transactionRequest, TransactionFragment.this);
            }
        });

        mAmountTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0)
                    mPayBtn.setEnabled(readerReady);
                else
                    mPayBtn.setEnabled(false);
            }
        });

        alertDialog = new AlertDialog.Builder(getActivity()).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setCancelable(false).create();
        progressDialog = new ProgressDialog(getActivity());
        resetProgressDialog = new ProgressDialog(getActivity());
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        mCloverGo.stopBluetoothReaderScan();
        mCloverGo.release();
        mCloverGo.release();
        progressDialog.dismiss();
        alertDialog.dismiss();
        readerReady = false;
    }

    @Override
    public void onStart() {
        super.onStart();
        mCloverGo.init(CloverGoConstants.CARD_READER_TYPE.RP450X, TransactionFragment.this);
        mCloverGo.startBluetoothReaderScan();
    }

    @Override
    public void onConnected() {
        Toast.makeText(getActivity(), "Reader Connected, please make sure to Insert your Card", Toast.LENGTH_LONG).show();
        mCardReaderInfo = mCloverGo.getCardReaderInfo();
    }

    @Override
    public void onReady() {
//        public void onReady(CardReaderInfo mCardReaderInfo) {
        Toast.makeText(getActivity(), "Reader Ready", Toast.LENGTH_SHORT).show();
        readerReady = true;
//        if (mCardReaderInfo.isResetRequired()) {
//            progressDialog.setTitle("Resetting Reader");
//            progressDialog.setMessage("Resetting reader please wait");
//            progressDialog.show();
//            mCloverGo.resetReader();
//        }
        if (mAmountTxt.length() > 0) {
            mPayBtn.setEnabled(true);
        } else {
            mPayBtn.setEnabled(false);
        }
        resetReaderMenu.setEnabled(true);
        cardReaderMenu.setEnabled(true);
    }

    @Override
    public void onDisconnected() {
        Toast.makeText(getActivity(), "Reader Disconnected", Toast.LENGTH_SHORT).show();
        progressDialog.dismiss();
        alertDialog.dismiss();
        mPayBtn.setEnabled(false);
        readerReady = false;
    }

    @Override
    public void onError(CardReaderErrorEvent readerErrorEvent) {
        Toast.makeText(getActivity(), readerErrorEvent.getData(), Toast.LENGTH_SHORT).show();
        progressDialog.dismiss();
    }

    @Override
    public void onProgress(CardReaderEvent readerEvent) {
        alertDialog.dismiss();
        progressDialog.dismiss();
        Toast.makeText(getActivity(), readerEvent.getData(), Toast.LENGTH_SHORT).show();
        switch (readerEvent.getCardReaderEventType()) {
            case EMV_CARD_INSERTED:
                progressDialog.setTitle("Processing Transaction");
                progressDialog.setMessage("Please don't remove card");
                progressDialog.show();
                break;
            case EMV_CARD_DIP_FAILED:
                progressDialog.setTitle("Card Dip failed");
                progressDialog.setMessage("Please dip the card again");
                progressDialog.show();
                break;
            case EMV_CARD_SWIPED_ERROR:
                progressDialog.setTitle("Emv card swiped");
                progressDialog.setMessage("Please dip the card");
                progressDialog.show();
                break;
            case EMV_DIP_FAILED_3_ATTEMPTS:
                progressDialog.setTitle("Emv dip failed");
                progressDialog.setMessage("Please swipe the card");
                progressDialog.show();
                break;
            case SWIPE_FAILED:
                progressDialog.setTitle("Swipe failed");
                progressDialog.setMessage("Please swipe again");
                progressDialog.show();
                break;
            case EMV_CARD_REMOVED:
                progressDialog.dismiss();
                alertDialog.dismiss();
                break;
            case CARD_TAPPED:
            case CARD_SWIPED:
                progressDialog.setTitle("Processing Transaction");
                progressDialog.setMessage("Please wait");
                progressDialog.show();
                break;
        }
    }

    @Override
    public void onSuccess(final TransactionResponse transactionResponse) {
        progressDialog.dismiss();
        alertDialog.dismiss();

        transactionRequest.setExternalPaymentId(transactionResponse.getTransactionId());

        if (transactionResponse.getMode().equals("dip")) {
            progressDialog.setMessage("Please remove your card");
            progressDialog.setTitle("Transaction Complete");
            progressDialog.show();
        }

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
        alertDialog.dismiss();
        alertDialog.setMessage(errorResponse.getMessage());
        alertDialog.setTitle("Transaction Failed");
        alertDialog.show();
    }

    @Override
    public void onReaderCalibrationProgress(int progress) {
        Toast.makeText(getActivity(), "Progress Percentage:" + progress, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCardReaderDiscovered(CardReaderInfo cardReaderInfo) {
        boolean isAdded = false;
        for (CardReaderInfo readerInfo : mDetectedBTReadersList) {
            if (readerInfo.getBluetoothIdentifier().contentEquals(cardReaderInfo.getBluetoothIdentifier())) {
                isAdded = true;
                break;
            }
        }

        if (!isAdded) {
            mDetectedBTReadersList.add(cardReaderInfo);
            mDetectedBTReadersStringsList.add(cardReaderInfo.getBluetoothName());
            if (mDetectedReadersArrayAdapter != null)
                mDetectedReadersArrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPairingFailed() {
        Toast.makeText(getActivity(), "Pairing Failed", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onPairingSuccess() {
        mCloverGo.connectToBluetoothReader(mCardReaderInfo);
        Toast.makeText(getActivity(), "Pairing Succeeded", Toast.LENGTH_SHORT).show();

    }

    @Override
    public boolean proceedOnError(TransactionEvent transactionEvent) {
        return true; // return false to cancel transaction on Address Verification failure or Duplicate transaction, true to proceed with transaction
    }

    @Override
    public void onReaderResetProgress(CardReaderEvent readerEvent) {
        String data = readerEvent.getData();
        int status = Integer.valueOf(data.substring(0, data.indexOf("%")));
        resetProgressDialog.setProgress(status);
        switch (readerEvent.getCardReaderEventType()) {
            case INITIALIZATION_COMPLETE:
                android.os.Handler handler = new android.os.Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        resetProgressDialog.dismiss();
                        resetProgressDialog.setProgress(0);
                    }
                }, 500);
                break;
        }
    }

    @Override
    public void onAidMatch(final List applicationIdentifierList, final AidSelection aidSelection) {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1);
        for (ApplicationIdentifier applicationIdentifier : (List<ApplicationIdentifier>) applicationIdentifierList) {
            arrayAdapter.add(applicationIdentifier.getApplicationLabel());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setSingleChoiceItems(arrayAdapter, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                aidSelection.selectAid((ApplicationIdentifier) applicationIdentifierList.get(which));
            }
        });
        builder.setCancelable(false);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                aidSelection.selectAid(null);
            }
        });
        builder.setTitle("Please choose card");
        builder.create().show();
    }

    private MenuItem resetReaderMenu;
    private MenuItem cardReaderMenu;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getActivity().getMenuInflater().inflate(R.menu.menu, menu);
        resetReaderMenu = menu.findItem(R.id.resetReader);
        cardReaderMenu = menu.findItem(R.id.cardReaderDetails);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.resetReader:
                if (readerReady) {
                    item.setEnabled(true);
                    resetProgressDialog.setTitle("Resetting Reader");
                    resetProgressDialog.setMessage("Resetting reader please wait");
                    resetProgressDialog.setIndeterminate(false);
                    resetProgressDialog.setMax(100);
                    resetProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    resetProgressDialog.setCancelable(false);
                    resetProgressDialog.show();
                    resetProgressDialog.setProgress(0);
                    mCloverGo.resetReader();
                } else {
                    item.setEnabled(false);
                }
                break;

            case R.id.cardReaderDetails:
                if (mCardReaderInfo != null) {
                    item.setEnabled(true);
                    AlertDialog alertDialog2 = new AlertDialog.Builder(getActivity()).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                        }
                    }).setCancelable(true).create();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("CardReaderType- ").append(mCardReaderInfo.getCardReaderType()).append("\n")
                            .append("BatteryPercentage- ").append(mCardReaderInfo.getBatteryPercentage()).append("\n")
                            .append("SerialNumber- ").append(mCardReaderInfo.getSerialNumber());
                    alertDialog2.setMessage(stringBuilder.toString());
                    alertDialog2.setTitle("Card Reader Details");
                    alertDialog2.show();
                } else {
                    item.setEnabled(false);
                }
                break;
        }
        return true;
    }

//    @Override
//    public void onDeviceDiscovered(final List<CardReaderInfo> list) { /*  don;t use this method. This is for future need */
//        ArrayAdapter<String> arrayAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1);
//        for (CardReaderInfo mCardReaderInfo : list) {
//            arrayAdapter.add(mCardReaderInfo.getBluetoothName());
//        }
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setSingleChoiceItems(arrayAdapter, 0, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//
//            }
//        });
//        builder.setCancelable(false);
//        builder.setNegativeButton("Cancel", null);
//        builder.setTitle("Please choose reader");
//        builder.create().show();
//    }

    private void showBluetoothReaders() {
        showSingleSelectChoices("Choose a Bluetooth Reader", new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CardReaderInfo readerInfo = mDetectedBTReadersList.get(position);
                mCardReaderInfo = readerInfo;

                btMac = readerInfo.getBluetoothIdentifier();
                btName = readerInfo.getBluetoothName();

                mCloverGo.stopBluetoothReaderScan();

                if (readerInfo.isPaired())
                    mCloverGo.connectToBluetoothReader(readerInfo);
                else
                    mCloverGo.pairReader(readerInfo);

                Toast.makeText(getActivity(), btName + "\n" + btMac, Toast.LENGTH_SHORT).show();
                mChoiceDialog.dismiss();
            }
        }, getActivity());
    }

    public void showSingleSelectChoices(String title, AdapterView.OnItemClickListener itemClickListener, Context context) {
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

        mDetectedReadersArrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, android.R.id.text1, mDetectedBTReadersStringsList);
        mDetectedBTReadersListVw = (ListView) mChoiceDialog.findViewById(R.id.choiceDiaList);
        mDetectedBTReadersListVw.setAdapter(mDetectedReadersArrayAdapter);
        mDetectedBTReadersListVw.setOnItemClickListener(itemClickListener);
        mDetectedReadersArrayAdapter.notifyDataSetChanged();

        mChoiceDialog.show();
    }

}
