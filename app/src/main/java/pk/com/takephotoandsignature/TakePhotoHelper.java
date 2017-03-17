package pk.com.takephotoandsignature;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;


import java.io.File;
import java.util.List;

import pk.com.takephotoandsignature.signaturepad.utils.BitmapUtil;

/**
 * Created by pukai on 2016-7-5.
 */
public class TakePhotoHelper {
    /**
     * 回调图片路径的extra 的key值 在 Yours  onActivityResult中接收
     */
    public final static String URL_EXTRA_STR = "urlofimage";
    /**
     * 回调code 在 Yours  onActivityResult中接收 (0保存成功,1保存失败,2取消回退)
     */
    public final static int SUCCESS_CODE = 0;

    public final static int FAILE_CODE = 1;

    public final static int CANCLE_CODE = 2;
    /**
     * 照片的 position  在 Yours  onActivityResult中接收
     */
    public final static String POSITION = "position";
    /**
     * 照片的类型  在 Yours  onActivityResult中接收
     */
    public final static String PHOTO_TYPE = "photoType";
    /**
     * 选择照相 启动的code (除0以外的其它表示 从相册选择) 在 startActivityForResult 传递的code
     */
    public final static int TAKE_PHOTO = 0;

    /**
     * 选择从相册 启动的code 在 startActivityForResult 传递的code
     */
    public final static int SELECT_PHOTO = 1;
    /**
     * 选择照相启动的key 在 startActivityForResult 传递的code 的key值
     */
    public final static String TYPE = "typeCode";

    public final static String WIDTH = "default_width";

    public final static String HEIGHT = "default_height";

    //运行时权限申请码
    public final static int RUNTIME_PERMISSION_REQUEST_CODE = 0x1;

    private static void startActivityForResult(Fragment fragment, Intent intent, int requestCode) {
        if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode);
        }
    }

    private static void startActivityForResult(Activity activity, Intent intent, int requestCode) {
        if (activity != null) {
            activity.startActivityForResult(intent, requestCode);
        }
    }

    /**
     * 通过uri 获取 照片的实际路径 在5.0此方法有效,5.0及以上 cursor会获取不到 报空指针异常 返回null(除此以外与部分机型也有关)
     *
     * @param activity
     * @param uri
     * @return
     */
    private Activity activity;
    private String dirPath;
    private String bigimageName;
    private int code;

    public TakePhotoHelper(Activity activity, String dirPath, String bigImageName, int code) {
        this.activity = activity;
        this.dirPath = dirPath;
        this.bigimageName = bigImageName;
        this.code = code;
    }

    public String getFilePath(Uri uri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.managedQuery(uri, proj, null, null, null);
        try {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            // cursor.close();
        }
    }


    /**
     * 判断系统中是否存在可以启动的相机应用
     *
     * @return 存在返回true，不存在返回false
     */
    public boolean hasCamera() {
        PackageManager packageManager = activity.getPackageManager();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return !list.isEmpty();
    }

    /**
     * 启动相机拍照
     */
    public File startCamera() {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED) && hasCamera()) {
            Intent intent = new Intent();
            intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            BitmapUtil.ensureFileExists(dirPath);
            BitmapUtil.delelteFile(dirPath + "/" + bigimageName);
            File mRestorePhotoFile = new File(dirPath + "/" + bigimageName);
            Uri mOutPutFileUri = Uri.fromFile(mRestorePhotoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mOutPutFileUri);
            activity.startActivityForResult(intent, code);
            return mRestorePhotoFile;
        } else {
            return null;
        }
    }

    public void selectPhoto() {
        BitmapUtil.ensureFileExists(dirPath);
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(intent, code);
    }

    public Bitmap setBitmap(int width, int height, int requestCode, int resultCode, Intent data) {
        Bitmap bitmap = null;
        if (null == activity) {
            return null;
        }
        if (requestCode == TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.hasExtra("data")) {
                    bitmap = data.getParcelableExtra("data");
                }
                if (bitmap == null) {
                    File file = new File(dirPath + "/" + bigimageName);
                    Uri mOutPutFileUri = Uri.fromFile(file);
                    try {
                        bitmap = BitmapUtil.decodeBitmapFromFile(mOutPutFileUri.getPath(), width, height,true);
                        if (bitmap == null) {
                            //直接从相册获取照片
                            bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), mOutPutFileUri);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (requestCode == SELECT_PHOTO && resultCode == Activity.RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String path = getFilePath(selectedImage);
            try {
                bitmap = BitmapUtil.decodeBitmapFromFile(path == null ? selectedImage.getPath() : path, width, height,true);
                if (bitmap == null) {
                    bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), selectedImage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    /**
     * 申请写入sd卡的权限
     */
    public void requestPermission() {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, RUNTIME_PERMISSION_REQUEST_CODE);
    }

    /**
     * 显示打开权限提示的对话框
     */
    public void showMissingPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.help);
        builder.setMessage(R.string.help_content);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(activity, R.string.camera_open_error, Toast.LENGTH_SHORT).show();
                activity.finish();
            }
        });

        builder.setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                turnOnSettings();
            }
        });
        builder.show();
    }

    /**
     * 启动系统权限设置界面
     */
    public void turnOnSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
    }

    public File turnOnCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //Android M 处理Runtime Permission
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {//检查是否有写入SD卡的授权
                return startCamera();
            } else {
/*                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                }*/
                requestPermission();
            }
        } else {
            return startCamera();
        }
        return null;
    }

    public void destroy() {
        activity = null;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
