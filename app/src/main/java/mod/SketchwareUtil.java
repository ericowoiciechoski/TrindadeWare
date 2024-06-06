package mod;

import static com.besome.sketch.SketchApplication.getContext;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.sketchware.remod.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

import a.a.a.aB;
import a.a.a.bB;
import a.a.a.mB;
import mod.agus.jcoderz.lib.FileUtil;
import mod.hey.studios.util.Helper;
import mod.jbk.util.LogUtil;

public class SketchwareUtil {

    public static boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void showMessage(Context context, String _s) {
        Toast.makeText(context, _s, Toast.LENGTH_SHORT).show();
    }

    public static int getRandom(int _min, int _max) {
        return new Random().nextInt(_max - _min + 1) + _min;
    }

    public static float getDip(int input) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, input, getContext().getResources().getDisplayMetrics());
    }

    /**
     * Show a Toast styled Sketchware-like.
     *
     * @param message The message to toast
     * @param length  The toast's length, either {@link Toast#LENGTH_SHORT} or {@link Toast#LENGTH_LONG}
     */
    public static void toast(String message, int length) {
        try {
            bB.a(getContext(), message, length).show();
        } catch (RuntimeException e) {
            LogUtil.e("SketchwareUtil", "Failed to toast regular message, " +
                    "Toast's message was: \"" + message + "\"", e);
        }
    }

    /**
     * Show a Toast styled Sketchware-like and with length {@link Toast#LENGTH_SHORT}.
     *
     * @param message The message to toast
     */
    public static void toast(String message) {
        toast(message, Toast.LENGTH_SHORT);
    }

    /**
     * Show an error Toast styled Sketchware-like and with length {@link Toast#LENGTH_SHORT}.
     *
     * @param message The message to toast
     * @param length  The toast's length, either {@link Toast#LENGTH_SHORT} or {@link Toast#LENGTH_LONG}
     */
    public static void toastError(String message, int length) {
        try {
            bB.b(getContext(), message, length).show();
        } catch (RuntimeException e) {
            LogUtil.e("SketchwareUtil", "Failed to toast regular message, " +
                    "Toast's message was: \"" + message + "\"", e);
        }
    }

    /**
     * Show an error Toast styled Sketchware-like and with length {@link Toast#LENGTH_SHORT}.
     *
     * @param message The message to toast
     */
    public static void toastError(String message) {
        toastError(message, Toast.LENGTH_SHORT);
    }

    /**
     * Converts dps into pixels.
     *
     * @param dp The amount of density-independent pixels to convert
     * @return {@code dp} in pixels
     */
    public static int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    /**
     * @return An optional display name of a document picked with Storage access framework.
     */
    public static Optional<String> getSafDocumentDisplayName(Uri uri) {
        return doSingleStringContentQuery(uri, DocumentsContract.Document.COLUMN_DISPLAY_NAME);
    }

    public static Optional<String> doSingleStringContentQuery(Uri uri, String columnName) {
        try (Cursor cursor = getContext().getContentResolver().query(uri,
                new String[]{columnName}, null, null, null)) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return Optional.of(cursor.getString(0));
            } else {
                return Optional.empty();
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            LogUtil.e("SketchwareUtil", "Failed to do single string content query for Uri " + uri + " and column name " + columnName, e);
            return Optional.empty();
        }
    }

    public static void copySafDocumentToTempFile(Uri document, Activity context, String tempFileExtension, Consumer<File> tempFileConsumer, Consumer<IOException> exceptionHandler) {
        new Thread(() -> {
            try (ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(document, "r");
                 FileInputStream inputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor())) {
                File temporaryFile = File.createTempFile("document", "." + tempFileExtension);
                try (FileOutputStream outputStream = new FileOutputStream(temporaryFile);
                     BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
                    byte[] buffer = new byte[4096];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        bufferedOutputStream.write(buffer, 0, length);
                    }
                }

                context.runOnUiThread(() -> tempFileConsumer.accept(temporaryFile));
            } catch (IOException e) {
                exceptionHandler.accept(e);
            }
        }).start();
    }

    /**
     * @param componentLabel Label of component that failed to be parsed, e.g. Block selector menus
     */
    public static void showFailedToParseJsonDialog(Activity context, File json, String componentLabel, Consumer<Void> afterRenameLogic) {
        aB dialog = new aB(context);
        dialog.a(R.drawable.break_warning_96_red);
        dialog.b("Couldn't get " + componentLabel);
        dialog.a("Failed to parse " + componentLabel + " from file " + json + ". Fix by renaming old file to " + json.getName() + ".bak? " +
                "If not, no " + componentLabel + " will be used.");
        dialog.b("Rename", v -> {
            FileUtil.renameFile(json.getAbsolutePath(), json.getAbsolutePath() + ".bak");
            afterRenameLogic.accept(null);
            dialog.dismiss();
        });
        dialog.a(Helper.getResString(R.string.common_word_cancel), Helper.getDialogDismissListener(dialog));
        dialog.show();
    }

    public static void showAnErrorOccurredDialog(Activity activity, String errorMessage) {
        aB dialog = new aB(activity);
        dialog.a(R.drawable.break_warning_96_red);
        dialog.b(Helper.getResString(R.string.common_error_an_error_occurred));

        ScrollView scrollView = new ScrollView(activity);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView errorMessageThingy = new TextView(activity);
        errorMessageThingy.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        errorMessageThingy.setText(errorMessage);
        scrollView.addView(errorMessageThingy);

        dialog.a(scrollView);
        dialog.b(Helper.getResString(R.string.common_word_ok), v -> {
            if (!mB.a()) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}