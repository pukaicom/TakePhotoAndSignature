package pk.com.takephotoandsignature.signaturepad;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;

/**
 * Created by pukai on 16/5/5.
 */
public class SignatureContract {

    public interface SignatureView {
        /**
         * 获取 Bitmap
         *
         * @return 签字版当前的Bitmap
         */
        Bitmap getBitmap();

        /**
         * 清除签字版内容
         */
        void clear();

        /**
         * 清除的回调事件
         *
         * @param IsSuccess
         */
        void clearSuccess(boolean IsSuccess);

        /**
         * 保存图片的回调事件
         *
         * @param IsSuccess
         */
        void saveBitmapSuccess(boolean IsSuccess, String url);

        /**
         * 根据传递的屏幕宽高设置图片的尺寸 使得图片在不改变尺寸的情况下 尽可能的填充整个屏幕
         *
         * @param bitmap 图片
         * @param width  当前屏幕的宽
         * @param height 当前屏幕的高
         */
        void setBitmap(Bitmap bitmap, int width, int height);

        /**
         * 获取Context
         *
         * @return
         */
        Fragment getFragment();

        /**
         * 获取Context
         *
         * @return
         */
        Activity getMyActivity();
    }

    public interface SignaturePresenter {
        void clear();

        void saveBitmap();
    }
}
