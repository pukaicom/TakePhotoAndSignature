package pk.com.takephotoandsignature;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import pk.com.takephotoandsignature.signaturepad.SignatureContract;
import pk.com.takephotoandsignature.signaturepad.SignaturePhotoPresenter;
import pk.com.takephotoandsignature.signaturepad.views.SignaturePad;

/**
 * <p>从相册 或者是 照相机拍照 后获取 图片  需通过 intent传递 参数 </p>
 * Created by pukai on 16/5/4.
 */
public class SignaturePhotoActivity extends AppCompatActivity implements SignatureContract.SignatureView {
    private boolean isTakephoto = true; //判断刚开始打开app时 是否是调用照相机拍照
    //运行时权限申请码
    private final static int RUNTIME_PERMISSION_REQUEST_CODE = 0x1;

    private int position;
    private int photoType;
    private int type;

    private Button clearSignatureBtn;

    private LinearLayout closeSignature;
    private TextView isSiganatureText;
    private ImageView enableSignatureImg;
    private Button useSignatureBtn;
    private LinearLayout reTakePhonto;
    private LinearLayout enableSignature;
    private SignaturePad signaturePad;
    private LinearLayout linearLayout;
    private Bundle bundle;
    private SignaturePhotoPresenter mSignaturePresenter;

    private int width;
    private int height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_signature_photo);
        bundle = getIntent().getExtras();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (null != bundle) {
            position = bundle.getInt(TakePhotoHelper.POSITION);
            photoType = bundle.getInt(TakePhotoHelper.PHOTO_TYPE);
            type = bundle.getInt(TakePhotoHelper.TYPE);
            if (bundle.getInt(TakePhotoHelper.TYPE) != TakePhotoHelper.TAKE_PHOTO) {
                isTakephoto = false;
            }
        }
        clearSignatureBtn = (Button) findViewById(R.id.clearSignatureBtn);
        closeSignature = (LinearLayout) findViewById(R.id.closeSignature);
        enableSignature = (LinearLayout) findViewById(R.id.enableSignature);
        useSignatureBtn = (Button) findViewById(R.id.useSignatureBtn);
        reTakePhonto = (LinearLayout) findViewById(R.id.reTakePhoto);
        linearLayout = (LinearLayout) findViewById(R.id.signatureLayout);
        isSiganatureText = (TextView) findViewById(R.id.isSiganatureText);
        enableSignatureImg = (ImageView) findViewById(R.id.enableSignatureImg);
        signaturePad = new SignaturePad(this);
        mSignaturePresenter = new SignaturePhotoPresenter(this);
        mSignaturePresenter.createTakePhotoHelper(this,type);
        mSignaturePresenter.onRestoreInstanceState(savedInstanceState);
        signaturePad.setMinWidth(4.0f);
        signaturePad.setMaxWidth(4.0f);
        signaturePad.setVelocityFilterWeight(0.9f);
        signaturePad.setPenColor(Color.RED);
        signaturePad.setLayoutParams(new FrameLayout.LayoutParams(0, 0));
        linearLayout.addView(signaturePad);
        /**
         * 签字版的相关事件监听
         */
        signaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {
            @Override
            public void onStartSigning() {
            }

            @Override
            public void onSigned() {
                useSignatureBtn.setEnabled(true);
                clearSignatureBtn.setEnabled(true);
            }

            @Override
            public void onClear() {
                useSignatureBtn.setEnabled(false);
                clearSignatureBtn.setEnabled(false);
            }
        });
        /**
         * 关闭
         */
        closeSignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                setResult(TakePhotoHelper.CANCLE_CODE, intent);
                finish();
            }
        });
        /**
         * 清除
         */
        clearSignatureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignaturePresenter.clear();
            }
        });

        /**
         * 重拍
         */
        reTakePhonto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignaturePresenter.getTakePhotoHelper().setCode(TakePhotoHelper.TAKE_PHOTO);
                mSignaturePresenter.turnOnCamera();
            }
        });
        /**
         * 使用
         */
        useSignatureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSignaturePresenter.saveBitmap();
            }
        });

        enableSignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (signaturePad.isEnabled()) {
                    signaturePad.setEnabled(false);
                    enableSignatureImg.setImageResource(R.drawable.disable_signature);
                    isSiganatureText.setText(R.string.close_signature);
                    isSiganatureText.setAlpha(0.6f);
                } else {
                    signaturePad.setEnabled(true);
                    enableSignatureImg.setImageResource(R.drawable.enable_signature);
                    isSiganatureText.setAlpha(1.0f);
                    isSiganatureText.setText(R.string.open_siganture);
                }
            }
        });
        ViewTreeObserver viewTreeObserver = linearLayout.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                linearLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                width = linearLayout.getWidth();
                height = linearLayout.getHeight();
                if (mSignaturePresenter.getIsRetake()) {
                    if (isTakephoto) {
                        mSignaturePresenter.turnOnCamera();
                    } else {
                        mSignaturePresenter.selectPhoto();
                    }
                }else{
                    mSignaturePresenter.getBitmapfromStore(width,height);
                }
                return true;
            }
        });
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
        intent.putExtra(TakePhotoHelper.POSITION, position);
        intent.putExtra(TakePhotoHelper.TYPE, type);
        intent.putExtra(TakePhotoHelper.PHOTO_TYPE, photoType);

        intent.putExtra(TakePhotoHelper.URL_EXTRA_STR, path);
        if (isSuccess) {
            setResult(TakePhotoHelper.SUCCESS_CODE, intent);
        } else {
            setResult(TakePhotoHelper.FAILE_CODE, intent);
        }
        finish();
    }

    @Override
    public void setBitmap(Bitmap bitmap, int width, int height) {
        if (width != signaturePad.getWidth() || height != signaturePad.getHeight()) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) signaturePad.getLayoutParams();
            layoutParams.width = width;
            layoutParams.height = height;
            signaturePad.setLayoutParams(layoutParams);
        }
        signaturePad.setSignatureBitmap(bitmap, width, height);
    }

    @Override
    public Fragment getFragment() {
        return null;
    }

    @Override
    public Activity getMyActivity() {
        return SignaturePhotoActivity.this;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mSignaturePresenter.setBitmap(width, height, requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        signaturePad.recycleBitmap();
        signaturePad = null;
        bundle = null;
        mSignaturePresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSignaturePresenter.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RUNTIME_PERMISSION_REQUEST_CODE) {
            for (int index = 0; index < permissions.length; index++) {
                String permission = permissions[index];
                if (Manifest.permission.CAMERA.equals(permission)) {
                    if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                        mSignaturePresenter.startCamera();
                    } else {
                        mSignaturePresenter.getTakePhotoHelper().showMissingPermissionDialog();
                    }
                }
            }
        }
    }

}
