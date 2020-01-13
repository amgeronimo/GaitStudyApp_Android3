//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.mbientlab.metawear.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.Build.VERSION;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;


import com.mbientlab.bletoolbox.scanner.BleScannerFragment;
import com.mbientlab.bletoolbox.scanner.R.id;
import com.mbientlab.bletoolbox.scanner.R.layout;
import com.mbientlab.bletoolbox.scanner.R.string;
//import com.mbientlab.bletoolbox.scanner.BleScannerFragment;
import com.mbientlab.bletoolbox.scanner.ScannedDeviceInfo;
import com.mbientlab.bletoolbox.scanner.BuildConfig;
import com.mbientlab.bletoolbox.scanner.MacAddressEntryDialogFragment;
import com.mbientlab.bletoolbox.scanner.ScannedDeviceInfoAdapter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

public class BleScannerFragmentAuto extends Fragment {
    public static final long DEFAULT_SCAN_PERIOD = 5000L;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;
    private ScannedDeviceInfoAdapter scannedDevicesAdapter;
    private Button scanControl;
    private Handler mHandler;
    private boolean isScanning = false;
    private BluetoothAdapter btAdapter = null;
    private HashSet<UUID> filterServiceUuids;
    private HashSet<ParcelUuid> api21FilterServiceUuids;
    private boolean isScanReady;
    private ScannerCommunicationBus commBus = null;
    private LeScanCallback deprecatedScanCallback = null;
    private ScanCallback api21ScallCallback = null;

    public BleScannerFragmentAuto() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity owner = this.getActivity();
        if (!(owner instanceof ScannerCommunicationBus)) {
            throw new ClassCastException(String.format(Locale.US, "%s %s", owner.toString(), owner.getString(string.error_scanner_listener)));
        } else {
            this.commBus = (ScannerCommunicationBus)owner;
            this.btAdapter = ((BluetoothManager)owner.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
            if (this.btAdapter == null) {
//                (new Builder(owner)).setTitle(string.dialog_title_error).setMessage(string.error_no_bluetooth_adapter).setCancelable(false).setPositiveButton(17039370, new OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        owner.finish();
//                    }
//                }).create().show();
            } else if (!this.btAdapter.isEnabled()) {
                Intent enableIntent = new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE");
                this.startActivityForResult(enableIntent, 1);
            } else {
                this.isScanReady = true;
            }


        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case 1:
                if (resultCode == 0) {
                    this.getActivity().finish();
                } else {
                    this.startBleScan();
                }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.scannedDevicesAdapter = new ScannedDeviceInfoAdapter(this.getActivity(), id.blescan_entry_layout);
        this.scannedDevicesAdapter.setNotifyOnChange(true);
        this.mHandler = new Handler();
        return inflater.inflate(layout.blescan_device_list, container);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        UUID[] filterUuids = this.commBus.getFilterServiceUuids();
        if (VERSION.SDK_INT < 21) {
            this.filterServiceUuids = new HashSet();
            if (filterUuids != null) {
                this.filterServiceUuids.addAll(Arrays.asList(filterUuids));
            }
        } else {
            this.api21FilterServiceUuids = new HashSet();
            UUID[] var4 = filterUuids;
            int var5 = filterUuids.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                UUID uuid = var4[var6];
                this.api21FilterServiceUuids.add(new ParcelUuid(uuid));
            }
        }

        ListView scannedDevices = (ListView)view.findViewById(id.blescan_devices);
        scannedDevices.setAdapter(this.scannedDevicesAdapter);
        scannedDevices.setOnItemClickListener((adapterView, view1, i, l) -> {
            BleScannerFragmentAuto.this.stopBleScan();
            BleScannerFragmentAuto.this.commBus.onDeviceSelected(((ScannedDeviceInfo)BleScannerFragmentAuto.this.scannedDevicesAdapter.getItem(i)).btDevice);
        }); //AG commented out in autoconnect version 11/13/19
        this.scanControl = (Button)view.findViewById(id.blescan_control);
        this.scanControl.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(View view) {
                if (isScanning) {
                    stopBleScan();
                } else {
                    startBleScan();
                }

            }
        });
        if (this.isScanReady) {
            this.startBleScan();
        }

    }

    public void onDestroyView() {
        this.stopBleScan();
        super.onDestroyView();
    }

    @TargetApi(22)
    public void startBleScan() {
        if (!this.checkLocationPermission()) {
            this.scanControl.setText(string.ble_scan);
        } else {
            this.scannedDevicesAdapter.clear();
            this.isScanning = true;
            this.scanControl.setText(string.ble_scan_cancel);
            this.mHandler.postDelayed(new Runnable() {
                public void run()
                {
                    stopBleScan();
                }
            }, this.commBus.getScanDuration());
            if (VERSION.SDK_INT < 21) {
                this.deprecatedScanCallback = new LeScanCallback() {
                    private void foundDevice(final BluetoothDevice btDevice, final int rssi) {
                        mHandler.post(new Runnable() {
                            public void run() {
                                scannedDevicesAdapter.update(new ScannedDeviceInfo(btDevice, rssi));
                            }
                        });
                    }

                    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
                        ByteBuffer buffer = ByteBuffer.wrap(scanRecord).order(ByteOrder.LITTLE_ENDIAN);
                        boolean stop = false;

                        while(!stop && buffer.remaining() > 2) {
                            byte length = buffer.get();
                            if (length == 0) {
                                break;
                            }

                            byte type = buffer.get();
                            switch(type) {
                                case 2:
                                case 3:
                                    for(; length >= 2; length = (byte)(length - 2)) {
                                        UUID serviceUUID = UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", buffer.getShort()));
                                        stop = filterServiceUuids.isEmpty() || filterServiceUuids.contains(serviceUUID);
                                        if (stop) {
                                            this.foundDevice(bluetoothDevice, rssi);
                                        }
                                    }
                                    break;
                                case 4:
                                case 5:
                                default:
                                    buffer.position(buffer.position() + length - 1);
                                    break;
                                case 6:
                                case 7:
                                    for(; !stop && length >= 16; length = (byte)(length - 16)) {
                                        long lsb = buffer.getLong();
                                        long msb = buffer.getLong();
                                        stop = filterServiceUuids.isEmpty() || filterServiceUuids.contains(new UUID(msb, lsb));
                                        if (stop) {
                                            this.foundDevice(bluetoothDevice, rssi);
                                        }
                                    }
                            }
                        }

                        if (!stop && filterServiceUuids.isEmpty()) {
                            this.foundDevice(bluetoothDevice, rssi);
                        }

                    }
                };
                this.btAdapter.startLeScan(this.deprecatedScanCallback);
            } else {
                this.api21ScallCallback = new ScanCallback() {
                    public void onScanResult(int callbackType, final ScanResult result) {
                        if (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
                            boolean valid = true;

                            ParcelUuid it;
                            for(Iterator var4 = result.getScanRecord().getServiceUuids().iterator(); var4.hasNext(); valid &= api21FilterServiceUuids.contains(it)) {
                                it = (ParcelUuid)var4.next();
                            }

                            if (valid) {
                                mHandler.post(new Runnable() {
                                    public void run() {
                                        scannedDevicesAdapter.update(new ScannedDeviceInfo(result.getDevice(), result.getRssi()));
//                                        stopBleScan(); //AG added 10/17/19
                                    }
                                });
                            }
                        }

                        super.onScanResult(callbackType, result);
                    }
                };
                this.btAdapter.getBluetoothLeScanner().startScan(this.api21ScallCallback);
            }

        }
    }

    public void stopBleScan() {
        if (this.isScanning) {
            if (VERSION.SDK_INT < 21) {
                this.btAdapter.stopLeScan(this.deprecatedScanCallback);
            } else {
                this.btAdapter.getBluetoothLeScanner().stopScan(this.api21ScallCallback);
            }





            this.isScanning = false;
            this.scanControl.setText(string.ble_scan);


//            if (!scannedDevicesAdapter.isEmpty())
//                    commBus.onDeviceSelected(((ScannedDeviceInfo)scannedDevicesAdapter.getItem(0)).btDevice);

        }

    }

    @TargetApi(23)
    private boolean checkLocationPermission() {
        if (VERSION.SDK_INT >= 23 && this.getActivity().checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
            Builder builder = new Builder(this.getActivity());
            builder.setTitle(string.title_request_permission);
            builder.setMessage(string.error_location_access);
//            builder.setPositiveButton(17039370, (OnClickListener)null);
            builder.setOnDismissListener(new OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{"android.permission.ACCESS_COARSE_LOCATION"}, 2);
                }
            });
            builder.show();
            return false;
        } else {
            return true;
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case 2:
                if (grantResults[0] != 0) {
                    (new MacAddressEntryDialogFragment()).show(this.getFragmentManager(), "mac_address_entry");
                } else {
                    this.isScanReady = true;
                    this.startBleScan();
                }
            default:
        }
    }

    public interface ScannerCommunicationBus {
        UUID[] getFilterServiceUuids();

        long getScanDuration();

        void onDeviceSelected(BluetoothDevice var1);
    }
}
