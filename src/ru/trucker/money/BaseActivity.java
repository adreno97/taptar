package ru.trucker.money;

import android.app.Activity;
import android.os.Bundle;

public abstract class BaseActivity extends Activity {
    protected boolean themeDark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeDark = Ui.dark(this);
        if (themeDark) setTheme(android.R.style.Theme_Material);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Ui.dark(this) != themeDark) {
            recreate();
        }
    }
}
