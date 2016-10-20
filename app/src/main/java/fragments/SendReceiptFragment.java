package fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firstdata.clovergo.MainActivity;
import com.firstdata.clovergo.R;
import com.firstdata.clovergo.client.callback.SendReceiptCallBack;
import com.firstdata.clovergo.client.model.ErrorResponse;
import com.firstdata.clovergo.client.model.ReceiptResponse;
import com.firstdata.clovergo.client.util.CloverGo;

import domain.SampleCloverConstants;
import domain.Validator;

public class SendReceiptFragment extends Fragment implements SendReceiptCallBack {

    private Button mSendButton, mNoReceipt;
    private EditText mPhoneEditText;
    private EditText mEmailEditText;
    private ProgressDialog progressDialog;
    private AlertDialog alertDialog, alertDialog1;
    private String orderId;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send_receipt, container, false);
        getActivity().getActionBar().setTitle("Send Receipt ");
        alertDialog1 = new AlertDialog.Builder(getActivity()).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                startActivity(new Intent(getActivity(), MainActivity.class));
                getActivity().finish();
            }
        }).setCancelable(true).create();
        alertDialog1.setMessage("Are you sure, you want to exit?");
        alertDialog1.setTitle("Exit?");
        Bundle bundle = getArguments();
        orderId = bundle.getString(SampleCloverConstants.BUNDLE_MAPPING.ORDER_ID.name());

        mEmailEditText = (EditText) view.findViewById(R.id.sendReceiptEmailEdit);
        mPhoneEditText = (EditText) view.findViewById(R.id.sendReceiptPhoneEdit);

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        mSendButton = (Button) view.findViewById(R.id.send_confirm);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final CloverGo mCloverGo = MainActivity.getCloverGo();

                String phoneNumber = mPhoneEditText.getText().toString().replaceAll("\\D", "");
                String email = mEmailEditText.getText().toString();

                progressDialog.dismiss();
                if (Validator.validateEmailInput(email) || Validator.validatePhoneNumberInput(phoneNumber)) {
                    mCloverGo.sendReceipt(orderId, phoneNumber, email, SendReceiptFragment.this);
                    progressDialog.setMessage("Sending Receipt.....");
                    progressDialog.show();
                } else {
                    Toast.makeText(getActivity(), "Please enter a valid phone number or email", Toast.LENGTH_SHORT).show();
                }
            }
        });
        alertDialog = new AlertDialog.Builder(getActivity()).setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                final Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        }).setCancelable(false).create();


        mNoReceipt = (Button) view.findViewById(R.id.no_receipt);
        mNoReceipt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });
        return view;
    }

    @Override
    public void onSuccess(ReceiptResponse receiptResponse) {
        progressDialog.dismiss();
        alertDialog.setMessage("Receipt Sent");
        alertDialog.setTitle("Send Receipt Status");
        alertDialog.show();
    }

    @Override
    public void onFailure(ErrorResponse errorResponse) {
        progressDialog.dismiss();
        Toast.makeText(getActivity(), "Receipt not sent", Toast.LENGTH_SHORT).show();
    }

}
