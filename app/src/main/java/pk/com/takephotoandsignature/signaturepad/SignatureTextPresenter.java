package pk.com.takephotoandsignature.signaturepad;

import android.graphics.Bitmap;
import android.os.Environment;


import java.io.File;

import pk.com.takephotoandsignature.signaturepad.utils.BitmapUtil;

/**
 * Created by pukai on 16/5/5.
 */
public class SignatureTextPresenter implements SignatureContract.SignaturePresenter{
    private SignatureContract.SignatureView signatureView;
    public final static String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/asignature";

    public SignatureTextPresenter(SignatureContract.SignatureView signatureView) {
        this.signatureView = signatureView;
    }
    @Override
    public void clear() {
        signatureView.clear();
        signatureView.clearSuccess(true);
    }
    @Override
    public void saveBitmap() {
        Bitmap signatureBitmap = signatureView.getBitmap();
        if (signatureBitmap == null) {
            return;
        }
        File file =new File(dirPath);
        if(!file.exists()){
            file.mkdirs();
        }
        String path = dirPath+"/"+ String.format("%d.jpg", System.currentTimeMillis());
        if (BitmapUtil.addJpgSignatureToGallery(signatureBitmap,path)) {
            signatureView.saveBitmapSuccess(true,path);
        } else {
            signatureView.saveBitmapSuccess(false,null);
        }
    }
}
