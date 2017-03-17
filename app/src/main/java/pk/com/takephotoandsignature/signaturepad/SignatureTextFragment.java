package pk.com.takephotoandsignature.signaturepad;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import pk.com.takephotoandsignature.R;
import pk.com.takephotoandsignature.TakePhotoHelper;
import pk.com.takephotoandsignature.signaturepad.views.SignaturePad;


/**
 * Created by pukai on 16/5/4.
 */
public class SignatureTextFragment extends Fragment implements SignatureContract.SignatureView {

    private TextView tipsText;
    private TextView clearText;
    private Button useSignatureLayout;
    private ImageView closeSignatureImg;
    private SignaturePad signaturePad;
    private SignatureTextPresenter mSignaturePresenter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_signature_text, container, false);

        tipsText = (TextView) view.findViewById(R.id.signatureTipsTxt);
        clearText = (TextView) view.findViewById(R.id.clearSignatureText);
        useSignatureLayout = (Button) view.findViewById(R.id.useSignatureLayout);
        closeSignatureImg = (ImageView) view.findViewById(R.id.saveSignatureImg);
        signaturePad = (SignaturePad) view.findViewById(R.id.signature);

        mSignaturePresenter = new SignatureTextPresenter(this);
        signaturePad.setMinWidth(4.0f);
        signaturePad.setMaxWidth(4.0f);
        signaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {
            @Override
            public void onStartSigning() {
                tipsText.setVisibility(View.GONE);
            }

            @Override
            public void onSigned() {
                useSignatureLayout.setEnabled(true);
                clearText.setEnabled(true);
            }

            @Override
            public void onClear() {
//                useSignatureLayout.setEnabled(false);
                clearText.setEnabled(false);
                tipsText.setVisibility(View.VISIBLE);
            }
        });
//        useSignatureLayout.setEnabled(false);
        clearText.setEnabled(false);
        clearText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignaturePresenter.clear();
            }
        });

        closeSignatureImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                getActivity().setResult(TakePhotoHelper.CANCLE_CODE, intent);
                getActivity().finish();
            }
        });

        useSignatureLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (signaturePad.isEmpty()) {
                    return;
                }
                mSignaturePresenter.saveBitmap();
            }
        });
        return view;
    }

    @Override
    public Bitmap getBitmap() {
        return signaturePad.getSignatureBitmap();
    }

    @Override
    public void clear() {
        signaturePad.clear();
    }

    @Override
    public void clearSuccess(boolean IsSuccess) {
    }

    @Override
    public void saveBitmapSuccess(boolean isSuccess, String path) {
        Intent intent = new Intent();
        intent.putExtra(TakePhotoHelper.URL_EXTRA_STR, path);
        if (isSuccess) {
            getActivity().setResult(TakePhotoHelper.SUCCESS_CODE, intent);
        } else {
            getActivity().setResult(TakePhotoHelper.FAILE_CODE, intent);
        }
        getActivity().finish();
    }

    @Override
    public void setBitmap(Bitmap bitmap, int width, int height) {
        signaturePad.setSignatureBitmap(bitmap, width, height);
    }

    @Override
    public Fragment getFragment() {
        return this;
    }

    @Override
    public Activity getMyActivity() {
        return getActivity();
    }

    @Override
    public void onDestroy() {
        signaturePad.recycleBitmap();
        signaturePad = null;
        super.onDestroy();
    }

    public static Fragment getInstance(Bundle bundle) {
        SignatureTextFragment s = new SignatureTextFragment();
        s.setArguments(bundle);
        return s;
    }
}
