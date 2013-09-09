package com.example.usbmanager;
         
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.usbmanager.PL2303Driver.BaudRate;
import com.example.usbmanager.PL2303Driver.DataBits;
import com.example.usbmanager.PL2303Driver.FlowControl;
import com.example.usbmanager.PL2303Driver.Parity;
import com.example.usbmanager.PL2303Driver.StopBits;
         
public class MainActivity extends Activity {
	
	 private static final boolean SHOW_DEBUG = true;
		
	// Defines of Display Settings
	  private static final int DISP_CHAR = 0;

	  // Linefeed Code Settings
	//  private static final int LINEFEED_CODE_CR = 0;
	  private static final int LINEFEED_CODE_CRLF = 1;
	  private static final int LINEFEED_CODE_LF = 2;
	  

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
	  
	  private static final String ACTION_USB_PERMISSION = "USB_PERMISSION";   

	  // Linefeed
	//  private final static String BR = System.getProperty("line.separator");
	  
	  public Spinner PL2303HXD_BaudRate_spinner;
	  public int PL2303HXD_BaudRate;
	/*  public String PL2303HXD_BaudRate_str="B4800";*/
	  
		private String strStr;

	  private byte[] mPortSetting = new byte[7];

	  private static final String TAG = "MainActivity";   //记录标识
    private Button btsend;      //发送按钮
    private Button btreceive;      //发送按钮
    private UsbManager manager;   //USB管理器
    private UsbDevice mUsbDevice;  //找到的USB设备
    private ListView lsv1;         //显示USB信息的
    private UsbInterface mInterface;   
    private UsbDeviceConnection mDeviceConnection;
    
    public static final int READBUF_SIZE = 4096;
    public static final int WRITEBUF_SIZE = 4096;
    byte[] mReadbuf = new byte[4096];
    int ifread=0;
    private int WrCTRLTransferTimeOut = 100;
    
    
    PL2303Driver mSerial;
 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etRead = (EditText) findViewById(R.id.etxreceive);	
        btsend = (Button) findViewById(R.id.btsend);
         
        btsend.setOnClickListener(btsendListener);
        
        
        btRead = (Button) findViewById(R.id.btreceive);        
        btRead.setOnClickListener(new Button.OnClickListener() {		
			public void onClick(View v) {	
				ifread=1;			
					readDataFromSerial();
			}
		});
        
        Button mButton01 = (Button)findViewById(R.id.button1);
		mButton01.setOnClickListener(new Button.OnClickListener() {		
			public void onClick(View v) {
				openUsbSerial();
			}

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
		});
         
//        lsv1 = (ListView) findViewById(R.id.lsv1);
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
//        lsv1.setAdapter(new ArrayAdapter<String>(this,
//                android.R.layout.simple_list_item_1, USBDeviceList));
        findIntfAndEpt();
     // get service
        mSerial = new PL2303Driver(manager,this, ACTION_USB_PERMISSION);          
        Log.d(TAG, "Leave onCreate");
                 
    }
    
    private byte[] Sendbytes;    //发送信息字节
    protected int res;
    private OnClickListener btsendListener = new OnClickListener() {
        private int WrCTRLTransferTimeOut=100;
		private boolean isRS485Mode;

		@Override
        public void onClick(View v) {
			
			writeDataToSerial();
        	      	         
			ifread=0;
            Log.i(TAG,"已经发送!");
     
                                             
        }
    };
    
    @Override
    protected void onDestroy() {
    	Log.d(TAG, "Enter onDestroy");      
    	if(mSerial!=null) {
    		mSerial.end();
    		mSerial = null;
    	}    	
    	super.onDestroy();        
        Log.d(TAG, "Leave onDestroy");
    }    

    public void onStart() {
    	Log.d(TAG, "Enter onStart");
    	super.onStart();
    	Log.d(TAG, "Leave onStart");
    }
    
    public void onResume() {
    	Log.d(TAG, "Enter onResume"); 
        super.onResume();
        String action =  getIntent().getAction();
    	Log.d(TAG, "onResume:"+action);
    	
        //if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))        
       	if(!mSerial.isConnected()) {
             if (SHOW_DEBUG) {
              	  Log.d(TAG, "New instance : " + mSerial);
             }
             
    		 if( !mSerial.enumerate() ) {
              	Toast.makeText(this, "no more devices found", Toast.LENGTH_SHORT).show();     
              	return;
              } else {
                 Log.d(TAG, "onResume:enumerate succeeded!");
              }    		 
        }//if isConnected  
		Toast.makeText(this, "attached", Toast.LENGTH_SHORT).show();	
        Log.d(TAG, "Leave onResume"); 
    } 
    
    private void openUsbSerial() {
      	 Log.d(TAG, "Enter  openUsbSerial");
   	   if(null==mSerial)
   		   return;   	 
      	 
          if (mSerial.isConnected()) {
              if (SHOW_DEBUG) {
                  Log.d(TAG, "openUsbSerial : isConnected ");
              }
              int baudRate= 115200;
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
          
          Log.d(TAG, "Leave openUsbSerial");
      }//openUsbSerial
         
    
    private String Receiveytes;  //接收信息字节
    private void readDataFromSerial() {

        int len;
        byte[] rbuf = new byte[4096];
        StringBuffer sbHex=new StringBuffer();
        
        Log.d(TAG, "Enter readDataFromSerial");

		if(null==mSerial)
			return;        
        
        if(!mSerial.isConnected()) 
        	return;
        
        len = mSerial.read(rbuf);
        if(len<0) {
        	Log.d(TAG, "Fail to bulkTransfer(read data)");
        	return;
        }

        if (len > 0) {        	
               if (SHOW_DEBUG) {
            	   Log.d(TAG, "read len : " + len);
               }                
               //rbuf[len] = 0;
               for (int j = 0; j < len; j++) {            	   
            	   sbHex.append((char) (rbuf[j]&0x000000FF));
               } 
               Receiveytes = clsPublic.Bytes2HexString(rbuf);  
//               etRead.setText(sbHex.toString());    
               etRead.setText(Receiveytes); 
               Toast.makeText(this, "len="+len, Toast.LENGTH_SHORT).show();

        }
        else {     	
        	 if (SHOW_DEBUG) {
               Log.d(TAG, "read len : 0 ");
             }
        	 etRead.setText("empty");
        	 return;
        }

        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Leave readDataFromSerial");	
    }//readDataFromSerial
    
    private void writeDataToSerial() {
    	 
    	Log.d(TAG, "Enter writeDataToSerial");
    	
		if(null==mSerial)
			return;
    	
    	if(!mSerial.isConnected()) 
    		return;
    	
        String strWrite = "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd";
//        String strWrite = etWrite.getText().toString();
        if (SHOW_DEBUG) {
            Log.d(TAG, "PL2303Driver Write(" + strWrite.length() + ") : " + strWrite);
        }
        for(int i=0;i<100;i++)
        {
        int res = mSerial.write(strWrite.getBytes(), strWrite.length());
		
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		if( res<0 ) {
			Log.d(TAG, "setup: fail to controlTransfer: "+ res);
//			return;
		} 
        }

		Log.d(TAG, "Leave writeDataToSerial");
    }//writeDataToSerial
    
    
    // 显示提示的函数，这样可以省事
    public void DisplayToast(CharSequence str) {
        Toast toast = Toast.makeText(this, str, Toast.LENGTH_LONG);
        // 设置Toast显示的位置
        toast.setGravity(Gravity.TOP, 0, 200);
        // 显示Toast
        toast.show();
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
	//用UsbDeviceConnection 与 UsbInterface 进行端点设置和通讯
    private void getEndpoint(UsbDeviceConnection connection, UsbInterface intf) {
        if (intf.getEndpoint(1) != null) {
            epOut = intf.getEndpoint(1);
        }
        if (intf.getEndpoint(0) != null) {
            epIn = intf.getEndpoint(0);
        }
    }

             
             
}