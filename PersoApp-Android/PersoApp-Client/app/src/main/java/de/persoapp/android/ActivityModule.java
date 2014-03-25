package de.persoapp.android;

import android.app.Activity;

import net.vrallev.android.base.BaseActivityModule;
import net.vrallev.android.base.BaseActivitySupport;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.persoapp.android.activity.CommonChangePinActivity;
import de.persoapp.android.activity.AuthenticateActivity;
import de.persoapp.android.activity.MainActivity;
import de.persoapp.android.activity.PinOptionsActivity;
import de.persoapp.android.activity.dialog.CanDialog;
import de.persoapp.android.activity.dialog.QuestionDialog;
import de.persoapp.android.activity.fragment.CommonChangePinFragment;
import de.persoapp.android.activity.fragment.AuthenticateFragment;
import de.persoapp.android.activity.fragment.ConfirmPinFragment;
import de.persoapp.android.activity.fragment.DeviceNotNpaCapableFragment;
import de.persoapp.android.activity.fragment.InitializeAppFragment;
import de.persoapp.android.activity.fragment.NewPinFragment;
import de.persoapp.android.nfc.NpaTester;
import de.persoapp.android.view.MenuHelper;
import de.persoapp.android.view.PinRow;

/**
 * @author Ralf Wondratschek
 */
@SuppressWarnings("UnusedDeclaration")
@Module(
        addsTo = AppModule.class,
        includes = {
                BaseActivityModule.class
        },
        injects = {
                MainActivity.class,
                AuthenticateActivity.class,
                PinOptionsActivity.class,
                CommonChangePinActivity.class,

                NewPinFragment.class,
                ConfirmPinFragment.class,
                QuestionDialog.class,
                CanDialog.class,
                AuthenticateFragment.class,
                InitializeAppFragment.class,
                CommonChangePinFragment.class,
                DeviceNotNpaCapableFragment.class,

                PinRow.class,
                NpaTester.class
        }
)
public class ActivityModule {

    @Provides
    @Singleton
    MenuHelper provideMenuHelper(Activity activity) {
        return new MenuHelper((BaseActivitySupport) activity);
    }

    @Provides
    @Singleton
    NpaTester provideNpaTester(Activity activity) {
        return new NpaTester((BaseActivitySupport) activity);
    }
}