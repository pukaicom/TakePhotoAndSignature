package pk.com.takephotoandsignature;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;


import java.io.File;

import pk.com.takephotoandsignature.signaturepad.utils.BitmapUtil;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * <p>用于非验车拍照的空的Activity</p>
 * Created by pukai on 2016-7-5.
 */
public class TakePhotoActivity extends AppCompatActivity {

    //文件路径
    public final static String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/didipic";
    private String bigimageName = "picture" + System.currentTimeMillis();//+".jpg";
    private String imagePath = "";
    private Uri mOutPutFileUri = null;
    private final static String EXTRA_RESTORE_PHOTO = "restore_photo";
    private File mRestorePhotoFile = new File(dirPath + File.separator + bigimageName);//+".jpg");
    private Bitmap bitmap;
    private int currentWidth;
    private int currentHeight;
    private Bundle bundle;
    private boolean isTakephoto = true;
    private int position;
    private int photoType;
    private int type;

    private Handler handler;

    private final static int SUCCESS_CODE = 0;
    private final static int FAILED_CODE = 1;


    private TakePhotoHelper mTakePhotoHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (currentWidth == 0 || currentHeight == 0) {
            currentWidth = BitmapUtil.FIT_SIZIE;//DeviceUtil.getScreenWidth(TakePhotoActivity.this);
            currentHeight = BitmapUtil.FIT_SIZIE;//DeviceUtil.getScreenHeight(TakePhotoActivity.this);
        }
        if (savedInstanceState != null) {
            mRestorePhotoFile = (File) savedInstanceState.getSerializable(EXTRA_RESTORE_PHOTO);
        }
        if (!isActivityReCreate()) {
            bundle = getIntent().getExtras();
            if (null != bundle) {
                currentWidth = bundle.getInt(TakePhotoHelper.WIDTH, 0);
                currentHeight = bundle.getInt(TakePhotoHelper.HEIGHT, 0);
                position = bundle.getInt(TakePhotoHelper.POSITION);
                photoType = bundle.getInt(TakePhotoHelper.PHOTO_TYPE);
                type = bundle.getInt(TakePhotoHelper.TYPE);
                if (bundle.getInt(TakePhotoHelper.TYPE) != TakePhotoHelper.TAKE_PHOTO) {
                    isTakephoto = false;
                }
            }
            mTakePhotoHelper = new TakePhotoHelper(this, dirPath, bigimageName, type);
            if (isTakephoto) {
                mRestorePhotoFile = mTakePhotoHelper.turnOnCamera();
            } else {
                mTakePhotoHelper.selectPhoto();
            }
        } else {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (handler == null) {
                        handler = new MyHandler(TakePhotoActivity.this.getMainLooper());
                    }
                    Message message = handler.obtainMessage();
                    getBitmapFromUrl(mRestorePhotoFile);
                    imagePath = dirPath + File.separator + System.currentTimeMillis();//+ ".jpg";
                    if (BitmapUtil.saveBitmapToJPG(bitmap, imagePath)) {
                        message.what = SUCCESS_CODE;
                        message.obj = imagePath;
                    } else {
                        message.what = FAILED_CODE;
                    }
                    message.sendToTarget();
                }
            }).start();
        }
    }

    private boolean isActivityReCreate() {
        return mRestorePhotoFile.exists();
        //return mRestorePhotoFile != null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mRestorePhotoFile != null) {
            outState.putSerializable(EXTRA_RESTORE_PHOTO, mRestorePhotoFile);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mRestorePhotoFile = (File) savedInstanceState.getSerializable(EXTRA_RESTORE_PHOTO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == TakePhotoHelper.RUNTIME_PERMISSION_REQUEST_CODE) {
            for (int index = 0; index < permissions.length; index++) {
                String permission = permissions[index];
                if (Manifest.permission.CAMERA.equals(permission)) {
                    if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                        mRestorePhotoFile = mTakePhotoHelper.startCamera();
                    } else {
                        mTakePhotoHelper.showMissingPermissionDialog();
                    }
                }
            }
        }
    }

    public void goBack(boolean success, String path) {
        BitmapUtil.delelteFiles(dirPath);
        Intent intent = new Intent();
        intent.putExtra(TakePhotoHelper.POSITION, position);
        intent.putExtra(TakePhotoHelper.TYPE, type);
        intent.putExtra(TakePhotoHelper.PHOTO_TYPE, photoType);
        intent.putExtra(TakePhotoHelper.URL_EXTRA_STR, path);
        if (success) {
            setResult(TakePhotoHelper.SUCCESS_CODE, intent);
        } else {
            setResult(TakePhotoHelper.FAILE_CODE, intent);
        }
        finish();

    }

    class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SUCCESS_CODE:
                    goBack(true, (String) msg.obj);
                    break;
                case FAILED_CODE:
                    goBack(false, null);
                    break;
                default:
                    goBack(false, null);
                    break;
            }
        }
    }

    ;

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        recyleBitmap();
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                int code;
                if (currentWidth == 0 || currentHeight == 0) {
                    currentWidth = BitmapUtil.FIT_SIZIE;
                    currentHeight = BitmapUtil.FIT_SIZIE;
                }
                bitmap = mTakePhotoHelper.setBitmap(currentWidth, currentHeight, requestCode, resultCode, data);
                if (null == bitmap) {
                    code = FAILED_CODE;
                } else {
                    imagePath = dirPath + File.separator + System.currentTimeMillis();// + ".jpg";
                    if (BitmapUtil.saveBitmapToJPG(bitmap, imagePath)) {
                        code = SUCCESS_CODE;
                    } else {
                        code = FAILED_CODE;
                    }
                }
                subscriber.onNext(code);
                subscriber.onCompleted();
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Integer>() {
                               @Override
                               public void onCompleted() {

                               }

                               @Override
                               public void onError(Throwable e) {
                                   if (handler == null) {
                                       handler = new MyHandler(TakePhotoActivity.this.getMainLooper());
                                   }
                                   Message message = handler.obtainMessage();
                                   message.what = FAILED_CODE;
                                   message.sendToTarget();
                               }

                               @Override
                               public void onNext(Integer integer) {
                                   if (handler == null) {
                                       handler = new MyHandler(TakePhotoActivity.this.getMainLooper());
                                   }
                                   Message message = handler.obtainMessage();
                                   message.what = integer.intValue();
                                   message.obj = imagePath;
                                   message.sendToTarget();
                               }
                           }
                );
    }


    private void getBitmapFromUrl(File file) {
        mOutPutFileUri = Uri.fromFile(file);
        try {
            bitmap = BitmapUtil.decodeBitmapFromFile(mOutPutFileUri.getPath(), currentWidth, currentHeight, true);
            if (bitmap == null) {
                //直接从相册获取照片
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mOutPutFileUri);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recyleBitmap();
        if (mTakePhotoHelper != null) {
            mTakePhotoHelper.destroy();
            mTakePhotoHelper = null;
        }
        if (handler != null) {
            handler.removeMessages(SUCCESS_CODE);
            handler.removeMessages(FAILED_CODE);
            handler = null;
        }
    }

    private void recyleBitmap() {
        if (bitmap != null) {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            bitmap = null;
        }
    }
}
