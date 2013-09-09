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

	  private static final String TAG = "MainActivity";   //��¼��ʶ
    private Button btsend;      //���Ͱ�ť
    private Button btreceive;      //���Ͱ�ť
    private UsbManager manager;   //USB������
    private UsbDevice mUsbDevice;  //�ҵ���USB�豸
    private ListView lsv1;         //��ʾUSB��Ϣ��
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
        // ��ȡUSB�豸
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (manager == null) {
            return;
        } else {
            Log.i(TAG, "usb�豸��" + String.valueOf(manager.toString()));
        }
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Log.i(TAG, "usb�豸��" + String.valueOf(deviceList.size()));
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        ArrayList<String> USBDeviceList = new ArrayList<String>(); // ���USB�豸������
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
         
            USBDeviceList.add(String.valueOf(device.getVendorId()));
            USBDeviceList.add(String.valueOf(device.getProductId()));
            Log.d(TAG, "vid is: "+ device.getVendorId());
            Log.d(TAG, "pid is: fail to controlTransfer: "+ device.getProductId());
         
            // ��������Ӵ����豸�Ĵ���
            if (device.getVendorId() == 1659 && device.getProductId() == 8963) {
                mUsbDevice = device;
                Log.i(TAG, "�ҵ��豸");
            }
        }
        // ����һ��ArrayAdapter
//        lsv1.setAdapter(new ArrayAdapter<String>(this,
//                android.R.layout.simple_list_item_1, USBDeviceList));
        findIntfAndEpt();
     // get service
        mSerial = new PL2303Driver(manager,this, ACTION_USB_PERMISSION);          
        Log.d(TAG, "Leave onCreate");
                 
    }
    
    private byte[] Sendbytes;    //������Ϣ�ֽ�
    protected int res;
    private OnClickListener btsendListener = new OnClickListener() {
        private int WrCTRLTransferTimeOut=100;
		private boolean isRS485Mode;

		@Override
        public void onClick(View v) {
			
			writeDataToSerial();
        	      	         
			ifread=0;
            Log.i(TAG,"�Ѿ�����!");
     
                                             
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
         
    
    private String Receiveytes;  //������Ϣ�ֽ�
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
    
    
    // ��ʾ��ʾ�ĺ�������������ʡ��
    public void DisplayToast(CharSequence str) {
        Toast toast = Toast.makeText(this, str, Toast.LENGTH_LONG);
        // ����Toast��ʾ��λ��
        toast.setGravity(Gravity.TOP, 0, 200);
        // ��ʾToast
        toast.show();
    }

    

	// Ѱ�ҽӿںͷ�����
    private void findIntfAndEpt() {
        if (mUsbDevice == null) {
            Log.i(TAG,"û���ҵ��豸");
            return;
        }
        for (int i = 0; i < mUsbDevice.getInterfaceCount();) {
            // ��ȡ�豸�ӿڣ�һ�㶼��һ���ӿڣ�����Դ�ӡgetInterfaceCount()�����鿴��
            // �ڵĸ�����������ӿ����������˵㣬OUT �� IN 
            UsbInterface intf = mUsbDevice.getInterface(i);
            Log.d(TAG, i + " " + intf);
            mInterface = intf;
            break;
        }
         
        if (mInterface != null) {
            UsbDeviceConnection connection = null;
            // �ж��Ƿ���Ȩ��
            if(manager.hasPermission(mUsbDevice)) {
                // ���豸����ȡ UsbDeviceConnection ���������豸�����ں����ͨѶ
                connection = manager.openDevice(mUsbDevice); 
                if (connection == null) {
                    return;
                }
                if (connection.claimInterface(mInterface, true)) {
                    Log.i(TAG,"�ҵ��ӿ�");
                    mDeviceConnection = connection;
                    //��UsbDeviceConnection �� UsbInterface ���ж˵����ú�ͨѶ
                    getEndpoint(mDeviceConnection,mInterface);
                } else {
                    connection.close();
                }
            } else {
                Log.i(TAG,"û��Ȩ��");
            }
        }
        else {
            Log.i(TAG,"û���ҵ��ӿ�");
        }
    }
             
             
    private UsbEndpoint epOut;
    private UsbEndpoint epIn;
	//��UsbDeviceConnection �� UsbInterface ���ж˵����ú�ͨѶ
    private void getEndpoint(UsbDeviceConnection connection, UsbInterface intf) {
        if (intf.getEndpoint(1) != null) {
            epOut = intf.getEndpoint(1);
        }
        if (intf.getEndpoint(0) != null) {
            epIn = intf.getEndpoint(0);
        }
    }

             
             
}