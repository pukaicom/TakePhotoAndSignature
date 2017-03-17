package pk.com.takephotoandsignature;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import pk.com.takephotoandsignature.signaturepad.SignatureTextFragment;


/**
 * Created by pukai on 16/5/4.
 */
public class SignatureTextActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_signature);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.signature_container, SignatureTextFragment.getInstance(bundle))
                    .commit();
    }
}
