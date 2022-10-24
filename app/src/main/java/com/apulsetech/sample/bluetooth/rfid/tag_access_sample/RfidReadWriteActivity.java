package com.apulsetech.sample.bluetooth.rfid.tag_access_sample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.apulsetech.lib.event.ReaderEventListener;
import com.apulsetech.lib.rfid.Reader;
import com.apulsetech.lib.rfid.type.RFID;
import com.apulsetech.lib.rfid.type.RfidResult;
import com.apulsetech.lib.rfid.type.SelectionCriterias;
import com.apulsetech.lib.rfid.vendor.chip.impinj.EpcMatch;
import com.apulsetech.lib.util.SysUtil;
import com.apulsetech.sample.bluetooth.rfid.tag_access_sample.R;

import java.util.ArrayList;
import java.util.Locale;

public class RfidReadWriteActivity extends AppCompatActivity implements ReaderEventListener {

    private static final String TAB_READ = "Read";
    private static final String TAB_WRITE = "Write";

    private final int MAX_RETRY_COUNT_CMD_RUNNING_CHECK = 50;

    private TabHost mTabHost;

    private Context mContext;
    private Reader mReader;
    //private SoundUtil mSoundUtil;

    private TextView mReadValueTextView;
    private TextView mSelectionMaskInfoTextView;
    private TextView mSelectionMaskInventorySettingInfoTextView;
    private TextView mEpcMatchInfoTextView;
    private TextView mOperationResultTextView;

    private ImageButton mOperationSettingsDrawerButton;

    private LinearLayout mOperationSettingsDrawerLayout;
    private ScrollView mOperationSettingsScrollView;

    private Spinner mPowerGainSpinner;
    private ArrayAdapter<CharSequence> mPowerGainArrayAdapter;
    private Spinner mBankSpinner;

    private EditText mWriteValueEditText;
    private EditText mOffsetEditText;
    private EditText mLengthEditText;

    private LinearLayout mLengthLayout;

    private CheckBox mEnergizingCarrierCheckBox;

    private Button mReadButton;
    private Button mWriteButton;
    private Button mClearButton;
    private Button mSelectionMaskButton;
    private Button mAccessPasswordButton;

    private MenuItem mSettingMenuItem;

    private Animation mDrawerExpandAnimation;
    private Animation mDrawerCollapseAnimation;

    private String[] mInventorySessionStringArray;
    private String[] mInventoryTargetStringArray;
    private String[] mInventorySelectionStringArray;

    private int mBankType = RFID.Bank.EPC;
    private int mOffset = 1;
    private int mLength = 7;
    private String mValue = "";

    private boolean mReadTriggered = false;
    private boolean mSelectionMaskEnabled = false;
    private boolean mOperationSettingsExpanded = true;
    private boolean mEnergizingCarrierEnabled = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rfid_activity_read_write);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //getActionBar().setDisplayShowHomeEnabled(true);
        //getActionBar().setDisplayHomeAsUpEnabled(true);

        //mSoundUtil = new SoundUtil(this);

        mContext = this;

        mTabHost = (TabHost) findViewById(R.id.rfid_read_write_tabhost);
        mTabHost.setup();
        setupTab(new TextView(this), TAB_READ,
                R.id.rfid_read, R.string.common_label_read_memory);
        setupTab(new TextView(this), TAB_WRITE,
                R.id.rfid_write, R.string.common_label_write_memory);
        mTabHost.setOnTabChangedListener(mTabChangeListener);

        mReadValueTextView = (TextView) findViewById(R.id.rfid_read_write_textview_value);
        mWriteValueEditText = (EditText) findViewById(R.id.rfid_read_write_edittext_value);
        setStringHint(mWriteValueEditText, getString(R.string.rfid_read_write_edittext_hint_write_value));
        mWriteValueEditText.setOnFocusChangeListener(mFocusChangeListener);
        mWriteValueEditText.addTextChangedListener(mWriteValueEditTextWatcher);

        mSelectionMaskInfoTextView =
                (TextView) findViewById(R.id.rfid_read_write_textview_selection_mask_info);
        mSelectionMaskInventorySettingInfoTextView =
                (TextView) findViewById(R.id.rfid_read_write_textview_selection_mask_inventory_setting_info);
        mEpcMatchInfoTextView = (TextView) findViewById(R.id.rfid_read_write_textview_epc_match_info);

        mOperationSettingsDrawerButton = (ImageButton) findViewById(R.id.rfid_read_write_button_operation_settings_drawer);

        mOperationSettingsDrawerLayout = (LinearLayout) findViewById(R.id.rfid_read_write_layout_drawer);
        mOperationSettingsDrawerLayout.setOnClickListener(mClickListener);
        mOperationSettingsScrollView = (ScrollView) findViewById(R.id.rfid_read_write_scrollview_operation_settings);

        mBankSpinner = (Spinner) findViewById(R.id.rfid_read_write_spinner_bank);
        ArrayAdapter adapter =
                ArrayAdapter.createFromResource(this, R.array.memtype_array,
                        android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBankSpinner.setAdapter(adapter);
        mBankSpinner.setSelection(mBankType);
        mBankSpinner.setOnItemSelectedListener(mItemSelectedListener);

        mOffsetEditText = (EditText) findViewById(R.id.rfid_read_write_edittext_offset);
        mOffsetEditText.setOnFocusChangeListener(mFocusChangeListener);
        setWordSizeHint(mOffsetEditText, mOffset);
        mLengthEditText = (EditText) findViewById(R.id.rfid_read_write_edittext_length);
        mLengthEditText.setOnFocusChangeListener(mFocusChangeListener);
        setWordSizeHint(mLengthEditText, mLength);

        mLengthLayout = (LinearLayout) findViewById(R.id.rfid_read_write_layout_length);

        mPowerGainSpinner = (Spinner) findViewById(R.id.rfid_read_write_spinner_power_gain);
        mPowerGainArrayAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        for (int i = RFID.Power.MIN_POWER; i <= RFID.Power.MAX_POWER; i++) {
            mPowerGainArrayAdapter.add(String.format(Locale.US, "%.1f dBm", i / 1.0F));
        }
        mPowerGainArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPowerGainSpinner.setAdapter(mPowerGainArrayAdapter);
        mPowerGainSpinner.setOnItemSelectedListener(mItemSelectedListener);

        mEnergizingCarrierCheckBox = (CheckBox) findViewById(R.id.rfid_read_write_checkbox_energizing_carrier);
        mEnergizingCarrierCheckBox.setOnCheckedChangeListener(mCheckedChangeListener);

        mOperationResultTextView = (TextView) findViewById(R.id.rfid_read_write_op_result);

        mReadButton = (Button) findViewById(R.id.rfid_read_write_button_read);
        mReadButton.setOnClickListener(mClickListener);
        mWriteButton = (Button) findViewById(R.id.rfid_read_write_button_write);
        mWriteButton.setOnClickListener(mClickListener);
        mWriteButton.setEnabled(false);
        mClearButton = (Button) findViewById(R.id.rfid_read_write_button_clear);
        mClearButton.setOnClickListener(mClickListener);
        mSelectionMaskButton = (Button) findViewById(R.id.rfid_read_write_button_selection_mask);
        mSelectionMaskButton.setOnClickListener(mClickListener);
        mAccessPasswordButton = (Button) findViewById(R.id.rfid_read_write_button_access_password);
        mAccessPasswordButton.setOnClickListener(mClickListener);

        mInventorySessionStringArray = getResources().getStringArray(R.array.inventory_session_arrary);
        mInventoryTargetStringArray = getResources().getStringArray(R.array.inventory_target_arrary);
        mInventorySelectionStringArray = getResources().getStringArray(R.array.inventory_selection_arrary);

        mDrawerExpandAnimation = AnimationUtils.loadAnimation(this, R.anim.drawer_expand);
        mDrawerCollapseAnimation = AnimationUtils.loadAnimation(this, R.anim.drawer_collapse);
        mDrawerCollapseAnimation.setAnimationListener(mAnimationListener);
    }

    @Override
    protected void onStart() {

        super.onStart();
    }

    @Override
    protected void onRestart() {

        super.onRestart();
    }

    @Override
    protected void onResume() {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //mSoundUtil.createSoundPool(R.raw.success, R.raw.fail);
        mReader = Reader.getReader(mContext);
        if (mReader != null) {
            mReader.setEventListener(this);

            updateWidgetState();
        }
        enableWidgets(true);

        super.onResume();
    }

    @Override
    protected void onPause() {

        enableWidgets(false);

        if (mReader != null) {
            if (mReader.isOperationRunning()) {
                mReader.stopOperation();
            }
            if (mReader.getEpcMatch().getEnabled()) {
                mReader.setEpcMatch(null);

            }
            mReader.removeEventListener(this);
        }

        //mSoundUtil.deleteSoundPool();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        super.onPause();
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    private TextWatcher mWriteValueEditTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            int textLength = (charSequence != null) ? charSequence.length() : 0;
            if (textLength >= 64) {
                Toast.makeText(RfidReadWriteActivity.this, String.format(Locale.US,
                                getString(R.string.rfid_alert_write_value_should_not_longer_than_max_digits)
                                , 64),
                        Toast.LENGTH_SHORT).show();
            }
            if ((textLength == 0) || (textLength % 4) != 0) {
                mWriteButton.setEnabled(false);
            } else {
                mWriteButton.setEnabled(true);
            }

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private void setStringHint(EditText editText, String hint) {
        AbsoluteSizeSpan ass = new AbsoluteSizeSpan(14, true);
        SpannableString ss = new SpannableString(hint);
        ss.setSpan(ass, 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        editText.setHint(new SpannableString(ss));
    }

    private void setHexStringHint(EditText editText, String string) {

        if ((string == null) || (string.length() == 0)) {

            setHexStringHint(mWriteValueEditText,
                    getString(R.string.rfid_read_write_edittext_hint_write_value));
            return;
        }

        int length = string.length();
        int nSpace = (length + 1) / 2 - 1;
        String hexString = string;
        for (int i = nSpace; i > 0; i--) {
            String head = hexString.substring(0, 2 * i);
            String tail = hexString.substring(2 * i);
            hexString = head + " " + tail;
        }

        AbsoluteSizeSpan ass = new AbsoluteSizeSpan(14, true);
        SpannableString ss = new SpannableString(hexString);
        ss.setSpan(ass, 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        editText.setHint(new SpannableString(ss));
    }

    private void setHexValueText(TextView view, String value) {
        int length = value.length();
        int nSpace = (length + 1) / 2 - 1;
        String hexString = value;
        for (int i = nSpace; i > 0; i--) {
            String head = hexString.substring(0, 2 * i);
            String tail = hexString.substring(2 * i);
            hexString = head + " " + tail;
        }
        view.setText(hexString);
    }

    private void setupTab(final View view, final String tag, final int layoutId, final int stringId) {

        View tabView = createTabView(mTabHost.getContext(), getString(stringId));

        TabHost.TabSpec tabSpec = mTabHost.newTabSpec(tag).setIndicator(tabView);
        tabSpec.setContent(layoutId);

        mTabHost.addTab(tabSpec);
    }

    private static View createTabView(final Context context, final String text) {

        View view = LayoutInflater.from(context).inflate(R.layout.tab_view, null);

        TextView textView = view.findViewById(R.id.text);
        textView.setText(text);
        return view;
    }

    private TabHost.OnTabChangeListener mTabChangeListener = new TabHost.OnTabChangeListener() {
        @Override
        public void onTabChanged(String s) {

            if (s.equals(TAB_READ)) {
                mReadValueTextView.setVisibility(View.VISIBLE);
                mWriteValueEditText.setVisibility(View.GONE);
                mLengthLayout.setVisibility(View.VISIBLE);
                mReadButton.setVisibility(View.VISIBLE);
                mWriteButton.setVisibility(View.GONE);
            } else if (s.equals(TAB_WRITE)) {
                mReadValueTextView.setVisibility(View.GONE);
                mWriteValueEditText.setVisibility(View.VISIBLE);
                mLengthLayout.setVisibility(View.GONE);
                mReadButton.setVisibility(View.GONE);
                mWriteButton.setVisibility(View.VISIBLE);
            }


        }
    };

    private AdapterView.OnItemSelectedListener mItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long id) {


            switch (adapterView.getId()) {
                case R.id.rfid_read_write_spinner_bank:
                    switch (i) {
                        case RFID.Bank.RESERVED:
                        case RFID.Bank.EPC:
                        case RFID.Bank.TID:
                        case RFID.Bank.USER:
                            mBankType = i;
                            break;
                    }
                    break;

                case R.id.rfid_read_write_spinner_power_gain:
                    mReader.setRadioPower(i + RFID.Power.MIN_POWER);
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private View.OnFocusChangeListener mFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean b) {

            switch (v.getId()) {
                case R.id.rfid_read_write_edittext_value:
                    if (!b) {
                        if (mWriteValueEditText.getText().length() > 0) {
                            String valueString = mWriteValueEditText.getText().toString().toUpperCase();
                            if ((valueString.length() % 4) == 0) {
                                mValue = valueString;
                            } else {
                                Toast.makeText(RfidReadWriteActivity.this, R.string.rfid_alert_must_value_word_align,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        setHexStringHint(mWriteValueEditText, mValue);
                        mWriteValueEditText.setText(null);
                        if (mValue.length() > 0) {
                            mWriteButton.setEnabled(true);
                        } else {
                            mWriteButton.setEnabled(false);
                        }
                    }
                    break;

                case R.id.rfid_read_write_edittext_offset:
                    if (!b) {
                        if (mOffsetEditText.getText().length() > 0) {
                            mOffset = Integer.valueOf(mOffsetEditText.getText().toString());
                        }
                        setWordSizeHint(mOffsetEditText, mOffset);
                        mOffsetEditText.setText(null);
                    }
                    break;

                case R.id.rfid_read_write_edittext_length:
                    if (!b) {
                        if (mLengthEditText.getText().length() > 0) {
                            mLength = Integer.valueOf(mLengthEditText.getText().toString());
                        }
                        setWordSizeHint(mLengthEditText, mLength);
                        mLengthEditText.setText(null);
                    }
                    break;
            }

        }
    };

    private Animation.AnimationListener mAnimationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mOperationSettingsScrollView.setVisibility(View.GONE);

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };


    private void setWordSizeHint(EditText editText, int value) {

        AbsoluteSizeSpan ass = new AbsoluteSizeSpan(16, true);
        SpannableString ss = new SpannableString(String.format(Locale.US,
                "%d %s", value, getString(R.string.common_unit_word)));
        ss.setSpan(ass, 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        editText.setHint(new SpannableString(ss));

    }

    private void unfocusEditTextWidgets() {

        mOffsetEditText.clearFocus();
        mLengthEditText.clearFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mOffsetEditText.getWindowToken(), 0);
    }

    public void enableWidgets(boolean enabled) {

        String tabTag = mTabHost.getCurrentTabTag();
        if (TAB_READ.equals(tabTag)) {
            mReadButton.setEnabled(enabled);
        } else {
            mWriteButton.setEnabled(enabled);
        }
        mBankSpinner.setEnabled(enabled);
        mOffsetEditText.setEnabled(enabled);
        mLengthEditText.setEnabled(enabled);
        mPowerGainSpinner.setEnabled(enabled);
        mEnergizingCarrierCheckBox.setEnabled(enabled);
        mClearButton.setEnabled(enabled);
        mSelectionMaskButton.setEnabled(enabled);
        mAccessPasswordButton.setEnabled(enabled);

        if (mSettingMenuItem != null) {
            mSettingMenuItem.setEnabled(enabled);
        }
        mTabHost.getTabWidget().setEnabled(enabled);

    }

    private void updateWidgetState() {
        if (mReader != null) {
            SelectionCriterias selectionCriterias = mReader.getSelectionMask();
            ArrayList<SelectionCriterias.Criteria> criterias = selectionCriterias.getCriteria();
            int criteriaCount = criterias.size();

            int session = mReader.getSession();
            String inventorySessionString;
            if ((session >= RFID.Session.SESSION_S0) &&
                    (session <= RFID.Session.SESSION_S3)) {
                inventorySessionString = mInventorySessionStringArray[session];
            } else {
                inventorySessionString = getString(R.string.common_not_available);
            }

            int target = mReader.getInventorySessionTarget();
            String inventorySessionTarget;
            if ((target >= RFID.InvSessionTarget.TARGET_A) &&
                    (target <= RFID.InvSessionTarget.TARGET_B)) {
                inventorySessionTarget = mInventoryTargetStringArray[target];
            } else {
                inventorySessionTarget = getString(R.string.common_not_available);
            }

            int selection = mReader.getInventorySelectionTarget();
            String inventorySelectionTarget;
            if ((selection >= RFID.InvSelectionTarget.ALL) &&
                    (selection <= RFID.InvSelectionTarget.SELECTED)) {
                inventorySelectionTarget = mInventorySelectionStringArray[selection];
            } else {
                inventorySelectionTarget = getString(R.string.common_not_available);
            }

            mSelectionMaskEnabled = mReader.getSelectionMaskState() == RFID.ON;
            mSelectionMaskInfoTextView.setText(
                    String.format(Locale.US, getString(R.string.rfid_inventory_selection_mask_info), criteriaCount,
                            mSelectionMaskEnabled ? getString(R.string.common_enabled) :
                                    getString(R.string.common_disabled)));
            if (criteriaCount > 0) {
                mSelectionMaskInfoTextView.setTextColor(
                        getResources().getColor(R.color.color_info_text_enabled));

            } else {
                mSelectionMaskInfoTextView.setTextColor(
                        getResources().getColor(R.color.color_info_text_disabled));
            }

            mSelectionMaskInventorySettingInfoTextView.setText(
                    String.format(getString(R.string.rfid_inventory_selection_query_info),
                            inventorySessionString,
                            inventorySessionTarget,
                            inventorySelectionTarget));

            EpcMatch epcMatch = mReader.getEpcMatch();
            if ((epcMatch != null) && (epcMatch.getEnabled())) {
                String EpcMatchInfo = getString(R.string.rfid_common_epc_match_info) +
                        epcMatch.toInfoString();
                mEpcMatchInfoTextView.setText(EpcMatchInfo);
                mEpcMatchInfoTextView.setVisibility(View.VISIBLE);
            }

            int powerGain = mReader.getRadioPower();
            if ((powerGain >= RFID.Power.MIN_POWER) &&
                    (powerGain <= RFID.Power.MAX_POWER)) {
                mPowerGainSpinner.setSelection(powerGain - RFID.Power.MIN_POWER);
            }

            mEnergizingCarrierCheckBox.setChecked(mEnergizingCarrierEnabled);
        }
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            unfocusEditTextWidgets();

            int id = v.getId();
            switch (id) {
                case R.id.rfid_read_write_layout_drawer:
                    if (mOperationSettingsExpanded) {
                        mOperationSettingsDrawerButton.setImageResource(R.drawable.drawer_expand);
                        mOperationSettingsScrollView.startAnimation(mDrawerCollapseAnimation);
                        mOperationSettingsExpanded = false;
                    } else {
                        mOperationSettingsDrawerButton.setImageResource(R.drawable.drawer_collapse);
                        mOperationSettingsScrollView.setVisibility(View.VISIBLE);
                        mOperationSettingsScrollView.startAnimation(mDrawerExpandAnimation);
                        mOperationSettingsExpanded = true;
                    }
                    break;

                case R.id.rfid_read_write_button_read: {
                    enableWidgets(false);
                    clearOperationResult();

                    String accessPassword = mReader.getAccessPassword();

                    if (mEnergizingCarrierEnabled) {
                        beginSensorAccess();
                    }

                        int result = mReader.read(mBankType, mOffset,
                                mLength, accessPassword, mSelectionMaskEnabled);
                        if (result != RfidResult.SUCCESS) {
                            setOperationResult(result, false);
                            Toast.makeText(RfidReadWriteActivity.this,
                                    getString(R.string.rfid_alert_read_fail) + "(" + result + ")",
                                    Toast.LENGTH_SHORT).show();
                        //mSoundUtil.playSound(R.raw.fail);
                        enableWidgets(true);
                    }

                }
                break;

                case R.id.rfid_read_write_button_write: {
                    if (mWriteValueEditText.hasFocus()) {
                        mWriteValueEditText.clearFocus();
                    }
                    enableWidgets(false);
                    clearOperationResult();

                    String accessPassword = mReader.getAccessPassword();

                    if (mEnergizingCarrierEnabled) {
                        beginSensorAccess();
                    }

                    int result = mReader.write(mBankType, mOffset,
                            mValue, accessPassword, mSelectionMaskEnabled);

                    if (result != RfidResult.SUCCESS) {
                        setOperationResult(result, false);
                        Toast.makeText(RfidReadWriteActivity.this,
                                getString(R.string.rfid_alert_read_fail) + "(" + result + ")",
                                Toast.LENGTH_SHORT).show();
                        //mSoundUtil.playSound(R.raw.fail);
                        enableWidgets(true);
                    }
                }
                break;

                case R.id.rfid_read_write_button_clear:
                    clearEpcMatch();
                    clearReadWriteValue();
                    mWriteButton.setEnabled(false);
                    clearOperationResult();
                    break;

                case R.id.rfid_read_write_button_selection_mask:
                    Intent intent = new Intent(RfidReadWriteActivity.this, SelectMaskActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    break;

                case R.id.rfid_read_write_button_access_password:
                    showAccessPasswordDialog();
            }
        }
    };

    private CompoundButton.OnCheckedChangeListener mCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            int id = buttonView.getId();
            switch (id) {
                case R.id.rfid_read_write_checkbox_energizing_carrier:
                    mEnergizingCarrierEnabled = isChecked;
                    break;
            }

        }
    };

    private void clearOperationResult() {

        mOperationResultTextView.setText(null);
        mOperationResultTextView.setBackgroundColor(
                getResources().getColor(R.color.color_background_status_none));
    }

    private int beginSensorAccess() {

        int result;
        int retryCount = MAX_RETRY_COUNT_CMD_RUNNING_CHECK;

        do {
            SysUtil.sleep(10);
            result = mReader.Rfmicron_enableContinuousCarrier(true);
        } while ((result == RfidResult.OTHER_CMD_RUNNING_ERROR) && (--retryCount > 0));

        return result;
    }

    private int finalizeSensorAccess() {

        int result;
        int retryCount = MAX_RETRY_COUNT_CMD_RUNNING_CHECK;
        do {
            SysUtil.sleep(10);
            result = mReader.Rfmicron_enableContinuousCarrier(false);
        } while ((result == RfidResult.OTHER_CMD_RUNNING_ERROR) && (--retryCount > 0));

        return result;
    }

    private void setOperationResult(int result, boolean success) {
        setOperationResult(RfidResult.getErrorMessage(result), success);
    }

    private void setOperationResult(String message, boolean success) {

        Resources resources = getResources();
        mOperationResultTextView.setText(message);
        if (success) {
            mOperationResultTextView.setTextColor(
                    resources.getColor(R.color.color_text_status_success));
            mOperationResultTextView.setBackgroundColor(
                    resources.getColor(R.color.color_background_status_success));
        } else {
            mOperationResultTextView.setTextColor(
                    resources.getColor(R.color.color_text_status_fail));
            mOperationResultTextView.setBackgroundColor(
                    resources.getColor(R.color.color_background_status_fail));
        }
    }


    private void clearEpcMatch() {

        mReader.setEpcMatch(null);
        mEpcMatchInfoTextView.setText(null);
        mEpcMatchInfoTextView.setVisibility(View.GONE);

    }

    private void clearReadWriteValue() {

        mValue = "";
        mReadValueTextView.setText(null);
        mWriteValueEditText.setText(null);
        mWriteValueEditText.setHint(R.string.rfid_read_write_edittext_hint_write_value);

    }

    private void showAccessPasswordDialog() {
        View view = getLayoutInflater().inflate(R.layout.rfid_dialog_access_password, null);

        final EditText accessPasswordEditText =
             view.findViewById(R.id.rfid_dialog_edittext_access_password);
        accessPasswordEditText.setHint(mReader.getAccessPassword());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setTitle(R.string.rfid_common_label_access_password).
                setView(view).setNegativeButton(R.string.common_button_label_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int result = mReader.writeAccessPassword(
                                        accessPasswordEditText.getText().toString().toUpperCase(),
                                        mReader.getAccessPassword(), mSelectionMaskEnabled);
                                if (result == RfidResult.SUCCESS) {
                                    result = mReader.setAccessPassword(
                                            accessPasswordEditText.getText().toString().toUpperCase());
                                    if (result == RfidResult.SUCCESS) {
                                        Toast.makeText(mContext,
                                                R.string.common_alert_success,
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        setOperationResult(result,false);
                                        Toast.makeText(mContext,
                                                R.string.rfid_alert_failed_to_save_access_password,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    setOperationResult(result,false);
                                    Toast.makeText(mContext,
                                            R.string.rfid_alert_failed_to_write_access_password,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).
                setPositiveButton(R.string.common_button_label_save,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int result = mReader.setAccessPassword(
                                        accessPasswordEditText.getText().toString().toUpperCase());
                                if (result == RfidResult.SUCCESS) {
                                    Toast.makeText(mContext,
                                            R.string.common_alert_success,
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    setOperationResult(result,false);
                                    Toast.makeText(mContext,
                                            R.string.rfid_alert_failed_to_save_access_password,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).create();

            dialog.show();
            }


    @Override
    public void onReaderEvent(int event, int result, String data) {


        switch (event) {
            case Reader.READER_CALLBACK_EVENT_READ:
                if (mEnergizingCarrierEnabled) {
                    finalizeSensorAccess();
                }

                if (result == RfidResult.SUCCESS) {
                    setHexValueText(mReadValueTextView, data);
                    setOperationResult(getResources().getString(R.string.rfid_op_status_success),
                            true);
                    Toast.makeText(mContext,
                            R.string.common_alert_success,
                            Toast.LENGTH_SHORT).show();
                } else {
                    setOperationResult(result, false);
                    Toast.makeText(mContext,
                            getString(R.string.rfid_alert_read_fail) + " (" + result + ")",
                            Toast.LENGTH_SHORT).show();
                }
                enableWidgets(true);

                mReadTriggered = false;
                break;


            case Reader.READER_CALLBACK_EVENT_WRITE:
                if (mEnergizingCarrierEnabled) {
                    finalizeSensorAccess();
                }

                if (result == RfidResult.SUCCESS) {
                    setOperationResult(getResources().getString(R.string.rfid_op_status_success),
                            true);
                    Toast.makeText(mContext,
                            R.string.common_alert_success,
                            Toast.LENGTH_SHORT).show();
                } else {
                    setOperationResult(result,false);
                    Toast.makeText(mContext,
                            getString(R.string.rfid_alert_write_fail) + " (" + result + ")",
                            Toast.LENGTH_SHORT).show();
                }
                enableWidgets(true);
                break;

            case Reader.READER_CALLBACK_EVENT_WRITE_ACCESS_PASSWORD:
                if (result == RfidResult.SUCCESS) {
                    setOperationResult(getResources().getString(R.string.rfid_op_status_success),
                            true);
                    Toast.makeText(mContext,
                            R.string.common_alert_success,
                            Toast.LENGTH_SHORT).show();
                } else {
                    setOperationResult(result,false);
                    Toast.makeText(mContext,
                            getString(R.string.rfid_alert_failed_to_write_access_password) + " (" + result + ")",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}