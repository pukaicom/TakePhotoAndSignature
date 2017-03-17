package pk.com.takephotoandsignature.signaturepad.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by pukai on 2016-4-30.
 */
public class BitmapUtil {

    private static final String TAG = BitmapUtil.class.getCanonicalName();
    public static final String JPG_SUFFIX = ".jpg";
    private static final String TIME_FORMAT = "yyyyMMddHHmmss";
    private static final long DELETE_ROLE = 86400000l;
    /**
     * 压缩后图片的最小边界
     **/
    public static final int FIT_SIZIE = 480;

    /**
     * 倍率浮动
     **/
    public final static float PX = 3.0f;

    public BitmapUtil() {
    }

    public static void ensureFileExists(String path) {
        File file = new File(path);
        if (!file.mkdirs()) {
            Log.v("SignaturePad", "Directory not created");
        }
    }

    public static void delelteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    public static void delelteFiles(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        File[] t = file.listFiles();
        for (int i = 0; i < t.length; i++) {
            if (System.currentTimeMillis() - t[i].lastModified() >= 86400000) {
                t[i].delete();
            }
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }

    public static boolean saveBitmapToJPG(Bitmap bitmap, String path) {
        try {
            saveBitmapToJPG(bitmap, new File(path));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void saveBitmapToJPG(Bitmap bitmap, File photo) throws Exception {
        if (bitmap == null) {
            return;
        }
        /** 当传入的指定宽高的值 大于 尺寸边界 的两倍时 需对图片再次压缩  目前验车标注的会用到**/
        if (bitmap.getHeight() >= PX * FIT_SIZIE || bitmap.getWidth() >= PX * FIT_SIZIE) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            InputStream in = new ByteArrayInputStream(baos.toByteArray());
            BitmapFactory.Options options1 = new BitmapFactory.Options();
            options1.inJustDecodeBounds = true;
            options1.outWidth = bitmap.getWidth();
            options1.outHeight = bitmap.getHeight();
            options1.inSampleSize = calculateInSampleSize(options1, FIT_SIZIE, FIT_SIZIE, false);
            options1.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeStream(in, null, options1);
        }
        FileOutputStream out = new FileOutputStream(photo);
        if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
            out.flush();
            out.close();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }
        }
    }

    public static boolean addJpgSignatureToGallery(Bitmap signature, String path) {
        boolean result = false;
        try {
            File photo = new File(path);
            saveBitmapToJPG(signature, photo);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static File saveToFile(Bitmap bitmap, File folder, String fileName) {
        if (bitmap != null) {
            if (!folder.exists()) {
                folder.mkdir();
            }

            File file = new File(folder, fileName + ".jpg");
            if (file.exists()) {
                file.delete();
            }

            try {
                file.createNewFile();
                BufferedOutputStream e = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, e);
                e.flush();
                e.close();
                return file;
            } catch (IOException var5) {
                var5.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public static int getBitmapDegree(String path) {
        short degree = 0;

        try {
            ExifInterface e = new ExifInterface(path);
            int orientation = e.getAttributeInt("Orientation", 1);
            switch (orientation) {
                case 3:
                    degree = 180;
                    break;
                case 6:
                    degree = 90;
                    break;
                case 8:
                    degree = 270;
                    break;
                default:
                    break;
            }
        } catch (IOException var4) {
            var4.printStackTrace();
        }

        return degree;
    }

    public static Bitmap decodeBitmapFromFile(File imageFile, int requestWidth, int requestHeight, boolean isDefault) {
        return imageFile != null ? decodeBitmapFromFile(imageFile.getAbsolutePath(), requestWidth, requestHeight, isDefault) : null;
    }

    public static Bitmap decodeBitmapFromFile(String imagePath, int requestWidth, int requestHeight, boolean isDefault) {
        if (TextUtils.isEmpty(imagePath)) {
            return null;
        } else {
            try {
                if (requestWidth > 0 && requestHeight > 0) {
                    BitmapFactory.Options options1 = new BitmapFactory.Options();
                    options1.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(imagePath, options1);
                    if (options1.outHeight == -1 || options1.outWidth == -1) {
                        ExifInterface e = new ExifInterface(imagePath);
                        int height = e.getAttributeInt("ImageLength", 1);
                        int width = e.getAttributeInt("ImageWidth", 1);
                        options1.outWidth = width;
                        options1.outHeight = height;
                    }
                    options1.inSampleSize = calculateInSampleSize(options1, requestWidth, requestHeight, isDefault);
                    options1.inJustDecodeBounds = false;
                    return BitmapFactory.decodeFile(imagePath, options1);
                } else {
                    Bitmap options = BitmapFactory.decodeFile(imagePath);
                    return options;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight, boolean isDefault) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            for (; halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth; inSampleSize *= 2) {
            }
            /** 如果图片的经过处理后 因为长宽比例的关系 使得长或则宽比 指定的值 的PX 倍还要大的时候 再次对图片进行压缩**/
            if (isDefault && height / inSampleSize > reqHeight * PX || width / inSampleSize > reqWidth * PX) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

}
