package com.firstdata.clovergo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firstdata.clovergo.client.callback.SignatureCaptureCallback;
import com.firstdata.clovergo.client.internal.model.CloverGoSignatureRequest;
import com.firstdata.clovergo.client.model.ErrorResponse;
import com.firstdata.clovergo.client.model.SignatureResponse;
import com.firstdata.clovergo.client.util.CloverGo;

import java.util.ArrayList;
import java.util.List;

import domain.SampleCloverConstants;

public class SignatureActivity extends FragmentActivity implements SignatureCaptureCallback {

    private GestureOverlayView gestureOverlayView;
    private Button signatureDoneBtn;

    private ProgressDialog progressDialog;
    private AlertDialog alertDialog;

    private CloverGo cloverGo;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getActionBar().setTitle("Signature");

        gestureOverlayView = (GestureOverlayView) findViewById(R.id.signaturePad);
        gestureOverlayView.setKeepScreenOn(true);
        gestureOverlayView.setContentDescription("Sign with two fingers");

        gestureOverlayView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                signatureDoneBtn.setVisibility(View.VISIBLE);
                return false;
            }
        });

        alertDialog = new AlertDialog.Builder(this).setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString(SampleCloverConstants.BUNDLE_MAPPING.ORDER_ID.name(), orderId);
                intent.putExtras(bundle);

                setResult(RESULT_OK, intent);
                finish();

            }
        }).setCancelable(false).create();

        signatureDoneBtn = (Button) findViewById(R.id.done_button_id);
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        cloverGo = MainActivity.getCloverGo();
        final Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            orderId = bundle.getString(SampleCloverConstants.BUNDLE_MAPPING.ORDER_ID.name());
        }

        final String deviceId = cloverGo.getDeviceId();
        final String merchantId = cloverGo.getMerchantId();
        final String employeeId = cloverGo.getEmployeeId();

        signatureDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gestureOverlayView.getGesture() != null) {
                    CloverGoSignatureRequest signatureRequest = new CloverGoSignatureRequest();
                    ArrayList<CloverGoSignatureRequest.Strokes> strokesList = new ArrayList<>();
                    CloverGoSignatureRequest.Strokes strokes;

                    List<int[][]> list = new ArrayList();
                    int[][] xy;
                    int gestureCount = gestureOverlayView.getGesture().getStrokesCount();
                    int count;
                    float[] points;

                    for (int i = 0; i < gestureCount; i++) {
                        points = gestureOverlayView.getGesture().getStrokes().get(i).points;
                        strokes = signatureRequest.new Strokes();
                        count = 0;
                        xy = new int[points.length / 2][2];
                        CloverGoSignatureRequest.Points point = signatureRequest.new Points();

                        for (int j = 0; j < points.length; j += 2) {
                            xy[count][0] = (int) points[j];
                            xy[count][1] = (int) points[j + 1];
                            count++;
                        }

                        list.add(xy);
                        point.setPoints(xy);
                        strokes.setPoints(point);
                        strokesList.add(strokes);
                    }
                    signatureRequest.setStrokes(strokesList);

                    progressDialog.setTitle("Processing Signature");
                    progressDialog.setMessage("Please wait....");

                    cloverGo.setSignatureCaptureCallback(SignatureActivity.this);
                    String transactionId = "";
                    if (bundle != null) {
                        transactionId = bundle.getString(SampleCloverConstants.BUNDLE_MAPPING.TRANSACTION_ID.name());
                    }

                    signatureRequest.setTransactionId(transactionId);
                    signatureRequest.setDeviceId(deviceId);
                    signatureRequest.setMerchantId(merchantId);
                    signatureRequest.setEmployeeId(employeeId);

                    cloverGo.captureSignature(transactionId, list);
                    progressDialog.show();
                }
            }
        });
    }

    @Override
    public void onSuccess(SignatureResponse signatureResponse) {
        progressDialog.dismiss();
        alertDialog.setMessage("Approved");
        alertDialog.setTitle("Signature Status");
        alertDialog.show();
    }

    @Override
    public void onFailure(ErrorResponse errorResponse) {
        progressDialog.dismiss();
        Toast.makeText(this, "Signature not Captured", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
    }
}