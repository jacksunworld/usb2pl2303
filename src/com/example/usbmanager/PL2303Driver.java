package com.example.usbmanager;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class PL2303Driver
{
  private boolean mReadPakcetChecker = false;
  public static final int BAUD0 = 0;
  public static final int BAUD75 = 75;
  public static final int BAUD150 = 150;
  public static final int BAUD300 = 300;
  public static final int BAUD600 = 600;
  public static final int BAUD1200 = 1200;
  public static final int BAUD1800 = 1800;
  public static final int BAUD2400 = 2400;
  public static final int BAUD4800 = 4800;
  public static final int BAUD9600 = 9600;
  public static final int BAUD14400 = 14400;
  public static final int BAUD19200 = 19200;
  public static final int BAUD38400 = 38400;
  public static final int BAUD57600 = 57600;
  public static final int BAUD115200 = 115200;
  public static final int BAUD230400 = 230400;
  public static final int BAUD460800 = 460800;
  public static final int BAUD614400 = 614400;
  public static final int BAUD921600 = 921600;
  public static final int BAUD1228800 = 1228800;
  public static final int BAUD2457600 = 2457600;
  public static final int BAUD3000000 = 3000000;
  public static final int BAUD6000000 = 6000000;
  private byte[] mPortSetting = new byte[7];

  private FlowControl mFlowCtrl = FlowControl.OFF;

  private int mControlLines = 0;

  private byte mStatusLines = 0;

  private int mPL2303Type = 0;
  static final int PL_SIO_SET_BITMODE_REQUEST = 11;
  static final int PL_SIO_READ_PINS_REQUEST = 12;
  private static final int SET_LINE_REQUEST_TYPE = 33;
  private static final int SET_LINE_REQUEST = 32;
  private static final int BREAK_REQUEST_TYPE = 33;
  private static final int BREAK_REQUEST = 35;
  private static final int BREAK_OFF = 0;
  private static final int GET_LINE_REQUEST_TYPE = 161;
  private static final int GET_LINE_REQUEST = 33;
  private static final int VENDOR_WRITE_REQUEST_TYPE = 64;
  private static final int VENDOR_WRITE_REQUEST = 1;
  private static final int VENDOR_READ_REQUEST_TYPE = 192;
  private static final int VENDOR_READ_REQUEST = 1;
  private static final int SET_CONTROL_REQUEST_TYPE = 33;
  private static final int SET_CONTROL_REQUEST = 34;
  private static final int CONTROL_DTR = 1;
  private static final int CONTROL_RTS = 2;
  public static final int PL_MAX_INTERFACE_NUM = 4;
  private static final String TAG = "PL2303HXDDriver";
  private final int mPacketSize = 64;
  private UsbManager mManager;
  private UsbDevice mDevice;
  private UsbDeviceConnection mDeviceConnection;
  private UsbInterface mInterface;
  private UsbEndpoint mPLEndpointBulkIN;
  private UsbEndpoint mPLEndpointBulkOUT;
  private UsbEndpoint mPLEndpointIntr;
  public static final int READBUF_SIZE = 4096;
  public static final int WRITEBUF_SIZE = 4096;
  private int mReadbufOffset;
  private int mReadbufRemain;
  byte[] mReadbuf = new byte[4096];

  private int RdTransferTimeOut = 10;
  private int WrCTRLTransferTimeOut = 10;
  private ArrayBlockingQueue<Integer> iReadQueueArray = new ArrayBlockingQueue(4096, true);
  public static Object ReadQueueLock = new Object();
  private ReadDataThread mThread;
  private boolean iThreadAlive;
  private int incReadCount = 0;
  private int totalReadCount = 0;
  private boolean updateReadCount = false;

  private boolean isRS485Mode = false;
  private final String ACTION_USB_PERMISSION;
  private int MAX_SUPPORT_DEVICE_COUNT = 4;

  private ArrayList<String> Supported_VID_PID = new ArrayList();
  private int iSupportedDevListCnt;
  Context mContext;
  private final int PL2303HXDTYPE = 4;
  private boolean bHasPermission;
  private boolean bIsPL2551BTYPE;
  private boolean bIsDisconnectShow;
  private final BroadcastReceiver mPermissionReceiver = new BroadcastReceiver()
  {
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      UsbDevice device = (UsbDevice)intent.getParcelableExtra("device");
      Log.i("PL2303HXDDriver", "Enter BroadcastReceiver" + action);

      if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action)) {
        Log.i("PL2303HXDDriver", "lib:ACTION_USB_DEVICE_ATTACHED");
      } else if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action)) {
        String deviceName = device.getDeviceName();
        if ((PL2303Driver.this.mDevice != null) && (PL2303Driver.this.mDevice.equals(deviceName))) {
          Log.d("PL2303HXDDriver", "USB interface removed");
          PL2303Driver.this.end();
          if (PL2303Driver.this.bIsDisconnectShow) {
            Toast.makeText(PL2303Driver.this.mContext, "disconnect", 0).show();
          }
        }
        Log.i("PL2303HXDDriver", "ACTION_USB_DEVICE_DETACHED");
      } else if (action.equals(PL2303Driver.this.ACTION_USB_PERMISSION)) {
        synchronized (this) {
          if (!intent.getBooleanExtra("permission", false)) {
            Log.i("PL2303HXDDriver", "Permission not granted :(");
          } else {
            if (device != null) {
              for (int i = 0; i < PL2303Driver.this.iSupportedDevListCnt; i++)
              {
                if (String.format("%04X:%04X", new Object[] { Integer.valueOf(device.getVendorId()), 
                  Integer.valueOf(device.getProductId()) }).equals(PL2303Driver.this.Supported_VID_PID.get(i))) {
                  PL2303Driver.this.getInformation(device);
                  return;
                }
              }
              Log.i("PL2303HXDDriver", String.format("%04X:%04X", new Object[] { Integer.valueOf(device.getVendorId()), 
                Integer.valueOf(device.getProductId()) }) + " device not present!");
            }
            Log.i("PL2303HXDDriver", "ACTION_USB_PERMISSION: Permission granted");
          }
        }
      }

      Log.i("PL2303HXDDriver", "Leave BroadcastReceiver");
    }
  };

  public static UsbDevice sDevice = null;

  private Runnable mLoop = new Runnable() {
    public void run() {
      UsbDevice dev = PL2303Driver.sDevice;

      if (!PL2303Driver.this.isConnected()) {
        PL2303Driver.this.setUsbInterfaces(dev);
        PL2303Driver.this.bHasPermission = true;
      }
    }
  };

  public PL2303Driver(UsbManager manager, Context mContext, String sAppName)
  {
    this.mManager = manager;
    this.mReadbufOffset = 0;
    this.mReadbufRemain = 0;
    this.bHasPermission = false;
    this.iThreadAlive = false;
    this.bIsPL2551BTYPE = false;
    this.mContext = mContext;
    this.bIsDisconnectShow = true;
    this.ACTION_USB_PERMISSION = sAppName;
    this.Supported_VID_PID.add("067B:2303");
    this.Supported_VID_PID.add("067B:2551");
    this.Supported_VID_PID.add("067B:AAA5");
    this.Supported_VID_PID.add("0557:2008");
    this.Supported_VID_PID.add("05AD:0FBA");
    this.iSupportedDevListCnt = this.Supported_VID_PID.size();
  }

  private void setUsbInterfaces(UsbDevice device)
  {
    int UARTintf = 0;

    if (this.mDeviceConnection != null) {
      if (this.mInterface != null) {
        this.mDeviceConnection.releaseInterface(this.mInterface);
        this.mInterface = null;
      }
      this.mDeviceConnection.close();
      this.mDevice = null;
      this.mDeviceConnection = null;
    }

    if (device == null) {
      return;
    }
    for (int index = 0; index < device.getInterfaceCount(); index++) {
      UsbInterface intf = device.getInterface(index);
      if ((255 == intf.getInterfaceClass()) && (intf.getInterfaceProtocol() == 0) && 
        (intf.getInterfaceSubclass() == 0)) {
        UARTintf = index;
        break;
      }
    }
    Log.d("PL2303HXDDriver", "UARTintf index = " + UARTintf);

    UsbInterface intf = device.getInterface(UARTintf);
    Log.d("PL2303HXDDriver", "Found " + intf);
    if ((device != null) && (intf != null)) {
      UsbDeviceConnection connection = this.mManager.openDevice(device);
      if (connection != null) {
        if (connection.claimInterface(intf, true)) {
          Log.d("PL2303HXDDriver", "claim interface succeeded");
          this.mDevice = device;
          this.mDeviceConnection = connection;
          this.mInterface = intf;
          if (getPLEndpoints(this.mInterface)) {
            Log.i("PL2303HXDDriver", "setPLEndpoints succeeded");
            return;
          }
          Log.i("PL2303HXDDriver", "not setPLEndpoints");
        } else {
          Log.d("PL2303HXDDriver", "claim interface failed");
          connection.close();
        }
      }
    }

    Log.i("PL2303HXDDriver", "USB interface not found");
  }

  boolean DoubleVerifyDeviceName(String strDevPath) {
    String s = "";

    boolean bResult = true;
    try {
      String cmd = "toolbox ls " + strDevPath;
      Log.d("PL2303HXDDriver", "cmd:" + cmd);
      Process p = Runtime.getRuntime().exec(cmd);
      BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line = null;
      while ((line = in.readLine()) != null) {
        s = s + line;
      }
      Log.d("PL2303HXDDriver", "verify: " + s);

      if (strDevPath.compareTo(s) != 0)
        bResult = false;
    }
    catch (IOException e) {
      e.printStackTrace();
      bResult = false;
    }
    return bResult;
  }

  public boolean Set_NewVID_PID(String vid_pid)
  {
    this.Supported_VID_PID.add(vid_pid);
    this.iSupportedDevListCnt = this.Supported_VID_PID.size();
    return true;
  }

  public boolean enumerate()
  {
      Log.i("PL2303HXDDriver", "enumerating");
      mManager = (UsbManager)mContext.getSystemService("usb");
      HashMap devlist = mManager.getDeviceList();
      Iterator deviter = devlist.values().iterator();
      PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
      while(deviter.hasNext()) 
      {
          UsbDevice d = (UsbDevice)deviter.next();
          Log.i("PL2303HXDDriver", (new StringBuilder("Found device: ")).append(String.format("%04X:%04X", new Object[] {
              Integer.valueOf(d.getVendorId()), Integer.valueOf(d.getProductId())
          })).toString());
          Log.i("PL2303HXDDriver", (new StringBuilder("iSupportedDevListCnt: ")).append(iSupportedDevListCnt).toString());
          for(int i = 0; i < iSupportedDevListCnt; i++)
              if(String.format("%04X:%04X", new Object[] {
  Integer.valueOf(d.getVendorId()), Integer.valueOf(d.getProductId())
}).equals(Supported_VID_PID.get(i)) && DoubleVerifyDeviceName(d.getDeviceName()))
              {
                  IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                  filter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
                  mContext.registerReceiver(mPermissionReceiver, filter);
                  if(!mManager.hasPermission(d))
                  {
                      mManager.requestPermission(d, pi);
                  } else
                  {
                      getInformation(d);
                      if(String.format("%04X:%04X", new Object[] {
  Integer.valueOf(d.getVendorId()), Integer.valueOf(d.getProductId())
}).equals("067B:2551"))
                          bIsPL2551BTYPE = true;
                      return true;
                  }
              }

      }
      Log.i("PL2303HXDDriver", "no more devices found");
      return false;
  }
  
  private void getInformation(UsbDevice d)
  {
    sDevice = d;
    new Thread(this.mLoop).start();
  }

  private boolean init()
  {
    if (!this.bHasPermission) {
      Log.d("PL2303HXDDriver", "has not permission to access usb device");
      return false;
    }

    if (this.mDevice == null) {
      Log.i("PL2303HXDDriver", "mDevice == null");
      return false;
    }

    int res = initPL2303Chip(this.mDeviceConnection);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to init:initPL2303Chip" + res);
      return false;
    }

//    if (this.mPL2303Type != 4) {
//      Log.d("PL2303HXDDriver", "No PL2303HXD chip");
//      return false;
//    }

    this.mThread = new ReadDataThread();

    return true;
  }

  public boolean InitByDefualtValue()
  {
    if (!init()) {
      return false;
    }

    StartReadThread();

    return true;
  }

  public boolean InitByBaudRate(BaudRate R)
  {
    if (!init()) {
      return false;
    }
    int res = 0;
    res = setup(R, DataBits.D8, StopBits.S1, Parity.NONE, FlowControl.OFF);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to InitByBaudRate" + R + "res:" + res);
      return false;
    }

    StartReadThread();

    return true;
  }

  public boolean InitByPortSetting(BaudRate R, DataBits D, StopBits S, Parity P, FlowControl F)
  {
    if (!init()) {
      return false;
    }
    int res = 0;
    res = setup(R, D, S, P, F);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to InitByPortSetting");
      return false;
    }

    StartReadThread();

    return true;
  }

  public void end()
  {
    if (this.mDevice != null)
    {
      StopReadThread();
      this.bIsPL2551BTYPE = false;
      this.mContext.unregisterReceiver(this.mPermissionReceiver);
      setUsbInterfaces(null);
    }
  }

  public boolean isConnected()
  {
    if ((this.mDevice != null) && (this.mPLEndpointBulkIN != null) && (this.mPLEndpointBulkOUT != null)) {
      return true;
    }
    return false;
  }

  private boolean getPLEndpoints(UsbInterface usbIf)
  {
    if (usbIf == null) {
      return false;
    }

    for (int i = 0; i < usbIf.getEndpointCount(); i++) {
      Log.i("PL2303HXDDriver", "EP: " + String.format("0x%02X", new Object[] { Integer.valueOf(usbIf.getEndpoint(i).getAddress()) }));
      if (usbIf.getEndpoint(i).getType() == 2) {
        Log.i("PL2303HXDDriver", "Bulk Endpoint");
        if (usbIf.getEndpoint(i).getDirection() == 128)
          this.mPLEndpointBulkIN = usbIf.getEndpoint(i);
        else
          this.mPLEndpointBulkOUT = usbIf.getEndpoint(i);
      } else if (usbIf.getEndpoint(i).getType() == 3) {
        if (usbIf.getEndpoint(i).getDirection() == 128)
          this.mPLEndpointIntr = usbIf.getEndpoint(i);
      } else {
        Log.i("PL2303HXDDriver", "Not any ep");
      }
    }

    return true;
  }

  private void isShowDisconnect(boolean bIsShow) {
    this.bIsDisconnectShow = bIsShow;
  }

  private void StartReadThread()
  {
    if (!this.iThreadAlive) {
      this.mThread.start();
      this.iThreadAlive = this.mThread.isAlive();
      Log.i("PL2303HXDDriver", "Start ReadThread:" + this.iThreadAlive);
    }
  }

  private void StopReadThread() {
    if ((this.iThreadAlive) && (this.mThread != null)) {
      this.mThread.StopReadDataThread();
      this.iThreadAlive = this.mThread.isAlive();
      Log.i("PL2303HXDDriver", "Stop ReadThread:" + this.iThreadAlive);
    }
  }

  private void SetThreadDelayTime(BaudRate R)
  {
    int[] DelayTimeLevel = { 0, 2, 2, 2, 5, 5, 5, 10, 10, 10 };
    int time = DelayTimeLevel[2];

    switch (R) {
    case B600:
    case B6000000:
    case B614400:
    case B75:
    case B921600:
    case B9600:
      time = DelayTimeLevel[0];
      break;
    case B460800:
    case B4800:
    case B57600:
      time = DelayTimeLevel[1];
      break;
    case B2457600:
    case B300:
    case B3000000:
    case B38400:
      time = DelayTimeLevel[2];
      break;
    case B230400:
    case B2400:
      time = DelayTimeLevel[3];
      break;
    case B1800:
    case B19200:
      time = DelayTimeLevel[4];
      break;
    case B150:
      time = DelayTimeLevel[5];
      break;
    case B14400:
      time = DelayTimeLevel[6];
      break;
    case B1228800:
      time = DelayTimeLevel[7];
      break;
    case B1200:
      time = DelayTimeLevel[8];
      break;
    case B115200:
      time = DelayTimeLevel[9];
      break;
    case B0:
      time = 0;
      break;
    default:
      Log.d("PL2303HXDDriver", "Baudrate not supported");
      return;
    }

    Log.i("PL2303HXDDriver", "baudrate:" + R + "; time:" + time);

    if (this.mThread != null)
      this.mThread.SetDelayTimeMS(time);
  }

  public int read(byte buf[])
  {
      int buflen = buf.length;
      if(buflen == 0)
      {
          Log.i("PL2303HXDDriver", "buf length :no data ");
          return 0;
      }
      if(buflen > 4096)
      {
          Log.i("PL2303HXDDriver", "buf length over READBUF_SIZE, re-assign buf size");
          buf = new byte[4096];
      }
      int ret;
      synchronized(ReadQueueLock)
      {
          int queuelen = iReadQueueArray.size();
          if(queuelen > 0)
          {
              Log.i("PL2303HXDDriver", (new StringBuilder("QueueCount=")).append(queuelen).toString());
              if(buflen >= queuelen)
                  ret = queuelen;
              else
                  ret = buflen;
              for(int i = 0; i < ret; i++)
              {
                  Integer mdata = (Integer)iReadQueueArray.poll();
                  if(mdata != null)
                  {
                      buf[i] = (byte)(mdata.intValue() & 255);
                      continue;
                  }
                  Log.i("PL2303HXDDriver", (new StringBuilder("this queue is empty")).append(ret).toString());
                  break;
              }

          } else
          {
              ret = 0;
          }
      }
      return ret;
  }


  private int ReadFromHW(byte[] buf, int rlength)
  {
    if ((buf.length == 0) || (rlength == 0)) {
      Log.i("PL2303HXDDriver", "buf length :no data ");
      return 0;
    }

    if ((this.mReadbufRemain > 0) && (rlength <= this.mReadbufRemain)) {
      if (!this.mReadPakcetChecker) {
        System.arraycopy(this.mReadbuf, this.mReadbufOffset, buf, 0, rlength);
      }
      else
      {
        for (int i = 0; i < rlength; i++) {
          buf[i] = this.mReadbuf[(this.mReadbufOffset++)];
          this.incReadCount += 1;

          while ((this.incReadCount - 1) % 10 != Byte.valueOf(buf[i]).byteValue() - 48) {
            Log.d("PL2303HXDDriver", "!!! Lost Data !!! count : " + (
              this.incReadCount - 1) + ", data : " + buf[i]);
            this.incReadCount += 1;
          }
        }
        Log.d("PL2303HXDDriver", "read buf length 1 : " + Integer.toString(rlength));
        this.totalReadCount += rlength;
        this.updateReadCount = true;
      }

      this.mReadbufRemain -= rlength;
      return rlength;
    }
    int ofst = 0;
    int needlen = rlength;
    if (this.mReadbufRemain > 0) {
      needlen -= this.mReadbufRemain;
      System.arraycopy(this.mReadbuf, this.mReadbufOffset, buf, ofst, this.mReadbufRemain);
    }

    int len = this.mDeviceConnection.bulkTransfer(this.mPLEndpointBulkIN, this.mReadbuf, this.mReadbuf.length, this.RdTransferTimeOut);
    if (len < 0)
      return len;
    if (len == 0) {
      return 0;
    }
    Log.i("PL2303HXDDriver", "ReadFromHW:Read Length:" + len + ";data=" + new String(this.mReadbuf).substring(0, len));

    int blocks = len / 64;
    int remain = len % 64;
    if (remain > 0) {
      blocks++;
    }

    this.mReadbufRemain = len;
    int rbufindex = 0;
    for (int block = 0; block < blocks; block++) {
      int blockofst = block * 64;

      for (int i = 0; i < 64; i++) {
        this.mReadbuf[(rbufindex++)] = this.mReadbuf[(blockofst + i)];
      }
    }

    this.mReadbufOffset = 0;
    for (; (this.mReadbufRemain > 0) && (needlen > 0); needlen--) {
      buf[(ofst++)] = this.mReadbuf[(this.mReadbufOffset++)];
      if (this.mReadPakcetChecker)
      {
        this.incReadCount += 1;
        while ((this.incReadCount - 1) % 10 != Byte.valueOf(buf[(ofst - 1)]).byteValue() - 48) {
          Log.d("PL2303HXDDriver", 
            "!!! Lost Data !!! count : " + (this.incReadCount - 1) + 
            ", data : " + 
            Byte.toString(buf[(ofst - 1)]));
          this.incReadCount += 1;
        }
      }
      this.mReadbufRemain -= 1;
    }

    if (this.mReadPakcetChecker) {
      if (ofst > 0) {
        Log.d("PL2303HXDDriver", "read buf length 2 : " + Integer.toString(ofst));
        this.totalReadCount += ofst;
        this.updateReadCount = true;
      }
      if (this.updateReadCount) {
        Log.d("PL2303HXDDriver", "Total of Read Count : " + this.totalReadCount);
        Log.d("PL2303HXDDriver", "Increment Read Count : " + this.incReadCount);
        this.updateReadCount = false;
      }

    }

    return ofst;
  }

  public int write(byte[] buf)
  {
    return write(buf, buf.length);
  }

  public int write(byte[] buf, int wlength)
  {
    int offset = 0;

    byte[] write_buf = new byte[4096];

    while (offset < wlength) {
      int write_size = 4096;

      if (offset + write_size > wlength) {
        write_size = wlength - offset;
      }

      System.arraycopy(buf, offset, write_buf, 0, write_size);
      Log.i("PL2303HXDDriver", "offset:" + offset + ",write_size:" + write_size + ",wlength:" + wlength);

      int actual_length = this.mDeviceConnection.bulkTransfer(this.mPLEndpointBulkOUT, write_buf, write_size, this.WrCTRLTransferTimeOut);
      if (actual_length < 0) {
        Log.i("PL2303HXDDriver", "fail to write:" + actual_length);
        return -1;
      }
      offset += actual_length;
    }

    Log.i("PL2303HXDDriver", "Write Length:" + offset + ";" + new String(write_buf).substring(0, offset));
    return offset;
  }

  public int setup(BaudRate R, DataBits D, StopBits S, Parity P, FlowControl F)
  {
    int res = 0;

	  res = this.mDeviceConnection.controlTransfer(161, 33, 0, 0, this.mPortSetting, 7, this.WrCTRLTransferTimeOut);
	    if (res < 0) {
	      Log.d("PL2303HXDDriver", "fail to setup:get line request");
	      return res;
	    }
	    
	    
	    int baud = 0;
	    switch (R) {
	    case B0:
	      baud = 0; break;
	    case B115200:
	      baud = 115200; break;
	    case B1200:
	      baud = 1200; break;
	    case B1228800:
	      baud = 1228800; break;
	    case B14400:
	      baud = 14400; break;
	    case B150:
	      baud = 150; break;
	    case B1800:
	      baud = 1800; break;
	    case B19200:
	      baud = 19200; break;
	    case B230400:
	      baud = 230400; break;
	    case B2400:
	      baud = 2400; break;
	    case B2457600:
	      baud = 2457600; break;
	    case B300:
	      baud = 300; break;
	    case B3000000:
	      baud = 3000000; break;
	    case B38400:
	      baud = 38400; break;
	    case B460800:
	      baud = 460800; break;
	    case B4800:
	      baud = 4800; break;
	    case B57600:
	      baud = 57600; break;
	    case B600:
	      baud = 600; break;
	    case B6000000:
	      baud = 6000000; break;
	    case B614400:
	      baud = 614400; break;
	    case B75:
	      baud = 75; break;
	    case B921600:
	      baud = 921600; break;
	    case B9600:
	      baud = 9600; break;
	    default:
	      Log.d("PL2303HXDDriver", "Baudrate not supported");
	      return -2;
	    }
	    
	    Log.d("PL2303HXDDriver", "setup:" + baud);

	    this.mPortSetting[0] = ((byte)(baud & 0xFF));
	    this.mPortSetting[1] = ((byte)(baud >> 8 & 0xFF));
	    this.mPortSetting[2] = ((byte)(baud >> 16 & 0xFF));
	    this.mPortSetting[3] = ((byte)(baud >> 24 & 0xFF));

	    switch (S) { 
	    case S1:
	      this.mPortSetting[4] = 0; break;
	    case S2:
	      this.mPortSetting[4] = 1; break;
	    default:
	      Log.d("PL2303HXDDriver", "Stopbit setting not supported");
	      return -3;
	    }

	    switch (P) { 
	    case NONE:
	      this.mPortSetting[5] = 0; break;
	    case ODD:
	      this.mPortSetting[5] = 1; break;
	    case EVEN:
	      this.mPortSetting[5] = 2; break;
	    default:
	      Log.d("PL2303HXDDriver", "Parity setting not supported");
	      return -4;
	    }

	    switch (D) { 
	    case D5:
	      this.mPortSetting[6] = 5; break;
	    case D6:
	      this.mPortSetting[6] = 6; break;
	    case D7:
	      this.mPortSetting[6] = 7; break;
	    case D8:
	      this.mPortSetting[6] = 8; break;
	    default:
	      Log.d("PL2303HXDDriver", "Databit setting not supported");
	      return -5;
	    }

	    res = this.mDeviceConnection.controlTransfer(33, 32, 0, 0, this.mPortSetting, 7, this.WrCTRLTransferTimeOut);
	    if (res < 0) {
	      Log.e("PL2303HXDDriver", "Error in setting serial configuration");
	      return res;
	    }   
	    
	return 0;
  }

  private int initPL2303Chip(UsbDeviceConnection conn)
  {
    int res = 0;
    this.isRS485Mode = false;
//    if (this.bIsPL2551BTYPE) {
//      this.mPL2303Type = 4;
//    }
//    else {
//      if (conn.getRawDescriptors()[13] == 4) this.mPL2303Type = 4;
//
//      if ((res = checkPL2303ChipType(conn)) < 0) {
//        return res;
//      }
//      if ((res = checkRS485Mode(conn)) < 0) {
//        return res;
//      }
//    }
//    if (this.mPL2303Type != 4) return -1;

    byte[] buffer = new byte[1];

    res = conn.controlTransfer(192, 1, 33924, 0, buffer, 1, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
      return res;
    }
    res = conn.controlTransfer(64, 1, 1028, 0, null, 0, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
      return res;
    }
    res = conn.controlTransfer(192, 1, 33924, 0, buffer, 1, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
      return res;
    }
    res = conn.controlTransfer(192, 1, 33667, 0, buffer, 1, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
      return res;
    }

    res = conn.controlTransfer(192, 1, 33924, 0, buffer, 1, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
      return res;
    }
    res = conn.controlTransfer(64, 1, 1028, 1, null, 0, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
      return res;
    }
    res = conn.controlTransfer(192, 1, 33924, 0, buffer, 1, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
      return res;
    }
    res = conn.controlTransfer(192, 1, 33667, 0, buffer, 1, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
      return res;
    }

    if (this.isRS485Mode) {
      res = conn.controlTransfer(64, 1, 0, 49, null, 0, this.WrCTRLTransferTimeOut);
      if (res < 0) {
        Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
        return res;
      }
      res = conn.controlTransfer(64, 1, 1, 8, null, 0, this.WrCTRLTransferTimeOut);
      if (res < 0) {
        Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
        return res;
      }
      Log.d("PL2303HXDDriver", "RS485 Mode detected");
    }
    else {
      res = conn.controlTransfer(64, 1, 0, 1, null, 0, this.WrCTRLTransferTimeOut);
      if (res < 0) {
        Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
        return res;
      }
      res = conn.controlTransfer(64, 1, 1, 0, null, 0, this.WrCTRLTransferTimeOut);
      if (res < 0) {
        Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
        return res;
      }
      Log.d("PL2303HXDDriver", "RS232 Mode detected");
    }

    res = conn.controlTransfer(64, 1, 2, 68, null, 0, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
      return res;
    }

      res = setup(BaudRate.B9600, DataBits.D8, StopBits.S1, Parity.NONE, FlowControl.OFF);


    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to initPL2303Chip: setup");
      return res;
    }

    return 0;
  }

  private int checkPL2303ChipType(UsbDeviceConnection conn)
  {
    int res = 0;
    byte[] buffer = new byte[1];

    res = conn.controlTransfer(64, 1, 1, 255, null, 0, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to checkPL2303ChipType");
      return res;
    }
    res = conn.controlTransfer(192, 1, 33153, 0, buffer, 1, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to checkPL2303ChipType");
      return res;
    }
    return 0;
  }

  private int checkRS485Mode(UsbDeviceConnection conn)
  {
    int readAddress = 9; int res = 0;
    byte[] buffer = new byte[1];

    res = conn.controlTransfer(192, 1, 33924, 0, buffer, 1, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to CheckRS485Mode:read");
      return res;
    }
    res = conn.controlTransfer(64, 1, 1028, readAddress, null, 0, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to CheckRS485Mode:write");
      return res;
    }
    res = conn.controlTransfer(192, 1, 33924, 0, buffer, 1, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to CheckRS485Mode:read");
      return res;
    }
    res = conn.controlTransfer(192, 1, 33667, 0, buffer, 1, this.WrCTRLTransferTimeOut);
    if (res < 0) {
      Log.d("PL2303HXDDriver", "fail to CheckRS485Mode:read");
      return res;
    }

    if (buffer[0] == 8) this.isRS485Mode = true;
    return 0;
  }

  public static enum BaudRate
  {
    B0, 
    B75, 
    B150, 
    B300, 
    B600, 
    B1200, 
    B1800, 
    B2400, 
    B4800, 
    B9600, 
    B14400, 
    B19200, 
    B38400, 
    B57600, 
    B115200, 
    B230400, 
    B460800, 
    B614400, 
    B921600, 
    B1228800, 
    B2457600, 
    B3000000, 
    B6000000;
  }

  public static enum DataBits {
    D5, 
    D6, 
    D7, 
    D8;
  }

  public static enum FlowControl
  {
    OFF, 
    RTSCTS, 
    RFRCTS, 
    DTRDSR, 
    XONXOFF;
  }

  public static enum Parity
  {
    NONE, 
    ODD, 
    EVEN;
  }

  class ReadDataThread extends Thread
  {
    private int iReadCnt;
    private int iQueueCount;
    private boolean ret = true; private boolean bStop = false;

    private AtomicInteger iDelayTimeMS = new AtomicInteger(20);

    ReadDataThread() 
    {  
    	
    } 
    public void ReadDataThead()
    { 
    	this.iQueueCount = 0;
      this.iReadCnt = 0;
      PL2303Driver.this.iReadQueueArray.clear();
      }

    public void ReadDataThead(int mTimeMS)
    {
      ReadDataThead();
      SetDelayTimeMS(mTimeMS);
    }

    public void SetDelayTimeMS(int mTimeMS)
    {
      this.iDelayTimeMS.set(mTimeMS);
    }

    public void StopReadDataThread() {
      this.bStop = true;

      while (isAlive());
      PL2303Driver.this.iReadQueueArray.clear();
    }

    private void DelayTime(int dwTimeMS)
    {
      if (dwTimeMS == 0) {
        return;
      }long StartTime = System.currentTimeMillis();
      long CheckTime;
      do { CheckTime = System.currentTimeMillis();
        Thread.yield();
      }
      while (
        CheckTime - StartTime <= dwTimeMS);
    }

    public void run() {
      try {
        byte[] rbuf = new byte[4096];

        while (!this.bStop) {
          this.iReadCnt = PL2303Driver.this.ReadFromHW(rbuf, rbuf.length);        
          if (this.iReadCnt > 0)
          {   
        	  PL2303Driver.this.write(rbuf,iReadCnt);   
            synchronized (PL2303Driver.ReadQueueLock) {
              this.iQueueCount = PL2303Driver.this.iReadQueueArray.size();

              if (4096 == this.iQueueCount) {
                Log.i("PL2303HXDDriver", "Queue is full");
              } 
              else 
              {
            	  
                int i = 0;
                do
                {
                  this.ret = PL2303Driver.this.iReadQueueArray.offer(Integer.valueOf(rbuf[i]));
                  if (!this.ret) {
                    Log.i("PL2303HXDDriver", "Queue is full");
                    break;
                  }
                  this.iQueueCount = PL2303Driver.this.iReadQueueArray.size();

                  i++; 
                  if (i >= this.iReadCnt) 
                	  break; 
                 } while (this.iQueueCount < 4096);            
              }
            }  
          
          }
//          int len;
//          byte[] rbuf1 = new byte[4096];
//          len = PL2303Driver.this.read(rbuf1);
//          if (len>0)
//        	  PL2303Driver.this.write(rbuf1,len); 
          int time = this.iDelayTimeMS.get();
          DelayTime(time);
        }
      }
      catch (Exception e) {
        Log.i("PL2303HXDDriver", "error: " + e.getMessage());
      }
    }
  }

  public static enum StopBits
  {
    S1, 
    S2;
  }
}