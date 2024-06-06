package a.a.a;

import android.content.Context;
import android.text.Spanned;

import com.google.android.material.textfield.TextInputLayout;
import com.sketchware.remod.R;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class WB2 extends MB {
    public String[] reservedKeywords;
    public ArrayList<String> fontNames;
    public String h;
    public Pattern pattern;

    public WB2(Context context, TextInputLayout textInputLayout, String[] reservedKeywordsArr, ArrayList<String> arrayList) {
        super(context, textInputLayout);
        this.pattern = Pattern.compile("^[a-z][a-z0-9_]*");
        this.reservedKeywords = reservedKeywordsArr;
        this.fontNames = arrayList;
    }

    public WB2(Context context, TextInputLayout textInputLayout, String[] strArr, ArrayList<String> arrayList, String str) {
        super(context, textInputLayout);
        this.pattern = Pattern.compile("^[a-z][a-z0-9_]*");
        this.reservedKeywords = strArr;
        this.fontNames = arrayList;
        this.h = str;
    }

    public CharSequence filter(CharSequence charSequence, int i, int i2, Spanned spanned, int i3, int i4) {
        return null;
    }

    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        TextInputLayout textInputLayout;
        String a2;
        xB b;
        Context context;
        int i4;
        String trim = charSequence.toString().trim();
        if (trim.length() < 3) {
            this.b.setErrorEnabled(true);
            textInputLayout = this.b;
            a2 = xB.b().a(this.a, R.string.invalid_value_min_lenth, 3);
        } else if (trim.length() > 70) {
            this.b.setErrorEnabled(true);
            textInputLayout = this.b;
            a2 = xB.b().a(this.a, R.string.invalid_value_max_lenth, 70);
        } else if (trim.equals("default_image") || "NONE".equalsIgnoreCase(trim) || (!trim.equals(this.h) && (fontNames != null && this.fontNames.contains(trim)))) {
            this.b.setErrorEnabled(true);
            textInputLayout = this.b;
            a2 = xB.b().a(this.a, R.string.common_message_name_unavailable);
        } else {
            String[] strArr = this.reservedKeywords;
            int length = strArr.length;
            int i5 = 0;
            while (true) {
                if (i5 < length) {
                    if (charSequence.toString().equals(strArr[i5])) {
                        this.b.setErrorEnabled(true);
                        textInputLayout = this.b;
                        b = xB.b();
                        context = this.a;
                        i4 = R.string.logic_editor_message_reserved_keywords;
                        break;
                    }
                    i5++;
                } else if (Character.isLetter(charSequence.charAt(0))) {
                    if (this.pattern.matcher(charSequence.toString()).matches()) {
                        this.b.setErrorEnabled(false);
                        this.d = true;
                        return;
                    }
                    this.b.setErrorEnabled(true);
                    this.b.setError(xB.b().a(this.a, R.string.invalid_value_rule_4));
                    this.d = false;
                    return;
                } else {
                    this.b.setErrorEnabled(true);
                    textInputLayout = this.b;
                    b = xB.b();
                    context = this.a;
                    i4 = R.string.logic_editor_message_variable_name_must_start_letter;
                }
            }
            a2 = b.a(context, i4);
        }
        textInputLayout.setError(a2);
        this.d = false;
    }
}
