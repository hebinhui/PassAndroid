package org.ligi.passandroid.ui.edit_fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.google.zxing.BarcodeFormat;
import java.util.Collections;
import java.util.UUID;
import org.ligi.axt.simplifications.SimpleTextWatcher;
import org.ligi.passandroid.App;
import org.ligi.passandroid.R;
import org.ligi.passandroid.helper.BarcodeHelper;
import org.ligi.passandroid.helper.Strings;
import org.ligi.passandroid.model.BarCode;
import org.ligi.passandroid.model.PassImpl;
import static android.text.TextUtils.isEmpty;

public class BarcodeEditFragment extends Fragment {

    @OnClick(R.id.scanButton)
    public void onScanButtonClick() {
        final BarCodeIntentIntegrator barCodeIntentIntegrator = new BarCodeIntentIntegrator(this);

        if (qrCheck.isChecked()) {
            barCodeIntentIntegrator.initiateScan(BarCodeIntentIntegrator.QR_CODE_TYPES);
        } else if (aztecCheck.isChecked()) {
            barCodeIntentIntegrator.initiateScan(Collections.singleton("AZTEC"));
        } else if (pdfCheck.isChecked()) {
            barCodeIntentIntegrator.initiateScan(Collections.singleton("PDF417"));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data != null && data.hasExtra("SCAN_RESULT")) {
            messageInput.setText(data.getStringExtra("SCAN_RESULT"));
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @InjectView(R.id.selectorQR)
    ImageView selectorQR;

    @InjectView(R.id.selectorPDF417)
    ImageView selectorPDF417;

    @InjectView(R.id.selectorAZTEC)
    ImageView selectorAztec;

    @InjectView(R.id.messageInput)
    EditText messageInput;

    @InjectView(R.id.alternativeMessageInput)
    EditText alternativeMessageInput;

    @InjectView(R.id.PDFCheck)
    RadioButton pdfCheck;

    @InjectView(R.id.QRCheck)
    RadioButton qrCheck;

    @InjectView(R.id.AZTecCheck)
    RadioButton aztecCheck;

    @InjectView(R.id.barcodeTypeRadioGroup)
    RadioGroup typeGroup;

    private final PassImpl pass;
    private int barcodeSize;


    public BarcodeEditFragment() {
        pass = (PassImpl) App.getPassStore().getCurrentPass();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (pass.getBarCode() != null) {
            pass.setBarCode(new BarCode(BarcodeFormat.QR_CODE, UUID.randomUUID().toString()));
        }

        final View inflate = inflater.inflate(R.layout.edit_barcode, container, false);
        ButterKnife.inject(this, inflate);

        final Display display = getActivity().getWindowManager().getDefaultDisplay();
        barcodeSize = display.getWidth() / 3;

        final BarCode barCode = pass.getBarCode();

        if (barCode!=null) {
            messageInput.setText(barCode.getMessage());
            final String alternativeTextOptional = barCode.getAlternativeText();

            alternativeMessageInput.setText(Strings.nullToEmpty(alternativeTextOptional));

            switch (barCode.getFormat()) {
                case PDF_417:
                    pdfCheck.setChecked(true);
                    break;
                case AZTEC:
                    aztecCheck.setChecked(true);
                    break;
                case QR_CODE:
                    qrCheck.setChecked(true);
                    break;
            }
        }

        refresh();

        final SimpleTextWatcher refreshingTextWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                refresh();
            }
        };

        messageInput.addTextChangedListener(refreshingTextWatcher);
        alternativeMessageInput.addTextChangedListener(refreshingTextWatcher);

        typeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final RadioGroup group, final int checkedId) {
                refresh();
            }
        });

        return inflate;
    }

    private void refresh() {

        String message = messageInput.getText().toString();
        if (isEmpty(message)) {
            message = " ";
        }

        final BarcodeFormat format = getBarcodeFormatFromCheckedState();

        final BarCode newBarCode = new BarCode(format, message);
        newBarCode.setAlternativeText(alternativeMessageInput.getText().toString());
        pass.setBarCode(newBarCode);

        new AsyncSetBarCodeImageTask(selectorQR).execute(new BarCode(BarcodeFormat.QR_CODE, message));
        new AsyncSetBarCodeImageTask(selectorPDF417).execute(new BarCode(BarcodeFormat.PDF_417, message));
        new AsyncSetBarCodeImageTask(selectorAztec).execute(new BarCode(BarcodeFormat.AZTEC, message));
    }

    private BarcodeFormat getBarcodeFormatFromCheckedState() {
        if (pdfCheck.isChecked()) {
            return BarcodeFormat.PDF_417;
        } else if (aztecCheck.isChecked()) {
            return BarcodeFormat.AZTEC;
        }
        return BarcodeFormat.QR_CODE; // default/fallback
    }


    private class AsyncSetBarCodeImageTask extends AsyncTask<BarCode, Void, Bitmap> {

        private final ImageView view;

        private AsyncSetBarCodeImageTask(ImageView view) {
            this.view = view;
        }

        @Override
        protected Bitmap doInBackground(BarCode... params) {
            return BarcodeHelper.generateBarCodeBitmap(params[0].getMessage(), params[0].getFormat(), barcodeSize);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            view.setImageBitmap(bitmap);
        }
    }

}