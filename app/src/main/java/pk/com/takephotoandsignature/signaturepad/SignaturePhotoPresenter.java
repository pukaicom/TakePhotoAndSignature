package pk.com.takephotoandsignature.signaturepad;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;

import pk.com.takephotoandsignature.TakePhotoHelper;
import pk.com.takephotoandsignature.signaturepad.utils.BitmapUtil;


/**
 * Created by pukai on 16/5/1.
 */
public class SignaturePhotoPresenter implements SignatureContract.SignaturePresenter {
    //文件路径
    public final static String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/signature";
    public final static String bigimageName = "car";
    private static String imageName = "";
    private Uri mOutPutFileUri;

    private Bitmap bitmap = null;
    private int currentWidth = 0;
    private int currentHeight = 0;
    private SignatureContract.SignatureView signatureView;
    private File mRestorePhotoFile = null;
    private boolean isShouldBeReTake = true;
    private final static String EXTRA_RESTORE_PHOTO = "extra_restore_photo";

    private TakePhotoHelper mTakePhotoHelper;


    public SignaturePhotoPresenter(SignatureContract.SignatureView signatureView) {
        this.signatureView = signatureView;
    }

    public void createTakePhotoHelper(Activity activity, int code) {
        mTakePhotoHelper = new TakePhotoHelper(activity, dirPath, bigimageName, code);
    }

    public TakePhotoHelper getTakePhotoHelper() {
        return mTakePhotoHelper;
    }

    public void setBitmap(Bitmap bitmap, int screenWidth, int screenHeight) {
        if (bitmap != null) {
  /*         2016-5-30 图片旋转功能关闭
            if (bitmap.getWidth() < bitmap.getHeight()) {
                bitmap = BitmapUtil.adjustPhotoRotation(bitmap, -90);
            }*/
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width * screenHeight > height * screenWidth) {
                height = screenWidth * height / width;
                width = screenWidth;
            } else {
                width = screenHeight * width / height;
                height = screenHeight;
            }
            signatureView.setBitmap(bitmap, width, height);
        }
    }

    public void clear() {
        signatureView.clear();
        setBitmap(bitmap, currentWidth, currentHeight);
    }

    public void saveBitmap() {
        Bitmap signatureBitmap = signatureView.getBitmap();
        if (signatureBitmap == null) {
            signatureView.saveBitmapSuccess(false, null);
            return;
        }
        imageName = System.currentTimeMillis() + "";
        String path = dirPath + "/" + imageName;
        BitmapUtil.delelteFiles(dirPath);
        if (BitmapUtil.addJpgSignatureToGallery(signatureBitmap, path)) {
            signatureView.saveBitmapSuccess(true, path);
        } else {
            signatureView.saveBitmapSuccess(false, null);
        }
    }


    public void startCamera() {
        mRestorePhotoFile = mTakePhotoHelper.startCamera();
    }

    public void selectPhoto() {
        mTakePhotoHelper.selectPhoto();
    }

    public void turnOnCamera() {
        mRestorePhotoFile = mTakePhotoHelper.turnOnCamera();
    }

    public void setBitmap(int width, int height, int requestCode, int resultCode, Intent data) {
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        Activity activity = signatureView.getMyActivity();
        if (null == activity) {
            return;
        }
        currentWidth = width;
        currentHeight = height;
        bitmap = mTakePhotoHelper.setBitmap(currentWidth, currentHeight, requestCode, resultCode, data);
        if (null == bitmap) {
            //ToastUtil.showWarnToast(activity, "未获取到图片请点击重拍按钮重新拍摄");
        } else {
            setBitmap(bitmap, currentWidth, currentHeight);
        }
    }

    public void onDestroy() {
        if (null != bitmap) {
            bitmap.recycle();
            bitmap = null;
        }
        if (mTakePhotoHelper != null) {
            mTakePhotoHelper.destroy();
            mTakePhotoHelper = null;
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if (mRestorePhotoFile != null) {
            outState.putSerializable(EXTRA_RESTORE_PHOTO, mRestorePhotoFile);
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mRestorePhotoFile = (File) savedInstanceState.getSerializable(EXTRA_RESTORE_PHOTO);
            isShouldBeReTake = false;
        }
    }

    public boolean getBitmapfromStore(int width, int height) {
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        if (width == 0 || height == 0) {
            return false;
        }
        currentWidth = width;
        currentHeight = height;
        if (signatureView.getMyActivity() == null) {
            return false;
        }
        if (mRestorePhotoFile == null) {
            return false;
        }
        mOutPutFileUri = Uri.fromFile(mRestorePhotoFile);
        try {
            bitmap = BitmapUtil.decodeBitmapFromFile(mOutPutFileUri.getPath(), currentWidth, currentHeight, false);
            if (bitmap == null) {
                //直接从相册获取照片
                bitmap = MediaStore.Images.Media.getBitmap(signatureView.getMyActivity().getContentResolver(), mOutPutFileUri);
            }
            if (null != bitmap) {
                setBitmap(bitmap, currentWidth, currentHeight);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public boolean getIsRetake() {
        return isShouldBeReTake;
    }
}
