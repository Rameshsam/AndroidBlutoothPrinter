package net.londatiga.android.tech;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import net.londatiga.android.tech.pockdata.PocketPos;
import net.londatiga.android.tech.util.DateUtil;
import net.londatiga.android.tech.util.FontDefine;
import net.londatiga.android.tech.util.Printer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import static net.londatiga.android.tech.util.FontDefine.FONT_32PX;

/**
 * Demo Blue Bamboo P25 Thermal Printer.
 *
 * @author Lorensius W. L. T <lorenz@londatiga.net>
 */
public class BluttoothPrinterHelper extends Activity {
    private Button mConnectBtn;
    private Button mEnableBtn;
    private Button mPrintReceiptBtn;
    private Spinner mDeviceSp;

    private ProgressDialog mProgressDlg;
    private ProgressDialog mConnectingDlg;

    private BluetoothAdapter mBluetoothAdapter;

    private P25Connector mConnector;

    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bluttoh_printer_helper);

        mConnectBtn = (Button) findViewById(R.id.btn_connect);
        mEnableBtn = (Button) findViewById(R.id.btn_enable);

        mPrintReceiptBtn = (Button) findViewById(R.id.btn_print_receipt);
        mDeviceSp = (Spinner) findViewById(R.id.sp_device);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            showUnsupported();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                showDisabled();
            } else {
                showEnabled();

                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

                if (pairedDevices != null) {
                    mDeviceList.addAll(pairedDevices);

                    updateDeviceList();
                }
            }

            mProgressDlg = new ProgressDialog(this);

            mProgressDlg.setMessage("Scanning...");
            mProgressDlg.setCancelable(false);
            mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    mBluetoothAdapter.cancelDiscovery();
                }
            });

            mConnectingDlg = new ProgressDialog(this);

            mConnectingDlg.setMessage("Connecting...");
            mConnectingDlg.setCancelable(false);

            mConnector = new P25Connector(new P25Connector.P25ConnectionListener() {

                @Override
                public void onStartConnecting() {
                    mConnectingDlg.show();
                }

                @Override
                public void onConnectionSuccess() {
                    mConnectingDlg.dismiss();

                    showConnected();
                }

                @Override
                public void onConnectionFailed(String error) {
                    mConnectingDlg.dismiss();
                }

                @Override
                public void onConnectionCancelled() {
                    mConnectingDlg.dismiss();
                }

                @Override
                public void onDisconnected() {
                    showDisonnected();
                }
            });

            //enable bluetooth
            mEnableBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                    startActivityForResult(intent, 1000);
                }
            });

            //connect/disconnect
            mConnectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    connect();
                }
            });


            //print struk
            mPrintReceiptBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    printStruk();
                }
            });
        }

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        registerReceiver(mReceiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_scan) {
            mBluetoothAdapter.startDiscovery();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }

        if (mConnector != null) {
            try {
                mConnector.disconnect();
            } catch (P25ConnectionException e) {
                e.printStackTrace();
            }
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    private String[] getArray(ArrayList<BluetoothDevice> data) {
        String[] list = new String[0];

        if (data == null) return list;

        int size = data.size();
        list = new String[size];

        for (int i = 0; i < size; i++) {
            list[i] = data.get(i).getName();
        }

        return list;
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void updateDeviceList() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item, getArray(mDeviceList));

        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);

        mDeviceSp.setAdapter(adapter);
        mDeviceSp.setSelection(0);
    }

    private void showDisabled() {
        showToast("Bluetooth disabled");

        mEnableBtn.setVisibility(View.VISIBLE);
        mConnectBtn.setVisibility(View.GONE);
        mDeviceSp.setVisibility(View.GONE);
    }

    private void showEnabled() {
        showToast("Bluetooth enabled");

        mEnableBtn.setVisibility(View.GONE);
        mConnectBtn.setVisibility(View.VISIBLE);
        mDeviceSp.setVisibility(View.VISIBLE);
    }

    private void showUnsupported() {
        showToast("Bluetooth is unsupported by this device");

        mConnectBtn.setEnabled(false);
        mPrintReceiptBtn.setEnabled(false);
        mDeviceSp.setEnabled(false);
    }

    private void showConnected() {
        showToast("Connected");

        mConnectBtn.setText("Disconnect");
        mPrintReceiptBtn.setEnabled(true);
        mDeviceSp.setEnabled(false);
    }

    private void showDisonnected() {
        showToast("Disconnected");

        mConnectBtn.setText("Connect");
        mPrintReceiptBtn.setEnabled(false);
        mDeviceSp.setEnabled(true);
    }

    private void connect() {
        if (mDeviceList == null || mDeviceList.size() == 0) {
            return;
        }

        BluetoothDevice device = mDeviceList.get(mDeviceSp.getSelectedItemPosition());

        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            try {
                createBond(device);
            } catch (Exception e) {
                showToast("Failed to pair device");

                return;
            }
        }

        try {
            if (!mConnector.isConnected()) {
                mConnector.connect(device);
            } else {
                mConnector.disconnect();

                showDisonnected();
            }
        } catch (P25ConnectionException e) {
            e.printStackTrace();
        }
    }

    private void createBond(BluetoothDevice device) throws Exception {

        try {
            Class<?> cl = Class.forName("android.bluetooth.BluetoothDevice");
            Class<?>[] par = {};

            Method method = cl.getMethod("createBond", par);

            method.invoke(device);

        } catch (Exception e) {
            e.printStackTrace();

            throw e;
        }
    }

    private void sendData(byte[] bytes) {
        try {
            mConnector.sendData(bytes);
        } catch (P25ConnectionException e) {
            e.printStackTrace();
        }
    }

    private void printStruk() {
        Date d = new Date();
        CharSequence mDate = DateFormat.format("yyyy-MM-dd hh:mm", d.getTime());

//        byte[] command = new byte[0];
//        Bitmap bmp = BitmapFactory.decodeResource(getResources(),
//                R.drawable.ic_launcher);
//        if (bmp != null) {
//            command = Utils.decodeBitmap(bmp);
//        } else {
//            Log.e("Print Photo error", "the file isn't exists");
//        }

        StringBuilder mTitleSb = new StringBuilder();
        StringBuilder retailtTaxsb = new StringBuilder();
        StringBuilder retailVlaueSb = new StringBuilder();
        StringBuilder billsb = new StringBuilder();
        StringBuilder billValuesb = new StringBuilder();
        StringBuilder tablesb = new StringBuilder();
        StringBuilder grandtotalsb = new StringBuilder();

        mTitleSb.append("Title" + "\n\n");

        retailtTaxsb.append("" + "\n\n");
        retailtTaxsb.append("------------------------------------------------" + "\n");
        retailtTaxsb.append("------------------------------------------------" + "\n\n");
        retailtTaxsb.append("RETAIL TAX INVOICE" + "\n\n");

        retailVlaueSb.append("Purchase Date     :" + mDate.toString() + " \n\n");
        retailVlaueSb.append("Mode of Purchase  : CASH" + "\n\n");
        billsb.append("------------------------------------------------" + "\n");
        billsb.append("------------------------------------------------" + "\n\n");
        billsb.append("BILL TO & SHIP TO " + " \n\n");
        billValuesb.append("PHONE               : " + "987654321" + " \n\n");
        billValuesb.append("------------------------------------------------" + "\n");
        billValuesb.append("------------------------------------------------" + "\n\n");
        for (int i = 0; i < 2; i++) {
            tablesb.append("Qty             : 1 " + " \n\n");
            tablesb.append("Total           : 100      " + " \n\n");
        }
        grandtotalsb.append("TOTAL               : 20000 " + " \n\n");
        grandtotalsb.append("Grand Total         : 20360 " + " \n\n");
        grandtotalsb.append("------------------------------------------------" + "\n");
        grandtotalsb.append("------------------------------------------------" + "\n\n");


        long milis = System.currentTimeMillis();
        String date = DateUtil.timeMilisToString(milis, "dd-MM-yy / HH:mm") + " \n\n\n";


        byte[] titlesb = Printer.printfont(mTitleSb.toString(), FONT_32PX, FontDefine.Align_CENTER,
                (byte) 0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] retailTtitleByte = Printer.printfont(retailtTaxsb.toString(), FontDefine.FONT_32PX, FontDefine.Align_CENTER,
                (byte) 0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] retailValueByte = Printer.printfont(retailVlaueSb.toString(), FontDefine.FONT_32PX, FontDefine.Align_CENTER,
                (byte) 0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] billtoByte = Printer.printfont(billsb.toString(), FontDefine.FONT_32PX, FontDefine.Align_CENTER,
                (byte) 0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] billValueByte = Printer.printfont(billValuesb.toString(), FontDefine.FONT_32PX, FontDefine.Align_CENTER,
                (byte) 0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] tableByte = Printer.printfont(tablesb.toString(), FontDefine.FONT_32PX, FontDefine.Align_LEFT,
                (byte) 0x1A, PocketPos.LANGUAGE_ENGLISH);

        byte[] grandTotalByte = Printer.printfont(grandtotalsb.toString(), FontDefine.FONT_32PX, FontDefine.Align_LEFT,
                (byte) 0x1A, PocketPos.LANGUAGE_ENGLISH);


        byte[] dateByte = Printer.printfont(date, FontDefine.FONT_24PX, FontDefine.Align_CENTER, (byte) 0x1A,
                PocketPos.LANGUAGE_ENGLISH);


        byte[] totalByte = new byte[dateByte.length];

        int offset = 0;

        //PRINTING IMAGE

//
//        System.arraycopy(command, 0, totalByte, offset, command.length);
//        offset += command.length;

        System.arraycopy(dateByte, 0, totalByte, offset, dateByte.length);

        byte[] sendData = PocketPos.FramePack(PocketPos.FRAME_TOF_PRINT, totalByte, 0, totalByte.length);
        sendData(sendData);


    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {
                    showEnabled();
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    showDisabled();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                mDeviceList = new ArrayList<BluetoothDevice>();

                mProgressDlg.show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mProgressDlg.dismiss();

                updateDeviceList();
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                mDeviceList.add(device);

                showToast("Found device " + device.getName());
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED) {
                    showToast("Paired");

                    connect();
                }
            }
        }
    };

}