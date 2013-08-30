package com.example.usbmanager;
         
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.example.usbmanager.PL2303Driver.BaudRate;
import com.example.usbmanager.PL2303Driver.DataBits;
import com.example.usbmanager.PL2303Driver.FlowControl;
import com.example.usbmanager.PL2303Driver.Parity;
import com.example.usbmanager.PL2303Driver.StopBits;

import android.os.Bundle;
import android.R.string;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
         
public class MainActivity extends Activity {
	
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
	
    private static final String TAG = "MainActivity";   //记录标识
    private Button btsend;      //发送按钮
    private UsbManager manager;   //USB管理器
    private UsbDevice mUsbDevice;  //找到的USB设备
    private ListView lsv1;         //显示USB信息的
    private UsbInterface mInterface;   
    private UsbDeviceConnection mDeviceConnection;
    
    public static final int READBUF_SIZE = 4096;
    public static final int WRITEBUF_SIZE = 4096;
    private int mReadbufOffset;
    private int mReadbufRemain;
    byte[] mReadbuf = new byte[4096];

    private int RdTransferTimeOut = 5000;
    private int WrCTRLTransferTimeOut = 100;
    
    
    private static final boolean SHOW_DEBUG = true;
	
    // Defines of Display Settings
    private static final int DISP_CHAR = 0;

    // Linefeed Code Settings
  //  private static final int LINEFEED_CODE_CR = 0;
    private static final int LINEFEED_CODE_CRLF = 1;
    private static final int LINEFEED_CODE_LF = 2;  
    PL2303Driver mSerial;
    private Button btWrite;
    private EditText etWrite;
    
    private Button btRead;
    private EditText etRead;

    private Button btLoopBack;
    private ProgressBar pbLoopBack;    
    private TextView tvLoopBack;
    
    private int mDisplayType = DISP_CHAR;
    private int mReadLinefeedCode = LINEFEED_CODE_LF;
    private int mWriteLinefeedCode = LINEFEED_CODE_LF;
    
    //BaudRate.B4800, DataBits.D8, StopBits.S1, Parity.NONE, FlowControl.RTSCTS
    private PL2303Driver.BaudRate mBaudrate = PL2303Driver.BaudRate.B9600;
    private PL2303Driver.DataBits mDataBits = PL2303Driver.DataBits.D8;
    private PL2303Driver.Parity mParity = PL2303Driver.Parity.NONE;
    private PL2303Driver.StopBits mStopBits = PL2303Driver.StopBits.S1;
    private PL2303Driver.FlowControl mFlowControl = PL2303Driver.FlowControl.OFF;
    
    private static final String ACTION_USB_PERMISSION = "com.prolific.pl2303hxdsimpletest.USB_PERMISSION";   
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btsend = (Button) findViewById(R.id.btsend);
         
        btsend.setOnClickListener(btsendListener);
         
        lsv1 = (ListView) findViewById(R.id.lsv1);
        // 获取USB设备
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (manager == null) {
            return;
        } else {
            Log.i(TAG, "usb设备：" + String.valueOf(manager.toString()));
        }
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Log.i(TAG, "usb设备：" + String.valueOf(deviceList.size()));
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        ArrayList<String> USBDeviceList = new ArrayList<String>(); // 存放USB设备的数量
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
         
            USBDeviceList.add(String.valueOf(device.getVendorId()));
            USBDeviceList.add(String.valueOf(device.getProductId()));
            Log.d(TAG, "vid is: "+ device.getVendorId());
            Log.d(TAG, "pid is: fail to controlTransfer: "+ device.getProductId());
         
            // 在这里添加处理设备的代码
            if (device.getVendorId() == 1659 && device.getProductId() == 8963) {
                mUsbDevice = device;
                Log.i(TAG, "找到设备");
            }
        }
        // 创建一个ArrayAdapter
        lsv1.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, USBDeviceList));
        findIntfAndEpt();
     // get service
        mSerial = new PL2303Driver((UsbManager) getSystemService(Context.USB_SERVICE),
           	  	this, ACTION_USB_PERMISSION); 
        
        Log.d(TAG, "Leave onCreate");
                 
    }
    
    private void openUsbSerial() {
      	 Log.d(TAG, "Enter  openUsbSerial");
   	   if(null==mSerial)
   		   return;   	 
      	 
          if (mSerial.isConnected()) {
              if (SHOW_DEBUG) {
                  Log.d(TAG, "openUsbSerial : isConnected ");
              }
              int baudRate= Integer.parseInt("9600");
   		   switch (baudRate) {
                	case 9600:
                		mBaudrate = PL2303Driver.BaudRate.B9600;
                		break;
                	case 19200:
                		mBaudrate =PL2303Driver.BaudRate.B19200;
                		break;
                	case 115200:
                		mBaudrate =PL2303Driver.BaudRate.B115200;
                		break;
                	default:
                		mBaudrate =PL2303Driver.BaudRate.B9600;
                		break;
              }   		            
   		   Log.d(TAG, "baudRate:"+baudRate);
              if (!mSerial.InitByBaudRate(mBaudrate)) {
                  	  Toast.makeText(this, "cannot open, maybe no permission", Toast.LENGTH_SHORT).show();
              } else {        	      
                     Toast.makeText(this, "connected", Toast.LENGTH_SHORT).show();        	   
              }
          }//isConnected
          
    }
         
    private byte[] Sendbytes;    //发送信息字节
    private byte[] Receiveytes;  //接收信息字节
    
    private int mPL2303Type;
	protected int res;
    private OnClickListener btsendListener = new OnClickListener() {
        private int WrCTRLTransferTimeOut=100;
		private boolean isRS485Mode;

		@Override
        public void onClick(View v) {
        	      	
        	 mPL2303Type = 0;
        	 int ret=0;
        	 UsbDeviceConnection usbConn = null;
             byte[] buffer = new byte[8];
             // 打开设备，获取 UsbDeviceConnection 对象，连接设备，用于后面的通讯
             usbConn = manager.openDevice(mUsbDevice);
             
            try {
				initPL2303Chip(usbConn);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            res = usbConn.controlTransfer(192, 1, 33924, 0, buffer, 1, this.WrCTRLTransferTimeOut);
		    if (ret < 0) {
		      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		      return;
		    }
             
            String testString = "abcd";
            Sendbytes = clsPublic.HexString2Bytes(testString);   
            byte[] testbytes = new byte[8];
            for(byte i=0;i<8;i++)
            	testbytes[i]=(byte) (i);
                  
            mDeviceConnection.bulkTransfer(epOut, testbytes, testbytes.length, 5000); 
            Log.i(TAG,"已经发送!");
//            openUsbSerial();
                     
            // 2,接收发送成功信息
//            Receiveytes=new byte[2];     //这里的64是设备定义的，不是我随便乱写，大家要根据设备而定
//            ret = mDeviceConnection.bulkTransfer(epIn, Receiveytes, Receiveytes.length, 10000);
//            Log.i(TAG,"接收返回值:" + String.valueOf(ret));
//            if(ret != 64) {
//                DisplayToast("接收返回值"+String.valueOf(ret));
//                return;
//            }
//            else {
//                //查看返回值
//                DisplayToast(clsPublic.Bytes2HexString(Receiveytes));
//                Log.i(TAG,clsPublic.Bytes2HexString(Receiveytes));
//            }         
                                             
        }

		private void initPL2303Chip(UsbDeviceConnection usbConn) throws IOException {
			// TODO Auto-generated method stub
			
			byte[] buffer = new byte[1];

		    res = usbConn.controlTransfer(192, 1, 33924, 0, buffer, 1, this.WrCTRLTransferTimeOut);
		    if (res < 0) {
		      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		      return;
		    }
		    res = usbConn.controlTransfer(64, 1, 1028, 0, null, 0, this.WrCTRLTransferTimeOut);
		    if (res < 0) {
		      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		      return;
		    }
		    res = usbConn.controlTransfer(192, 1, 33924, 0, buffer, 1, this.WrCTRLTransferTimeOut);
		    if (res < 0) {
		      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		      return;
		    }
		    res = usbConn.controlTransfer(192, 1, 33667, 0, buffer, 1, this.WrCTRLTransferTimeOut);
		    if (res < 0) {
		      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		      return;
		    }

		    res = usbConn.controlTransfer(192, 1, 33924, 0, buffer, 1, this.WrCTRLTransferTimeOut);
		    if (res < 0) {
		      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		      return;
		    }
		    res = usbConn.controlTransfer(64, 1, 1028, 1, null, 0, this.WrCTRLTransferTimeOut);
		    if (res < 0) {
		      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		      return;
		    }
		    res = usbConn.controlTransfer(192, 1, 33924, 0, buffer, 1, this.WrCTRLTransferTimeOut);
		    if (res < 0) {
		      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		      return;
		    }
		    res = usbConn.controlTransfer(192, 1, 33667, 0, buffer, 1, this.WrCTRLTransferTimeOut);
		    if (res < 0) {
		      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		      return;
		    }

		    if (this.isRS485Mode) {
		      res = usbConn.controlTransfer(64, 1, 0, 49, null, 0, this.WrCTRLTransferTimeOut);
		      if (res < 0) {
		        Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		        return;
		      }
		      res = usbConn.controlTransfer(64, 1, 1, 8, null, 0, this.WrCTRLTransferTimeOut);
		      if (res < 0) {
		        Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		        return;
		      }
		      Log.d("PL2303HXDDriver", "RS485 Mode detected");
		    }
		    else {
		      res = usbConn.controlTransfer(64, 1, 0, 1, null, 0, this.WrCTRLTransferTimeOut);
		      if (res < 0) {
		        Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		        return;
		      }
		      res = usbConn.controlTransfer(64, 1, 1, 0, null, 0, this.WrCTRLTransferTimeOut);
		      if (res < 0) {
		        Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		        return;
		      }
		      Log.d("PL2303HXDDriver", "RS232 Mode detected");
		    }

		    res = usbConn.controlTransfer(64, 1, 2, 68, null, 0, this.WrCTRLTransferTimeOut);
		    if (res < 0) {
		      Log.d("PL2303HXDDriver", "fail to initPL2303Chip");
		      return;
		    }
		    res = setup(BaudRate.B9600, DataBits.D8, StopBits.S1, Parity.NONE, FlowControl.OFF);
		    if (res < 0) {
		      Log.d("PL2303HXDDriver", "fail to initPL2303Chip: setup");
		      return;
		    }

			
		}
    };
    

         
    // 显示提示的函数，这样可以省事
    public void DisplayToast(CharSequence str) {
        Toast toast = Toast.makeText(this, str, Toast.LENGTH_LONG);
        // 设置Toast显示的位置
        toast.setGravity(Gravity.TOP, 0, 200);
        // 显示Toast
        toast.show();
    }

    
    
    protected int setup(BaudRate R, DataBits D, StopBits S, Parity P, FlowControl F) {
		// TODO Auto-generated method stub
    	
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

    	    switch (S) { case S1:
    	      this.mPortSetting[4] = 0; break;
    	    case S2:
    	      this.mPortSetting[4] = 2; break;
    	    default:
    	      Log.d("PL2303HXDDriver", "Stopbit setting not supported");
    	      return -3;
    	    }

    	    switch (P) { case EVEN:
    	      this.mPortSetting[5] = 0; break;
    	    case NONE:
    	      this.mPortSetting[5] = 1; break;
    	    case ODD:
    	      this.mPortSetting[5] = 2; break;
    	    default:
    	      Log.d("PL2303HXDDriver", "Parity setting not supported");
    	      return -4;
    	    }

    	    switch (D) { case D5:
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

	// 寻找接口和分配结点
    private void findIntfAndEpt() {
        if (mUsbDevice == null) {
            Log.i(TAG,"没有找到设备");
            return;
        }
        for (int i = 0; i < mUsbDevice.getInterfaceCount();) {
            // 获取设备接口，一般都是一个接口，你可以打印getInterfaceCount()方法查看接
            // 口的个数，在这个接口上有两个端点，OUT 和 IN 
            UsbInterface intf = mUsbDevice.getInterface(i);
            Log.d(TAG, i + " " + intf);
            mInterface = intf;
            break;
        }
         
        if (mInterface != null) {
            UsbDeviceConnection connection = null;
            // 判断是否有权限
            if(manager.hasPermission(mUsbDevice)) {
                // 打开设备，获取 UsbDeviceConnection 对象，连接设备，用于后面的通讯
                connection = manager.openDevice(mUsbDevice); 
                if (connection == null) {
                    return;
                }
                if (connection.claimInterface(mInterface, true)) {
                    Log.i(TAG,"找到接口");
                    mDeviceConnection = connection;
                    //用UsbDeviceConnection 与 UsbInterface 进行端点设置和通讯
                    getEndpoint(mDeviceConnection,mInterface);
                } else {
                    connection.close();
                }
            } else {
                Log.i(TAG,"没有权限");
            }
        }
        else {
            Log.i(TAG,"没有找到接口");
        }
    }
             
             
    private UsbEndpoint epOut;
    private UsbEndpoint epIn;
	private boolean isRS485Mode;
    //用UsbDeviceConnection 与 UsbInterface 进行端点设置和通讯
    private void getEndpoint(UsbDeviceConnection connection, UsbInterface intf) {
        if (intf.getEndpoint(1) != null) {
            epOut = intf.getEndpoint(1);
        }
        if (intf.getEndpoint(0) != null) {
            epIn = intf.getEndpoint(0);
        }
    }
    
    private int checkPL2303ChipType(UsbDeviceConnection conn)
    {
        int res = 0;
        RdTransferTimeOut = 5000;
        WrCTRLTransferTimeOut = 100;
        byte buffer[] = new byte[1];
        res = conn.controlTransfer(64, 1, 1, 255, null, 0, WrCTRLTransferTimeOut);
        if(res < 0)
        {
            Log.d("PL2303HXDDriver", "fail to checkPL2303ChipType");
            return res;
        }
        res = conn.controlTransfer(192, 1, 33153, 0, buffer, 1, WrCTRLTransferTimeOut);
        if(res < 0)
        {
            Log.d("PL2303HXDDriver", "fail to checkPL2303ChipType");
            return res;
        } else
        {
            return 0;
        }
    }
    
    private int checkRS485Mode(UsbDeviceConnection conn)
    {
        int readAddress = 9;
        int res = 0;
        byte buffer[] = new byte[1];
        res = conn.controlTransfer(192, 1, 33924, 0, buffer, 1, WrCTRLTransferTimeOut);
        if(res < 0)
        {
            Log.d("PL2303HXDDriver", "fail to CheckRS485Mode:read");
            return res;
        }
        res = conn.controlTransfer(64, 1, 1028, readAddress, null, 0, WrCTRLTransferTimeOut);
        if(res < 0)
        {
            Log.d("PL2303HXDDriver", "fail to CheckRS485Mode:write");
            return res;
        }
        res = conn.controlTransfer(192, 1, 33924, 0, buffer, 1, WrCTRLTransferTimeOut);
        if(res < 0)
        {
            Log.d("PL2303HXDDriver", "fail to CheckRS485Mode:read");
            return res;
        }
        res = conn.controlTransfer(192, 1, 33667, 0, buffer, 1, WrCTRLTransferTimeOut);
        if(res < 0)
        {
            Log.d("PL2303HXDDriver", "fail to CheckRS485Mode:read");
            return res;
        }
        if(buffer[0] == 8)
            isRS485Mode = true;
        return 0;
    }

             
             
}